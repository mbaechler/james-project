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

package org.apache.james.mailbox.store;

import static org.apache.james.mailbox.fixture.MailboxFixture.MAILBOX_PATH1;
import static org.apache.james.mailbox.fixture.MailboxFixture.OTHER_USER;
import static org.apache.james.mailbox.fixture.MailboxFixture.THIRD_USER;
import static org.apache.james.mailbox.fixture.MailboxFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MailboxSession.SessionType;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.fixture.MailboxFixture;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.junit.Test;

public abstract class AbstractMessageManagerTest {
    public static final Flags FLAGS = new Flags();

    private static final MessageUid messageUid1 = MessageUid.of(111);
    private static final MessageUid messageUid2 = MessageUid.of(222);

    private MessageManagerTestSystem testSystem;
    private MailboxManager mailboxManager;
    private Mailbox mailbox1;
    private Mailbox mailbox2;
    private Mailbox mailbox3;
    private Mailbox mailbox4;
    private MailboxSession session;
    private MailboxSession otherSession;
    private MailboxSession systemSession;
    private MailboxSession thirdSession;

    protected abstract MessageManagerTestSystem createTestSystem() throws Exception;

    public void setUp() throws Exception {
        session = new MockMailboxSession(USER);
        otherSession = new MockMailboxSession(OTHER_USER);
        thirdSession = new MockMailboxSession(THIRD_USER);
        systemSession = new MockMailboxSession("systemuser", SessionType.System);
        testSystem = createTestSystem();
        mailboxManager = testSystem.getMailboxManager();

        mailbox1 = testSystem.createMailbox(MAILBOX_PATH1, session);
        mailbox2 = testSystem.createMailbox(MailboxFixture.MAILBOX_PATH2, session);
        mailbox3 = testSystem.createMailbox(MailboxFixture.MAILBOX_PATH3, session);
        mailbox4 = testSystem.createMailbox(MailboxFixture.MAILBOX_PATH4, otherSession);
    }

    @Test
    public void getMetadataShouldListUsersAclWhenShared() throws Exception {
        mailboxManager.applyRightsCommand(MAILBOX_PATH1, MailboxACL.command().forUser(OTHER_USER).rights(MailboxACL.Right.Read).asAddition(), session);
        mailboxManager.applyRightsCommand(MAILBOX_PATH1, MailboxACL.command().forUser(THIRD_USER).rights(MailboxACL.Right.Read).asAddition(), session);
        MessageManager messageManager = mailboxManager.getMailbox(MAILBOX_PATH1, session);

        MessageManager.MetaData actual = messageManager.getMetaData(false, session, MessageManager.MetaData.FetchGroup.NO_COUNT);
        assertThat(actual.getACL().getEntries()).containsKeys(MailboxACL.EntryKey.createUser(OTHER_USER), MailboxACL.EntryKey.createUser(THIRD_USER));
    }


    @Test
    public void getMetadataShouldNotExposeOtherUsersWhenSessionIsNotOwner() throws Exception {
        mailboxManager.applyRightsCommand(MAILBOX_PATH1, MailboxACL.command().forUser(OTHER_USER).rights(MailboxACL.Right.Read).asAddition(), session);
        mailboxManager.applyRightsCommand(MAILBOX_PATH1, MailboxACL.command().forUser(THIRD_USER).rights(MailboxACL.Right.Read).asAddition(), session);
        MessageManager messageManager = mailboxManager.getMailbox(MAILBOX_PATH1, session);

        MessageManager.MetaData actual = messageManager.getMetaData(false, otherSession, MessageManager.MetaData.FetchGroup.NO_COUNT);
        assertThat(actual.getACL().getEntries()).containsOnlyKeys(MailboxACL.EntryKey.createUser(OTHER_USER));
    }

}
