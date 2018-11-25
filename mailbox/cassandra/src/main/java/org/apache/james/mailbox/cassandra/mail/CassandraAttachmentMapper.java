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

package org.apache.james.mailbox.cassandra.mail;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.apache.james.blob.api.BlobStore;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAOV2.DAOAttachment;
import org.apache.james.mailbox.exception.AttachmentNotFoundException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.model.Username;
import org.apache.james.util.FluentFutureStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CassandraAttachmentMapper implements AttachmentMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraAttachmentMapper.class);

    private final CassandraAttachmentDAO attachmentDAO;
    private final CassandraAttachmentDAOV2 attachmentDAOV2;
    private final BlobStore blobStore;
    private final CassandraAttachmentMessageIdDAO attachmentMessageIdDAO;
    private final CassandraAttachmentOwnerDAO ownerDAO;

    @Inject
    public CassandraAttachmentMapper(CassandraAttachmentDAO attachmentDAO, CassandraAttachmentDAOV2 attachmentDAOV2, BlobStore blobStore, CassandraAttachmentMessageIdDAO attachmentMessageIdDAO, CassandraAttachmentOwnerDAO ownerDAO) {
        this.attachmentDAO = attachmentDAO;
        this.attachmentDAOV2 = attachmentDAOV2;
        this.blobStore = blobStore;
        this.attachmentMessageIdDAO = attachmentMessageIdDAO;
        this.ownerDAO = ownerDAO;
    }

    @Override
    public void endRequest() {
    }

    @Override
    public <T> T execute(Transaction<T> transaction) throws MailboxException {
        return transaction.run();
    }

    @Override
    public Attachment getAttachment(AttachmentId attachmentId) throws AttachmentNotFoundException {
        Preconditions.checkArgument(attachmentId != null);
        return Optional.ofNullable(getAttachmentInternal(attachmentId)
            .block())
            .orElseThrow(() -> new AttachmentNotFoundException(attachmentId.getId()));
    }

    private Mono<Optional<Attachment>> retrievePayload(Optional<DAOAttachment> daoAttachmentOptional) {
        if (!daoAttachmentOptional.isPresent()) {
            return Mono.just(Optional.empty());
        }
        DAOAttachment daoAttachment = daoAttachmentOptional.get();
        return blobStore.readBytes(daoAttachment.getBlobId())
            .map(bytes -> Optional.of(daoAttachment.toAttachment(bytes)));
    }

    @Override
    public List<Attachment> getAttachments(Collection<AttachmentId> attachmentIds) {
        return getAttachmentsAsFuture(attachmentIds).block();
    }

    public Mono<ImmutableList<Attachment>> getAttachmentsAsFuture(Collection<AttachmentId> attachmentIds) {
        Preconditions.checkArgument(attachmentIds != null);

        return Flux.fromIterable(attachmentIds)
            .distinct()
            .flatMapSequential(id -> getAttachmentInternal(id)
                .or(logNotFound(id)))
            .flatMapSequential(Mono::justOrEmpty)
            .collect(Guavate.toImmutableList());
    }

    private Mono<Attachment> getAttachmentInternal(AttachmentId id) {
        return Mono.fromCompletionStage(attachmentDAOV2.getAttachment(id))
            .flatMap(this::retrievePayload)
            .flatMap(v2Value -> fallbackToV1(id, v2Value));
    }

    private Mono<Attachment> fallbackToV1(AttachmentId attachmentId, Optional<Attachment> v2Value) {
        if (v2Value.isPresent()) {
            return Mono.justOrEmpty(v2Value);
        }
        return Mono.fromCompletionStage(attachmentDAO.getAttachment(attachmentId))
            .flatMap(Mono::justOrEmpty);
    }

    @Override
    public void storeAttachmentForOwner(Attachment attachment, Username owner) throws MailboxException {
        ownerDAO.addOwner(attachment.getAttachmentId(), owner)
            .thenCompose(any -> blobStore.save(attachment.getBytes()).toFuture())
            .thenApply(blobId -> CassandraAttachmentDAOV2.from(attachment, blobId))
            .thenCompose(attachmentDAOV2::storeAttachment)
            .join();
    }

    @Override
    public void storeAttachmentsForMessage(Collection<Attachment> attachments, MessageId ownerMessageId) throws MailboxException {
        FluentFutureStream.of(
            attachments.stream()
                .map(attachment -> storeAttachmentAsync(attachment, ownerMessageId)))
            .join();
    }

    @Override
    public Collection<MessageId> getRelatedMessageIds(AttachmentId attachmentId) throws MailboxException {
        return attachmentMessageIdDAO.getOwnerMessageIds(attachmentId)
            .join();
    }

    @Override
    public Collection<Username> getOwners(AttachmentId attachmentId) throws MailboxException {
        return ownerDAO.retrieveOwners(attachmentId).join().collect(Guavate.toImmutableList());
    }

    public CompletableFuture<Void> storeAttachmentAsync(Attachment attachment, MessageId ownerMessageId) {
        return blobStore.save(attachment.getBytes()).toFuture()
            .thenApply(blobId -> CassandraAttachmentDAOV2.from(attachment, blobId))
            .thenCompose(daoAttachment -> storeAttachmentWithIndex(daoAttachment, ownerMessageId));
    }

    private CompletableFuture<Void> storeAttachmentWithIndex(DAOAttachment daoAttachment, MessageId ownerMessageId) {
        return attachmentDAOV2.storeAttachment(daoAttachment)
                .thenCompose(any -> attachmentMessageIdDAO.storeAttachmentForMessageId(daoAttachment.getAttachmentId(), ownerMessageId));
    }

    private Mono<Attachment> logNotFound(AttachmentId attachmentId) {
        LOGGER.warn("Failed retrieving attachment {}", attachmentId);
        return Mono.empty();
    }
}
