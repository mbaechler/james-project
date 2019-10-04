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

package org.apache.james.webadmin.integration;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.RestAssured.with;
import static org.apache.james.webadmin.Constants.JSON_CONTENT_TYPE;
import static org.apache.james.webadmin.Constants.SEPARATOR;
import static org.apache.james.webadmin.vault.routes.DeletedMessagesVaultRoutes.USERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.apache.james.CassandraRabbitMQAwsS3JmapTestRule;
import org.apache.james.DockerCassandraRule;
import org.apache.james.GuiceJamesServer;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionManager;
import org.apache.james.core.User;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.events.Event;
import org.apache.james.mailbox.events.EventDeadLetters;
import org.apache.james.mailbox.events.GenericGroup;
import org.apache.james.mailbox.events.Group;
import org.apache.james.mailbox.events.MailboxListener;
import org.apache.james.mailbox.inmemory.InMemoryId;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.probe.MailboxProbe;
import org.apache.james.mailbox.store.event.EventFactory;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.mailrepository.api.MailRepositoryUrl;
import org.apache.james.modules.MailboxProbeImpl;
import org.apache.james.probe.DataProbe;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.MailRepositoryProbeImpl;
import org.apache.james.utils.WebAdminGuiceProbe;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.routes.AliasRoutes;
import org.apache.james.webadmin.routes.CassandraMappingsRoutes;
import org.apache.james.webadmin.routes.DomainsRoutes;
import org.apache.james.webadmin.routes.ForwardRoutes;
import org.apache.james.webadmin.routes.GroupsRoutes;
import org.apache.james.webadmin.routes.HealthCheckRoutes;
import org.apache.james.webadmin.routes.MailQueueRoutes;
import org.apache.james.webadmin.routes.MailRepositoriesRoutes;
import org.apache.james.webadmin.routes.TasksRoutes;
import org.apache.james.webadmin.routes.UserMailboxesRoutes;
import org.apache.james.webadmin.routes.UserRoutes;
import org.apache.james.webadmin.swagger.routes.SwaggerRoutes;
import org.apache.james.webadmin.vault.routes.DeletedMessagesVaultRoutes;
import org.apache.mailbox.tools.indexer.FullReindexingTask;
import org.apache.mailbox.tools.indexer.MessageIdReIndexingTask;
import org.apache.mailbox.tools.indexer.SingleMessageReindexingTask;
import org.apache.mailbox.tools.indexer.UserReindexingTask;
import org.apache.mailet.base.test.FakeMail;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import javax.mail.Flags;

public class WebAdminServerIntegrationTest {

    private static final String DOMAIN = "domain";
    private static final String USERNAME = "username@" + DOMAIN;
    private static final String USERNAME_2 = "username2@" + DOMAIN;
    private static final String ALIAS_1 = "alias1@" + DOMAIN;
    private static final String ALIAS_2 = "alias2@" + DOMAIN;
    private static final String GROUP = "group@" + DOMAIN;
    private static final String SPECIFIC_DOMAIN = DomainsRoutes.DOMAINS + SEPARATOR + DOMAIN;
    private static final String SPECIFIC_USER = UserRoutes.USERS + SEPARATOR + USERNAME;
    private static final String MAILBOX = "mailbox";
    private static final String SPECIFIC_MAILBOX = SPECIFIC_USER + SEPARATOR + UserMailboxesRoutes.MAILBOXES + SEPARATOR + MAILBOX;
    private static final String VERSION = "/cassandra/version";
    private static final String VERSION_LATEST = VERSION + "/latest";
    private static final String UPGRADE_VERSION = VERSION + "/upgrade";
    private static final String UPGRADE_TO_LATEST_VERSION = UPGRADE_VERSION + "/latest";

    @Rule
    public DockerCassandraRule cassandra = new DockerCassandraRule();

    @Rule
    public CassandraRabbitMQAwsS3JmapTestRule jamesTestRule = CassandraRabbitMQAwsS3JmapTestRule.defaultTestRule();

    private GuiceJamesServer guiceJamesServer;
    private DataProbe dataProbe;
    private MailboxProbe mailboxProbe;

    @Before
    public void setUp() throws Exception {
        guiceJamesServer = jamesTestRule.jmapServer(cassandra.getModule());
        guiceJamesServer.start();
        dataProbe = guiceJamesServer.getProbe(DataProbeImpl.class);
        dataProbe.addDomain(DOMAIN);
        WebAdminGuiceProbe webAdminGuiceProbe = guiceJamesServer.getProbe(WebAdminGuiceProbe.class);

        mailboxProbe = guiceJamesServer.getProbe(MailboxProbeImpl.class);

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminGuiceProbe.getWebAdminPort())
            .build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @After
    public void tearDown() {
        guiceJamesServer.stop();
    }

