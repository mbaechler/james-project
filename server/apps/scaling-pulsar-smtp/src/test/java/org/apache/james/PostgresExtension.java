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

package org.apache.james;

import java.util.UUID;

import org.apache.james.backends.pulsar.DockerPulsarExtension;
import org.apache.james.backends.pulsar.PulsarConfiguration;
import org.apache.james.modules.data.JPAConfiguration;
import org.apache.pulsar.client.api.PulsarClientException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import com.google.inject.Module;
import com.google.inject.util.Modules;

public class PostgresExtension implements GuiceModuleTestExtension {

    private static final Logger logger = LoggerFactory.getLogger(PostgresExtension.class);

    private static void displayDockerLog(OutputFrame outputFrame) {
        logger.info(outputFrame.getUtf8String().trim());
    }

    private final PostgreSQLContainer container;
    private JPAConfiguration jpaConfiguration;

    public PostgresExtension() {
        container = new PostgreSQLContainer<>("postgres:11")
            .withLogConsumer(PostgresExtension::displayDockerLog);
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws PulsarClientException {
        container.start();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        container.stop();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        //FIXME create pg config to transfer pg url
        var dbName = UUID.randomUUID().toString();
        var result = container.execInContainer("psql",  "-U", container.getUsername(), "-c", "create database \"" + dbName + "\"" );
        logger.warn("execution result : {}, stdout {}, stderr {}",result.getExitCode(), result.getStdout(), result.getStderr());
        container.withDatabaseName(dbName);
        jpaConfiguration = JPAConfiguration.builder()
            .driverName(container.getDriverClassName())
            .driverURL(container.getJdbcUrl())
            .username(container.getUsername())
            .password(container.getPassword())
            .build();
    }

    @Override
    public Module getModule() {
        return Modules.combine(binder -> binder.bind(JPAConfiguration.class)
            .toInstance(jpaConfiguration));
    }
}
