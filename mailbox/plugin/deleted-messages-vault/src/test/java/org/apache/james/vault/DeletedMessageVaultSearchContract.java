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

package org.apache.james.vault;

import org.apache.james.core.MailAddress;
import org.apache.james.core.MaybeSender;
import org.apache.james.core.User;
import org.apache.james.mailbox.inmemory.InMemoryMessageId;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.vault.search.CriterionFactory;
import org.apache.james.vault.search.Query;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.james.vault.DeletedMessageFixture.*;
import static org.apache.mailet.base.MailAddressFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

public interface DeletedMessageVaultSearchContract {
    DeletedMessageVault getVault();

    interface AllContracts extends SubjectContract, DeletionDateContract, DeliveryDateContract, RecipientsContract, SenderContract,
        HasAttachmentsContract, OriginMailboxesContract, PerUserContract, MultipleSearchCriterionsContract {
    }

    interface DeliveryDateContract extends DeletedMessageVaultSearchContract {

        @Test
        default void shouldReturnMessagesWithDeliveryBeforeDateWhenBeforeOrEquals() {
            DeletedMessage message1 = storeMessageWithDeliveryDate(DELIVERY_DATE);
            DeletedMessage message2 = storeMessageWithDeliveryDate(DELIVERY_DATE.plusMinutes(60));

            assertThat(search(Query.of(CriterionFactory.deliveryDate().beforeOrEquals(DELIVERY_DATE.plusMinutes(30)))))
                .containsOnly(message1);
        }

        @Test
        default void shouldReturnMessagesWithDeliveryEqualDateWhenBeforeOrEquals() {
            DeletedMessage message1 = storeMessageWithDeliveryDate(DELIVERY_DATE);
            DeletedMessage message2 = storeMessageWithDeliveryDate(DELIVERY_DATE.plusMinutes(60));

            assertThat(search(Query.of(CriterionFactory.deliveryDate().beforeOrEquals(DELIVERY_DATE))))
                .containsOnly(message1);
        }

        @Test
        default void shouldReturnMessagesWithDeliveryAfterDateWhenAfterOrEquals() {
            DeletedMessage message1 = storeMessageWithDeliveryDate(DELIVERY_DATE);
            DeletedMessage message2 = storeMessageWithDeliveryDate(DELIVERY_DATE.plusMinutes(60));

            assertThat(search(Query.of(CriterionFactory.deliveryDate().afterOrEquals(DELIVERY_DATE.plusMinutes(30)))))
                .containsOnly(message2);
        }

        @Test
        default void shouldReturnMessagesWithDeliveryEqualDateWhenAfterOrEquals() {
            DeletedMessage message1 = storeMessageWithDeliveryDate(DELIVERY_DATE);
            DeletedMessage message2 = storeMessageWithDeliveryDate(DELIVERY_DATE.plusMinutes(60));

            assertThat(search(Query.of(CriterionFactory.deliveryDate().afterOrEquals(DELIVERY_DATE.plusMinutes(60)))))
                .containsOnly(message2);
        }
    }

    interface DeletionDateContract extends DeletedMessageVaultSearchContract {

        @Test
        default void shouldReturnMessagesWithDeletionBeforeDateWhenBeforeOrEquals() {
            DeletedMessage message1 = storeMessageWithDeletionDate(DELIVERY_DATE);
            DeletedMessage message2 = storeMessageWithDeletionDate(DELIVERY_DATE.plusMinutes(60));

            assertThat(search(Query.of(CriterionFactory.deletionDate().beforeOrEquals(DELIVERY_DATE.plusMinutes(30)))))
                .containsOnly(message1);
        }

        @Test
        default void shouldReturnMessagesWithDeletionEqualDateWhenBeforeOrEquals() {
            DeletedMessage message1 = storeMessageWithDeletionDate(DELIVERY_DATE);
            DeletedMessage message2 = storeMessageWithDeletionDate(DELIVERY_DATE.plusMinutes(60));

            assertThat(search(Query.of(CriterionFactory.deletionDate().beforeOrEquals(DELIVERY_DATE))))
                .containsOnly(message1);
        }

