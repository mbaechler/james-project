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

package org.apache.james.jmap.model;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.james.jmap.methods.ValidationResult;
import org.apache.james.jmap.model.MessageProperties.MessageProperty;
import org.apache.james.mailbox.store.mail.model.Mailbox;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@JsonDeserialize(builder = CreationMessage.Builder.class)
public class CreationMessage {

    private static final String RECIPIENT_PROPERTY_NAMES = ImmutableList.of(MessageProperty.to, MessageProperty.cc, MessageProperty.bcc).stream()
            .map(MessageProperty::asFieldName)
            .collect(Collectors.joining(", "));

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private ImmutableList<String> mailboxIds;
        private String inReplyToMessageId;
        private boolean isUnread;
        private boolean isFlagged;
        private boolean isAnswered;
        private boolean isDraft;
        private final ImmutableMap.Builder<String, String> headers;
        private Optional<DraftEmailer> from = Optional.empty();
        private final ImmutableList.Builder<DraftEmailer> to;
        private final ImmutableList.Builder<DraftEmailer> cc;
        private final ImmutableList.Builder<DraftEmailer> bcc;
        private final ImmutableList.Builder<DraftEmailer> replyTo;
        private String subject;
        private ZonedDateTime date;
        private String textBody;
        private String htmlBody;
        private final ImmutableList.Builder<Attachment> attachments;
        private final ImmutableMap.Builder<String, SubMessage> attachedMessages;

        private Builder() {
            to = ImmutableList.builder();
            cc = ImmutableList.builder();
            bcc = ImmutableList.builder();
            replyTo = ImmutableList.builder();
            attachments = ImmutableList.builder();
            attachedMessages = ImmutableMap.builder();
            headers = ImmutableMap.builder();
        }

        public Builder mailboxId(String... mailboxIds) {
            return mailboxIds(Arrays.asList(mailboxIds));
        }

        @JsonDeserialize
        public Builder mailboxIds(List<String> mailboxIds) {
            this.mailboxIds = ImmutableList.copyOf(mailboxIds);
            return this;
        }

        public Builder inReplyToMessageId(String inReplyToMessageId) {
            this.inReplyToMessageId = inReplyToMessageId;
            return this;
        }

        public Builder isUnread(boolean isUnread) {
            this.isUnread = isUnread;
            return this;
        }

        public Builder isFlagged(boolean isFlagged) {
            this.isFlagged = isFlagged;
            return this;
        }

        public Builder isAnswered(boolean isAnswered) {
            this.isAnswered = isAnswered;
            return this;
        }

        public Builder isDraft(boolean isDraft) {
            this.isDraft = isDraft;
            return this;
        }

