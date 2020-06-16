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

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.{Attributes, Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.typed.javadsl.ActorSink
import akka.stream.typed.scaladsl.ActorSource
import akka.util.Timeout
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
import org.apache.activemq.artemis.api.core.RoutingType
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
import org.apache.james.queue.reactiveActivemq.ActiveMqSession.Enqueue
import org.reactivestreams.Publisher
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json}
import reactor.core.publisher.Mono

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
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
      Option(mail.getState),
      Option(mail.getErrorMessage),
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
                        state: Option[String],
                        errorMessage: Option[String],
                        lastUpdated: Option[Instant],
                        attributes: Map[String, String],
                        remoteAddr: String,
                        remoteHost: String,
                        perRecipientHeaders: Map[String, Iterable[Header]],
                        headerBlobId: String,
                        bodyBlobId: String)

object ReactiveActiveMQMailQueue {
  private[reactiveActivemq] final case class Message(body: String, clientMessage: ClientMessage,activeMqSession: ActorRef[ActiveMqSession.Command])
}

class ReactiveActiveMQMailQueue(val session: ClientSession,
                                val queueName: MailQueueName,
                                blobIdFactory: BlobId.Factory,
                                mimeMessageStore: Store[MimeMessage, MimeMessagePartsId],
                                actorSystem:ActorSystem[_]) extends MailQueue {
  import ReactiveActiveMQMailQueue._
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
      .remoteAddr(mailMetadata.remoteAddr)
      .remoteHost(mailMetadata.remoteHost)
      .mimeMessage(mimeMessage)

    mailMetadata.state.foreach(builder.state)
    mailMetadata.errorMessage.foreach(builder.errorMessage)

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

  implicit val headerFormat: Format[Header] = Json.format[Header]
  implicit val enqueueIdFormat: Format[EnqueueId] = new Format[EnqueueId] {
    override def writes(o: EnqueueId): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[EnqueueId] =
      json.validate[String].map(EnqueueId.apply).flatMap(_.fold(JsError.apply, JsSuccess(_)))
  }
  implicit val mailMetadataFormat: Format[MailMetadata] = Json.format[MailMetadata]

  session.createQueue(queueName.asString(), RoutingType.ANYCAST, queueName.asString(), true)
  session.start()

  override def getName: MailQueueName = queueName

  override def enQueue(mail: Mail, delay: Duration): Unit = {
    ???
  }




  @throws[MailQueueException]
  override def enQueue(mail: Mail): Unit = {
    enqueueStream.queue.offer(mail)
  }

  protected def getJMSProperties(mail: Mail, nextDelivery: Long): util.Map[String, AnyRef] = {
    val props: util.Map[String, AnyRef] = new util.HashMap[String, AnyRef]

    // won't serialize the empty headers so it is mandatory
    // to handle nulls when reconstructing mail from message


    props
  }
  
  class MyMailQueueItem(mail: Mail, clientMessage: ClientMessage, activeMqSession: ActorRef[ActiveMqSession.Command]) extends MailQueueItem {
    override def getMail: Mail = mail
    override def done(success: Boolean): Unit = {
      import akka.actor.typed.scaladsl.AskPattern.Askable
      implicit val scheduler = actorSystem.scheduler
      implicit val timeout: Timeout = 1.minute

      val command = if (success) ActiveMqSession.Ack else ActiveMqSession.Nack
      Await.ready(
        activeMqSession.ask[ActiveMqSession.EnqueueResult](replyTo => command(clientMessage, replyTo)),
        1.minute
      )
    }
  }
  val consumer: ClientConsumer = session.createConsumer(queueName.asString())

  val (dequeueActorRef, dequeuePublisher)=

  ActorSource.actorRef[Message](
    PartialFunction.empty,
    PartialFunction.empty,
    10,
    OverflowStrategy.fail
  ).map(message =>
    (
      message.clientMessage,
      Json.fromJson[MailMetadata](Json.parse(message.body)).get,
      message.activeMqSession)
    )
    .flatMapConcat{case ( msg,metadata, activeMqSession)=> {
      val partsId = MimeMessagePartsId.builder()
        .headerBlobId(blobIdFactory.from(metadata.headerBlobId))
        .bodyBlobId(blobIdFactory.from(metadata.bodyBlobId))
        .build()
      Source.fromPublisher(readMimeMessage(partsId)).map(message=>
        (msg,readMail(metadata ,message),activeMqSession)
      )
    }}
    .map{case (msg,mail,activeMqSession)=>new MyMailQueueItem(mail, msg,activeMqSession)}
    .toMat(Sink.asPublisher[MailQueue.MailQueueItem](false))(Keep.both).run()

  //consumer.setMessageHandler(clientMessage=>dequeueQueue.offer(clientMessage))

  override def deQueue: Publisher[MailQueue.MailQueueItem] = {
    dequeuePublisher
  }

  private val enqueueFlow =  Source.queue[Mail](10,OverflowStrategy.backpressure)
    .flatMapConcat(mail => Source.fromPublisher(saveMimeMessage(mail.getMessage))
      .map(partsId => {
        val mailReference = MailMetadata.of(EnqueueId.generate(), mail, partsId)
        session.createMessage(true).writeBodyBufferString(Json.stringify(Json.toJson(mailReference)))
      }))
    .map(Enqueue())
    .log(name = "enqueue")
    .addAttributes(
      Attributes.logLevels(
        onElement = Attributes.LogLevels.Off,
        onFinish = Attributes.LogLevels.Off,
        onFailure = Attributes.LogLevels.Error))
    .to(ActorSink.actorRef())

  private val enqueueStream = enqueueFlow.run()

  @throws[IOException]
  override def close() = {
    session.close()
  }
}

