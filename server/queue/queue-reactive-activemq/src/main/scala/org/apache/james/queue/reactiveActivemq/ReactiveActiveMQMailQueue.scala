/** **************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                 *
 * *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 * ***************************************************************/
package org.apache.james.queue.reactiveActivemq

import java.io.{IOException, Serializable}
import java.time.{Duration, Instant}
import java.util
import java.util.UUID

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Sink, Source}
import com.fasterxml.jackson.core.JsonProcessingException
import com.github.steveash.guavate.Guavate
import com.google.common.collect.{ImmutableList, ImmutableMap}
import eu.timepit.refined
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import javax.mail.MessagingException
import javax.mail.internet.MimeMessage
import org.apache.activemq.artemis.api.core.{Message, RoutingType}
import org.apache.activemq.artemis.api.core.client.{ClientConsumer, ClientMessage, ClientProducer, ClientSession, SendAcknowledgementHandler}
import org.apache.james.blob.api.Store
import org.apache.james.blob.mail.MimeMessagePartsId
import org.apache.james.queue.api.MailQueue.MailQueueException
import org.apache.james.queue.api.{MailQueue, MailQueueName}
import org.apache.james.queue.reactiveActivemq.EnqueueId.EnqueueId
import org.apache.james.util.SerializationUtil
import org.apache.mailet.{AttributeName, Mail, MailAddress, PerRecipientHeaders}
import org.reactivestreams.Publisher
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json}
import reactor.core.publisher.Mono

import scala.concurrent.{Future, Promise}
import scala.util.Try
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import scala.jdk.StreamConverters._

object EnqueueId {
  type EnqueueIdConstraint = Uuid
  type EnqueueId = String Refined EnqueueIdConstraint

  def apply(s: String): Either[String, EnqueueId] =
    refined.refineV[EnqueueIdConstraint](s)

  def apply(s: UUID): EnqueueId =
    refined.refineV[EnqueueIdConstraint](s.toString).toOption.get

  def generate() = EnqueueId(UUID.randomUUID())
}

object Header {

  def of: ((String, Iterable[String])) => Header = (this.apply _).tupled
}

case class Header(key: String, values: Iterable[String])

object MailMetadata {
  def of(enqueueId: EnqueueId, mail: Mail, partsId: MimeMessagePartsId): MailMetadata = {
    MailMetadata(
      enqueueId.value,
      Option(mail.getRecipients).map(_.asScala.map(_.asString).toSeq).getOrElse(Seq.empty),
      mail.getName,
      mail.getMaybeSender.asOptional().map(_.asString()).toScala,
      mail.getState,
      mail.getErrorMessage,
      Option(mail.getLastUpdated).map(_.toInstant),
      serializedAttributes(mail),
      mail.getRemoteAddr,
      mail.getRemoteHost,
      fromPerRecipientHeaders(mail),
      partsId.getHeaderBlobId.asString(),
      partsId.getBodyBlobId.asString()
    )
  }

  private def serializedAttributes(mail: Mail): Map[String, String] =
    mail.attributes().toScala(LazyList)
      .map(attribute => attribute.getName.asString() -> attribute.getValue.toJson.toString)
      .toMap

  private def fromPerRecipientHeaders(mail: Mail): Map[String, Iterable[Header]] = {
    mail.getPerRecipientSpecificHeaders
      .getHeadersByRecipient
      .asMap
      .asScala
      .view
      .map { case (mailAddress, headers) =>
        mailAddress.asString() -> headers
          .asScala
          .groupMap(_.getName)(_.getValue)
          .map(Header.of)
      }.toMap

  }

}

case class MailMetadata(enqueueId: String,
                        recipients: Seq[String],
                        name: String,
                        sender: Option[String],
                        state: String,
                        errorMessage: String,
                        lastUpdated: Option[Instant],
                        attributes: Map[String, String],
                        remoteAddr: String,
                        remoteHost: String,
                        perRecipientHeaders: Map[String, Iterable[Header]],
                        headerBlobId: String,
                        bodyBlobId: String)

object MailReference {
  def of(enqueueId: EnqueueId, mail: Mail, partsIds: MimeMessagePartsId): MailReference =
    MailReference(
      enqueueId,
      MailMetadata.of(enqueueId, mail, partsIds),
      partsIds
    )
}

case class MailReference(enqueueId: EnqueueId.EnqueueId, metadata: MailMetadata, partsIds: MimeMessagePartsId)

class ReactiveActiveMQMailQueue(val session: ClientSession,
                                val queueName: MailQueueName,
                                mimeMessageStore: Store[MimeMessage, MimeMessagePartsId]) extends MailQueue {


  @throws[MailQueue.MailQueueException]
  private def saveMail(mail: Mail): Publisher[MimeMessagePartsId] =
    try {
      mimeMessageStore.save(mail.getMessage)
    } catch {
      case e: MessagingException =>
        throw new MailQueue.MailQueueException("Error while saving blob", e)
    }

  implicit val mailMetadataFormat: Format[MailMetadata] = Json.format[MailMetadata]
  implicit val mailReferenceFormat: Format[MailReference] = Json.format[MailReference]
  implicit val headerFormat: Format[Header] = Json.format[Header]
  implicit val enqueueIdFormat: Format[EnqueueId] = new Format[EnqueueId] {
    override def writes(o: EnqueueId): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[EnqueueId] =
      json.validate[String].map(EnqueueId.apply).flatMap(_.fold(JsError.apply, JsSuccess(_)))
  }

  session.createQueue(queueName.asString(), RoutingType.ANYCAST, queueName.asString(), true)
  session.start()

  override def getName: MailQueueName = queueName

  override def enQueue(mail: Mail, delay: Duration): Unit = {
  }


  val enqueueFlow=  Source.queue[Mail](10,OverflowStrategy.backpressure)
    .flatMapConcat(mail=> Source.fromPublisher(saveMail(mail)))
    .map(partsId => {
      val mailReference = MailReference.of(EnqueueId.generate(), mail, partsId)
      session.createMessage(true).writeBodyBufferString(Json.stringify(Json.toJson(mailReference)))
    })
    .to(Sink.foreachAsync(1)(send).async("singleThreadedDispatcher"))
  val x = enqueueFlow.run()(???)

  @throws[MailQueueException]
  override def enQueue(mail: Mail): Unit = {
    x.queue.offer(mail)
  }


  def send(message: ClientMessage): Future[Unit] = {
    val promise = Promise[Unit]()

    val producer = session.createProducer("example")
    try {
      producer.send(message, new SendAcknowledgementHandler {
        override def sendAcknowledged(message: Message): Unit =
          promise.success()

        override def sendFailed(message: Message, e: Exception): Unit =
          promise.failure(new MailQueueException(e.getMessage, e))
      })
    } catch {
      case e: Exception => promise.failure(new MailQueueException(e.getMessage, e))
    } finally {
      producer.close()
    }
    promise.future
  }
  protected def getJMSProperties(mail: Mail, nextDelivery: Long): util.Map[String, AnyRef] = {
    val props: util.Map[String, AnyRef] = new util.HashMap[String, AnyRef]





    // won't serialize the empty headers so it is mandatory
    // to handle nulls when reconstructing mail from message


    props
  }

  override def deQueue: Publisher[MailQueue.MailQueueItem] = {
    val consumer: ClientConsumer = session.createConsumer("example")
    val msgReceived: ClientMessage = consumer.receive()
    ???
  }


  @throws[IOException]
  override def close() = {
    session.close()
  }
}