        public Builder headers(ImmutableMap<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder from(DraftEmailer from) {
            this.from = Optional.ofNullable(from);
            return this;
        }

        public Builder to(List<DraftEmailer> to) {
            this.to.addAll(to);
            return this;
        }

        public Builder cc(List<DraftEmailer> cc) {
            this.cc.addAll(cc);
            return this;
        }

        public Builder bcc(List<DraftEmailer> bcc) {
            this.bcc.addAll(bcc);
            return this;
        }

        public Builder replyTo(List<DraftEmailer> replyTo) {
            this.replyTo.addAll(replyTo);
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder date(ZonedDateTime date) {
            this.date = date;
            return this;
        }

        public Builder textBody(String textBody) {
            this.textBody = textBody;
            return this;
        }

        public Builder htmlBody(String htmlBody) {
            this.htmlBody = htmlBody;
            return this;
        }

        public Builder attachments(Attachment... attachments) {
            return attachments(Arrays.asList(attachments));
        }
        
        @JsonDeserialize
        public Builder attachments(List<Attachment> attachments) {
            this.attachments.addAll(attachments);
            return this;
        }

        public Builder attachedMessages(Map<String, SubMessage> attachedMessages) {
            this.attachedMessages.putAll(attachedMessages);
            return this;
        }

        private static boolean areAttachedMessagesKeysInAttachments(ImmutableList<Attachment> attachments, ImmutableMap<String, SubMessage> attachedMessages) {
            return attachments.stream()
                    .map(Attachment::getBlobId)
                    .allMatch(attachedMessages::containsKey);
        }

        public CreationMessage build() {
            Preconditions.checkState(mailboxIds != null, "'mailboxIds' is mandatory");
            Preconditions.checkState(headers != null, "'headers' is mandatory");
            ImmutableList<Attachment> attachments = this.attachments.build();
            ImmutableMap<String, SubMessage> attachedMessages = this.attachedMessages.build();
            //TODO: need to be sorted out
            //Preconditions.checkState(areAttachedMessagesKeysInAttachments(attachments, attachedMessages), "'attachedMessages' keys must be in 'attachments'");

            if (date == null) {
                date = ZonedDateTime.now();
            }

            return new CreationMessage(mailboxIds, Optional.ofNullable(inReplyToMessageId), isUnread, isFlagged, isAnswered, isDraft, headers.build(), from,
                    to.build(), cc.build(), bcc.build(), replyTo.build(), subject, date, Optional.ofNullable(textBody), Optional.ofNullable(htmlBody), attachments, attachedMessages);
        }
    }

    private final ImmutableList<String> mailboxIds;
    private final Optional<String> inReplyToMessageId;
    private final boolean isUnread;
    private final boolean isFlagged;
    private final boolean isAnswered;
    private final boolean isDraft;
    private final ImmutableMap<String, String> headers;
    private final Optional<DraftEmailer> from;
    private final ImmutableList<DraftEmailer> to;
    private final ImmutableList<DraftEmailer> cc;
    private final ImmutableList<DraftEmailer> bcc;
    private final ImmutableList<DraftEmailer> replyTo;
    private final String subject;
    private final ZonedDateTime date;
    private final Optional<String> textBody;
    private final Optional<String> htmlBody;
    private final ImmutableList<Attachment> attachments;
    private final ImmutableMap<String, SubMessage> attachedMessages;

    @VisibleForTesting
    CreationMessage(ImmutableList<String> mailboxIds, Optional<String> inReplyToMessageId, boolean isUnread, boolean isFlagged, boolean isAnswered, boolean isDraft, ImmutableMap<String, String> headers, Optional<DraftEmailer> from,
                    ImmutableList<DraftEmailer> to, ImmutableList<DraftEmailer> cc, ImmutableList<DraftEmailer> bcc, ImmutableList<DraftEmailer> replyTo, String subject, ZonedDateTime date, Optional<String> textBody, Optional<String> htmlBody, ImmutableList<Attachment> attachments,
                    ImmutableMap<String, SubMessage> attachedMessages) {
        this.mailboxIds = mailboxIds;
        this.inReplyToMessageId = inReplyToMessageId;
        this.isUnread = isUnread;
        this.isFlagged = isFlagged;
        this.isAnswered = isAnswered;
        this.isDraft = isDraft;
        this.headers = headers;
        this.from = from;
        this.to = to;
        this.cc = cc;
        this.bcc = bcc;
        this.replyTo = replyTo;
        this.subject = subject;
        this.date = date;
        this.textBody = textBody;
        this.htmlBody = htmlBody;
        this.attachments = attachments;
        this.attachedMessages = attachedMessages;
    }

    public ImmutableList<String> getMailboxIds() {
        return mailboxIds;
    }

    public Optional<String> getInReplyToMessageId() {
        return inReplyToMessageId;
    }

    public boolean isIsUnread() {
        return isUnread;
    }

    public boolean isIsFlagged() {
        return isFlagged;
    }

    public boolean isIsAnswered() {
        return isAnswered;
    }

    public boolean isIsDraft() {
        return isDraft;
    }

    public ImmutableMap<String, String> getHeaders() {
        return headers;
    }

    public Optional<DraftEmailer> getFrom() {
        return from;
    }

    public ImmutableList<DraftEmailer> getTo() {
        return to;
    }

    public ImmutableList<DraftEmailer> getCc() {
        return cc;
    }

    public ImmutableList<DraftEmailer> getBcc() {
        return bcc;
    }

    public ImmutableList<DraftEmailer> getReplyTo() {
        return replyTo;
    }

    public String getSubject() {
        return subject;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public Optional<String> getTextBody() {
        return textBody;
    }

    public Optional<String> getHtmlBody() {
        return htmlBody;
    }

    public ImmutableList<Attachment> getAttachments() {
        return attachments;
    }

    public ImmutableMap<String, SubMessage> getAttachedMessages() {
        return attachedMessages;
    }

    public boolean isValid() {
        return validate().isEmpty();
    }

    public List<ValidationResult> validate() {
        ImmutableList.Builder<ValidationResult> errors = ImmutableList.builder();
        assertValidFromProvided(errors);
        assertAtLeastOneValidRecipient(errors);
        assertSubjectProvided(errors);
        return errors.build();
    }

    private void assertSubjectProvided(ImmutableList.Builder<ValidationResult> errors) {
        if (Strings.isNullOrEmpty(subject)) {
            errors.add(ValidationResult.builder().message("'subject' is missing").property(MessageProperty.subject.asFieldName()).build());
        }
    }

    private void assertAtLeastOneValidRecipient(ImmutableList.Builder<ValidationResult> errors) {
        ImmutableList<DraftEmailer> recipients = ImmutableList.<DraftEmailer>builder().addAll(to).addAll(cc).addAll(bcc).build();
        boolean hasAtLeastOneAddressToSendTo = recipients.stream().anyMatch(DraftEmailer::hasValidEmail);
        boolean recipientsHaveValidAddresses = recipients.stream().allMatch(e1 -> e1.getEmail() != null);
        if (!(recipientsHaveValidAddresses && hasAtLeastOneAddressToSendTo)) {
            errors.add(ValidationResult.builder().message("no recipient address set").property(RECIPIENT_PROPERTY_NAMES).build());
        }
    }

    private void assertValidFromProvided(ImmutableList.Builder<ValidationResult> errors) {
        ValidationResult invalidPropertyFrom = ValidationResult.builder()
                .property(MessageProperty.from.asFieldName())
                .message("'from' address is mandatory")
                .build();
        if (!from.isPresent()) {
            errors.add(invalidPropertyFrom);
        }
        from.filter(f -> !f.hasValidEmail()).ifPresent(f -> errors.add(invalidPropertyFrom));
    }

    public boolean isIn(Mailbox mailbox) {
        return mailboxIds.contains(mailbox.getMailboxId().serialize());
    }
    
    @JsonDeserialize(builder = DraftEmailer.Builder.class)
    public static class DraftEmailer {

        public static Builder builder() {
            return new Builder();
        }

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder {
            private Optional<String> name = Optional.empty();
            private Optional<String> email = Optional.empty();

            public Builder name(String name) {
                this.name = Optional.ofNullable(name);
                return this;
            }

            public Builder email(String email) {
                this.email = Optional.ofNullable(email);
                return this;
            }

            public DraftEmailer build() {
                return new DraftEmailer(name, email);
            }
        }

        private final Optional<String> name;
        private final Optional<String> email;
        private final EmailValidator emailValidator;

        @VisibleForTesting
        DraftEmailer(Optional<String> name, Optional<String> email) {
            this.name = name;
            this.email = email;
            this.emailValidator = new EmailValidator();
        }

        public Optional<String> getName() {
            return name;
        }

        public Optional<String> getEmail() {
            return email;
        }

        public boolean hasValidEmail() {
            return getEmail().isPresent() && emailValidator.isValid(getEmail().get());
        }

        public EmailUserAndDomain getEmailUserAndDomain() {
            String[] splitAddress = email.get().split("@", 2);
            return new EmailUserAndDomain(Optional.ofNullable(splitAddress[0]), Optional.ofNullable(splitAddress[1]));
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DraftEmailer) {
                DraftEmailer otherEMailer = (DraftEmailer) o;
                return Objects.equals(name, otherEMailer.name)
                        && Objects.equals(email.orElse("<unset>"), otherEMailer.email.orElse("<unset>") );
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, email);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("name", name)
                    .add("email", email.orElse("<unset>"))
                    .toString();
        }
    }

    public static class EmailUserAndDomain {
        private final Optional<String> user;
        private final Optional<String> domain;

        public EmailUserAndDomain(Optional<String> user, Optional<String> domain) {
            this.user = user;
            this.domain = domain;
        }

        public Optional<String> getUser() {
            return user;
        }

        public Optional<String> getDomain() {
            return domain;
        }
    }

    public static class EmailValidator {

        public boolean isValid(String email) {
            boolean result = true;
            try {
                InternetAddress emailAddress = new InternetAddress(email);
                // verrrry permissive validator !
                emailAddress.validate();
            } catch (AddressException ex) {
                result = false;
            }
            return result;
        }
    }
}
