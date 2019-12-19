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

import org.apache.james.util.Host;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.GenericContainer;


public class DockerCassandraRule extends ExternalResource {

    private final DockerCassandra container;
    private final Runnable start;
    private final Runnable stop;
    private final ThrowingConsumer<Boolean> before;
    private boolean allowRestart = false;


    public DockerCassandraRule(DockerCassandra container, Runnable start, Runnable stop, ThrowingConsumer<Boolean> before) {
        this.container = container;
        this.start = start;
        this.stop = stop;
        this.before = before;
    }

    public DockerCassandraRule() {
        this(DockerCassandraSingleton.singleton, DockerCassandraSingleton.singleton::start, () -> {
        }, allowRestart -> {
            if (allowRestart) {
                DockerCassandraSingleton.restartAfterMaxTestsPlayed();
            }
            DockerCassandraSingleton.incrementTestsPlayed();
        });
    }


    public DockerCassandraRule allowRestart() {
        allowRestart = true;
        return this;
    }

    @Override
    protected void before() throws Exception {
        try {
            before.accept(allowRestart);
        } catch (Exception exception) {
            throw exception;
        } catch (Throwable throwable) {
            throw new Exception(throwable);
        }
    }

    public void start() {
        start.run();
    }

    public void stop() {
        stop.run();
    }

    public Host getHost() {
        return container.getHost();
    }

    public String getIp() {
        return container.getIp();
    }

    public int getBindingPort() {
        return container.getBindingPort();
    }

    public GenericContainer<?> getRawContainer() {
        return container.getRawContainer();
    }

    public void pause() {
        container.pause();
    }

    public void unpause() {
        container.unpause();
    }

}
