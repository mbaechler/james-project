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

package org.apache.james.jmap.methods.integration;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.hamcrest.collection.IsMapWithSize.anEmptyMap;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.mail.Flags;

import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.JmapAuthentication;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.ResponseSpecification;

public abstract class SetMessagesMethodTest {

    private static final String NAME = "[0][0]";
    private static final String ARGUMENTS = "[0][1]";
    private static final String SECOND_NAME = "[1][0]";
    private static final String SECOND_ARGUMENTS = "[1][1]";
    private static final String USERS_DOMAIN = "domain.tld";
    private static final String NOT_UPDATED = ARGUMENTS + ".notUpdated";

    private ConditionFactory calmlyAwait;

    protected abstract GuiceJamesServer createJmapServer();

    protected abstract void await();

    private AccessToken accessToken;
    private String username;
    private GuiceJamesServer jmapServer;

    @Before
    public void setup() throws Throwable {
        jmapServer = createJmapServer();
        jmapServer.start();
        RestAssured.requestSpecification = new RequestSpecBuilder()
        		.setContentType(ContentType.JSON)
        		.setAccept(ContentType.JSON)
        		.setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8)))
        		.setPort(jmapServer.getJmapPort())
        		.build();
        
        username = "username@" + USERS_DOMAIN;
        String password = "password";
        jmapServer.serverProbe().addDomain(USERS_DOMAIN);
        jmapServer.serverProbe().addUser(username, password);
        jmapServer.serverProbe().createMailbox("#private", username, "inbox");
        accessToken = JmapAuthentication.authenticateJamesUser(username, password);

        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "outbox");
        await();

        Duration slowPacedPollInterval = Duration.FIVE_HUNDRED_MILLISECONDS;
        calmlyAwait = Awaitility.with().pollInterval(slowPacedPollInterval).and().with().pollDelay(slowPacedPollInterval).await();
    }

    @After
    public void teardown() {
        jmapServer.stop();
    }
    
    private String getOutboxId(AccessToken accessToken) {
        return getMailboxId(accessToken, Role.OUTBOX);
    }

    private String getMailboxId(AccessToken accessToken, Role role) {
        return getAllMailboxesIds(accessToken).stream()
                .filter(x -> x.get("role").equals(role.serialize()))
                .map(x -> x.get("id"))
                .findFirst().get();
    }
    
    private List<Map<String, String>> getAllMailboxesIds(AccessToken accessToken) {
        return with()
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMailboxes\", {\"properties\": [\"role\", \"id\"]}, \"#0\"]]")
        .post("/jmap")
                .andReturn()
                .body()
                .jsonPath()
                .getList(ARGUMENTS + ".list");
    }

    @Test
    public void setMessagesShouldReturnErrorNotSupportedWhenRequestContainsNonNullAccountId() throws Exception {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"accountId\": \"1\"}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("Not yet implemented"));
    }

    @Test
    public void setMessagesShouldReturnErrorNotSupportedWhenRequestContainsNonNullIfInState() throws Exception {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"ifInState\": \"1\"}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("Not yet implemented"));
    }

    @Test
    public void setMessagesShouldReturnNotDestroyedWhenUnknownMailbox() throws Exception {

        String unknownMailboxMessageId = username + "|unknown|12345";
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + unknownMailboxMessageId + "\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(ARGUMENTS + ".destroyed", empty())
            .body(ARGUMENTS + ".notDestroyed", hasEntry(equalTo(unknownMailboxMessageId), Matchers.allOf(
                    hasEntry("type", "anErrorOccurred"),
                    hasEntry("description", "An error occurred while deleting message " + unknownMailboxMessageId),
                    hasEntry(equalTo("properties"), isEmptyOrNullString())))
            );
    }

    @Test
    public void setMessagesShouldReturnNotDestroyedWhenNoMatchingMessage() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        String messageId = username + "|mailbox|12345";
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + messageId + "\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(ARGUMENTS + ".destroyed", empty())
            .body(ARGUMENTS + ".notDestroyed", hasEntry(equalTo(messageId), Matchers.allOf(
                    hasEntry("type", "notFound"),
                    hasEntry("description", "The message " + messageId + " can't be found"),
                    hasEntry(equalTo("properties"), isEmptyOrNullString())))
            );
    }

    @Test
    public void setMessagesShouldReturnDestroyedWhenMatchingMessage() throws Exception {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + username + "|mailbox|1\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(ARGUMENTS + ".notDestroyed", anEmptyMap())
            .body(ARGUMENTS + ".destroyed", hasSize(1))
            .body(ARGUMENTS + ".destroyed", contains(username + "|mailbox|1"));
    }

    @Test
    public void setMessagesShouldDeleteMessageWhenMatchingMessage() throws Exception {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        // When
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + username + "|mailbox|1\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        // Then
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessages\", {\"ids\": [\"" + username + "|mailbox|1\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messages"))
            .body(ARGUMENTS + ".list", empty());
    }

    @Test
    public void setMessagesShouldReturnDestroyedNotDestroyWhenMixed() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test3\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        String missingMessageId = username + "|mailbox|4";
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + username + "|mailbox|1\", \"" + missingMessageId + "\", \"" + username + "|mailbox|3\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(ARGUMENTS + ".destroyed", hasSize(2))
            .body(ARGUMENTS + ".notDestroyed", aMapWithSize(1))
            .body(ARGUMENTS + ".destroyed", contains(username + "|mailbox|1", username + "|mailbox|3"))
            .body(ARGUMENTS + ".notDestroyed", hasEntry(equalTo(missingMessageId), Matchers.allOf(
                    hasEntry("type", "notFound"),
                    hasEntry("description", "The message " + missingMessageId + " can't be found")))
            );
    }

    @Test
    public void setMessagesShouldDeleteMatchingMessagesWhenMixed() throws Exception {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test3\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        // When
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + username + "|mailbox|1\", \"" + username + "|mailbox|4\", \"" + username + "|mailbox|3\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        // Then
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessages\", {\"ids\": [\"" + username + "|mailbox|1\", \"" + username + "|mailbox|2\", \"" + username + "|mailbox|3\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messages"))
            .body(ARGUMENTS + ".list", hasSize(1));
    }

    @Test
    public void setMessagesShouldReturnUpdatedIdAndNoErrorWhenIsUnreadPassedToFalse() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        String presumedMessageId = username + "|mailbox|1";

        // When
        given()
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : false } } }, \"#0\"]]", presumedMessageId))
        .when()
            .post("/jmap")
        // Then
        .then()
            .spec(getSetMessagesUpdateOKResponseAssertions(presumedMessageId))
            .log().ifValidationFails();
    }

    private ResponseSpecification getSetMessagesUpdateOKResponseAssertions(String messageId) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectBody(NAME, equalTo("messagesSet"))
                .expectBody(ARGUMENTS + ".updated", hasSize(1))
                .expectBody(ARGUMENTS + ".updated", contains(messageId))
                .expectBody(ARGUMENTS + ".error", isEmptyOrNullString())
                .expectBody(NOT_UPDATED, not(hasKey(messageId)));
        return builder.build();
    }

    @Test
    public void setMessagesShouldMarkAsReadWhenIsUnreadPassedToFalse() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        String presumedMessageId = username + "|mailbox|1";
        given()
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : false } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap");
        // Then
        with()
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
                .post("/jmap")
        .then()
                .statusCode(200)
                .body(NAME, equalTo("messages"))
                .body(ARGUMENTS + ".list", hasSize(1))
                .body(ARGUMENTS + ".list[0].isUnread", equalTo(false))
                .log().ifValidationFails();
    }

    @Test
    public void setMessagesShouldReturnUpdatedIdAndNoErrorWhenIsUnreadPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags(Flags.Flag.SEEN));
        await();

        String presumedMessageId = username + "|mailbox|1";
        given()
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap")
        // Then
        .then()
                .spec(getSetMessagesUpdateOKResponseAssertions(presumedMessageId))
                .log().ifValidationFails();
    }

    @Test
    public void setMessagesShouldMarkAsUnreadWhenIsUnreadPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags(Flags.Flag.SEEN));
        await();

        String presumedMessageId = username + "|mailbox|1";
        given()
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap");
        // Then
        with()
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
                .post("/jmap")
        .then()
                .body(NAME, equalTo("messages"))
                .body(ARGUMENTS + ".list", hasSize(1))
                .body(ARGUMENTS + ".list[0].isUnread", equalTo(true))
                .log().ifValidationFails();
    }


    @Test
    public void setMessagesShouldReturnUpdatedIdAndNoErrorWhenIsFlaggedPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        String presumedMessageId = username + "|mailbox|1";
        given()
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isFlagged\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap")
        // Then
        .then()
                .spec(getSetMessagesUpdateOKResponseAssertions(presumedMessageId))
                .log().ifValidationFails();
    }

    @Test
    public void setMessagesShouldMarkAsFlaggedWhenIsFlaggedPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        String presumedMessageId = username + "|mailbox|1";
        given()
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isFlagged\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap");
        // Then
        with()
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
                .post("/jmap")
        .then()
                .body(NAME, equalTo("messages"))
                .body(ARGUMENTS + ".list", hasSize(1))
                .body(ARGUMENTS + ".list[0].isFlagged", equalTo(true))
                .log().ifValidationFails();
    }

    @Test
    public void setMessagesShouldRejectUpdateWhenPropertyHasWrongType() throws MailboxException {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        await();

        String messageId = username + "|mailbox|1";

        given()
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : \"123\" } } }, \"#0\"]]", messageId))
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(NOT_UPDATED, hasKey(messageId))
                .body(NOT_UPDATED + "[\""+messageId+"\"].type", equalTo("invalidProperties"))
                .body(NOT_UPDATED + "[\""+messageId+"\"].properties[0]", equalTo("isUnread"))
                .body(NOT_UPDATED + "[\""+messageId+"\"].description", equalTo("isUnread: Can not construct instance of java.lang.Boolean from String value '123': only \"true\" or \"false\" recognized\n" +
                        " at [Source: {\"isUnread\":\"123\"}; line: 1, column: 2] (through reference chain: org.apache.james.jmap.model.Builder[\"isUnread\"])"))
                .body(ARGUMENTS + ".updated", hasSize(0));
    }

    @Test
    @Ignore("Jackson json deserializer stops after first error found")
    public void setMessagesShouldRejectUpdateWhenPropertiesHaveWrongTypes() throws MailboxException {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        await();

        String messageId = username + "|mailbox|1";

        given()
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : \"123\", \"isFlagged\" : 456 } } }, \"#0\"]]", messageId))
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(NOT_UPDATED, hasKey(messageId))
                .body(NOT_UPDATED + "[\""+messageId+"\"].type", equalTo("invalidProperties"))
                .body(NOT_UPDATED + "[\""+messageId+"\"].properties", hasSize(2))
                .body(NOT_UPDATED + "[\""+messageId+"\"].properties[0]", equalTo("isUnread"))
                .body(NOT_UPDATED + "[\""+messageId+"\"].properties[1]", equalTo("isFlagged"))
                .body(ARGUMENTS + ".updated", hasSize(0));
    }

    @Test
    public void setMessagesShouldMarkMessageAsAnsweredWhenIsAnsweredPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        String presumedMessageId = username + "|mailbox|1";
        // When
        given()
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isAnswered\" : true } } }, \"#0\"]]", presumedMessageId))
        .when()
                .post("/jmap")
        // Then
        .then()
                .spec(getSetMessagesUpdateOKResponseAssertions(presumedMessageId))
                .log().ifValidationFails();
    }

    @Test
    public void setMessagesShouldMarkAsAnsweredWhenIsAnsweredPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        String presumedMessageId = username + "|mailbox|1";
        given()
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isAnswered\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap");
        // Then
        with()
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
                .post("/jmap")
        .then()
                .body(NAME, equalTo("messages"))
                .body(ARGUMENTS + ".list", hasSize(1))
                .body(ARGUMENTS + ".list[0].isAnswered", equalTo(true))
                .log().ifValidationFails();
    }

    @Test
    public void setMessageShouldReturnNotFoundWhenUpdateUnknownMessage() {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        String nonExistingMessageId = username + "|mailbox|12345";

        given()
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : true } } }, \"#0\"]]", nonExistingMessageId))
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(NOT_UPDATED, hasKey(nonExistingMessageId))
            .body(NOT_UPDATED + "[\""+nonExistingMessageId+"\"].type", equalTo("notFound"))
            .body(NOT_UPDATED + "[\""+nonExistingMessageId+"\"].description", equalTo("message not found"))
            .body(ARGUMENTS + ".updated", hasSize(0));
    }

    @Test
    public void setMessageShouldReturnCreatedMessageWhenSendingMessage() {
        String messageCreationId = "user|inbox|1";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"name\": \"Me\", \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".notCreated", aMapWithSize(0))
                // note that assertions on result message had to be split between
                // string-typed values and boolean-typed value assertions on the same .created entry
                // make sure only one creation has been processed
                .body(ARGUMENTS + ".created", aMapWithSize(1))
                // assert server-set attributes are returned
                .body(ARGUMENTS + ".created", hasEntry(equalTo(messageCreationId), Matchers.allOf(
                        hasEntry(equalTo("id"), not(isEmptyOrNullString())),
                        hasEntry(equalTo("blobId"), not(isEmptyOrNullString())),
                        hasEntry(equalTo("threadId"), not(isEmptyOrNullString())),
                        hasEntry(equalTo("size"), not(isEmptyOrNullString()))
                )))
                // assert that message flags are all unset
                .body(ARGUMENTS + ".created", hasEntry(equalTo(messageCreationId), Matchers.allOf(
                        hasEntry(equalTo("isDraft"), equalTo(false)),
                        hasEntry(equalTo("isUnread"), equalTo(false)),
                        hasEntry(equalTo("isFlagged"), equalTo(false)),
                        hasEntry(equalTo("isAnswered"), equalTo(false))
                )))
                ;
    }

    @Test
    public void setMessageShouldSupportArbitraryMessageId() {
        String messageCreationId = "1717fcd1-603e-44a5-b2a6-1234dbcd5723";
        String fromAddress = username;
        String requestBody = "[" +
            "  [" +
            "    \"setMessages\","+
            "    {" +
            "      \"create\": { \"" + messageCreationId  + "\" : {" +
            "        \"from\": { \"name\": \"Me\", \"email\": \"" + fromAddress + "\"}," +
            "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
            "        \"subject\": \"Thank you for joining example.com!\"," +
            "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
            "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
            "      }}" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(ARGUMENTS + ".notCreated", aMapWithSize(0))
            .body(ARGUMENTS + ".created", aMapWithSize(1));
    }

    @Test
    public void setMessagesShouldCreateMessageInOutboxWhenSendingMessage() throws MailboxException {
        // Given
        String messageCreationId = "user|inbox|1";
        String presumedMessageId = "username@domain.tld|outbox|1";
        String fromAddress = username;
        String messageSubject = "Thank you for joining example.com!";
        String outboxId = getOutboxId(accessToken);
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"name\": \"Me\", \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
                "        \"subject\": \"" + messageSubject + "\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + outboxId + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        with()
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
        .post("/jmap")
        .then()
                .body(NAME, equalTo("messages"))
                .body(ARGUMENTS + ".list", hasSize(1))
                .body(ARGUMENTS + ".list[0].subject", equalTo(messageSubject))
                .body(ARGUMENTS + ".list[0].mailboxIds", contains(outboxId))
                ;
    }

    @Test
    public void setMessagesShouldMoveMessageInSentWhenMessageIsSent() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "sent");
        String sentMailboxId = getMailboxId(accessToken, Role.SENT);

        String fromAddress = username;
        String messageCreationId = "user|inbox|1";
        String messageSubject = "Thank you for joining example.com!";
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"name\": \"Me\", \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
                "        \"subject\": \"" + messageSubject + "\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until( () -> messageHasBeenMovedToSentBox(sentMailboxId));
    }

    private boolean messageHasBeenMovedToSentBox(String sentMailboxId) {
        try {
            with()
                    .header("Authorization", accessToken.serialize())
                    .body("[[\"getMessageList\", {\"fetchMessages\":true, \"filter\":{\"inMailboxes\":[\"" + sentMailboxId + "\"]}}, \"#0\"]]")
            .when()
                    .post("/jmap")
            .then()
                    .statusCode(200)
                    .body(SECOND_NAME, equalTo("messages"))
                    .body(SECOND_ARGUMENTS + ".list", hasSize(1));
            return true;
        } catch(AssertionError e) {
            return false;
        }
    }

    @Test
    public void setMessagesShouldRejectWhenSendingMessageHasNoValidAddress() {
        String messageCreationId = "user|inbox|1";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"name\": \"Me\", \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com@example.com\"}]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
                .when()
                .post("/jmap")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))

                .body(ARGUMENTS + ".notCreated", hasKey(messageCreationId))
                .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].type", equalTo("invalidProperties"))
                .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].description", endsWith("no recipient address set"))
                .body(ARGUMENTS + ".created", aMapWithSize(0));
    }

    @Test
    public void setMessagesShouldRejectWhenSendingMessageHasMissingFrom() {
        String messageCreationId = "user|inbox|1";
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".notCreated", hasKey(messageCreationId))
                .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].type", equalTo("invalidProperties"))
                .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].description", endsWith("'from' address is mandatory"))
                .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].properties", hasSize(1))
                .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].properties", contains("from"))
                .body(ARGUMENTS + ".created", aMapWithSize(0));
    }

    @Test
    public void setMessagesShouldSucceedWhenSendingMessageWithOnlyFromAddress() {
        String messageCreationId = "user|inbox|1";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".created", aMapWithSize(1))
                .body(ARGUMENTS + ".created", hasKey(messageCreationId))
                .body(ARGUMENTS + ".created[\""+messageCreationId+"\"].headers.from", equalTo(fromAddress))
                .body(ARGUMENTS + ".created[\""+messageCreationId+"\"].from.name", equalTo(fromAddress))
                .body(ARGUMENTS + ".created[\""+messageCreationId+"\"].from.email", equalTo(fromAddress));
    }

    @Test
    public void setMessagesShouldSucceedWithHtmlBody() {
        String messageCreationId = "user|inbox|1";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"htmlBody\": \"Hello <i>someone</i>, and thank <b>you</b> for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".created", aMapWithSize(1))
                .body(ARGUMENTS + ".created", hasKey(messageCreationId))
                .body(ARGUMENTS + ".created[\""+messageCreationId+"\"].headers.from", equalTo(fromAddress))
                .body(ARGUMENTS + ".created[\""+messageCreationId+"\"].from.name", equalTo(fromAddress))
                .body(ARGUMENTS + ".created[\""+messageCreationId+"\"].from.email", equalTo(fromAddress));
    }

    @Test
    public void setMessagesShouldMoveToSentWhenSendingMessageWithOnlyFromAddress() {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "sent");
        String sentMailboxId = getMailboxId(accessToken, Role.SENT);

        String messageCreationId = "user|inbox|1";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        // Given
        given()
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");
        // Then
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until( () -> messageHasBeenMovedToSentBox(sentMailboxId));
    }


    @Test
    public void setMessagesShouldRejectWhenSendingMessageHasMissingSubject() {
        String messageCreationId = "user|inbox|1";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"name\": \"Me\", \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".notCreated", hasKey(messageCreationId))
                .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].type", equalTo("invalidProperties"))
                .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].properties", hasSize(1))
                .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].properties", contains("subject"))
                .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].description", endsWith("'subject' is missing"))
                .body(ARGUMENTS + ".created", aMapWithSize(0));
    }


    @Test
    public void setMessagesShouldRejectWhenSendingMessageUseSomeoneElseFromAddress() {
        String messageCreationId = "user|inbox|1";
        String requestBody = "[" +
            "  [" +
            "    \"setMessages\","+
            "    {" +
            "      \"create\": { \"" + messageCreationId  + "\" : {" +
            "        \"from\": { \"name\": \"Me\", \"email\": \"other@domain.tld\"}," +
            "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
            "        \"subject\": \"Thank you for joining example.com!\"," +
            "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
            "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
            "      }}" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(ARGUMENTS + ".notCreated", hasKey(messageCreationId))
            .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].type", equalTo("invalidProperties"))
            .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].properties", hasSize(1))
            .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].properties", contains("from"))
            .body(ARGUMENTS + ".notCreated[\""+messageCreationId+"\"].description", endsWith("Invalid 'from' field. Must be one of username@domain.tld"))
            .body(ARGUMENTS + ".created", aMapWithSize(0));
    }

    @Test
    public void setMessagesShouldDeliverMessageToRecipient() throws Exception {
        // Sender
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "sent");
        // Recipient
        String recipientAddress = "recipient" + "@" + USERS_DOMAIN;
        String password = "password";
        jmapServer.serverProbe().addUser(recipientAddress, password);
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, recipientAddress, "inbox");
        await();
        AccessToken recipientToken = JmapAuthentication.authenticateJamesUser(recipientAddress, password);

        String messageCreationId = "user|inbox|1";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"" + recipientAddress + "\"}]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        // Given
        given()
                .header("Authorization", this.accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until( () -> isAnyMessageFoundInRecipientsMailboxes(recipientToken));
    }

    @Test
    public void setMessagesShouldStripBccFromDeliveredEmail() throws Exception {
        // Sender
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "sent");
        // Recipient
        String recipientAddress = "recipient" + "@" + USERS_DOMAIN;
        String password = "password";
        jmapServer.serverProbe().addUser(recipientAddress, password);
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, recipientAddress, "inbox");
        await();
        AccessToken recipientToken = JmapAuthentication.authenticateJamesUser(recipientAddress, password);

        String messageCreationId = "user|inbox|1";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"recipient\", \"email\": \"" + recipientAddress + "\"}]," +
                "        \"bcc\": [{ \"name\": \"BOB\", \"email\": \"bob@" + USERS_DOMAIN + "\"}]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        // Given
        given()
                .header("Authorization", this.accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until( () -> isAnyMessageFoundInRecipientsMailboxes(recipientToken));
        with()
            .header("Authorization", recipientToken.serialize())
            .body("[[\"getMessageList\", {\"fetchMessages\": true, \"fetchMessageProperties\": [\"bcc\"] }, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(SECOND_NAME, equalTo("messages"))
            .body(SECOND_ARGUMENTS + ".list", hasSize(1)) 
            .body(SECOND_ARGUMENTS + ".list[0].bcc", empty());
    }

    @Test
    public void setMessagesShouldKeepBccInSentMailbox() throws Exception {
        // Sender
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "sent");
        String sentMailboxId = getMailboxId(accessToken, Role.SENT);

        // Recipient
        String recipientAddress = "recipient" + "@" + USERS_DOMAIN;
        String password = "password";
        jmapServer.serverProbe().addUser(recipientAddress, password);
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, recipientAddress, "inbox");
        await();

        String messageCreationId = "user|inbox|1";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"recipient\", \"email\": \"" + recipientAddress + "\"}]," +
                "        \"bcc\": [{ \"name\": \"BOB\", \"email\": \"bob@" + USERS_DOMAIN + "\" }]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        // Given
        given()
                .header("Authorization", this.accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until( () -> messageHasBeenMovedToSentBox(sentMailboxId));
        with()
            .header("Authorization", this.accessToken.serialize())
            .body("[[\"getMessageList\", {\"fetchMessages\":true, \"fetchMessageProperties\": [\"bcc\"], \"filter\":{\"inMailboxes\":[\"" + sentMailboxId + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(SECOND_NAME, equalTo("messages"))
            .body(SECOND_ARGUMENTS + ".list", hasSize(1)) 
            .body(SECOND_ARGUMENTS + ".list[0].bcc", hasSize(1));
    }

    @Test
    public void setMessagesShouldSendMessageToBcc() throws Exception {
        // Sender
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "sent");

        // Recipient
        String recipientAddress = "recipient" + "@" + USERS_DOMAIN;
        String password = "password";
        jmapServer.serverProbe().addUser(recipientAddress, password);
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, recipientAddress, "inbox");

        String bccAddress = "bob" + "@" + USERS_DOMAIN;
        jmapServer.serverProbe().addUser(bccAddress, password);
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, bccAddress, "inbox");
        await();
        AccessToken bccToken = JmapAuthentication.authenticateJamesUser(bccAddress, password);

        String messageCreationId = "user|inbox|1";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"recipient\", \"email\": \"" + recipientAddress + "\"}]," +
                "        \"bcc\": [{ \"name\": \"BOB\", \"email\": \"" + bccAddress + "\" }]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        // Given
        given()
                .header("Authorization", this.accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until( () -> isAnyMessageFoundInRecipientsMailboxes(bccToken));
        with()
            .header("Authorization", bccToken.serialize())
            .body("[[\"getMessageList\", {\"fetchMessages\": true, \"fetchMessageProperties\": [\"bcc\"] }, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(SECOND_NAME, equalTo("messages"))
            .body(SECOND_ARGUMENTS + ".list", hasSize(1)) 
            .body(SECOND_ARGUMENTS + ".list[0].bcc", empty());
    }

    private boolean isAnyMessageFoundInRecipientsMailboxes(AccessToken recipientToken) {
        try {
            with()
                    .header("Authorization", recipientToken.serialize())
                    .body("[[\"getMessageList\", {}, \"#0\"]]")
            .when()
                    .post("/jmap")
            .then()
                    .statusCode(200)
                    .body(NAME, equalTo("messageList"))
                    .body(ARGUMENTS + ".messageIds", hasSize(1));
            return true;
            
        } catch (AssertionError e) {
            return false;
        }
    }


    @Test
    public void setMessagesShouldSendAReadableHtmlMessage() throws Exception {
        // Sender
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "sent");
        // Recipient
        String recipientAddress = "recipient" + "@" + USERS_DOMAIN;
        String password = "password";
        jmapServer.serverProbe().addUser(recipientAddress, password);
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, recipientAddress, "inbox");
        await();
        AccessToken recipientToken = JmapAuthentication.authenticateJamesUser(recipientAddress, password);

        String messageCreationId = "user|inbox|1";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"" + recipientAddress + "\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"htmlBody\": \"Hello <b>someone</b>, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        // Given
        given()
                .header("Authorization", this.accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until( () -> isHtmlMessageReceived(recipientToken));
    }


    @Test
    public void setMessagesWhenSavingToDraftsShouldNotSendMessage() throws Exception {
        String sender = username;
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, sender, "sent");
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, sender, "drafts");
        String recipientAddress = "recipient" + "@" + USERS_DOMAIN;
        String recipientPassword = "password";
        jmapServer.serverProbe().addUser(recipientAddress, recipientPassword);
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, recipientAddress, "inbox");
        await();
        AccessToken recipientToken = JmapAuthentication.authenticateJamesUser(recipientAddress, recipientPassword);

        String senderDraftsMailboxId = getMailboxId(accessToken, Role.DRAFTS);

        String messageCreationId = "creationId";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"" + recipientAddress + "\"}]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + senderDraftsMailboxId + "\"], " +
                "        \"isDraft\": false" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        //We need to wait for an async event to not happen, we couldn't found any
        //robust way to check that.
        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        assertThat(isAnyMessageFoundInRecipientsMailboxes(recipientToken)).isFalse();
    }

    @Test
    public void setMessagesWhenSavingToRegularMailboxShouldNotSendMessage() throws Exception {
        String sender = username;
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, sender, "sent");
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, sender, "drafts");
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, sender, "regular");
        Mailbox regularMailbox = jmapServer.serverProbe().getMailbox(MailboxConstants.USER_NAMESPACE, sender, "regular");
        String recipientAddress = "recipient" + "@" + USERS_DOMAIN;
        String recipientPassword = "password";
        jmapServer.serverProbe().addUser(recipientAddress, recipientPassword);
        await();

        String messageCreationId = "creationId";
        String fromAddress = username;
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"" + recipientAddress + "\"}]," +
                "        \"cc\": [{ \"name\": \"ALICE\"}]," +
                "        \"subject\": \"Thank you for joining example.com!\"," +
                "        \"textBody\": \"Hello someone, and thank you for joining example.com!\"," +
                "        \"mailboxIds\": [\"" + regularMailbox.getMailboxId().serialize() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        String notCreatedMessage = ARGUMENTS + ".notCreated[\""+messageCreationId+"\"]";
        given()
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(ARGUMENTS + ".notCreated", hasKey(messageCreationId))
            .body(notCreatedMessage + ".type", equalTo("invalidProperties"))
            .body(notCreatedMessage + ".description", equalTo("Not yet implemented"))
            .body(ARGUMENTS + ".created", aMapWithSize(0));
    }

    
    private boolean isHtmlMessageReceived(AccessToken recipientToken) {
        try {
            with()
                .header("Authorization", recipientToken.serialize())
                .body("[[\"getMessageList\", {\"fetchMessages\": true, \"fetchMessageProperties\": [\"htmlBody\"]}, \"#0\"]]")
            .post("/jmap")
            .then()
                .statusCode(200)
                .body(SECOND_NAME, equalTo("messages"))
                .body(SECOND_ARGUMENTS + ".list", hasSize(1))
                .body(SECOND_ARGUMENTS + ".list[0].htmlBody", equalTo("Hello <b>someone</b>, and thank you for joining example.com!"))
            ;
            return true;
        } catch(AssertionError e) {
            return false;
        }
    }
    
    @Test
    public void movingAMessageIsNotSupported() throws Exception {
        String newMailboxName = "heartFolder";
        jmapServer.serverProbe().createMailbox("#private", username, newMailboxName);
        Mailbox heartFolder = jmapServer.serverProbe().getMailbox("#private", username, newMailboxName);
        String heartFolderId = heartFolder.getMailboxId().serialize();

        ZonedDateTime dateTime = ZonedDateTime.parse("2014-10-30T14:12:00Z");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath("#private", username, "inbox"),
                new ByteArrayInputStream("Subject: my test subject\r\n\r\ntestmail".getBytes()), Date.from(dateTime.toInstant()), false, new Flags());

        String messageToMoveId = "user|inbox|1";

        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"update\": { \"" + messageToMoveId + "\" : {" +
                "        \"mailboxIds\": [\"" + heartFolderId + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .header("Authorization", this.accessToken.serialize())
                .body(requestBody)
        .when()
                .post("/jmap")
        .then()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(NOT_UPDATED, hasKey(messageToMoveId))
                .body(NOT_UPDATED + "[\""+messageToMoveId+"\"].type", equalTo("invalidProperties"))
                .body(NOT_UPDATED + "[\""+messageToMoveId+"\"].properties[0]", equalTo("mailboxIds"))
                .body(NOT_UPDATED + "[\""+messageToMoveId+"\"].description", equalTo("mailboxIds: moving a message is not supported "
                        + "(through reference chain: org.apache.james.jmap.model.Builder[\"mailboxIds\"])"))
               .body(ARGUMENTS + ".updated", hasSize(0));
    }
    
    @Test
    public void setMessagesShouldReturnAttachmentsNotFoundWhenBlobIdDoesntExist() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "sent");
        await();
        String messageCreationId = "creationId";
        String fromAddress = username;
        String outboxId = getOutboxId(accessToken);
        String requestBody = "[" +
                "  [" +
                "    \"setMessages\","+
                "    {" +
                "      \"create\": { \"" + messageCreationId  + "\" : {" +
                "        \"from\": { \"name\": \"Me\", \"email\": \"" + fromAddress + "\"}," +
                "        \"to\": [{ \"name\": \"BOB\", \"email\": \"someone@example.com\"}]," +
                "        \"subject\": \"Message with a broken blobId\"," +
                "        \"textBody\": \"Test body\"," +
                "        \"mailboxIds\": [\"" + outboxId + "\"], " +
                "        \"attachments\": [" +
                "				{\"blobId\" : \"brokenId1\", \"type\" : \"image/gif\", \"size\" : 1337}," +
                "				{\"blobId\" : \"brokenId2\", \"type\" : \"image/jpeg\", \"size\" : 1337}" +
                " 			]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        String notCreatedPath = ARGUMENTS + ".notCreated[\""+messageCreationId+"\"]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messagesSet"))
            .body(ARGUMENTS + ".notCreated", hasKey(messageCreationId))
            .body(notCreatedPath + ".type", equalTo("invalidProperties"))
            .body(notCreatedPath + ".attachmentsNotFound", contains("brokenId1", "brokenId2"))
            .body(ARGUMENTS + ".created", aMapWithSize(0));
    }
    
    @Test
    public void setMessagesShouldGenerateMultipartMixedMessagesWhenMessageHasAttachment() throws Exception {
    	
    }
}