    @Test
    public void postShouldAddTheGivenDomain() throws Exception {
        when()
            .put(SPECIFIC_DOMAIN)
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(dataProbe.listDomains()).contains(DOMAIN);
    }

    @Test
    public void mailQueueRoutesShouldBeExposed() {
        when()
            .get(MailQueueRoutes.BASE_URL)
        .then()
            .statusCode(HttpStatus.OK_200);
    }

    @Test
    public void mailRepositoriesRoutesShouldBeExposed() {
        when()
            .get(MailRepositoriesRoutes.MAIL_REPOSITORIES)
        .then()
            .statusCode(HttpStatus.OK_200)
            .body("repository", containsInAnyOrder(
                "var/mail/error",
                "var/mail/relay-denied",
                "var/mail/address-error"));
    }

    @Test
    public void gettingANonExistingMailRepositoryShouldNotCreateIt() {
        given()
            .get(MailRepositoriesRoutes.MAIL_REPOSITORIES + "file%3A%2F%2Fvar%2Fmail%2Fcustom");

        when()
            .get(MailRepositoriesRoutes.MAIL_REPOSITORIES)
        .then()
            .statusCode(HttpStatus.OK_200)
            .body("repository", containsInAnyOrder(
                "var/mail/error",
                "var/mail/relay-denied",
                "var/mail/address-error"));
    }

    @Test
    public void deleteShouldRemoveTheGivenDomain() throws Exception {
        when()
            .delete(SPECIFIC_DOMAIN)
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(dataProbe.listDomains()).doesNotContain(DOMAIN);
    }

    @Test
    public void postShouldAddTheUser() throws Exception {
        given()
            .body("{\"password\":\"password\"}")
        .when()
            .put(SPECIFIC_USER)
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(dataProbe.listUsers()).contains(USERNAME);
    }

    @Test
    public void deleteShouldRemoveTheUser() throws Exception {
        dataProbe.addUser(USERNAME, "anyPassword");

        given()
            .body("{\"username\":\"" + USERNAME + "\",\"password\":\"password\"}")
        .when()
            .delete(SPECIFIC_USER)
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(dataProbe.listUsers()).doesNotContain(USERNAME);
    }

    @Test
    public void getUsersShouldDisplayUsers() throws Exception {
        dataProbe.addUser(USERNAME, "anyPassword");

        when()
            .get(UserRoutes.USERS)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(JSON_CONTENT_TYPE)
            .body(is("[{\"username\":\"username@domain\"}]"));
    }

    @Test
    public void putMailboxShouldAddAMailbox() throws Exception {
        dataProbe.addUser(USERNAME, "anyPassword");

        when()
            .put(SPECIFIC_MAILBOX)
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(guiceJamesServer.getProbe(MailboxProbeImpl.class).listUserMailboxes(USERNAME)).containsExactly(MAILBOX);
    }

    @Test
    public void deleteMailboxShouldRemoveAMailbox() throws Exception {
        dataProbe.addUser(USERNAME, "anyPassword");
        guiceJamesServer.getProbe(MailboxProbeImpl.class).createMailbox("#private", USERNAME, MAILBOX);

        when()
            .delete(SPECIFIC_MAILBOX)
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(guiceJamesServer.getProbe(MailboxProbeImpl.class).listUserMailboxes(USERNAME)).isEmpty();
    }

    @Test
    public void getCurrentVersionShouldReturnNullForCurrentVersionAsBeginning() {
        when()
            .get(VERSION)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(JSON_CONTENT_TYPE)
            .body(is("{\"version\":null}"));
    }

    @Test
    public void getLatestVersionShouldReturnTheConfiguredLatestVersion() {
        when()
            .get(VERSION_LATEST)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(JSON_CONTENT_TYPE)
            .body(is("{\"version\":" + CassandraSchemaVersionManager.MAX_VERSION.getValue() + "}"));
    }

    @Test
    public void postShouldDoMigrationAndUpdateCurrentVersion() {
        String taskId = with()
            .body(String.valueOf(CassandraSchemaVersionManager.MAX_VERSION.getValue()))
        .post(UPGRADE_VERSION)
            .jsonPath()
            .get("taskId");

        with()
            .get("/tasks/" + taskId + "/await")
        .then()
            .body("status", is("completed"));

        Awaitility.await()
            .atMost(Duration.TEN_SECONDS)
            .await()
            .untilAsserted(() ->
                when()
                    .get(VERSION)
                .then()
                    .statusCode(HttpStatus.OK_200)
                    .contentType(JSON_CONTENT_TYPE)
                    .body(is("{\"version\":" + CassandraSchemaVersionManager.MAX_VERSION.getValue() + "}")));
    }