        @Test
        default void shouldReturnMessagesWithDeletionAfterDateWhenAfterOrEquals() {
            DeletedMessage message1 = storeMessageWithDeletionDate(DELIVERY_DATE);
            DeletedMessage message2 = storeMessageWithDeletionDate(DELIVERY_DATE.plusMinutes(60));

            assertThat(search(Query.of(CriterionFactory.deletionDate().afterOrEquals(DELIVERY_DATE.plusMinutes(30)))))
                .containsOnly(message2);
        }

        @Test
        default void shouldReturnMessagesWithDeletionEqualDateWhenAfterOrEquals() {
            DeletedMessage message1 = storeMessageWithDeletionDate(DELIVERY_DATE);
            DeletedMessage message2 = storeMessageWithDeletionDate(DELIVERY_DATE.plusMinutes(60));

            assertThat(search(Query.of(CriterionFactory.deletionDate().afterOrEquals(DELIVERY_DATE.plusMinutes(60)))))
                .containsOnly(message2);
        }
    }

    interface RecipientsContract extends DeletedMessageVaultSearchContract {

        @Test
        default void shouldReturnMessagesWithRecipientWhenContains() {
            DeletedMessage message1 = storeMessageWithRecipients(RECIPIENT1, RECIPIENT2);
            DeletedMessage message2 = storeMessageWithRecipients(RECIPIENT1);
            DeletedMessage message3 = storeMessageWithRecipients(RECIPIENT3);

            assertThat(search(Query.of(CriterionFactory.recipients().contains(RECIPIENT1))))
                .containsOnly(message1, message2);

        }

        @Test
        default void shouldReturnNoMessageWhenDoesntContains() {
            storeMessageWithRecipients(RECIPIENT1, RECIPIENT2);
            storeMessageWithRecipients(RECIPIENT1);
            storeMessageWithRecipients(RECIPIENT2);

            assertThat(search(Query.of(CriterionFactory.recipients().contains(RECIPIENT3))))
                .isEmpty();
        }
    }

    interface SenderContract extends DeletedMessageVaultSearchContract {

        @Test
        default void shouldReturnMessagesWithSenderWhenEquals() {
            DeletedMessage message1 = storeMessageWithSender(SENDER);
            DeletedMessage message2 = storeMessageWithSender(SENDER2);

            assertThat(search(Query.of(CriterionFactory.sender().equalsMatcher(SENDER))))
                .containsOnly(message1);

        }

        @Test
        default void shouldReturnNoMessageWhenSenderDoesntEquals() {
            storeMessageWithSender(SENDER);
            storeMessageWithSender(SENDER2);

            assertThat(search(Query.of(CriterionFactory.sender().equalsMatcher(SENDER3))))
                .isEmpty();
        }

        @Test
        default void shouldNotReturnMessagesWithNullSenderWhenEquals() {
            DeletedMessage message1 = storeMessageWithSender(SENDER);
            storeMessageWithSender(SENDER2);
            storeMessageWithSender(null);
            storeMessageWithSender(null);

            assertThat(search(Query.of(CriterionFactory.sender().equalsMatcher(SENDER))))
                .containsOnly(message1);
        }
    }

    interface HasAttachmentsContract extends DeletedMessageVaultSearchContract {

        @Test
        default void shouldReturnMessagesWithAttachmentWhenHasAttachment() {
            DeletedMessage message1 = storeMessageWithHasAttachment(true);
            DeletedMessage message2 = storeMessageWithHasAttachment(false);
            DeletedMessage message3 = storeMessageWithHasAttachment(true);

            assertThat(search(Query.of(CriterionFactory.hasAttachment().equalsMatcher(true))))
                .containsOnly(message1, message3);
        }

        @Test
        default void shouldReturnMessagesWithOutAttachmentWhenHasNoAttachement() {
            DeletedMessage message1 = storeMessageWithHasAttachment(false);
            DeletedMessage message2 = storeMessageWithHasAttachment(false);
            DeletedMessage message3 = storeMessageWithHasAttachment(true);

            assertThat(search(Query.of(CriterionFactory.hasAttachment().equalsMatcher(false))))
                .containsOnly(message1, message2);
        }
    }

    interface OriginMailboxesContract extends DeletedMessageVaultSearchContract {

