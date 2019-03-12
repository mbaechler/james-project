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

package org.apache.james.backend.rabbitmq;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import com.github.fge.lambdas.Throwing;
import com.google.common.annotations.VisibleForTesting;
import com.rabbitmq.client.Connection;
import reactor.core.publisher.Mono;

public class SimpleConnectionPool implements AutoCloseable {
    private final AtomicReference<Connection> connectionReference;
    private final RabbitMQConnectionFactory connectionFactory;

    @Inject
    @VisibleForTesting
    public SimpleConnectionPool(RabbitMQConnectionFactory factory) {
        this.connectionFactory = factory;
        this.connectionReference = new AtomicReference<>();
    }

    @PreDestroy
    @Override
    public void close() {
        Optional.ofNullable(connectionReference.get())
            .filter(Connection::isOpen)
            .ifPresent(Throwing.<Connection>consumer(Connection::close).orDoNothing());
    }

    public Mono<Connection> getResilientConnection() {
        return Mono.defer(this::getOpenConnection)
            .retryBackoff(100, Duration.ofMillis(100));
    }

    private Mono<Connection> getOpenConnection() {
        Connection previous = connectionReference.get();
        Connection current = Optional.ofNullable(previous)
            .filter(Connection::isOpen)
            .orElseGet(connectionFactory::create);
        boolean updated = connectionReference.compareAndSet(previous, current);
        if (updated) {
            return Mono.just(current);
        } else {
            return Mono.error(new RuntimeException("unable to create and register a new Connection"));
        }
    }
}
