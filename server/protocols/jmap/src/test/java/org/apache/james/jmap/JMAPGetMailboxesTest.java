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
package org.apache.james.jmap;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

import org.apache.james.http.jetty.Configuration;
import org.apache.james.http.jetty.JettyHttpServer;
import org.apache.james.jmap.api.AccessTokenManager;
import org.apache.james.jmap.api.ContinuationTokenManager;
import org.apache.james.jmap.methods.GetMailboxesMethod;
import org.apache.james.jmap.methods.ProtocolArgumentsManager;
import org.apache.james.jmap.methods.ProtocolArgumentsManagerImpl;
import org.apache.james.jmap.methods.RequestHandler;
import org.apache.james.jmap.methods.RequestHandlerImpl;
import org.apache.james.user.api.UsersRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public class JMAPGetMailboxesTest {

    private UsersRepository mockedUsersRepository;
    private RequestHandler requestHandler;
    private JettyHttpServer server;

    @Before
    public void setup() throws Exception {
        mockedUsersRepository = mock(UsersRepository.class);
        AccessTokenManager mockedAccessTokenManager = mock(AccessTokenManager.class);
        ContinuationTokenManager mockedContinuationTokenManager = mock(ContinuationTokenManager.class);
        ProtocolArgumentsManager protocolArgumentsManager = new ProtocolArgumentsManagerImpl(); 
        
        requestHandler = new RequestHandlerImpl(ImmutableSet.of(new GetMailboxesMethod(protocolArgumentsManager)));
        JMAPServlet jmapServlet = new JMAPServlet(requestHandler);

        AuthenticationServlet authenticationServlet = new AuthenticationServlet(mockedUsersRepository, mockedContinuationTokenManager, mockedAccessTokenManager);
        
        server = JettyHttpServer.create(
                Configuration.builder()
                .serve("/authentication")
                .with(authenticationServlet)
                .serve("/jmap")
                .with(jmapServlet)
                .randomPort()
                .build());
        
        server.start();

        RestAssured.port = server.getPort();
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset("UTF-8"));
    }

    @After
    public void teardown() throws Exception {
        server.stop();
    }
    
    @Test
    public void getMailboxesShouldErrorNotSupportedWhenRequestContainsNonNullAccountId() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("[[\"getMailboxes\", {\"accountId\": \"1\"}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .content(equalTo("[[\"error\",{\"type\":\"Not yet implemented\"},\"#0\"]]"));
    }

    
    @Test
    public void getMailboxesShouldErrorNotSupportedWhenRequestContainsNonNullIds() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("[[\"getMailboxes\", {\"ids\": []}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .content(equalTo("[[\"error\",{\"type\":\"Not yet implemented\"},\"#0\"]]"));
    }
    
    @Test
    public void getMailboxesShouldErrorNotSupportedWhenRequestContainsNonNullProperties() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("[[\"getMailboxes\", {\"properties\": []}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .content(equalTo("[[\"error\",{\"type\":\"Not yet implemented\"},\"#0\"]]"));
    }
    
    @Test
    public void getMailboxesShouldErrorInvalidArgumentsWhenRequestIsInvalid() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body("[[\"getMailboxes\", {\"ids\": true}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .content(equalTo("[[\"error\",{\"type\":\"invalidArguments\"},\"#0\"]]"));
    }
}
