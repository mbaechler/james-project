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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.google.common.base.Preconditions;

public class ContainerLifecycleConfiguration {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private static int DEFAULT_TTL = 20;

        private GenericContainer<?> container;
        private int ttl = DEFAULT_TTL;

        private Builder() {}

        public Builder container(GenericContainer<?> container) {
            this.container = container;
            return this;
        }

        public Builder ttl(int ttl) {
            this.ttl = ttl;
            return this;
        }

        public ContainerLifecycleConfiguration build() {
            Preconditions.checkState(container != null);
            return new ContainerLifecycleConfiguration(container, ttl);
        }
    }

    private final GenericContainer<?> container;
    private final int ttl;
    private AtomicInteger nbTestBeforeRestart;

    public ContainerLifecycleConfiguration(GenericContainer<?> container, int ttl) {
        this.container = container;
        this.ttl = ttl;
        this.nbTestBeforeRestart = new AtomicInteger(ttl);
    }

    private void restartContainer() {
        nbTestBeforeRestart.set(ttl);
        container.stop();
        container.start();
    }

    private boolean needsRestart() {
        return nbTestBeforeRestart.decrementAndGet() <= 0;
    }

    private void restartContainerIfNeeded() {
        if (needsRestart()) {
            restartContainer();
        }
    }

    public TestRule asTestRule() {
        return (base, description) -> new Statement() {
            @Override
            public void evaluate() throws Throwable {
                restartContainerIfNeeded();
                base.evaluate();
            }
        };
    }

}