    @Test
    public void postShouldDoMigrationAndUpdateToTheLatestVersion() {
        String taskId = with().post(UPGRADE_TO_LATEST_VERSION)
            .jsonPath()
            .get("taskId");

        with()
            .get("/tasks/" + taskId + "/await")
        .then()
            .body("status", is("completed"));

        when()
            .get(VERSION)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(JSON_CONTENT_TYPE)
            .body(is("{\"version\":" + CassandraSchemaVersionManager.MAX_VERSION.getValue() + "}"));
    }

    @Test
    public void addressGroupsEndpointShouldHandleRequests() throws Exception {
        with()
            .put(GroupsRoutes.ROOT_PATH + SEPARATOR + GROUP + SEPARATOR + USERNAME);
        with()
            .put(GroupsRoutes.ROOT_PATH + SEPARATOR + GROUP + SEPARATOR + USERNAME_2);

        List<String> members = when()
            .get(GroupsRoutes.ROOT_PATH + SEPARATOR + GROUP)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(JSON_CONTENT_TYPE)
            .extract()
            .jsonPath()
            .getList(".");
        assertThat(members).containsOnly(USERNAME, USERNAME_2);
    }

    @Test
    public void addressForwardsEndpointShouldListForwardAddresses() throws Exception {
        dataProbe.addUser(USERNAME, "anyPassword");
        dataProbe.addUser(USERNAME_2, "anyPassword");

        with()
            .put(ForwardRoutes.ROOT_PATH + SEPARATOR + USERNAME + "/targets/to1@domain.com");
        with()
            .put(ForwardRoutes.ROOT_PATH + SEPARATOR + USERNAME_2 + "/targets/to2@domain.com");

        List<String> members = when()
            .get(ForwardRoutes.ROOT_PATH)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(JSON_CONTENT_TYPE)
            .extract()
            .jsonPath()
            .getList(".");
        assertThat(members).containsOnly(USERNAME, USERNAME_2);
    }

    @Test
    public void addressAliasesEndpointShouldListAliasesAddresses() {
        with()
            .put(AliasRoutes.ROOT_PATH + SEPARATOR + USERNAME + "/sources/" + ALIAS_1);
        with()
            .put(AliasRoutes.ROOT_PATH + SEPARATOR + USERNAME_2 + "/sources/" + ALIAS_2);

        List<String> members = when()
            .get(AliasRoutes.ROOT_PATH)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(JSON_CONTENT_TYPE)
            .extract()
            .jsonPath()
            .getList(".");
        assertThat(members).containsOnly(USERNAME, USERNAME_2);
    }

    @Test
    public void getSwaggerShouldReturnJsonDataForSwagger() {
        when()
            .get(SwaggerRoutes.SWAGGER_ENDPOINT)
        .then()
            .statusCode(HttpStatus.OK_200)
            .body(containsString("\"swagger\":\"2.0\""))
            .body(containsString("\"info\":{\"description\":\"All the web administration API for JAMES\",\"version\":\"V1.0\",\"title\":\"JAMES Web Admin API\"}"))
            .body(containsString("\"tags\":[\"User's Mailbox\"]"))
            .body(containsString("\"tags\":[\"GlobalQuota\"]"))
            .body(containsString("\"tags\":[\"DomainQuota\"]"))
            .body(containsString("\"tags\":[\"UserQuota\"]"))
            .body(containsString("\"tags\":[\"Domains\"]"))
            .body(containsString("\"tags\":[\"Users\"]"))
            .body(containsString("\"tags\":[\"MailRepositories\"]"))
            .body(containsString("\"tags\":[\"MailQueues\"]"))
            .body(containsString("\"tags\":[\"Address Forwards\"]"))
            .body(containsString("\"tags\":[\"Address Aliases\"]"))
            .body(containsString("\"tags\":[\"Address Groups\"]"))
            .body(containsString("\"tags\":[\"Cassandra Mappings Operations\"]"))
            .body(containsString("{\"name\":\"ReIndexing (mailboxes)\"}"))
            .body(containsString("{\"name\":\"MessageIdReIndexing\"}"));
    }

