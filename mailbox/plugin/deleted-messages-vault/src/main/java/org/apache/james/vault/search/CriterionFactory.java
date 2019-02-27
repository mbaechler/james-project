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

package org.apache.james.vault.search;

import static org.apache.james.vault.search.DeletedMessageField.DELETION_DATE;
import static org.apache.james.vault.search.DeletedMessageField.DELIVERY_DATE;
import static org.apache.james.vault.search.DeletedMessageField.HAS_ATTACHMENT;
import static org.apache.james.vault.search.DeletedMessageField.ORIGIN_MAILBOXES;
import static org.apache.james.vault.search.DeletedMessageField.RECIPIENTS;
import static org.apache.james.vault.search.DeletedMessageField.SENDER;
import static org.apache.james.vault.search.DeletedMessageField.SUBJECT;

import java.time.ZonedDateTime;
import java.util.List;

import org.apache.james.core.MailAddress;
import org.apache.james.mailbox.model.MailboxId;

public interface CriterionFactory {

    class StringCriterionFactory {

        private final DeletedMessageField<String> deletedMessageField;

        private StringCriterionFactory(DeletedMessageField<String> deletedMessageField) {
            this.deletedMessageField = deletedMessageField;
        }

        public Criterion<String> contains(String subString) {
            return new Criterion<>(deletedMessageField, ValueMatcher.SingleValueMatcher.contains(subString));
        }

        public Criterion<String> containsIgnoreCase(String subString) {
            return new Criterion<>(deletedMessageField, ValueMatcher.SingleValueMatcher.containsIgnoreCase(subString));
        }

        public Criterion<String> equals(String testedString) {
            return new Criterion<>(deletedMessageField, ValueMatcher.SingleValueMatcher.isEquals(testedString));
        }

        public Criterion<String> equalsIgnoreCase(String testedString) {
            return new Criterion<>(deletedMessageField, ValueMatcher.SingleValueMatcher.equalsIgnoreCase(testedString));
        }
    }

    class ZonedDateTimeCriterionFactory {

        private final DeletedMessageField<ZonedDateTime> deletedMessageField;

        private ZonedDateTimeCriterionFactory(DeletedMessageField<ZonedDateTime> deletedMessageField) {
            this.deletedMessageField = deletedMessageField;
        }

        public Criterion<ZonedDateTime> beforeOrEquals(ZonedDateTime testedInstant) {
            return new Criterion<>(deletedMessageField, ValueMatcher.SingleValueMatcher.beforeOrEquals(testedInstant));
        }

        public Criterion<ZonedDateTime> afterOrEquals(ZonedDateTime testedInstant) {
            return new Criterion<>(deletedMessageField, ValueMatcher.SingleValueMatcher.afterOrEquals(testedInstant));
        }
    }

    static ZonedDateTimeCriterionFactory deletionDate() {
        return new ZonedDateTimeCriterionFactory(DELETION_DATE);
    }

    static ZonedDateTimeCriterionFactory deliveryDate() {
        return new ZonedDateTimeCriterionFactory(DELIVERY_DATE);
    }

    static Criterion<List<MailAddress>> containsRecipient(MailAddress recipient) {
        return new Criterion<>(RECIPIENTS, ValueMatcher.ListContains.of(recipient));
    }

    static Criterion<MailAddress> hasSender(MailAddress sender) {
        return new Criterion<>(SENDER, ValueMatcher.SingleValueMatcher.isEquals(sender));
    }

    static Criterion<Boolean> hasAttachment() {
        return hasAttachment(true);
    }

    static Criterion<Boolean> hasNoAttachment() {
        return hasAttachment(false);
    }

    static Criterion<Boolean> hasAttachment(boolean hasAttachment) {
        return new Criterion<>(HAS_ATTACHMENT, ValueMatcher.SingleValueMatcher.isEquals(hasAttachment));
    }

    static StringCriterionFactory subject() {
        return new StringCriterionFactory(SUBJECT);
    }

    static Criterion<List<MailboxId>> containsOriginMailbox(MailboxId mailboxId) {
        return new Criterion<>(ORIGIN_MAILBOXES, ValueMatcher.ListContains.of(mailboxId));
    }
}
