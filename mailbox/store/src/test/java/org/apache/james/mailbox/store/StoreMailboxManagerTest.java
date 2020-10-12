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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.james.core.Username;
import org.apache.james.mailbox.AttachmentContentLoader;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MailboxSessionUtil;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.acl.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.events.EventBusTestFixture;
import org.apache.james.mailbox.events.InVMEventBus;
import org.apache.james.mailbox.events.MemoryEventDeadLetters;
import org.apache.james.mailbox.events.delivery.InVmEventDelivery;
import org.apache.james.mailbox.exception.BadCredentialsException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.exception.NotAdminException;
import org.apache.james.mailbox.exception.UserDoesNotExistException;
import org.apache.james.mailbox.model.Mailbox;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageId.Factory;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.mailbox.model.search.MailboxQuery;
import org.apache.james.mailbox.model.search.PrefixedRegex;
import org.apache.james.mailbox.store.extractor.DefaultTextExtractor;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.apache.james.mailbox.store.quota.QuotaComponents;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.mailbox.store.search.SimpleMessageSearchIndex;
import org.apache.james.metrics.tests.RecordingMetricFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

interface MailboxManagerContract {
    static final Username CURRENT_USER = Username.of("user");
    static final String CURRENT_USER_PASSWORD = "secret";
    static final Username ADMIN = Username.of("admin");
    static final String ADMIN_PASSWORD = "adminsecret";
    static final Username UNKNOWN_USER = Username.of("otheruser");
    static final String BAD_PASSWORD = "badpassword";
    static final String EMPTY_PREFIX = "";

    MailboxManager testee();

    MailboxSession createSession();

