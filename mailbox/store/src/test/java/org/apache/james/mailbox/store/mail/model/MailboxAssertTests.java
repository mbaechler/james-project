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

package org.apache.james.mailbox.store.mail.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.junit.jupiter.api.Test;

public class MailboxAssertTests {

    private static final long UID_VALIDITY = 42;
    private static final TestId MAILBOX_ID = TestId.of(24);

    @Test
    public void isEqualToShouldNotFailWithEqualMailbox() {
        SimpleMailbox mailbox1 = new SimpleMailbox(MailboxPath.forUser("user", "name"), UID_VALIDITY);
        SimpleMailbox mailbox2 = new SimpleMailbox(MailboxPath.forUser("user", "name"), UID_VALIDITY);
        mailbox1.setMailboxId(MAILBOX_ID);
        mailbox2.setMailboxId(MAILBOX_ID);
        MailboxAssert.assertThat(mailbox1).isEqualTo(mailbox2);
    }

    @Test
    public void isEqualToShouldFailWithNotEqualNamespace() {
        SimpleMailbox mailbox1 = new SimpleMailbox(MailboxPath.forUser("user", "name"), UID_VALIDITY);
        SimpleMailbox mailbox2 = new SimpleMailbox(new MailboxPath("other_namespace", "user", "name"), UID_VALIDITY);
        mailbox1.setMailboxId(MAILBOX_ID);
        mailbox2.setMailboxId(MAILBOX_ID);
        assertThatThrownBy(() -> MailboxAssert.assertThat(mailbox1).isEqualTo(mailbox2)).isInstanceOf(AssertionError.class);
    }

    @Test
    public void isEqualToShouldFailWithNotEqualUser() {
        SimpleMailbox mailbox1 = new SimpleMailbox(MailboxPath.forUser("user", "name"), UID_VALIDITY);
        SimpleMailbox mailbox2 = new SimpleMailbox(new MailboxPath("namespace", "other_user", "name"), UID_VALIDITY);
        mailbox1.setMailboxId(MAILBOX_ID);
        mailbox2.setMailboxId(MAILBOX_ID);
        assertThatThrownBy(() -> MailboxAssert.assertThat(mailbox1).isEqualTo(mailbox2)).isInstanceOf(AssertionError.class);
    }

    @Test
    public void isEqualToShouldFailWithNotEqualName() {
        SimpleMailbox mailbox1 = new SimpleMailbox(MailboxPath.forUser("user", "name"), UID_VALIDITY);
        SimpleMailbox mailbox2 = new SimpleMailbox(new MailboxPath("namespace", "user", "other_name"), UID_VALIDITY);
        mailbox1.setMailboxId(MAILBOX_ID);
        mailbox2.setMailboxId(MAILBOX_ID);
        assertThatThrownBy(() -> MailboxAssert.assertThat(mailbox1).isEqualTo(mailbox2)).isInstanceOf(AssertionError.class);
    }

    @Test
    public void isEqualToShouldFailWithNotEqualId() {
        SimpleMailbox mailbox1 = new SimpleMailbox(MailboxPath.forUser("user", "name"), UID_VALIDITY);
        SimpleMailbox mailbox2 = new SimpleMailbox(MailboxPath.forUser("user", "name"), UID_VALIDITY);
        mailbox1.setMailboxId(MAILBOX_ID);
        mailbox2.setMailboxId(TestId.of(MAILBOX_ID.id + 1));
        assertThatThrownBy(() -> MailboxAssert.assertThat(mailbox1).isEqualTo(mailbox2)).isInstanceOf(AssertionError.class);
    }

    @Test
    public void isEqualToShouldFailWithNotEqualUidValidity() {
        SimpleMailbox mailbox1 = new SimpleMailbox(MailboxPath.forUser("user", "name"), UID_VALIDITY);
        SimpleMailbox mailbox2 = new SimpleMailbox(MailboxPath.forUser("user", "name"), UID_VALIDITY + 1);
        mailbox1.setMailboxId(MAILBOX_ID);
        mailbox2.setMailboxId(MAILBOX_ID);
        assertThatThrownBy(() -> MailboxAssert.assertThat(mailbox1).isEqualTo(mailbox2)).isInstanceOf(AssertionError.class);
    }
}
