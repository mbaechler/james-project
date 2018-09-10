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
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MailboxSessionUtil;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.acl.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.exception.BadCredentialsException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.exception.NotAdminException;
import org.apache.james.mailbox.exception.UserDoesNotExistException;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageId.Factory;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.mailbox.model.search.MailboxQuery;
import org.apache.james.mailbox.model.search.PrefixedRegex;
import org.apache.james.mailbox.store.event.DefaultDelegatingMailboxListener;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StoreMailboxManagerTest {
    private static final String CURRENT_USER = "user";
    private static final String CURRENT_USER_PASSWORD = "secret";
    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "adminsecret";
    private static final MailboxId MAILBOX_ID = TestId.of(123);
    private static final String UNKNOWN_USER = "otheruser";
    private static final String BAD_PASSWORD = "badpassword";
    private static final String EMPTY_PREFIX = "";

    private StoreMailboxManager storeMailboxManager;
    private MailboxMapper mockedMailboxMapper;
    private MailboxSession mockedMailboxSession;

    @BeforeEach
    public void setUp() throws MailboxException {
        MailboxSessionMapperFactory mockedMapperFactory = mock(MailboxSessionMapperFactory.class);
        mockedMailboxSession = MailboxSessionUtil.create(CURRENT_USER);
        mockedMailboxMapper = mock(MailboxMapper.class);
        when(mockedMapperFactory.getMailboxMapper(mockedMailboxSession))
            .thenReturn(mockedMailboxMapper);
        Factory messageIdFactory = mock(MessageId.Factory.class);
        FakeAuthenticator authenticator = new FakeAuthenticator();
        authenticator.addUser(CURRENT_USER, CURRENT_USER_PASSWORD);
        authenticator.addUser(ADMIN, ADMIN_PASSWORD);

        DefaultDelegatingMailboxListener delegatingListener = new DefaultDelegatingMailboxListener();
        MailboxEventDispatcher mailboxEventDispatcher = new MailboxEventDispatcher(delegatingListener);

        StoreRightManager storeRightManager = new StoreRightManager(mockedMapperFactory, new UnionMailboxACLResolver(),
                                                                    new SimpleGroupMembershipResolver(), mailboxEventDispatcher);

        StoreMailboxAnnotationManager annotationManager = new StoreMailboxAnnotationManager(mockedMapperFactory, storeRightManager);
        storeMailboxManager = new StoreMailboxManager(mockedMapperFactory, authenticator, FakeAuthorizator.forUserAndAdmin(ADMIN, CURRENT_USER),
                new JVMMailboxPathLocker(), new MessageParser(), messageIdFactory,
                annotationManager, mailboxEventDispatcher, delegatingListener, storeRightManager);
        storeMailboxManager.init();
    }

    @Test
    public void getMailboxShouldThrowWhenUnknownId() throws Exception {
        when(mockedMailboxMapper.findMailboxById(MAILBOX_ID)).thenReturn(null);

        assertThatThrownBy(() -> storeMailboxManager.getMailbox(MAILBOX_ID, mockedMailboxSession)).isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    public void getMailboxShouldReturnMailboxManagerWhenKnownId() throws Exception {
        Mailbox mockedMailbox = mock(Mailbox.class);
        when(mockedMailbox.generateAssociatedPath())
            .thenReturn(MailboxPath.forUser(CURRENT_USER, "mailboxName"));
        when(mockedMailbox.getMailboxId()).thenReturn(MAILBOX_ID);
        when(mockedMailboxMapper.findMailboxById(MAILBOX_ID)).thenReturn(mockedMailbox);

        MessageManager expected = storeMailboxManager.getMailbox(MAILBOX_ID, mockedMailboxSession);

        assertThat(expected.getId()).isEqualTo(MAILBOX_ID);
    }

    @Test
    public void getMailboxShouldReturnMailboxManagerWhenKnownIdAndDifferentCaseUser() throws Exception {
        Mailbox mockedMailbox = mock(Mailbox.class);
        when(mockedMailbox.generateAssociatedPath())
            .thenReturn(MailboxPath.forUser("uSEr", "mailboxName"));
        when(mockedMailbox.getMailboxId()).thenReturn(MAILBOX_ID);
        when(mockedMailboxMapper.findMailboxById(MAILBOX_ID)).thenReturn(mockedMailbox);

        MessageManager expected = storeMailboxManager.getMailbox(MAILBOX_ID, mockedMailboxSession);

        assertThat(expected.getId()).isEqualTo(MAILBOX_ID);
    }

    @Test
    public void getMailboxShouldThrowWhenMailboxDoesNotMatchUserWithoutRight() throws Exception {
        Mailbox mockedMailbox = mock(Mailbox.class);
        when(mockedMailbox.getACL()).thenReturn(new MailboxACL());
        when(mockedMailbox.generateAssociatedPath())
            .thenReturn(MailboxPath.forUser("other.user", "mailboxName"));
        when(mockedMailbox.getMailboxId()).thenReturn(MAILBOX_ID);
        when(mockedMailboxMapper.findMailboxById(MAILBOX_ID)).thenReturn(mockedMailbox);
        when(mockedMailboxMapper.findMailboxByPath(any())).thenReturn(mockedMailbox);

        assertThatThrownBy(() -> storeMailboxManager.getMailbox(MAILBOX_ID, mockedMailboxSession)).isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    public void loginShouldCreateSessionWhenGoodPassword() throws Exception {
        MailboxSession expected = storeMailboxManager.login(CURRENT_USER, CURRENT_USER_PASSWORD);

        assertThat(expected.getUser().asString()).isEqualTo(CURRENT_USER);
    }

    @Test
    public void loginShouldThrowWhenBadPassword() throws Exception {
        assertThatThrownBy(() -> storeMailboxManager.login(CURRENT_USER, BAD_PASSWORD)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void loginAsOtherUserShouldNotCreateUserSessionWhenAdminWithBadPassword() throws Exception {
        assertThatThrownBy(() -> storeMailboxManager.loginAsOtherUser(ADMIN, BAD_PASSWORD, CURRENT_USER)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void loginAsOtherUserShouldNotCreateUserSessionWhenNotAdmin() throws Exception {
        assertThatThrownBy(() -> storeMailboxManager.loginAsOtherUser(CURRENT_USER, CURRENT_USER_PASSWORD, UNKNOWN_USER))
            .isInstanceOf(NotAdminException.class);
    }

    @Test
    public void loginAsOtherUserShouldThrowBadCredentialWhenBadPasswordAndNotAdminUser() throws Exception {
        assertThatThrownBy(() -> storeMailboxManager.loginAsOtherUser(CURRENT_USER, BAD_PASSWORD, CURRENT_USER))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void loginAsOtherUserShouldThrowBadCredentialWhenBadPasswordNotAdminUserAndUnknownUser() throws Exception {
        assertThatThrownBy(() -> storeMailboxManager.loginAsOtherUser(CURRENT_USER, BAD_PASSWORD, UNKNOWN_USER))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void loginAsOtherUserShouldThrowBadCredentialsWhenBadPasswordAndUserDoesNotExists() throws Exception {
        assertThatThrownBy(() -> storeMailboxManager.loginAsOtherUser(ADMIN, BAD_PASSWORD, UNKNOWN_USER))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void loginAsOtherUserShouldNotCreateUserSessionWhenDelegatedUserDoesNotExist() throws Exception {
        assertThatThrownBy(() -> storeMailboxManager.loginAsOtherUser(ADMIN, ADMIN_PASSWORD, UNKNOWN_USER))
            .isInstanceOf(UserDoesNotExistException.class);
    }

    @Test
    public void loginAsOtherUserShouldCreateUserSessionWhenAdminWithGoodPassword() throws Exception {
        MailboxSession expected = storeMailboxManager.loginAsOtherUser(ADMIN, ADMIN_PASSWORD, CURRENT_USER);

        assertThat(expected.getUser().asString()).isEqualTo(CURRENT_USER);
    }

    @Test
    public void getPathLikeShouldReturnUserPathLikeWhenNoPrefixDefined() throws Exception {
        //Given
        MailboxSession session = MailboxSessionUtil.create("user");
        MailboxQuery.Builder testee = MailboxQuery.builder()
            .expression(new PrefixedRegex(EMPTY_PREFIX, "abc", session.getPathDelimiter()));
        //When
        MailboxQuery mailboxQuery = testee.build();

        assertThat(StoreMailboxManager.getPathLike(mailboxQuery, session))
            .isEqualTo(MailboxPath.forUser("user", "abc%"));
    }

    @Test
    public void getPathLikeShouldReturnUserPathLikeWhenPrefixDefined() throws Exception {
        //Given
        MailboxSession session = MailboxSessionUtil.create("user");
        MailboxQuery.Builder testee = MailboxQuery.builder()
            .expression(new PrefixedRegex("prefix.", "abc", session.getPathDelimiter()));

        //When
        MailboxQuery mailboxQuery = testee.build();

        assertThat(StoreMailboxManager.getPathLike(mailboxQuery, session))
            .isEqualTo(MailboxPath.forUser("user", "prefix.abc%"));
    }
}