object ActiveMqSession {

  sealed trait Command
  final case class Ack(clientMessage: ClientMessage, replyTo: ActorRef[EnqueueResult]) extends Command
  final case class Nack(clientMessage: ClientMessage, replyTo: ActorRef[EnqueueResult]) extends Command
  //
  final case class Consume(clientMessage: ClientMessage) extends Command
  final case class Enqueue(payload: String, replyTo: ActorRef[EnqueueResult]) extends Command

  sealed trait EnqueueResult
  final case object EnqueueSuccess extends EnqueueResult
  final case class EnqueueFailure(throwable: Throwable) extends EnqueueResult

  def apply(session: ClientSession, producer: ClientProducer, consumer: ClientConsumer, dequeue: ActorRef[ReactiveActiveMQMailQueue.Message]): Behavior[Command] = Behaviors.setup(context => {

    val self = context.self
    consumer.setMessageHandler(clientMessage => self ! Consume(clientMessage))

    Behaviors.receiveMessage {
      case Ack(clientMessage, replyTo) =>
        Try(clientMessage.acknowledge()).fold(throwable => replyTo ! EnqueueFailure(throwable), _ => replyTo ! EnqueueSuccess)
        Behaviors.same
      case Nack(clientMessage, replyTo) =>
        replyTo ! EnqueueSuccess
        Behaviors.same
      case Consume(clientMessage) =>
        dequeue ! ReactiveActiveMQMailQueue.Message(clientMessage.getBodyBuffer.readString(), clientMessage, self)
        Behaviors.same
      case Enqueue(payload, replyTo) =>
        val message = session.createMessage(true).writeBodyBufferString(payload)

        import org.apache.activemq.artemis.api.core.{Message => ArtemisMessage}

        producer.send(message, new SendAcknowledgementHandler {
          override def sendAcknowledged(message: ArtemisMessage): Unit =
            replyTo ! EnqueueSuccess

          override def sendFailed(message: ArtemisMessage, e: Exception): Unit =
            replyTo ! EnqueueFailure(e)
        })
        Behaviors.same
    }
  })


}