        @Test
        default void shouldReturnMessagesWithOriginMailboxesWhenContains() {
            DeletedMessage message1 = storeMessageWithOriginMailboxes(MAILBOX_ID_1, MAILBOX_ID_2);
            DeletedMessage message2 = storeMessageWithOriginMailboxes(MAILBOX_ID_1);
            DeletedMessage message3 = storeMessageWithOriginMailboxes(MAILBOX_ID_3);

            assertThat(search(Query.of(CriterionFactory.originMailboxes().contains(MAILBOX_ID_1))))
                .containsOnly(message1, message2);

        }

        @Test
        default void shouldReturnNoMessageWhenOriginMailboxesDoesntContains() {
            storeMessageWithOriginMailboxes(MAILBOX_ID_1, MAILBOX_ID_2);
            storeMessageWithOriginMailboxes(MAILBOX_ID_1);
            storeMessageWithOriginMailboxes(MAILBOX_ID_2);

            assertThat(search(Query.of(CriterionFactory.originMailboxes().contains(MAILBOX_ID_3))))
                .isEmpty();
        }
    }

    interface SubjectContract extends DeletedMessageVaultSearchContract {

        String APACHE_JAMES_PROJECT = "apache james project";
        String OPEN_SOURCE_SOFTWARE = "open source software";

        @Test
        default void shouldReturnMessagesContainsAtTheMiddle() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(OPEN_SOURCE_SOFTWARE);

            assertThat(search(Query.of(CriterionFactory.subject().contains("james"))))
                .containsOnly(message1);
        }

        @Test
        default void shouldReturnMessagesContainsAtTheBeginning() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(OPEN_SOURCE_SOFTWARE);

