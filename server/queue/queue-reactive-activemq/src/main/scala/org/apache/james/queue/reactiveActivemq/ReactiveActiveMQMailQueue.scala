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
import java.time.Duration
import java.util
import java.util.{HashMap, Map}

import com.github.steveash.guavate.Guavate
import com.google.common.collect.ImmutableList
import org.apache.activemq.artemis.api.core.RoutingType
import org.apache.activemq.artemis.api.core.client.{ClientConsumer, ClientMessage, ClientProducer, ClientSession}
import org.apache.james.queue.api.MailQueue.MailQueueException
import org.apache.james.queue.api.{MailQueue, MailQueueName}
import org.apache.james.util.SerializationUtil
import org.apache.mailet.{AttributeName, Mail}
import org.reactivestreams.Publisher

import scala.util.Try



class ReactiveActiveMQMailQueue(val session: ClientSession, val queueName: MailQueueName) extends MailQueue {

  /** JMS Property which holds the recipient as String */
  val JAMES_MAIL_RECIPIENTS = "JAMES_MAIL_RECIPIENTS"
  /** JMS Property which holds the sender as String */
  val JAMES_MAIL_SENDER = "JAMES_MAIL_SENDER"
  /** JMS Property which holds the error message as String */
  val JAMES_MAIL_ERROR_MESSAGE = "JAMES_MAIL_ERROR_MESSAGE"
  /** JMS Property which holds the last updated time as long (ms) */
  val JAMES_MAIL_LAST_UPDATED = "JAMES_MAIL_LAST_UPDATED"
  /** JMS Property which holds the mail size as long (bytes) */
  val JAMES_MAIL_MESSAGE_SIZE = "JAMES_MAIL_MESSAGE_SIZE"
  /** JMS Property which holds the mail name as String */
  val JAMES_MAIL_NAME = "JAMES_MAIL_NAME"
  /** JMS Property which holds the association between recipients and specific headers */
  val JAMES_MAIL_PER_RECIPIENT_HEADERS = "JAMES_MAIL_PER_RECIPIENT_HEADERS"
  /**
   * Separator which is used for separate an array of String values in the JMS
   * Property value
   */
  val JAMES_MAIL_SEPARATOR = ";"
  /** JMS Property which holds the remote hostname as String */
  val JAMES_MAIL_REMOTEHOST = "JAMES_MAIL_REMOTEHOST"
  /** JMS Property which holds the remote ipaddress as String */
  val JAMES_MAIL_REMOTEADDR = "JAMES_MAIL_REMOTEADDR"
  /** JMS Property which holds the mail state as String */
  val JAMES_MAIL_STATE = "JAMES_MAIL_STATE"
  /** JMS Property which holds the mail attribute names as String */
  val JAMES_MAIL_ATTRIBUTE_NAMES = "JAMES_MAIL_ATTRIBUTE_NAMES"
  /** JMS Property which holds next delivery time as long (ms) */
  val JAMES_NEXT_DELIVERY = "JAMES_NEXT_DELIVERY"

  session.createQueue(queueName.asString(), RoutingType.ANYCAST, queueName.asString(), true)
  session.start()

  override def getName: MailQueueName = queueName

  override def enQueue(mail: Mail, delay: Duration): Unit = {
  }

  @throws[MailQueueException]
  override def enQueue(mail: Mail): Unit = {
    import scala.jdk.CollectionConverters._
    import scala.jdk.StreamConverters._

    val producer = session.createProducer("example")

    val sender: String = mail.getMaybeSender.asString("")

    val attributeNames: String = mail.attributeNames.toScala(LazyList).map(_.asString()).filterNot(_ == null).mkString(JAMES_MAIL_SEPARATOR)

    var message: ClientMessage = session.createMessage(true)
      .putStringProperty(JAMES_MAIL_ERROR_MESSAGE, mail.getErrorMessage)
      .putLongProperty(JAMES_MAIL_LAST_UPDATED, mail.getLastUpdated.getTime)
      .putLongProperty(JAMES_MAIL_MESSAGE_SIZE, mail.getMessageSize)
      .putStringProperty(JAMES_MAIL_NAME, mail.getName)

      .putStringProperty(JAMES_MAIL_RECIPIENTS, mail.getRecipients.asScala.filterNot(_ == null).mkString(JAMES_MAIL_SEPARATOR))
      .putStringProperty(JAMES_MAIL_REMOTEADDR, mail.getRemoteAddr)
      .putStringProperty(JAMES_MAIL_REMOTEHOST, mail.getRemoteHost)

      .putStringProperty(JAMES_MAIL_ATTRIBUTE_NAMES, attributeNames)
      .putStringProperty(JAMES_MAIL_SENDER, sender)
      .putStringProperty(JAMES_MAIL_STATE, mail.getState)

    message = mail.attributes().toScala(LazyList).foldLeft(message) {
      case (message, attribute) => message.putStringProperty(attribute.getName.asString(), attribute.getValue.toJson.toString)
    }

    if (!mail.getPerRecipientSpecificHeaders.getHeadersByRecipient.isEmpty) {
      message = message.putStringProperty(JAMES_MAIL_PER_RECIPIENT_HEADERS, SerializationUtil.serialize(mail.getPerRecipientSpecificHeaders))
    }

    try {
      producer.send(message)
    } catch {
      case e: Exception => throw new MailQueueException(e.getMessage, e)
    } finally {
      producer.close()
    }
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