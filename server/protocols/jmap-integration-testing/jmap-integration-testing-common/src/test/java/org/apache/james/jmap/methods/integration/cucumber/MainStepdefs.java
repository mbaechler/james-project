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

package org.apache.james.jmap.methods.integration.cucumber;

import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;

import org.apache.james.GuiceJamesServer;

import com.google.common.base.Charsets;
import io.restassured.RestAssured;

import cucumber.runtime.java.guice.ScenarioScoped;

@ScenarioScoped
public class MainStepdefs {

    public GuiceJamesServer jmapServer;
    public Runnable awaitMethod = () -> {};

    public void init() throws Exception {
        jmapServer.start();
        RestAssured.port = jmapServer.getJmapPort();
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8));
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
    
    public void tearDown() {
        jmapServer.stop();
    }
}