    @Test
    public void validateHealthChecksShouldReturnOk() {
        when()
            .get(HealthCheckRoutes.HEALTHCHECK)
        .then()
            .statusCode(HttpStatus.OK_200);
    }

    @Test
    public void cassandraMappingsEndpointShouldKeepDataConsistencyWhenDataValid() {
        with()
            .put(AliasRoutes.ROOT_PATH + SEPARATOR + USERNAME + "/sources/" + ALIAS_1);
        with()
            .put(AliasRoutes.ROOT_PATH + SEPARATOR + USERNAME + "/sources/" + ALIAS_2);

        String taskId = with()
            .queryParam("action", "SolveInconsistencies")
            .post(CassandraMappingsRoutes.ROOT_PATH)
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"));

        when()
            .get(AliasRoutes.ROOT_PATH + SEPARATOR + USERNAME)
        .then()
            .contentType(ContentType.JSON)
        .statusCode(HttpStatus.OK_200)
            .body("source", hasItems(ALIAS_1, ALIAS_2));
    }

    @Test
    public void fullReindexShouldSucceedWhenNoMail() {
        String taskId = with()
            .post("/mailboxes?task=reIndex")
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(notNullValue()))
            .body("type", is(FullReindexingTask.FULL_RE_INDEXING.asString()));
    }

    @Test
    public void deleteMailsTasksShouldCompleteWhenSenderIsValid() {
        String firstMailQueue = with()
                .basePath(MailQueueRoutes.BASE_URL)
            .get()
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getString("[0]");

        String taskId = with()
                .basePath(MailQueueRoutes.BASE_URL)
                .param("sender", USERNAME)
            .delete(firstMailQueue + "/mails")
                .jsonPath()
                .getString("taskId");

        given()
                .basePath(TasksRoutes.BASE)
            .when()
            .get(taskId + "/await")
            .then()
                .body("status", is("completed"));
    }

    @Test
    public void reprocessingAllTaskShouldCreateATask() throws Exception {
        String escapedRepositoryPath = with()
                .basePath(MailRepositoriesRoutes.MAIL_REPOSITORIES)
            .get()
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getString("[0].path");

        given()
                .basePath(MailRepositoriesRoutes.MAIL_REPOSITORIES)
                .param("action", "reprocess")
            .when()
            .patch(escapedRepositoryPath + "/mails")
                .then()
                .statusCode(HttpStatus.CREATED_201)
                .header("Location", is(Matchers.notNullValue()))
                .body("taskId", is(Matchers.notNullValue()));
    }

    @Test
    public void reprocessingOneTaskShouldCreateATask() throws Exception {
        MailRepositoryStore mailRepositoryStore = guiceJamesServer.getProbe(MailRepositoryProbeImpl.class).getMailRepositoryStore();
        Stream<MailRepositoryUrl> urls = mailRepositoryStore.getUrls();
        MailRepositoryUrl mailRepositoryUrl = urls.findAny().get();
        MailRepository repository = mailRepositoryStore.get(mailRepositoryUrl).get();

        repository.store(FakeMail.builder()
            .name("name1")
            .mimeMessage(MimeMessageBuilder.mimeMessageBuilder().build())
            .build());

        given()
            .basePath(MailRepositoriesRoutes.MAIL_REPOSITORIES)
            .param("action", "reprocess")
        .when()
        .patch(mailRepositoryUrl.urlEncoded() + "/mails/name1")
        .then()
            .statusCode(HttpStatus.CREATED_201)
            .header("Location", is(Matchers.notNullValue()))
            .body("taskId", is(Matchers.notNullValue()));
    }

    @Test
    public void messageReprocessingShouldReturnTaskDetailsWhenMail() throws Exception {
        MailboxId mailboxId = mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USERNAME, MailboxConstants.INBOX);
        ComposedMessageId composedMessageId = mailboxProbe.appendMessage(
                USERNAME,
                MailboxPath.forUser(USERNAME, MailboxConstants.INBOX),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()),
                new Date(),
                false,
                new Flags());

        String taskId = when()
                .post("/mailboxes/" + mailboxId.serialize() + "/mails/"
                        + composedMessageId.getUid().asLong() + "?task=reIndex")
                .jsonPath()
                .get("taskId");

        given()
                .basePath(TasksRoutes.BASE)
                .when()
                .get(taskId + "/await")
                .then()
                .body("status", is("completed"))
                .body("taskId", is(Matchers.notNullValue()))
                .body("type", is(SingleMessageReindexingTask.MESSAGE_RE_INDEXING.asString()));
    }

    @Test
    public void messageIdReprocessingShouldReturnTaskDetailsWhenMail() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USERNAME, MailboxConstants.INBOX);
        ComposedMessageId composedMessageId = mailboxProbe.appendMessage(
            USERNAME,
            MailboxPath.forUser(USERNAME, MailboxConstants.INBOX),
            new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()),
            new Date(),
            false,
            new Flags());

        String taskId = when()
            .post("/messages/" + composedMessageId.getMessageId().serialize() + "?task=reIndex")
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
            .when()
            .get(taskId + "/await")
            .then()
            .body("status", is("completed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is(MessageIdReIndexingTask.TYPE.asString()));
    }

    @Test
    public void userReindexingShouldReturnTaskDetails() {
        String taskId = with()
                .queryParam("user", USERNAME)
                .queryParam("task", "reIndex")
            .post("/mailboxes")
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
            .when()
            .get(taskId + "/await")
            .then()
            .body("status", is("completed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is(UserReindexingTask.USER_RE_INDEXING.asString()));
    }

    @Test
    public void deletedMessageVaultRestoreShouldCreateATask() throws Exception {
        dataProbe.addUser(USERNAME, "password");
        String query =
            "{" +
                "  \"fieldName\": \"subject\"," +
                "  \"operator\": \"contains\"," +
                "  \"value\": \"subject contains\"" +
                "}";

        String taskId =
            with()
                .basePath(DeletedMessagesVaultRoutes.ROOT_PATH)
                .queryParam("action", "restore")
                .body(query)
            .post(USERS + SEPARATOR + USERNAME)
                .jsonPath()
                .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
        .get(taskId + "/await")
        .then()
            .body("status", is("completed"));
    }

    @Test
    public void deletedMessageVaultExportShouldCreateATask() throws Exception {
        dataProbe.addUser(USERNAME, "password");
        String query = "{" +
            "\"combinator\": \"and\"," +
            "\"criteria\": []" +
            "}";

        with()
            .basePath(DeletedMessagesVaultRoutes.ROOT_PATH)
            .queryParam("action", "export")
            .queryParam("exportTo", "exportTo@james.org")
            .body(query)
        .post(USERS + SEPARATOR + USERNAME)
        .then()
            .statusCode(HttpStatus.CREATED_201)
            .body("taskId", Matchers.notNullValue());
    }

    @Test
    @Ignore("This task needs AdditionalInformation to work, will be fixed later")
    public void fixingReIndexingShouldNotFailWhenNoMail() {
        String taskId = with()
            .post("/mailboxes?task=reIndex")
            .jsonPath()
            .get("taskId");

        with()
            .basePath(TasksRoutes.BASE)
            .get(taskId + "/await");

        String fixingTaskId = with()
            .queryParam("reIndexFailedMessagesOf", taskId)
            .queryParam("task", "reIndex")
        .post("/mailboxes")
        .then()
            .statusCode(HttpStatus.CREATED_201)
            .extract()
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
        .get(fixingTaskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is("ErrorRecoveryIndexation"));
    }

    @Test
    public void postRedeliverAllEventsShouldCreateATask() {
        given()
            .queryParam("action", "reDeliver")
        .when()
        .post("/events/deadLetter")
        .then()
            .statusCode(HttpStatus.CREATED_201);
    }

    @Test
    public void postRedeliverGroupEventsShouldCreateATask() {

        String uuid = "6e0dd59d-660e-4d9b-b22f-0354479f47b4";
        String insertionUuid = "6e0dd59d-660e-4d9b-b22f-0354479f47b7";
        Group group = new GenericGroup("a");
        EventDeadLetters.InsertionId insertionId = EventDeadLetters.InsertionId.of(insertionUuid);
        MailboxListener.MailboxAdded event = EventFactory.mailboxAdded()
            .eventId(Event.EventId.of(uuid))
            .user(User.fromUsername(USERNAME))
            .sessionId(MailboxSession.SessionId.of(452))
            .mailboxId(InMemoryId.of(453))
            .mailboxPath(MailboxPath.forUser(USERNAME, "Important-mailbox"))
            .build();

        guiceJamesServer
            .getInjector()
            .getInstance(EventDeadLetters.class)
            .store(group, event, insertionId)
            .block();

        given()
            .queryParam("action", "reDeliver")
            .when()
            .post("/events/deadLetter/groups/" + group.asString())
            .then()
            .statusCode(HttpStatus.CREATED_201)
            .header("Location", is(Matchers.notNullValue()))
            .body("taskId", is(Matchers.notNullValue()));
    }
}