            assertThat(search(Query.of(CriterionFactory.subject().contains("apache"))))
                .containsOnly(message1);
        }

        @Test
        default void shouldReturnMessagesContainsAtTheEnd() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(OPEN_SOURCE_SOFTWARE);

            assertThat(search(Query.of(CriterionFactory.subject().contains("software"))))
                .containsOnly(message2);
        }

        @Test
        default void shouldNotReturnMissingSubjectMessagesWhenContains() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(null);

            assertThat(search(Query.of(CriterionFactory.subject().contains("james"))))
                .containsOnly(message1);
        }

        @Test
        default void shouldNotReturnMessagesContainsIgnoreCaseWhenContains() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(OPEN_SOURCE_SOFTWARE);

            assertThat(search(Query.of(CriterionFactory.subject().contains("SoftWare"))))
                .isEmpty();
        }

        @Test
        default void shouldReturnMessagesContainsIgnoreCaseAtTheMiddle() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(OPEN_SOURCE_SOFTWARE);

            assertThat(search(Query.of(CriterionFactory.subject().containsIgnoreCase("JAmEs"))))
                .containsOnly(message1);
        }

        @Test
        default void shouldReturnMessagesContainsIgnoreCaseAtTheBeginning() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(OPEN_SOURCE_SOFTWARE);

            assertThat(search(Query.of(CriterionFactory.subject().containsIgnoreCase("SouRCE"))))
                .containsOnly(message2);
        }

        @Test
        default void shouldReturnMessagesContainsIgnoreCaseAtTheEnd() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(OPEN_SOURCE_SOFTWARE);

            assertThat(search(Query.of(
                    CriterionFactory.subject().containsIgnoreCase("ProJECT"))))
                .containsOnly(message1);
        }

        @Test
        default void shouldReturnMessagesContainsWhenContainsIgnoreCase() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(OPEN_SOURCE_SOFTWARE);

            assertThat(search(Query.of(
                    CriterionFactory.subject().containsIgnoreCase("project"))))
                .containsOnly(message1);
        }

        @Test
        default void shouldNotReturnMissingSubjectMessagesWhenContainsIgnoreCase() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(null);

            assertThat(search(Query.of(CriterionFactory.subject().containsIgnoreCase("JAMes"))))
                .containsOnly(message1);
        }

        @Test
        default void shouldReturnMessagesStrictlyEquals() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(OPEN_SOURCE_SOFTWARE);

            assertThat(search(Query.of(CriterionFactory.subject().equals(APACHE_JAMES_PROJECT))))
                .containsOnly(message1);
        }

        @Test
        default void shouldNotReturnMessagesContainsWhenEquals() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(OPEN_SOURCE_SOFTWARE);

            assertThat(search(Query.of(CriterionFactory.subject().equals("james"))))
                .isEmpty();
        }

        @Test
        default void shouldNotReturnMessagesContainsIgnoreCaseWhenEquals() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(OPEN_SOURCE_SOFTWARE);

            assertThat(search(Query.of(CriterionFactory.subject().equals("proJECT"))))
                .isEmpty();
        }

        @Test
        default void shouldNotReturnMessagesEqualsIgnoreCaseWhenEquals() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(OPEN_SOURCE_SOFTWARE);

            assertThat(search(Query.of(CriterionFactory.subject().equals("Apache James Project"))))
                .isEmpty();
        }

        @Test
        default void shouldNotReturnMissingSubjectMessagesWhenEquals() {
            DeletedMessage message1 = storeMessageWithSubject(APACHE_JAMES_PROJECT);
            DeletedMessage message2 = storeMessageWithSubject(null);

            assertThat(search(Query.of(CriterionFactory.subject().equals(APACHE_JAMES_PROJECT))))
                .containsOnly(message1);
        }
    }

    interface MultipleSearchCriterionsContract extends DeletedMessageVaultSearchContract {

        @Test
        default void searchShouldReturnOnlyMessageWhichMatchMultipleCriterions() {
            DeletedMessage message1 = storeDefaultMessage();
            DeletedMessage message2 = storeDefaultMessage();
            DeletedMessage message3 = storeDefaultMessage();
            DeletedMessage message4 = storeMessageWithOriginMailboxes(MAILBOX_ID_2);
            DeletedMessage message5 = storeMessageWithSender(SENDER2);
            DeletedMessage message6 = storeMessageWithDeletionDate(DELETION_DATE.minusHours(1));

            assertThat(search(Query.of(
                    CriterionFactory.originMailboxes().contains(MAILBOX_ID_1),
                    CriterionFactory.sender().equalsMatcher(SENDER),
                    CriterionFactory.deletionDate().afterOrEquals(DELETION_DATE))))
                .containsOnly(message1, message2, message3);
        }

        @Test
        default void searchShouldReturnAllMessageWhenSearchForAllCriterions() {
            DeletedMessage message1 = storeDefaultMessage();
            DeletedMessage message2 = storeDefaultMessage();
            DeletedMessage message3 = storeDefaultMessage();
            DeletedMessage message4 = storeMessageWithOriginMailboxes(MAILBOX_ID_2);
            DeletedMessage message5 = storeMessageWithSender(SENDER2);
            DeletedMessage message6 = storeMessageWithDeletionDate(DELETION_DATE.minusHours(1));

            assertThat(search(Query.ALL))
                .containsOnly(message1, message2, message3, message4, message5, message6);
        }

        @Test
        default void searchShouldReturnAllMessageEvenNullSubjectWhenSearchForAllCriterions() {
            DeletedMessage message1 = storeDefaultMessage();
            DeletedMessage message2 = storeDefaultMessage();
            DeletedMessage message3 = storeDefaultMessage();
            DeletedMessage message4 = storeMessageWithSubject(null);

            assertThat(search(Query.ALL))
                .containsOnly(message1, message2, message3, message4);
        }

        @Test
        default void searchShouldReturnAllMessageEvenNullSenderWhenSearchForAllCriterions() {
            DeletedMessage message1 = storeDefaultMessage();
            DeletedMessage message2 = storeDefaultMessage();
            DeletedMessage message3 = storeDefaultMessage();
            DeletedMessage message4 = storeMessageWithSender(null);

            assertThat(search(Query.ALL))
                .containsOnly(message1, message2, message3, message4);
        }

        @Test
        default void searchShouldReturnMessageWhenHavingSameCreterionTypes() {
            DeletedMessage message1 = storeMessageWithRecipients(RECIPIENT1, RECIPIENT2, RECIPIENT3);
            DeletedMessage message2 = storeMessageWithRecipients(RECIPIENT1, RECIPIENT2);
            DeletedMessage message3 = storeMessageWithRecipients(RECIPIENT1);

            assertThat(search(Query.of(
                    CriterionFactory.recipients().contains(RECIPIENT1),
                    CriterionFactory.recipients().contains(RECIPIENT2))))
                .containsOnly(message1, message2);
        }

        @Test
        default void searchShouldReturnEmptyWhenHavingSameCreterionTypesButOppositeMatching() {
            DeletedMessage message1 = storeMessageWithDeletionDate(DELETION_DATE);
            DeletedMessage message2 = storeMessageWithDeletionDate(DELETION_DATE);
            DeletedMessage message3 = storeMessageWithDeletionDate(DELETION_DATE.plusHours(2));
            DeletedMessage message4 = storeMessageWithDeletionDate(DELETION_DATE.minusHours(2));

            assertThat(search(Query.of(
                    CriterionFactory.deletionDate().afterOrEquals(DELETION_DATE.plusHours(1)),
                    CriterionFactory.deletionDate().beforeOrEquals(DELETION_DATE.minusHours(1)))))
                .isEmpty();
        }
    }

    interface PerUserContract extends DeletedMessageVaultSearchContract {

        default DeletedMessage storeMessageWithUser(User user) {
            DeletedMessage deletedMessage = DeletedMessage.builder()
                .messageId(InMemoryMessageId.of(MESSAGE_ID_GENERATOR.incrementAndGet()))
                .originMailboxes(MAILBOX_ID_1)
                .user(USER)
                .deliveryDate(DELIVERY_DATE)
                .deletionDate(DELETION_DATE)
                .sender(MaybeSender.of(SENDER))
                .recipients(RECIPIENT1)
                .content(() -> new ByteArrayInputStream(CONTENT))
                .hasAttachment(false)
                .subject(SUBJECT)
                .build();

            Mono.from(getVault().append(user, deletedMessage))
                .block();
            return deletedMessage;
        }

        @Test
        default void searchForAnUserShouldNotReturnMessagesFromAnotherUser() {
            DeletedMessage message1 = storeMessageWithUser(USER);
            DeletedMessage message2 = storeMessageWithUser(USER);
            DeletedMessage message3 = storeMessageWithUser(USER_2);

            assertThat(search(USER, Query.ALL))
                .containsOnly(message1, message2);
        }
    }

    AtomicLong MESSAGE_ID_GENERATOR = new AtomicLong(0);

    default List<DeletedMessage> search(Query query) {
        return search(USER, query);
    }

    default List<DeletedMessage> search(User user, Query query) {
        return Flux.from(getVault().search(user, query)).collectList().block();
    }

    default DeletedMessage storeMessageWithDeliveryDate(ZonedDateTime deliveryDate) {
        DeletedMessage deletedMessage = DeletedMessage.builder()
            .messageId(InMemoryMessageId.of(MESSAGE_ID_GENERATOR.incrementAndGet()))
            .originMailboxes(MAILBOX_ID_1)
            .user(USER)
            .deliveryDate(deliveryDate)
            .deletionDate(DELETION_DATE)
            .sender(MaybeSender.of(SENDER))
            .recipients(RECIPIENT1)
            .content(() -> new ByteArrayInputStream(CONTENT))
            .hasAttachment(false)
            .build();

        Mono.from(getVault().append(USER, deletedMessage))
            .block();
        return deletedMessage;
    }

    default DeletedMessage storeMessageWithDeletionDate(ZonedDateTime delitionDate) {
        DeletedMessage deletedMessage = DeletedMessage.builder()
            .messageId(InMemoryMessageId.of(MESSAGE_ID_GENERATOR.incrementAndGet()))
            .originMailboxes(MAILBOX_ID_1)
            .user(USER)
            .deliveryDate(DELIVERY_DATE)
            .deletionDate(delitionDate)
            .sender(MaybeSender.of(SENDER))
            .recipients(RECIPIENT1)
            .content(() -> new ByteArrayInputStream(CONTENT))
            .hasAttachment(false)
            .build();

        Mono.from(getVault().append(USER, deletedMessage))
            .block();
        return deletedMessage;
    }

    default DeletedMessage storeMessageWithRecipients(MailAddress... recipients) {
        DeletedMessage deletedMessage = DeletedMessage.builder()
            .messageId(InMemoryMessageId.of(MESSAGE_ID_GENERATOR.incrementAndGet()))
            .originMailboxes(MAILBOX_ID_1)
            .user(USER)
            .deliveryDate(DELIVERY_DATE)
            .deletionDate(DELETION_DATE)
            .sender(MaybeSender.of(SENDER))
            .recipients(recipients)
            .content(() -> new ByteArrayInputStream(CONTENT))
            .hasAttachment(false)
            .build();

        Mono.from(getVault().append(USER, deletedMessage))
            .block();
        return deletedMessage;
    }

    default DeletedMessage storeMessageWithSender(MailAddress sender) {
        DeletedMessage deletedMessage = DeletedMessage.builder()
            .messageId(InMemoryMessageId.of(MESSAGE_ID_GENERATOR.incrementAndGet()))
            .originMailboxes(MAILBOX_ID_1)
            .user(USER)
            .deliveryDate(DELIVERY_DATE)
            .deletionDate(DELETION_DATE)
            .sender(MaybeSender.of(sender))
            .recipients(RECIPIENT1, RECIPIENT2)
            .content(() -> new ByteArrayInputStream(CONTENT))
            .hasAttachment(false)
            .build();

        Mono.from(getVault().append(USER, deletedMessage))
            .block();
        return deletedMessage;
    }

    default DeletedMessage storeMessageWithHasAttachment(boolean hasAttachment) {
        DeletedMessage deletedMessage = DeletedMessage.builder()
            .messageId(InMemoryMessageId.of(MESSAGE_ID_GENERATOR.incrementAndGet()))
            .originMailboxes(MAILBOX_ID_1)
            .user(USER)
            .deliveryDate(DELIVERY_DATE)
            .deletionDate(DELETION_DATE)
            .sender(MaybeSender.of(SENDER))
            .recipients(RECIPIENT1, RECIPIENT2)
            .content(() -> new ByteArrayInputStream(CONTENT))
            .hasAttachment(hasAttachment)
            .build();

        Mono.from(getVault().append(USER, deletedMessage))
            .block();
        return deletedMessage;
    }

    default DeletedMessage storeMessageWithOriginMailboxes(MailboxId... originMailboxIds) {
        DeletedMessage deletedMessage = DeletedMessage.builder()
            .messageId(InMemoryMessageId.of(MESSAGE_ID_GENERATOR.incrementAndGet()))
            .originMailboxes(originMailboxIds)
            .user(USER)
            .deliveryDate(DELIVERY_DATE)
            .deletionDate(DELETION_DATE)
            .sender(MaybeSender.of(SENDER))
            .recipients(RECIPIENT1, RECIPIENT2)
            .content(() -> new ByteArrayInputStream(CONTENT))
            .hasAttachment(true)
            .build();

        Mono.from(getVault().append(USER, deletedMessage))
            .block();
        return deletedMessage;
    }

    default DeletedMessage storeMessageWithSubject(String subject) {
        DeletedMessage deletedMessage = DeletedMessage.builder()
            .messageId(InMemoryMessageId.of(MESSAGE_ID_GENERATOR.incrementAndGet()))
            .originMailboxes(MAILBOX_ID_1)
            .user(USER)
            .deliveryDate(DELIVERY_DATE)
            .deletionDate(DELETION_DATE)
            .sender(MaybeSender.of(SENDER))
            .recipients(RECIPIENT1)
            .content(() -> new ByteArrayInputStream(CONTENT))
            .hasAttachment(false)
            .subject(subject)
            .build();

        Mono.from(getVault().append(USER, deletedMessage))
            .block();
        return deletedMessage;
    }

    default DeletedMessage storeDefaultMessage() {
        DeletedMessage deletedMessage = DeletedMessage.builder()
            .messageId(InMemoryMessageId.of(MESSAGE_ID_GENERATOR.incrementAndGet()))
            .originMailboxes(MAILBOX_ID_1)
            .user(USER)
            .deliveryDate(DELIVERY_DATE)
            .deletionDate(DELETION_DATE)
            .sender(MaybeSender.of(SENDER))
            .recipients(RECIPIENT1)
            .content(() -> new ByteArrayInputStream(CONTENT))
            .hasAttachment(false)
            .subject(SUBJECT)
            .build();

        Mono.from(getVault().append(USER, deletedMessage))
            .block();
        return deletedMessage;
    }
}
