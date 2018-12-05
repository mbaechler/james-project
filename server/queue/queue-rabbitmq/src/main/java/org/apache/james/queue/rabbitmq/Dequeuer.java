/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.queue.rabbitmq;

import static org.apache.james.queue.api.MailQueue.DEQUEUED_METRIC_NAME_PREFIX;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.james.metrics.api.Metric;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.rabbitmq.view.api.DeleteCondition;
import org.apache.james.queue.rabbitmq.view.api.MailQueueView;
import org.apache.mailet.Mail;

import com.github.fge.lambdas.consumers.ThrowingConsumer;
import com.rabbitmq.client.Delivery;
import reactor.rabbitmq.AcknowledgableDelivery;

class Dequeuer {

    private static final boolean REQUEUE = true;
    private final Iterator<AcknowledgableDelivery> messages;

    private static class RabbitMQMailQueueItem implements MailQueue.MailQueueItem {
        private final Consumer<Boolean> ack;
        private final Mail mail;

        private RabbitMQMailQueueItem(Consumer<Boolean> ack, Mail mail) {
            this.ack = ack;
            this.mail = mail;
        }

        @Override
        public Mail getMail() {
            return mail;
        }

        @Override
        public void done(boolean success) {
            ack.accept(success);
        }
    }

    private final Function<MailReferenceDTO, Mail> mailLoader;
    private final Metric dequeueMetric;
    private final MailReferenceSerializer mailReferenceSerializer;
    private final MailQueueView mailQueueView;

    Dequeuer(MailQueueName name, RabbitClient rabbitClient, Function<MailReferenceDTO, Mail> mailLoader,
             MailReferenceSerializer serializer, MetricFactory metricFactory,
             MailQueueView mailQueueView) {
        this.mailLoader = mailLoader;
        this.mailReferenceSerializer = serializer;
        this.mailQueueView = mailQueueView;
        this.dequeueMetric = metricFactory.generate(DEQUEUED_METRIC_NAME_PREFIX + name.asString());
        this.messages = messageIterator(name, rabbitClient);
    }

    private Iterator<AcknowledgableDelivery> messageIterator(MailQueueName name, RabbitClient rabbitClient) {
        return rabbitClient
            .receive(name)
            .filter(getResponse -> getResponse.getBody() != null)
            .toIterable()
            .iterator();
    }

    MailQueue.MailQueueItem deQueue() throws MailQueue.MailQueueException, InterruptedException {
        return loadItem(next());
    }

    private AcknowledgableDelivery next() throws InterruptedException {
        try {
            //In some very weird cases, there's a race condition
            //Blocking iterable pass through hasNext and then don't find any element to return
            return messages.next();
        } catch (IllegalStateException e) {
            throw new InterruptedException();
        }
    }

    private RabbitMQMailQueueItem loadItem(AcknowledgableDelivery response) throws MailQueue.MailQueueException {
        Mail mail = loadMail(response);
        ThrowingConsumer<Boolean> ack = ack(response, response.getEnvelope().getDeliveryTag(), mail);
        return new RabbitMQMailQueueItem(ack, mail);
    }

    private ThrowingConsumer<Boolean> ack(AcknowledgableDelivery response, long deliveryTag, Mail mail) {
        return success -> {
            if (success) {
                dequeueMetric.increment();
                response.ack();
                mailQueueView.delete(DeleteCondition.withName(mail.getName()));
            } else {
                response.nack(REQUEUE);
            }
        };
    }

    private Mail loadMail(Delivery response) throws MailQueue.MailQueueException {
        MailReferenceDTO mailDTO = toMailReference(response);
        return mailLoader.apply(mailDTO);
    }

    private MailReferenceDTO toMailReference(Delivery getResponse) throws MailQueue.MailQueueException {
        try {
            return mailReferenceSerializer.read(getResponse.getBody());
        } catch (IOException e) {
            throw new MailQueue.MailQueueException("Failed to parse DTO", e);
        }
    }

}
