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

package org.apache.james.deployment;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.james.jmap.HttpJmapAuthentication;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.utils.IMAPMessageReader;
import org.apache.james.utils.SMTPMessageSender;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.MountableFile;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class DeploymentTest {
    private static final String LOCALHOST = "localhost";
    private static final String SIMPSON = "simpson";
    private static final String HOMER = "homer@" + SIMPSON;
    private static final String HOMER_PASSWORD = "secret";
    private static final String BART = "bart@" + SIMPSON;
    private static final String BART_PASSWORD = "tellnobody";
    private static final int JMAP_PORT = 80;
    private static final int SMTP_PORT = 25;
    private static final int IMAP_PORT = 143;
    private static final int WEBADMIN_PORT = 8000;

    private static final Logger logger = LoggerFactory.getLogger(DeploymentTest.class);

    public Network network = Network.newNetwork();

    public GenericContainer<?> cassandra = new CassandraContainer()
        .withNetwork(network)
        .withNetworkAliases("cassandra");
    
    public GenericContainer<?> elasticsearch = new GenericContainer<>("elasticsearch:2.4.6")
        .withNetwork(network)
        .withNetworkAliases("elasticsearch");

    public GenericContainer<?> james = new GenericContainer<>("linagora/james-project:latest")
        .withExposedPorts(JMAP_PORT, SMTP_PORT, IMAP_PORT, WEBADMIN_PORT)
        .withCopyFileToContainer(MountableFile.forClasspathResource("/webadmin.properties"), "/root/conf/")
        .withCopyFileToContainer(MountableFile.forClasspathResource("/jmap.properties"), "/root/conf/")
        .withCopyFileToContainer(MountableFile.forClasspathResource("/keystore"), "/root/conf/")
        .withCopyFileToContainer(MountableFile.forClasspathResource("/jwt_publickey"), "/root/conf/")
        .withNetwork(network)
        .withLogConsumer(log -> logger.info(log.getUtf8String()))
        .waitingFor(new HttpWaitStrategy().forPort(WEBADMIN_PORT).forStatusCodeMatching(status -> true));

    @Rule
    public RuleChain chain = RuleChain.outerRule(network).around(cassandra).around(elasticsearch).around(james);

    private URIBuilder jmapApi;
    
    @Before
    public void setup() {
        RestAssured.requestSpecification = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(StandardCharsets.UTF_8)))
            .setPort(james.getMappedPort(WEBADMIN_PORT))
            .build();
        jmapApi = new URIBuilder().setScheme("http").setHost(LOCALHOST).setPort(james.getMappedPort(JMAP_PORT));
    }

    @Test
    public void shouldHaveAllServicesResponding() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, URISyntaxException {
        registerDomain();
        registerHomer();
        registerBart();

        sendMessageFromBartToHomer();
        assertImapMessageReceived();

        AccessToken homerAccessToken = HttpJmapAuthentication.authenticateJamesUser(jmapApi, HOMER, HOMER_PASSWORD);
        assertJmapWorks(homerAccessToken);
        assertJmapSearchWork(homerAccessToken);
    }

    private void registerDomain() {
        when()
            .put("/domains/" + SIMPSON)
        .then()
            .statusCode(HTTP_NO_CONTENT);
    }

    private void registerHomer() {
        given()
            .body(String.format("{\"password\": \"%s\"}", HOMER_PASSWORD))
        .when()
            .put("/users/" + HOMER)
        .then()
            .statusCode(HTTP_NO_CONTENT);
    }

    private void registerBart() {
        given()
            .body(String.format("{\"password\": \"%s\"}", BART_PASSWORD))
        .when()
            .put("/users/" + BART)
        .then()
            .statusCode(HTTP_NO_CONTENT);
    }

    private void sendMessageFromBartToHomer()
            throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        try (SMTPMessageSender smtp = new SMTPMessageSender(SIMPSON)) {
            smtp.connect(LOCALHOST, james.getMappedPort(SMTP_PORT))
                .authenticate(BART, BART_PASSWORD)
                .sendMessage(BART, HOMER);
        }
    }

    private void assertImapMessageReceived() throws IOException {
        try (IMAPMessageReader imapMessageReader = new IMAPMessageReader()) {
            imapMessageReader.connect(LOCALHOST, james.getMappedPort(IMAP_PORT))
                .login(HOMER, HOMER_PASSWORD)
                .select("INBOX");

            await().atMost(Duration.TEN_SECONDS).until(imapMessageReader::hasAMessage);
            assertThat(imapMessageReader.readFirstMessage()).contains("FROM: " + BART);
        }
    }

    private void assertJmapWorks(AccessToken homerAccessToken) throws ClientProtocolException, IOException, URISyntaxException {
        Content lastMessageId = Request.Post(jmapApi.setPath("/jmap").build())
            .addHeader("Authorization", homerAccessToken.serialize())
            .bodyString("[[\"getMessageList\", {\"sort\":[\"date desc\"]}, \"#0\"]]", org.apache.http.entity.ContentType.APPLICATION_JSON)
            .execute()
            .returnContent();
        
        JsonAssertions.assertThatJson(lastMessageId.asString(StandardCharsets.UTF_8))
            .inPath("$..messageIds[*]")
            .isArray()
            .hasSize(1);
    }

    private void assertJmapSearchWork(AccessToken homerAccessToken) throws ClientProtocolException, IOException, URISyntaxException {
        Content searchResult = Request.Post(jmapApi.setPath("/jmap").build())
                .addHeader("Authorization", homerAccessToken.serialize())
                .bodyString("[[\"getMessageList\", {\"filter\" : {\"text\": \"content\"}}, \"#0\"]]", org.apache.http.entity.ContentType.APPLICATION_JSON)
                .execute()
                .returnContent();
        
        JsonAssertions.assertThatJson(searchResult.asString(StandardCharsets.UTF_8))
            .inPath("$..messageIds[*]")
            .isArray()
            .hasSize(1);
    }
}
