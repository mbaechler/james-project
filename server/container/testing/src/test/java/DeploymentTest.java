import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.apache.http.client.utils.URIBuilder;
import org.apache.james.jmap.HttpJmapAuthentication;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.utils.IMAPMessageReader;
import org.apache.james.utils.SMTPMessageSender;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.MountableFile;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;

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

public class DeploymentTest {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentTest.class);

    public static final int JMAP_PORT = 80;
    public static final int SMTP_PORT = 25;
    public static final int IMAP_PORT = 143;
    public static final int WEBADMIN_PORT = 8000;
    public static final int CASSANDRA_PORT = 9142;


    @Rule
    public Network network = Network.newNetwork();

    @Rule
    public GenericContainer<?> cassandra = new CassandraContainer()
        .withNetwork(network)
        .withNetworkAliases("cassandra");
    
    @Rule
    public GenericContainer<?> james = new JamesContainer("linagora/james-project:latest")
        .withExposedPorts(JMAP_PORT, SMTP_PORT, IMAP_PORT, WEBADMIN_PORT)
        .withCopyFileToContainer(MountableFile.forClasspathResource("/webadmin.properties"), "/root/conf/")
        .withLogConsumer(log -> logger.info(log.getUtf8String()))
        .withNetwork(network)
        .waitingFor(new HttpWaitStrategy().forPort(WEBADMIN_PORT).forStatusCodeMatching(status -> true));

    @Before
    public void setup() {
        RestAssured.requestSpecification = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(StandardCharsets.UTF_8)))
            .setPort(james.getMappedPort(WEBADMIN_PORT))
            .build();
    }

    @Test
    public void shouldHaveAllServicesResponding() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        when()
            .put("/domains/simpson")
        .then()
            .statusCode(HTTP_NO_CONTENT);

        given()
            .body("{\"password\": \"secret\"}")
        .when()
            .put("/users/homer@simpson")
        .then()
            .statusCode(HTTP_NO_CONTENT);

        given()
            .body("{\"password\": \"tellnobody\"}")
        .when()
            .put("/users/bart@simpson")
        .then()
            .statusCode(HTTP_NO_CONTENT);

        try (SMTPMessageSender smtp = new SMTPMessageSender("simpson")) {
            smtp.connect("localhost", james.getMappedPort(SMTP_PORT))
                .authenticate("bart@simpson", "tellnobody")
                .sendMessage("bart@simpson", "homer@simpson");
        }

        try (IMAPMessageReader imapMessageReader = new IMAPMessageReader()
            .connect("localhost", james.getMappedPort(IMAP_PORT))
            .login("homer@simpson", "secret")
            .select("INBOX")) {
            await().atMost(Duration.TEN_SECONDS).until(imapMessageReader::hasAMessage);
            assertThat(imapMessageReader.readFirstMessage()).contains("FROM: bart@simpson");

            assertThat(imapMessageReader.searchMessageContaining("bart")).contains("1");
        }

        URIBuilder jmapApi = new URIBuilder().setHost("localhost").setPort(james.getMappedPort(JMAP_PORT)).setPath("/jmap");
        AccessToken accessToken = HttpJmapAuthentication.authenticateJamesUser(jmapApi, "homer@simpson", "secret");
        assertThat(accessToken).isNull();
    }
}
