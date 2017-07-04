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

package org.apache.james.backends.cassandra;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import com.google.common.collect.ImmutableMap;


public class DockerCassandraRule implements TestRule {

    private static final Logger logger = LoggerFactory.getLogger(DockerCassandraRule.class);

    private static final int CASSANDRA_PORT = 9042;

    private GenericContainer<?> cassandraContainer = new GenericContainer<>("cassandra_3_11_java_8:latest")
        .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                    .withTmpFs(ImmutableMap.of("/var/lib/cassandra", "rw,exec,size=1g")))
        .withExposedPorts(CASSANDRA_PORT)
        .withLogConsumer(outputFrame -> logger.info(outputFrame.getUtf8String()))
        .waitingFor(new CassandraWaitStrategy());


    @Override
    public Statement apply(Statement base, Description description) {
        return cassandraContainer.apply(base, description);
    }

    public void start() {
        cassandraContainer.start();
    }

    public void stop() {
        cassandraContainer.stop();
    }
    
    public String getIp() {
        return cassandraContainer.getContainerIpAddress();
    }

    public int getBindingPort() {
        return cassandraContainer.getMappedPort(CASSANDRA_PORT);
    }
}
