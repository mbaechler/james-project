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

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AddressResolver;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class RabbitMQConnectionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConnectionFactory.class);

    private class ConnectionCallable implements Callable<Connection> {
        private final ConnectionFactory connectionFactory;
        private Optional<Connection> connection;

        ConnectionCallable(ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            connection = Optional.empty();
        }

        @Override
        public synchronized Connection call() throws Exception {
            if (connection.map(Connection::isOpen).orElse(false)) {
                return connection.get();
            }
            Connection newConnection = connectionFactory.newConnection();
            connection = Optional.of(newConnection);
            return newConnection;
        }
    }

    private final ConnectionFactory connectionFactory;

    private final RabbitMQConfiguration configuration;

    @Inject
    public RabbitMQConnectionFactory(RabbitMQConfiguration rabbitMQConfiguration) {
        this.connectionFactory = from(rabbitMQConfiguration);
        this.configuration = rabbitMQConfiguration;
    }

    private ConnectionFactory from(RabbitMQConfiguration rabbitMQConfiguration) {
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory() {
                @Override
                public Connection newConnection(ExecutorService executor, AddressResolver addressResolver, String clientProvidedName) throws IOException, TimeoutException {
                    Connection connection = super.newConnection(executor, addressResolver, clientProvidedName);
                    connection.addShutdownListener(cause -> LOGGER.error("Shuting down connection", cause));
                    return connection;
                }
            };
            connectionFactory.setUri(rabbitMQConfiguration.getUri());
            return connectionFactory;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Connection create() {
        return connectionMono().block();
    }

    public Mono<Connection> connectionMono() {
        return Mono.fromCallable(new ConnectionCallable(connectionFactory))
            .retryBackoff(configuration.getMaxRetries(), Duration.ofMillis(configuration.getMinDelayInMs()))
            .publishOn(Schedulers.elastic());
    }
}
