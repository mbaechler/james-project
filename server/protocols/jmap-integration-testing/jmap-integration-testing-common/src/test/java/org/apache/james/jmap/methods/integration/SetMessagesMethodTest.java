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

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.mail.Flags;

import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.JmapAuthentication;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public abstract class SetMessagesMethodTest {

    private static final String NAME = "[0][0]";
    private static final String ARGUMENTS = "[0][1]";
    private static final String SECOND_NAME = "[1][0]";
    private static final String SECOND_ARGUMENTS = "[1][1]";
    private static final String USERS_DOMAIN = "domain.tld";

    private ConditionFactory calmlyAwait;

    protected abstract GuiceJamesServer createJmapServer();

    protected abstract void await();

    private AccessToken accessToken;
    private String username;
    private GuiceJamesServer jmapServer;
    private ParseContext jsonPath;

    @Before
    public void setup() throws Throwable {
        jmapServer = createJmapServer();
        jmapServer.start();
        RestAssured.port = jmapServer.getJmapPort();
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8));

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
        

        jsonPath = JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL));
    }

    @After
    public void teardown() {
        jmapServer.stop();
    }
    
    private String getOutboxId() {
        // Find username's outbox (using getMailboxes command on /jmap endpoint)
        return getAllMailboxesIds(accessToken).stream()
                .filter(x -> x.get("role").equals("outbox"))
                .map(x -> x.get("id"))
                .findFirst().get();
    }

    private List<Map<String, String>> getAllMailboxesIds(AccessToken accessToken) {
        InputStream json = with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMailboxes\", {\"properties\": [\"role\", \"id\"]}, \"#0\"]]")
        .post("/jmap")
                .andReturn()
                .asInputStream();
        return jsonPath.parse(json).read(ARGUMENTS + ".list");
    }

    @Test
    public void setMessagesShouldReturnErrorNotSupportedWhenRequestContainsNonNullAccountId() throws Exception {
        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"accountId\": \"1\"}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("error");
        assertThat(jsonDoc.<String>read(ARGUMENTS + ".type")).isEqualTo("Not yet implemented");
    }

    @Test
    public void setMessagesShouldReturnErrorNotSupportedWhenRequestContainsNonNullIfInState() throws Exception {
        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"ifInState\": \"1\"}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("error");
        assertThat(jsonDoc.<String>read(ARGUMENTS + ".type")).isEqualTo("Not yet implemented");
    }

    @Test
    public void setMessagesShouldReturnNotDestroyedWhenUnknownMailbox() throws Exception {

        String unknownMailboxMessageId = username + "|unknown|12345";
        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + unknownMailboxMessageId + "\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".destroyed")).isEmpty();
        assertThat(jsonDoc.<Map<String, Map<String, String>>>read(ARGUMENTS + ".notDestroyed"))
            .containsOnlyKeys(unknownMailboxMessageId);
        assertThat(jsonDoc.<Map<String, Map<String, String>>>read(ARGUMENTS + ".notDestroyed").get(unknownMailboxMessageId))
            .containsEntry("type", "anErrorOccurred")
            .containsEntry("description", "An error occurred while deleting message " + unknownMailboxMessageId)
            .containsEntry("properties",  null);
    }

    @Test
    public void setMessagesShouldReturnNotDestroyedWhenNoMatchingMessage() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        String messageId = username + "|mailbox|12345";
        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + messageId + "\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .extract().asInputStream();
        DocumentContext jsonDoc = JsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".destroyed")).isEmpty();
        assertThat(jsonDoc.<Map<String, Map<String, String>>>read(ARGUMENTS + ".notDestroyed"))
            .containsKey(messageId);
        assertThat(jsonDoc.<Map<String, Map<String, String>>>read(ARGUMENTS + ".notDestroyed").get(messageId))
            .containsEntry("type", "notFound")
            .containsEntry("description", "The message " + messageId + " can't be found")
            .containsEntry("properties", null);
    }

    @Test
    public void setMessagesShouldReturnDestroyedWhenMatchingMessage() throws Exception {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + username + "|mailbox|1\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .extract().asInputStream();
        DocumentContext jsonDoc = JsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<Map<String, Map<String, String>>>read(ARGUMENTS + ".notDestroyed"));
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".destroyed")).containsExactly(username + "|mailbox|1");
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
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + username + "|mailbox|1\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        // Then
        InputStream json = given()
           .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessages\", {\"ids\": [\"" + username + "|mailbox|1\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .extract().asInputStream();
        DocumentContext jsonDoc = JsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messages");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".list")).isEmpty();
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
        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + username + "|mailbox|1\", \"" + missingMessageId + "\", \"" + username + "|mailbox|3\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
            
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".destroyed")).containsExactly(username + "|mailbox|1", username + "|mailbox|3");
        assertThat(jsonDoc.<Map<String, Map<String, String>>>read(ARGUMENTS + ".notDestroyed"))
            .containsOnlyKeys(missingMessageId);
        assertThat(jsonDoc.<Map<String, Map<String, String>>>read(ARGUMENTS + ".notDestroyed").get(missingMessageId))
            .containsEntry("type", "notFound")
            .containsEntry("description", "The message " + missingMessageId + " can't be found");
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
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"setMessages\", {\"destroy\": [\"" + username + "|mailbox|1\", \"" + username + "|mailbox|4\", \"" + username + "|mailbox|3\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        // Then
        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessages\", {\"ids\": [\"" + username + "|mailbox|1\", \"" + username + "|mailbox|2\", \"" + username + "|mailbox|3\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messages");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".list")).hasSize(1);
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
        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : false } } }, \"#0\"]]", presumedMessageId))
        .when()
            .post("/jmap")
        // Then
        .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".updated")).containsExactly(presumedMessageId);
        assertThat(jsonDoc.<String>read(ARGUMENTS + ".error")).isNullOrEmpty();
        assertThat(jsonDoc.<Map<String, String>>read(ARGUMENTS + ".notUpdated")).isEmpty();
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
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : false } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap");

        // Then
        InputStream json = with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
                .post("/jmap")
        .then()
                .statusCode(200)
                .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messages");
        assertThat(jsonDoc.<List<Boolean>>read(ARGUMENTS + ".list[*].isUnread")).containsExactly(false);
    }

    @Test
    public void setMessagesShouldReturnUpdatedIdAndNoErrorWhenIsUnreadPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags(Flags.Flag.SEEN));
        await();

        String presumedMessageId = username + "|mailbox|1";
        InputStream json = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap")
        // Then
        .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".updated")).containsExactly(presumedMessageId);
        assertThat(jsonDoc.<String>read(ARGUMENTS + ".error")).isNullOrEmpty();
        assertThat(jsonDoc.<Map<String, String>>read(ARGUMENTS + ".notUpdated")).isEmpty();
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
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap");
        // Then
        InputStream json = with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
                .post("/jmap")
        .then()
            .statusCode(200)
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messages");
        assertThat(jsonDoc.<List<Boolean>>read(ARGUMENTS + ".list[*].isUnread")).containsExactly(true);
    }


    @Test
    public void setMessagesShouldReturnUpdatedIdAndNoErrorWhenIsFlaggedPassed() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        String presumedMessageId = username + "|mailbox|1";
        InputStream json = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isFlagged\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap")
        // Then
        .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".updated")).containsExactly(presumedMessageId);
        assertThat(jsonDoc.<String>read(ARGUMENTS + ".error")).isNullOrEmpty();
        assertThat(jsonDoc.<Map<String, String>>read(ARGUMENTS + ".notUpdated")).isEmpty();
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
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isFlagged\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap");
        // Then
        InputStream json = with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
                .post("/jmap")
        .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messages");
        assertThat(jsonDoc.<List<Boolean>>read(ARGUMENTS + ".list[*].isFlagged")).containsExactly(true);
    }

    @Test
    public void setMessagesShouldRejectUpdateWhenPropertyHasWrongType() throws MailboxException {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        await();

        String messageId = username + "|mailbox|1";

        InputStream json = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : \"123\" } } }, \"#0\"]]", messageId))
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().asInputStream();
                
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".updated")).isEmpty();
        String notUpdatedMap = ARGUMENTS + ".notUpdated";
        assertThat(jsonDoc.<Map<String, ?>>read(notUpdatedMap)).containsOnlyKeys(messageId);
        String notUpdatedEntry = notUpdatedMap + "['" + messageId + "']";
        assertThat(jsonDoc.<String>read(notUpdatedEntry + ".type")).isEqualTo("invalidProperties");
        assertThat(jsonDoc.<List<String>>read(notUpdatedEntry + ".properties")).containsExactly("isUnread");
        assertThat(jsonDoc.<String>read(notUpdatedEntry + ".description")).isEqualTo(
                "isUnread: Can not construct instance of java.lang.Boolean from String value '123': only \"true\" or \"false\" recognized\n" +
                " at [Source: {\"isUnread\":\"123\"}; line: 1, column: 2] (through reference chain: org.apache.james.jmap.model.Builder[\"isUnread\"])");
    }

    @Test
    @Ignore("Jackson json deserializer stops after first error found")
    public void setMessagesShouldRejectUpdateWhenPropertiesHaveWrongTypes() throws MailboxException {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        await();

        String messageId = username + "|mailbox|1";

        InputStream json = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : \"123\", \"isFlagged\" : 456 } } }, \"#0\"]]", messageId))
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().asInputStream();
                
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<Map<String, ?>>read(ARGUMENTS + ".updated")).isEmpty();
        String notUpdatedMap = ARGUMENTS + ".notUpdated";
        assertThat(jsonDoc.<Map<String, ?>>read(notUpdatedMap)).containsOnlyKeys(messageId);
        String notUpdatedEntry = notUpdatedMap + "." + messageId;
        assertThat(jsonDoc.<String>read(notUpdatedEntry + ".type")).isEqualTo("invalidProperties");
        assertThat(jsonDoc.<List<String>>read(notUpdatedEntry + ".properties")).containsExactly("isUnread", "isFlagged");
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
        InputStream json = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isAnswered\" : true } } }, \"#0\"]]", presumedMessageId))
        .when()
                .post("/jmap")
        // Then
        .then()
            .statusCode(200)
            .log().ifValidationFails()
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".updated")).containsExactly(presumedMessageId);
        assertThat(jsonDoc.<String>read(ARGUMENTS + ".error")).isNullOrEmpty();
        assertThat(jsonDoc.<Map<String, ?>>read(ARGUMENTS + ".notUpdated")).isEmpty();;
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
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isAnswered\" : true } } }, \"#0\"]]", presumedMessageId))
        // When
        .when()
                .post("/jmap");
        // Then
        InputStream json = with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
                .post("/jmap")
        .then()
            .statusCode(200)
            .extract().asInputStream();
        
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messages");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".list")).hasSize(1);
        assertThat(jsonDoc.<Boolean>read(ARGUMENTS + ".list[0].isAnswered")).isTrue();
    }

    @Test
    public void setMessageShouldReturnNotFoundWhenUpdateUnknownMessage() {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        String nonExistingMessageId = username + "|mailbox|12345";

        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"isUnread\" : true } } }, \"#0\"]]", nonExistingMessageId))
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .extract().asInputStream();
            
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".updated")).isEmpty();
        String notUpdatedMap = ARGUMENTS + ".notUpdated";
        assertThat(jsonDoc.<Map<String, ?>>read(notUpdatedMap)).containsOnlyKeys(nonExistingMessageId);
        String notUpdatedEntry = notUpdatedMap + "['" + nonExistingMessageId + "']";
        assertThat(jsonDoc.<String>read(notUpdatedEntry + "type")).isEqualTo("notFound");
        assertThat(jsonDoc.<String>read(notUpdatedEntry + "description")).isEqualTo("message not found");
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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        InputStream json = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<Map<?, ?>>read(ARGUMENTS + ".notCreated")).isEmpty();
        assertThat(jsonDoc.<Map<String, Map<String, ?>>>read(ARGUMENTS + ".created"))
                .containsOnlyKeys(messageCreationId);
        assertThat(jsonDoc.<Map<String, Map<String, ?>>>read(ARGUMENTS + ".created").get(messageCreationId))
            .containsKeys("id", "blobId", "threadId", "size");
        assertThat(jsonDoc.<Map<String, Map<String, Boolean>>>read(ARGUMENTS + ".created").get(messageCreationId))
            .containsEntry("isDraft", false)
            .containsEntry("isUnread", false)
            .containsEntry("isFlagged", false)
            .containsEntry("isAnswered", false);
    }

    @Test
    public void setMessagesShouldCreateMessageInOutboxWhenSendingMessage() throws MailboxException {
        // Given
        String messageCreationId = "user|inbox|1";
        String presumedMessageId = "username@domain.tld|outbox|1";
        String fromAddress = username;
        String messageSubject = "Thank you for joining example.com!";
        String outboxId = getOutboxId();
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

        with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        InputStream json = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessages\", {\"ids\": [\"" + presumedMessageId + "\"]}, \"#0\"]]")
        .post("/jmap")
        .then()
            .statusCode(200)
            .extract().asInputStream();
        
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messages");
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".list")).hasSize(1);
        assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".list[0].mailboxIds")).contains(outboxId);
    }

    @Test
    public void setMessagesShouldMoveMessageInSentWhenMessageIsSent() throws MailboxException {
        // Given
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "sent");
        String sentMailboxId = getAllMailboxesIds(accessToken).stream()
                .filter(x -> x.get("role").equals("sent"))
                .map(x -> x.get("id"))
                .findFirst().get();

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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
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
            InputStream json = with()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .header("Authorization", accessToken.serialize())
                    .body("[[\"getMessageList\", {\"fetchMessages\":true, \"filter\":{\"inMailboxes\":[\"" + sentMailboxId + "\"]}}, \"#0\"]]")
            .when()
                    .post("/jmap")
            .then()
                    .statusCode(200)
            .extract().asInputStream();
            DocumentContext jsonDoc = jsonPath.parse(json);
            assertThat(jsonDoc.<String>read(SECOND_NAME)).isEqualTo("messages");
            assertThat(jsonDoc.<List<String>>read(SECOND_ARGUMENTS + ".list")).hasSize(1);
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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .extract().asInputStream();
        
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<Map<?, ?>>read(ARGUMENTS + ".created")).isEmpty();
        String notCreatedMap = ARGUMENTS + ".notCreated";
        assertThat(jsonDoc.<Map<String, ?>>read(notCreatedMap)).containsOnlyKeys(messageCreationId);
        String notCreatedEntry = notCreatedMap + "." + messageCreationId;
        assertThat(jsonDoc.<String>read(notCreatedEntry + ".type")).isEqualTo("invalidProperties");
        assertThat(jsonDoc.<String>read(notCreatedEntry + ".description")).endsWith("no recipient address set");
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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        InputStream json = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().asInputStream();
            
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<Map<?, ?>>read(ARGUMENTS + ".created")).isEmpty();
        String notCreatedMap = ARGUMENTS + ".notCreated";
        assertThat(jsonDoc.<Map<String, ?>>read(notCreatedMap)).containsOnlyKeys(messageCreationId);
        String notCreatedEntry = notCreatedMap + "." + messageCreationId;
        assertThat(jsonDoc.<String>read(notCreatedEntry + ".type")).isEqualTo("invalidProperties");
        assertThat(jsonDoc.<String>read(notCreatedEntry + ".description")).endsWith("'from' address is mandatory");
        assertThat(jsonDoc.<List<String>>read(notCreatedEntry + ".properties")).containsExactly("from");
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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        InputStream json = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().asInputStream();
                
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        String createdMap = ARGUMENTS + ".created";
        assertThat(jsonDoc.<Map<String, ?>>read(createdMap)).containsOnlyKeys(messageCreationId);
        String notCreatedEntry = createdMap + "." + messageCreationId;
        assertThat(jsonDoc.<String>read(notCreatedEntry + ".headers.from")).isEqualTo(fromAddress);
        assertThat(jsonDoc.<String>read(notCreatedEntry + ".from.name")).isEqualTo(fromAddress);
        assertThat(jsonDoc.<String>read(notCreatedEntry + ".from.email")).isEqualTo(fromAddress);
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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        InputStream json = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().asInputStream();
        DocumentContext jsonDoc = JsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<Map<String, Map<String, String>>>read(ARGUMENTS + ".created")).hasSize(1).containsKey(messageCreationId);
        String createdEntry = ARGUMENTS + ".created." + messageCreationId;
        assertThat(jsonDoc.<String>read(createdEntry + ".headers.from")).isEqualTo(fromAddress);
        assertThat(jsonDoc.<String>read(createdEntry + ".from.name")).isEqualTo(fromAddress);
        assertThat(jsonDoc.<String>read(createdEntry + ".from.email")).isEqualTo(fromAddress);
    }

    @Test
    public void setMessagesShouldMoveToSentWhenSendingMessageWithOnlyFromAddress() {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "sent");
        String sentMailboxId = getAllMailboxesIds(accessToken).stream()
                .filter(x -> x.get("role").equals("sent"))
                .map(x -> x.get("id"))
                .findFirst().get();

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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        // Given
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        InputStream json = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body(requestBody)
        .when()
                .post("/jmap")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().asInputStream();
                
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<Map<String, ?>>read(ARGUMENTS + ".created")).isEmpty();
        String notCreatedMap = ARGUMENTS + ".notCreated";
        assertThat(jsonDoc.<Map<String, ?>>read(notCreatedMap)).containsOnlyKeys(messageCreationId);
        String notCreatedEntry = notCreatedMap + "." + messageCreationId;
        assertThat(jsonDoc.<String>read(notCreatedEntry + ".type")).isEqualTo("invalidProperties");
        assertThat(jsonDoc.<List<String>>read(notCreatedEntry + ".properties")).containsExactly("subject");
        assertThat(jsonDoc.<String>read(notCreatedEntry + ".description")).endsWith("'subject' is missing");
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
            "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
            "      }}" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]";

        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .extract().asInputStream();
                
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messagesSet");
        assertThat(jsonDoc.<Map<String, ?>>read(ARGUMENTS + ".created")).isEmpty();
        String notCreatedMap = ARGUMENTS + ".notCreated";
        assertThat(jsonDoc.<Map<String, ?>>read(notCreatedMap)).containsOnlyKeys(messageCreationId);
        String notCreatedEntry = notCreatedMap + "." + messageCreationId;
        assertThat(jsonDoc.<String>read(notCreatedEntry + ".type")).isEqualTo("invalidProperties");
        assertThat(jsonDoc.<List<String>>read(notCreatedEntry + ".properties")).containsExactly("from");
        assertThat(jsonDoc.<String>read(notCreatedEntry + ".description")).endsWith("Invalid 'from' field. Must be one of [username@domain.tld]");
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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        // Given
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        // Given
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", this.accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until( () -> isAnyMessageFoundInRecipientsMailboxes(recipientToken));
        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", recipientToken.serialize())
            .body("[[\"getMessageList\", {\"fetchMessages\": true, \"fetchMessageProperties\": [\"bcc\"] }, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .extract().asInputStream();

        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(SECOND_NAME)).isEqualTo("messages");
        assertThat(jsonDoc.<List<String>>read(SECOND_ARGUMENTS + ".list")).hasSize(1);
        assertThat(jsonDoc.<List<String>>read(SECOND_ARGUMENTS + ".list[0].bcc")).isEmpty();
    }

    @Test
    public void setMessagesShouldKeepBccInSentMailbox() throws Exception {
        // Sender
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "sent");
        String sentMailboxId = getAllMailboxesIds(accessToken).stream()
                .filter(x -> x.get("role").equals("sent"))
                .map(x -> x.get("id"))
                .findFirst().get();

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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        // Given
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", this.accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until( () -> messageHasBeenMovedToSentBox(sentMailboxId));
        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body("[[\"getMessageList\", {\"fetchMessages\":true, \"fetchMessageProperties\": [\"bcc\"], \"filter\":{\"inMailboxes\":[\"" + sentMailboxId + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(SECOND_NAME)).isEqualTo("messages");
        assertThat(jsonDoc.<List<String>>read(SECOND_ARGUMENTS + ".list")).hasSize(1);
        assertThat(jsonDoc.<List<String>>read(SECOND_ARGUMENTS + ".list[0].bcc")).hasSize(1);
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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        // Given
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", this.accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until( () -> isAnyMessageFoundInRecipientsMailboxes(bccToken));
        InputStream json = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", bccToken.serialize())
            .body("[[\"getMessageList\", {\"fetchMessages\": true, \"fetchMessageProperties\": [\"bcc\"] }, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .extract().asInputStream();
        DocumentContext jsonDoc = jsonPath.parse(json);
        assertThat(jsonDoc.<String>read(SECOND_NAME)).isEqualTo("messages");
        assertThat(jsonDoc.<List<String>>read(SECOND_ARGUMENTS + ".list")).hasSize(1);
        assertThat(jsonDoc.<List<String>>read(SECOND_ARGUMENTS + ".list[0].bcc")).isEmpty();
    }

    private boolean isAnyMessageFoundInRecipientsMailboxes(AccessToken recipientToken) {
        try {
            InputStream json = with()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .header("Authorization", recipientToken.serialize())
                    .body("[[\"getMessageList\", {}, \"#0\"]]")
            .when()
                    .post("/jmap")
            .then()
                    .statusCode(200)
                    .extract().asInputStream();
            DocumentContext jsonDoc = jsonPath.parse(json);
            assertThat(jsonDoc.<String>read(NAME)).isEqualTo("messageList");
            assertThat(jsonDoc.<List<String>>read(ARGUMENTS + ".messageIds")).hasSize(1);
            return true;
        } catch(AssertionError e) {
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
                "        \"mailboxIds\": [\"" + getOutboxId() + "\"]" +
                "      }}" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        // Given
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", this.accessToken.serialize())
                .body(requestBody)
        // When
        .when()
                .post("/jmap");

        // Then
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until( () -> isHtmlMessageReceived(recipientToken));
    }

    private boolean isHtmlMessageReceived(AccessToken recipientToken) {
        try {
            InputStream json = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", recipientToken.serialize())
                .body("[[\"getMessageList\", {\"fetchMessages\": true, \"fetchMessageProperties\": [\"htmlBody\"]}, \"#0\"]]")
            .post("/jmap")
            .then()
                .statusCode(200)
                .extract().asInputStream();
            DocumentContext jsonDoc = jsonPath.parse(json);
            assertThat(jsonDoc.<String>read(SECOND_NAME)).isEqualTo("messages");
            assertThat(jsonDoc.<List<String>>read(SECOND_ARGUMENTS + ".list")).hasSize(1);
            assertThat(jsonDoc.<String>read(SECOND_ARGUMENTS + ".list[0].htmlBody"))
                .isEqualTo("Hello <b>someone</b>, and thank you for joining example.com!");
            return true;
        } catch(AssertionError e) {
            return false;
        }
    }
}
