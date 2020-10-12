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

package org.apache.james.adapter.mailbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.james.core.Username;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxExistsException;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.apache.james.mailbox.model.FetchGroup;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.MessageResultIterator;
import org.apache.james.mailbox.model.UidValidity;
import org.apache.james.mailbox.model.search.ExactName;
import org.apache.james.mailbox.model.search.MailboxQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;

public class MailboxManagementTest {

    public static final Username USER = Username.of("user");
    public static final UidValidity UID_VALIDITY = UidValidity.of(10);
    public static final int LIMIT = 1;

    private MailboxManagerManagement mailboxManagerManagement;
    private MailboxSession session;
    private MailboxManager mailboxManager;

    @BeforeEach
    void setUp() throws Exception {
        mailboxManager = InMemoryIntegrationResources.defaultResources().getMailboxManager();

        mailboxManagerManagement = new MailboxManagerManagement();
        mailboxManagerManagement.setMailboxManager(mailboxManager);
        session = mailboxManager.createSystemSession(Username.of("TEST"));
    }

    @Test
    void deleteMailboxesShouldDeleteMailboxes() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, "name"), userSession);
        mailboxManagerManagement.deleteMailboxes(USER.asString());
        assertThat(mailboxManager.list(userSession)).isEmpty();
    }

    @Test
    void deleteMailboxesShouldDeleteInbox() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        mailboxManager.createMailbox(MailboxPath.inbox(USER), userSession);
        mailboxManagerManagement.deleteMailboxes(USER.asString());
        assertThat(mailboxManager.list(userSession)).isEmpty();
    }

    @Test
    void deleteMailboxesShouldDeleteMailboxesChildren() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, "INBOX.test"), userSession);
        mailboxManagerManagement.deleteMailboxes(USER.asString());
        assertThat(mailboxManager.list(userSession)).isEmpty();
    }

    @Test
    void deleteMailboxesShouldNotDeleteMailboxesBelongingToNotPrivateNamespace() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        MailboxPath mailboxPath = new MailboxPath("#top", USER, "name");
        mailboxManager.createMailbox(mailboxPath, userSession);
        mailboxManagerManagement.deleteMailboxes(USER.asString());
        assertThat(mailboxManager.list(userSession)).containsExactly(mailboxPath);
    }

    @Test
    void deleteMailboxesShouldNotDeleteMailboxesBelongingToOtherUsers() throws Exception {
        Username otherUser = Username.of("userbis");
        MailboxSession otherUserSession = mailboxManager.createSystemSession(otherUser);
        MailboxPath mailboxPath = MailboxPath.forUser(Username.of("userbis"), "name");
        mailboxManager.createMailbox(mailboxPath, otherUserSession);
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        mailboxManagerManagement.deleteMailboxes(USER.asString());
        assertThat(mailboxManager.list(otherUserSession)).containsExactly(mailboxPath);
    }

    @Test
    void deleteMailboxesShouldDeleteMailboxesWithEmptyNames() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, ""), userSession);
        mailboxManagerManagement.deleteMailboxes(USER.asString());
        assertThat(mailboxManager.list(userSession)).isEmpty();
    }

    @Test
    void deleteMailboxesShouldThrowOnNullUserName() throws Exception {
        assertThatThrownBy(() -> mailboxManagerManagement.deleteMailboxes(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deleteMailboxesShouldThrowOnEmptyUserName() throws Exception {
        assertThatThrownBy(() -> mailboxManagerManagement.deleteMailboxes(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteMailboxesShouldDeleteMultipleMailboxes() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, "name"), userSession);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, "INBOX"), userSession);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, "INBOX.test"), userSession);
        mailboxManagerManagement.deleteMailboxes(USER.asString());
        assertThat(mailboxManager.list(userSession)).isEmpty();
    }

    @Test
    void createMailboxShouldCreateAMailbox() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, "name"), userSession);
        assertThat(mailboxManager.list(userSession)).hasSize(1);
        assertThat(Flux.from(mailboxManager.search(MailboxQuery.builder().privateNamespace().expression(new ExactName("name")).user(USER).build(), userSession)).toIterable())
            .hasSize(1);
    }

    @Test
    void createMailboxShouldThrowIfMailboxAlreadyExists() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, "name"), userSession);

        assertThatThrownBy(() -> mailboxManagerManagement.createMailbox(MailboxConstants.USER_NAMESPACE, USER.asString(), "name"))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(MailboxExistsException.class);
    }

    @Test
    void createMailboxShouldNotCreateAdditionalMailboxesIfMailboxAlreadyExists() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        MailboxPath mailboxPath = MailboxPath.forUser(USER, "name");
        mailboxManager.createMailbox(mailboxPath, userSession);
        try {
            mailboxManagerManagement.createMailbox(MailboxConstants.USER_NAMESPACE, USER.asString(), "name");
        } catch (RuntimeException e) {}

        assertThat(mailboxManager.list(userSession)).containsExactly(mailboxPath);
    }

    @Test
    void createMailboxShouldThrowOnNullNamespace() {
        assertThatThrownBy(() -> mailboxManagerManagement.createMailbox(null, "a", "a"))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void createMailboxShouldThrowOnNullUser() {
        assertThatThrownBy(() -> mailboxManagerManagement.createMailbox("a", null, "a"))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void createMailboxShouldThrowOnNullName() {
        assertThatThrownBy(() -> mailboxManagerManagement.createMailbox("a", "a", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void createMailboxShouldThrowOnEmptyNamespace() {
        assertThatThrownBy(() -> mailboxManagerManagement.createMailbox("", "a", "a"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createMailboxShouldThrowOnEmptyUser() {
        assertThatThrownBy(() -> mailboxManagerManagement.createMailbox("a", "", "a"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createMailboxShouldThrowOnEmptyName() {
        assertThatThrownBy(() -> mailboxManagerManagement.createMailbox("a", "a", ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listMailboxesShouldReturnUserMailboxes() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        mailboxManager.createMailbox(new MailboxPath("#top", USER, "name1"), userSession);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, "name2"), userSession);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, "name4"), userSession);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, "INBOX"), userSession);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, "INBOX.toto"), userSession);
        Username otherUser = Username.of("other_user");
        MailboxSession otherUserSession = mailboxManager.createSystemSession(otherUser);
        mailboxManager.createMailbox(MailboxPath.forUser(otherUser, "name3"), otherUserSession);

        assertThat(mailboxManagerManagement.listMailboxes(USER.asString())).containsOnly("name2", "name4", "INBOX", "INBOX.toto");
    }

    @Test
    void listMailboxesShouldThrowOnNullUserName() {
        assertThatThrownBy(() -> mailboxManagerManagement.listMailboxes(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void listMailboxesShouldThrowOnEmptyUserName() {
        assertThatThrownBy(() -> mailboxManagerManagement.listMailboxes(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteMailboxShouldDeleteGivenMailbox() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        mailboxManager.createMailbox(MailboxPath.forUser(USER, "name"), userSession);
        mailboxManagerManagement.deleteMailbox(MailboxConstants.USER_NAMESPACE, USER.asString(), "name");
        assertThat(mailboxManager.list(userSession)).isEmpty();
    }

    @Test
    void deleteMailboxShouldNotDeleteGivenMailboxIfWrongNamespace() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        MailboxPath mailboxPath = new MailboxPath("#top", USER, "name");
        mailboxManager.createMailbox(mailboxPath, userSession);
        mailboxManagerManagement.deleteMailbox(MailboxConstants.USER_NAMESPACE, USER.asString(), "name");
        assertThat(mailboxManager.list(userSession)).containsOnly(mailboxPath);
    }

    @Test
    void deleteMailboxShouldNotDeleteGivenMailboxIfWrongUser() throws Exception {
        Username otherUser = Username.of("userbis");
        MailboxSession otherUserSession = mailboxManager.createSystemSession(otherUser);
        MailboxPath mailboxPath = MailboxPath.forUser(otherUser, "name");
        mailboxManager.createMailbox(mailboxPath, otherUserSession);
        mailboxManagerManagement.deleteMailbox(MailboxConstants.USER_NAMESPACE, USER.asString(), "name");
        assertThat(mailboxManager.list(otherUserSession)).containsOnly(mailboxPath);
    }

    @Test
    void deleteMailboxShouldNotDeleteGivenMailboxIfWrongName() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        MailboxPath mailboxPath = MailboxPath.forUser(USER, "wrong_name");
        mailboxManager.createMailbox(mailboxPath, userSession);
        mailboxManagerManagement.deleteMailbox(MailboxConstants.USER_NAMESPACE, USER.asString(), "name");
        assertThat(mailboxManager.list(userSession)).containsOnly(mailboxPath);
    }

    @Test
    void importEmlFileToMailboxShouldImportEmlFileToGivenMailbox() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        MailboxPath mailboxPath = MailboxPath.forUser(USER, "name");
        mailboxManager.createMailbox(mailboxPath, userSession);

        String emlpath = ClassLoader.getSystemResource("eml/frnog.eml").getFile();
        mailboxManagerManagement.importEmlFileToMailbox(MailboxConstants.USER_NAMESPACE, USER.asString(), "name", emlpath);

        assertThat(mailboxManager.getMailbox(mailboxPath, userSession).getMessageCount(userSession)).isEqualTo(1);

        MessageResultIterator iterator = mailboxManager.getMailbox(mailboxPath, userSession)
            .getMessages(MessageRange.all(), FetchGroup.FULL_CONTENT, userSession);

        assertThat(IOUtils.toString(new FileInputStream(new File(emlpath)), StandardCharsets.UTF_8))
                .isEqualTo(IOUtils.toString(iterator.next().getFullContent().getInputStream(), StandardCharsets.UTF_8));
    }

    @Test
    void importEmlFileToMailboxShouldNotImportEmlFileWithWrongPathToGivenMailbox() throws Exception {
        MailboxSession userSession = mailboxManager.createSystemSession(USER);
        MailboxPath mailboxPath = MailboxPath.forUser(USER, "name");
        mailboxManager.createMailbox(mailboxPath, userSession);

        String emlpath = ClassLoader.getSystemResource("eml/frnog.eml").getFile();
        mailboxManagerManagement.importEmlFileToMailbox(MailboxConstants.USER_NAMESPACE, USER.asString(), "name", "wrong_path" + emlpath);

        assertThat(mailboxManager.getMailbox(mailboxPath, userSession).getMessageCount(userSession)).isEqualTo(0);

        MessageResultIterator iterator = mailboxManager.getMailbox(mailboxPath, userSession)
            .getMessages(MessageRange.all(), FetchGroup.FULL_CONTENT, userSession);

        assertThat(iterator.hasNext()).isFalse();
    }


    @Test
    void deleteMailboxShouldThrowOnNullNamespace() {
        assertThatThrownBy(() -> mailboxManagerManagement.deleteMailbox(null, "a", "a"))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deleteMailboxShouldThrowOnNullUser() {
        assertThatThrownBy(() -> mailboxManagerManagement.deleteMailbox("a", null, "a"))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deleteMailboxShouldThrowOnNullName() {
        assertThatThrownBy(() -> mailboxManagerManagement.deleteMailbox("a", "a", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deleteMailboxShouldThrowOnEmptyNamespace() {
        assertThatThrownBy(() -> mailboxManagerManagement.deleteMailbox("", "a", "a"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteMailboxShouldThrowOnEmptyUser() {
        assertThatThrownBy(() -> mailboxManagerManagement.deleteMailbox("a", "", "a"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteMailboxShouldThrowOnEmptyName() {
        assertThatThrownBy(() -> mailboxManagerManagement.deleteMailbox("a", "a", ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

}
