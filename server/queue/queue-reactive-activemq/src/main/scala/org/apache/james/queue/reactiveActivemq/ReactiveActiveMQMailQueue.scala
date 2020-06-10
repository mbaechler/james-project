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
import java.util.{Date, UUID}

import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.fasterxml.jackson.core.JsonProcessingException
import com.github.fge.lambdas.Throwing
import com.github.fge.lambdas.consumers.ThrowingBiConsumer
import com.github.steveash.guavate.Guavate
import com.google.common.collect.{ImmutableList, ImmutableMap}
import eu.timepit.refined
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import javax.mail.MessagingException
import javax.mail.internet.MimeMessage
import org.apache.activemq.artemis.api.core.{Message, RoutingType}
import org.apache.activemq.artemis.api.core.client.{ClientConsumer, ClientMessage, ClientProducer, ClientSession, SendAcknowledgementHandler}
import org.apache.james.blob.api.{BlobId, Store}
import org.apache.james.blob.mail.MimeMessagePartsId
import org.apache.james.core.MaybeSender
import org.apache.james.queue.api.MailQueue.{MailQueueException, MailQueueItem}
import org.apache.james.queue.api.{MailQueue, MailQueueName}
import org.apache.james.queue.reactiveActivemq.EnqueueId.EnqueueId
import org.apache.james.server.core.MailImpl
import org.apache.james.util.SerializationUtil
import org.apache.mailet.{Attribute, AttributeName, AttributeValue, Mail, PerRecipientHeaders}
import org.apache.james.core.MailAddress
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

class ReactiveActiveMQMailQueue(val session: ClientSession,
                                val queueName: MailQueueName,
                                blobIdFactory: BlobId.Factory,
                                mimeMessageStore: Store[MimeMessage, MimeMessagePartsId],
                                actorSystem:ActorSystem) extends MailQueue {
  implicit val _actorSystem = actorSystem

  @throws[MailQueue.MailQueueException]
  private def saveMimeMessage(mimeMessage: MimeMessage): Publisher[MimeMessagePartsId] =
    try {
      mimeMessageStore.save(mimeMessage)
    } catch {
      case e: MessagingException =>
        throw new MailQueue.MailQueueException("Error while saving blob", e)
    }

  private def readMimeMessage(partsId:MimeMessagePartsId):Publisher[MimeMessage]=
    try{
      mimeMessageStore.read(partsId)
    }catch {
      case e: MessagingException =>
        throw new MailQueue.MailQueueException("Error while reading blob", e)

    }

  private def readMail(mailMetadata: MailMetadata, mimeMessage: MimeMessage): Mail = {
    val builder = MailImpl
      .builder
      .name(mailMetadata.name)
      .sender(mailMetadata.sender.map(MaybeSender.getMailSender).getOrElse(MaybeSender.nullSender))
      .addRecipients(mailMetadata.recipients.map(new MailAddress(_)).asJavaCollection)
      .errorMessage(mailMetadata.errorMessage)
      .remoteAddr(mailMetadata.remoteAddr)
      .remoteHost(mailMetadata.remoteHost)
      .state(mailMetadata.state)
      .mimeMessage(mimeMessage)

    mailMetadata.lastUpdated.map(Date.from).foreach(builder.lastUpdated)

    mailMetadata.attributes.foreach { case (name, value) => builder.addAttribute(new Attribute(AttributeName.of(name), AttributeValue.fromJsonString(value))) }

    builder.addAllHeadersForRecipients(retrievePerRecipientHeaders(mailMetadata.perRecipientHeaders))

    builder.build
  }

  def retrievePerRecipientHeaders(perRecipientHeaders: Map[String, Iterable[Header]]): PerRecipientHeaders = {
    val result = new PerRecipientHeaders();
    perRecipientHeaders.foreach{ case (key, value)=>
      value.foreach(headers => {
        headers.values.foreach(header => {
          val builder = PerRecipientHeaders.Header.builder().name(headers.key).value(header)
          result.addHeaderForRecipient(builder, new MailAddress(key))
        })
      })
    }
    result
  }

  implicit val mailMetadataFormat: Format[MailMetadata] = Json.format[MailMetadata]
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


  private val enqueueFlow =  Source.queue[Mail](10,OverflowStrategy.backpressure)
    .flatMapConcat(mail => Source.fromPublisher(saveMimeMessage(mail.getMessage))
    .map(partsId => {
      val mailReference = MailMetadata.of(EnqueueId.generate(), mail, partsId)
      session.createMessage(true).writeBodyBufferString(Json.stringify(Json.toJson(mailReference)))
    }))
    .to(Sink.foreachAsync(1)(send))
  private val enqueueStream = enqueueFlow.run()

  @throws[MailQueueException]
  override def enQueue(mail: Mail): Unit = {
    enqueueStream.queue.offer(mail)
  }


  def send(message: ClientMessage): Future[Unit] = {
    val promise = Promise[Unit]()

    val producer = session.createProducer(queueName.asString())
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

  class MyMailQueueItem(mail: Mail) extends MailQueueItem {
    override def getMail: Mail = mail
    override def done(success: Boolean): Unit = ()
  }
  val consumer: ClientConsumer = session.createConsumer(queueName.asString())
  val (dequeueQueue, dequeuePublisher) = Source.queue[ClientMessage](10,OverflowStrategy.backpressure)
    .map(clientMessage => clientMessage.acknowledge().getBodyBuffer.readString())
    .map(Json.parse)
    .map(Json.fromJson[MailMetadata](_))
    .map(metadataR=>metadataR.get)
    .flatMapConcat(metadata=> {
      val partsId = MimeMessagePartsId.builder()
        .headerBlobId(blobIdFactory.from(metadata.headerBlobId))
        .bodyBlobId(blobIdFactory.from(metadata.bodyBlobId))
        .build()
      Source.fromPublisher(readMimeMessage(partsId)).map(message=>
        readMail(metadata ,message)
      )
    })
    .map(new MyMailQueueItem(_))
    .toMat(Sink.asPublisher[MailQueue.MailQueueItem](false))(Keep.both).run()
  consumer.setMessageHandler(clientMessage=>dequeueQueue.offer(clientMessage))

  override def deQueue: Publisher[MailQueue.MailQueueItem] = {
    dequeuePublisher
  }


  @throws[IOException]
  override def close() = {
    session.close()
  }
}