    @Test
    default void getMailboxShouldThrowWhenUnknownId() {
        MailboxManager mailboxManager = testee();
        MailboxSession session = testee().createSystemSession(CURRENT_USER);

        TestId unknownMailboxId = TestId.of(123412);

        assertThatThrownBy(() -> mailboxManager.getMailbox(unknownMailboxId, session))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void getMailboxShouldReturnMailboxManagerWhenKnownId() throws Exception {
        MailboxManager mailboxManager = testee();
        MailboxSession session = testee().createSystemSession(CURRENT_USER);
        MailboxPath inbox = MailboxPath.inbox(session);
        MailboxId mailboxId = mailboxManager.createMailbox(inbox, session).get();

        MessageManager expected = mailboxManager.getMailbox(mailboxId, session);

        assertThat(expected.getId()).isEqualTo(mailboxId);
    }

    @Test
    default void getMailboxShouldReturnMailboxManagerWhenKnownIdAndDifferentCaseUser() throws Exception {
        MailboxManager mailboxManager = testee();
        MailboxSession session = testee().createSystemSession(Username.of("UsEr"));
        MailboxPath inbox = MailboxPath.inbox(session);
        MailboxId mailboxId = mailboxManager.createMailbox(inbox, session).get();

        MailboxSession secondSession = testee().createSystemSession(Username.of("UsEr"));

        MessageManager expected = mailboxManager.getMailbox(mailboxId, secondSession);

        assertThat(expected.getId()).isEqualTo(mailboxId);
    }

    @Test
    default void getMailboxShouldThrowWhenMailboxDoesNotMatchUserWithoutRight() throws MailboxException {
        Username otherUser = Username.of("other.user");
        MailboxSession otherUserSession = testee().createSystemSession(otherUser);
        MailboxManager mailboxManager = testee();
        MailboxPath inbox = MailboxPath.inbox(otherUserSession);
        MailboxId mailboxId = mailboxManager.createMailbox(inbox, otherUserSession).get();

        MailboxSession session = testee().createSystemSession(CURRENT_USER);

        assertThatThrownBy(() -> mailboxManager.getMailbox(mailboxId, session))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    default void loginShouldCreateSessionWhenGoodPassword() throws Exception {
        MailboxSession expected = testee().login(CURRENT_USER, CURRENT_USER_PASSWORD);

        assertThat(expected.getUser()).isEqualTo(CURRENT_USER);
    }

    @Test
    default void loginShouldThrowWhenBadPassword() {
        assertThatThrownBy(() -> testee().login(CURRENT_USER, BAD_PASSWORD))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    default void loginAsOtherUserShouldNotCreateUserSessionWhenAdminWithBadPassword() {
        assertThatThrownBy(() -> testee().loginAsOtherUser(ADMIN, BAD_PASSWORD, CURRENT_USER))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    default void loginAsOtherUserShouldNotCreateUserSessionWhenNotAdmin() {
        assertThatThrownBy(() -> testee().loginAsOtherUser(CURRENT_USER, CURRENT_USER_PASSWORD, UNKNOWN_USER))
            .isInstanceOf(NotAdminException.class);
    }

    @Test
    default void loginAsOtherUserShouldThrowBadCredentialWhenBadPasswordAndNotAdminUser() {
        assertThatThrownBy(() -> testee().loginAsOtherUser(CURRENT_USER, BAD_PASSWORD, CURRENT_USER))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    default void loginAsOtherUserShouldThrowBadCredentialWhenBadPasswordNotAdminUserAndUnknownUser() {
        assertThatThrownBy(() -> testee().loginAsOtherUser(CURRENT_USER, BAD_PASSWORD, UNKNOWN_USER))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    default void loginAsOtherUserShouldThrowBadCredentialsWhenBadPasswordAndUserDoesNotExists() {
        assertThatThrownBy(() -> testee().loginAsOtherUser(ADMIN, BAD_PASSWORD, UNKNOWN_USER))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    default void loginAsOtherUserShouldNotCreateUserSessionWhenDelegatedUserDoesNotExist() {
        assertThatThrownBy(() -> testee().loginAsOtherUser(ADMIN, ADMIN_PASSWORD, UNKNOWN_USER))
            .isInstanceOf(UserDoesNotExistException.class);
    }

    @Test
    default void loginAsOtherUserShouldCreateUserSessionWhenAdminWithGoodPassword() throws Exception {
        MailboxSession expected = testee().loginAsOtherUser(ADMIN, ADMIN_PASSWORD, CURRENT_USER);

        assertThat(expected.getUser()).isEqualTo(CURRENT_USER);
    }

    @Test
    default void getPathLikeShouldReturnUserPathLikeWhenNoPrefixDefined() {
        //Given
        MailboxSession session = MailboxSessionUtil.create(CURRENT_USER);
        MailboxQuery.Builder testee = MailboxQuery.builder()
            .expression(new PrefixedRegex(EMPTY_PREFIX, "abc", session.getPathDelimiter()));
        //When
        MailboxQuery mailboxQuery = testee.build();

        assertThat(MailboxManager.toSingleUserQuery(mailboxQuery, session))
            .isEqualTo(MailboxQuery.builder()
                .namespace(MailboxConstants.USER_NAMESPACE)
                .username(Username.of("user"))
                .expression(new PrefixedRegex(EMPTY_PREFIX, "abc*", session.getPathDelimiter()))
                .build()
                .asUserBound());
    }

    @Test
    default void getPathLikeShouldReturnUserPathLikeWhenPrefixDefined() {
        //Given
        MailboxSession session = MailboxSessionUtil.create(CURRENT_USER);
        MailboxQuery.Builder testee = MailboxQuery.builder()
            .expression(new PrefixedRegex("prefix.", "abc", session.getPathDelimiter()));

        //When
        MailboxQuery mailboxQuery = testee.build();

        assertThat(MailboxManager.toSingleUserQuery(mailboxQuery, session))
            .isEqualTo(MailboxQuery.builder()
                .namespace(MailboxConstants.USER_NAMESPACE)
                .username(Username.of("user"))
                .expression(new PrefixedRegex("prefix.", "abc*", session.getPathDelimiter()))
                .build()
                .asUserBound());
    }
}

