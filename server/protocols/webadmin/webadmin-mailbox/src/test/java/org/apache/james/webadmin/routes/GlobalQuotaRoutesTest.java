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

package org.apache.james.webadmin.routes;

import static com.jayway.restassured.RestAssured.given;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.apache.james.mailbox.inmemory.quota.InMemoryPerUserMaxQuotaManager;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.metrics.logger.DefaultMetricFactory;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.service.GlobalQuotaService;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;

public class GlobalQuotaRoutesTest {

    private WebAdminServer webAdminServer;
    private InMemoryPerUserMaxQuotaManager maxQuotaManager;

    @Before
    public void setUp() throws Exception {
        maxQuotaManager = new InMemoryPerUserMaxQuotaManager();
        webAdminServer = WebAdminUtils.createWebAdminServer(
            new DefaultMetricFactory(),
            new GlobalQuotaRoutes(new GlobalQuotaService(maxQuotaManager), new JsonTransformer()));
        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminServer)
            .build();
    }

    @After
    public void stop() {
        webAdminServer.destroy();
    }

    @Test
    public void getCountQuotaCountShouldReturnUnlimitedByDefault() {
        long quota =
            given()
                .get("/quota/count")
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .as(Long.class);

        assertThat(quota).isEqualTo(Quota.UNLIMITED);
    }

    @Test
    public void getCountShouldReturnStoredValue() throws Exception {
        int value = 42;
        maxQuotaManager.setDefaultMaxMessage(value);

        Long actual =
            given()
                .get("/quota/count")
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .as(Long.class);

        assertThat(actual).isEqualTo(value);
    }

    @Test
    public void putCountShouldRejectInvalid() {
        Map<String, Object> errors = given()
            .body("invalid")
            .put("/quota/count")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .contentType(ContentType.JSON)
            .extract()
            .body()
            .jsonPath()
            .getMap(".");

        assertThat(errors)
            .containsEntry("statusCode", HttpStatus.BAD_REQUEST_400)
            .containsEntry("type", "InvalidArgument")
            .containsEntry("message", "Invalid quota. Need to be an integer value greater than 0")
            .containsEntry("cause", "For input string: \"invalid\"");
    }

    @Test
    public void putCountShouldRejectNegative() {
        Map<String, Object> errors = given()
            .body("-1")
            .put("/quota/count")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .contentType(ContentType.JSON)
            .extract()
            .body()
            .jsonPath()
            .getMap(".");

        assertThat(errors)
            .containsEntry("statusCode", HttpStatus.BAD_REQUEST_400)
            .containsEntry("type", "InvalidArgument")
            .containsEntry("message", "Invalid quota. Need to be an integer value greater than 0");
    }

    @Test
    public void putCountShouldAcceptValidValue() throws Exception {
        given()
            .body("42")
            .put("/quota/count")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(maxQuotaManager.getDefaultMaxMessage()).isEqualTo(42);
    }

    @Test
    public void deleteCountShouldSetQuotaToUnlimited() throws Exception {
        maxQuotaManager.setDefaultMaxMessage(42);

        given()
            .delete("/quota/count")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(maxQuotaManager.getDefaultMaxMessage()).isEqualTo(Quota.UNLIMITED);
    }

    @Test
    public void getSizeQuotaCountShouldReturnUnlimitedByDefault() {
        long quota =
            given()
                .get("/quota/size")
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .as(Long.class);

        assertThat(quota).isEqualTo(Quota.UNLIMITED);
    }

    @Test
    public void getSizeShouldReturnStoredValue() throws Exception {
        long value = 42;
        maxQuotaManager.setDefaultMaxStorage(value);


        long quota =
            given()
                .get("/quota/size")
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .as(Long.class);

        assertThat(quota).isEqualTo(value);
    }

    @Test
    public void putSizeShouldRejectInvalid() {
        Map<String, Object> errors = given()
            .body("invalid")
            .put("/quota/size")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .contentType(ContentType.JSON)
            .extract()
            .body()
            .jsonPath()
            .getMap(".");

        assertThat(errors)
            .containsEntry("statusCode", HttpStatus.BAD_REQUEST_400)
            .containsEntry("type", "InvalidArgument")
            .containsEntry("message", "Invalid quota. Need to be an integer value greater than 0")
            .containsEntry("cause", "For input string: \"invalid\"");
    }

    @Test
    public void putSizeShouldRejectNegative() {
        Map<String, Object> errors = given()
            .body("-1")
            .put("/quota/size")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .contentType(ContentType.JSON)
            .extract()
            .body()
            .jsonPath()
            .getMap(".");

        assertThat(errors)
            .containsEntry("statusCode", HttpStatus.BAD_REQUEST_400)
            .containsEntry("type", "InvalidArgument")
            .containsEntry("message", "Invalid quota. Need to be an integer value greater than 0");
    }

    @Test
    public void putSizeShouldAcceptValidValue() throws Exception {
        given()
            .body("42")
            .put("/quota/size")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(maxQuotaManager.getDefaultMaxStorage()).isEqualTo(42);
    }

    @Test
    public void deleteSizeShouldSetQuotaToUnlimited() throws Exception {
        maxQuotaManager.setDefaultMaxStorage(42);

        given()
            .delete("/quota/count")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(maxQuotaManager.getDefaultMaxMessage()).isEqualTo(Quota.UNLIMITED);
    }

    @Test
    public void getQuotaShouldReturnBothWhenValueSpecified() throws Exception {
        int maxStorage = 42;
        int maxMessage = 52;
        maxQuotaManager.setDefaultMaxStorage(maxStorage);
        maxQuotaManager.setDefaultMaxMessage(maxMessage);

        JsonPath jsonPath =
            given()
                .get("/quota")
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath();

        assertThat(jsonPath.getLong("size")).isEqualTo(maxStorage);
        assertThat(jsonPath.getLong("count")).isEqualTo(maxMessage);
    }

    @Test
    public void getQuotaShouldReturnBothDefaultValues() {
        JsonPath jsonPath =
            given()
                .get("/quota")
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath();

        assertThat(jsonPath.getLong("size")).isEqualTo(Quota.UNLIMITED);
        assertThat(jsonPath.getLong("count")).isEqualTo(Quota.UNLIMITED);
    }

    @Test
    public void getQuotaShouldReturnBothWhenNoCount() throws Exception {
        int maxStorage = 42;
        maxQuotaManager.setDefaultMaxStorage(maxStorage);

        JsonPath jsonPath =
            given()
                .get("/quota")
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath();

        assertThat(jsonPath.getLong("size")).isEqualTo(maxStorage);
        assertThat(jsonPath.getLong("count")).isEqualTo(Quota.UNLIMITED);
    }

    @Test
    public void getQuotaShouldReturnBothWhenNoSize() throws Exception {
        int maxMessage = 42;
        maxQuotaManager.setDefaultMaxMessage(maxMessage);


        JsonPath jsonPath =
            given()
                .get("/quota")
                .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath();

        assertThat(jsonPath.getLong("size")).isEqualTo(Quota.UNLIMITED);
        assertThat(jsonPath.getLong("count")).isEqualTo(maxMessage);
    }

    @Test
    public void putQuotaShouldUpdateBothQuota() throws Exception {
        given()
            .body("{\"count\":52,\"size\":42}")
            .put("/quota")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(maxQuotaManager.getDefaultMaxMessage()).isEqualTo(52);
        assertThat(maxQuotaManager.getDefaultMaxStorage()).isEqualTo(42);
    }

    @Test
    public void putQuotaShouldBeAbleToRemoveBothQuota() throws Exception {
        given()
            .body("{\"count\":-1,\"size\":-1}")
            .put("/quota")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(maxQuotaManager.getDefaultMaxMessage()).isEqualTo(Quota.UNLIMITED);
        assertThat(maxQuotaManager.getDefaultMaxStorage()).isEqualTo(Quota.UNLIMITED);
    }

}
