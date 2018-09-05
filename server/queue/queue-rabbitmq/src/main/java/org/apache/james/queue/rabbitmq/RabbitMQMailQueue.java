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

import java.io.IOException;
import java.io.SequenceInputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.core.MailAddress;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.server.core.MailImpl;
import org.apache.james.util.CompletableFutureUtil;
import org.apache.james.util.SerializationUtil;
import org.apache.james.util.mime.MessageSplitter;
import org.apache.mailet.Mail;
import org.apache.mailet.PerRecipientHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.nurkiewicz.asyncretry.AsyncRetryExecutor;
import com.rabbitmq.client.GetResponse;

public class RabbitMQMailQueue implements MailQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQMailQueue.class);

    private static class NoMailYetException extends RuntimeException {
    }

    private static class RabbitMQMailQueueItem implements MailQueueItem {
        private final RabbitClient rabbitClient;
        private final long deliveryTag;
        private final Mail mail;

        private RabbitMQMailQueueItem(RabbitClient rabbitClient, long deliveryTag, Mail mail) {
            this.rabbitClient = rabbitClient;
            this.deliveryTag = deliveryTag;
            this.mail = mail;
        }

        @Override
        public Mail getMail() {
            return mail;
        }

        @Override
        public void done(boolean success) throws MailQueueException {
            try {
                rabbitClient.ack(deliveryTag);
            } catch (IOException e) {
                throw new MailQueueException("Failed to ACK " + mail.getName() + " with delivery tag " + deliveryTag, e);
            }
        }
    }

    static class Factory {
        private final RabbitClient rabbitClient;
        private final BlobStore blobStore;
        private final BlobId.Factory blobIdFactory;

        @Inject
        @VisibleForTesting Factory(RabbitClient rabbitClient, BlobStore blobStore, BlobId.Factory blobIdFactory) {
            this.rabbitClient = rabbitClient;
            this.blobStore = blobStore;
            this.blobIdFactory = blobIdFactory;
        }

        RabbitMQMailQueue create(MailQueueName mailQueueName) {
            return new RabbitMQMailQueue(mailQueueName, rabbitClient, blobStore, blobIdFactory);
        }
    }

    private static final int TEN_MS = 10;

    private final MailQueueName name;
    private final RabbitClient rabbitClient;
    private final BlobStore blobStore;
    private final BlobId.Factory blobIdFactory;
    private final ObjectMapper objectMapper;

    RabbitMQMailQueue(MailQueueName name, RabbitClient rabbitClient, BlobStore blobStore, BlobId.Factory blobIdFactory) {
        this.blobStore = blobStore;
        this.blobIdFactory = blobIdFactory;
        this.name = name;
        this.rabbitClient = rabbitClient;
        this.objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .registerModule(new GuavaModule());
    }

    @Override
    public String getName() {
        return name.asString();
    }

    @Override
    public void enQueue(Mail mail, long delay, TimeUnit unit) throws MailQueueException {
        LOGGER.info("Ignored delay upon enqueue of {} : {} {}.", mail.getName(), delay, unit);
        enQueue(mail);
    }

    @Override
    public void enQueue(Mail mail) throws MailQueueException {
        Pair<BlobId, BlobId> blobIds = saveBlobs(mail).join();
        MailDTO mailDTO = MailDTO.fromMail(mail, blobIds.getLeft(), blobIds.getRight());
        byte[] message = getMessageBytes(mailDTO);
        rabbitClient.publish(name, message);
    }

    private CompletableFuture<Pair<BlobId, BlobId>> saveBlobs(Mail mail) throws MailQueueException {
        try {
            Pair<byte[], byte[]> headerBody = MessageSplitter.splitHeaderBody(mail.getMessage());

            return CompletableFutureUtil.combine(
                blobStore.save(headerBody.getLeft()),
                blobStore.save(headerBody.getRight()),
                Pair::of);
        } catch (IOException | MessagingException e) {
            throw new MailQueueException("Error while saving blob", e);
        }
    }

    private byte[] getMessageBytes(MailDTO mailDTO) throws MailQueueException {
        try {
            return objectMapper.writeValueAsBytes(mailDTO);
        } catch (JsonProcessingException e) {
            throw new MailQueueException("Unable to serialize message", e);
        }
    }


    @Override
    public MailQueueItem deQueue() throws MailQueueException {
        GetResponse getResponse = pollChannel();
        MailDTO mailDTO = toDTO(getResponse);
        Mail mail = toMail(mailDTO);
        return new RabbitMQMailQueueItem(rabbitClient, getResponse.getEnvelope().getDeliveryTag(), mail);
    }

    private MailDTO toDTO(GetResponse getResponse) throws MailQueueException {
        try {
            return objectMapper.readValue(getResponse.getBody(), MailDTO.class);
        } catch (IOException e) {
            throw new MailQueueException("Failed to parse DTO", e);
        }
    }

    private GetResponse pollChannel() {
        return new AsyncRetryExecutor(Executors.newSingleThreadScheduledExecutor())
            .withFixedRate()
            .withMinDelay(TEN_MS)
            .retryOn(NoMailYetException.class)
            .getWithRetry(this::singleChannelRead)
            .join();
    }

    private GetResponse singleChannelRead() throws IOException {
        return rabbitClient.poll(name)
            .filter(getResponse -> getResponse.getBody() != null)
            .orElseThrow(NoMailYetException::new);
    }

    private Mail toMail(MailDTO dto) throws MailQueueException {
        try {
            MailImpl mail = new MailImpl(
                dto.getName(),
                MailAddress.getMailSender(dto.getSender()),
                dto.getRecipients()
                    .stream()
                    .map(Throwing.<String, MailAddress>function(MailAddress::new).sneakyThrow())
                    .collect(Guavate.toImmutableList()),
                new SequenceInputStream(
                    blobStore.read(blobIdFactory.from(dto.getHeaderBlobId())),
                    blobStore.read(blobIdFactory.from(dto.getBodyBlobId()))));
            mail.setErrorMessage(dto.getErrorMessage());
            mail.setRemoteAddr(dto.getRemoteAddr());
            mail.setRemoteHost(dto.getRemoteHost());
            mail.setState(dto.getState());
            mail.setLastUpdated(new Date(dto.getLastUpdated().toEpochMilli()));

            dto.getAttributes()
                .entrySet()
                .stream()
                .map(entry -> Pair.of(entry.getKey(), SerializationUtil.<Serializable>deserialize(entry.getValue())))
                .forEach(pair -> mail.setAttribute(pair.getKey(), pair.getValue()));

            Optional.ofNullable(SerializationUtil.<PerRecipientHeaders>deserialize(dto.getPerRecipientHeaders()))
                .ifPresent(mail::addAllSpecificHeaderForRecipient);

            return mail;
        } catch (AddressException e) {
            throw new MailQueueException("Failed to parse mail address", e);
        } catch (MessagingException e) {
            throw new MailQueueException("Failed to generate mime message", e);
        }
    }
}