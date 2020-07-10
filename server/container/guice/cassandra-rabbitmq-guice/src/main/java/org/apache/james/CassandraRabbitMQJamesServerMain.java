/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.james;

import static org.apache.james.CassandraJamesServerMain.REQUIRE_TASK_MANAGER_MODULE;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.apache.james.modules.DistributedTaskManagerModule;
import org.apache.james.modules.DistributedTaskSerializationModule;
import org.apache.james.modules.blobstore.BlobStoreCacheModulesChooser;
import org.apache.james.modules.blobstore.BlobStoreConfiguration;
import org.apache.james.modules.blobstore.BlobStoreModulesChooser;
import org.apache.james.modules.event.RabbitMQEventBusModule;
import org.apache.james.modules.rabbitmq.RabbitMQModule;
import org.apache.james.modules.server.JMXServerModule;
import org.apache.james.server.core.configuration.Configuration;

import com.google.inject.Module;
import com.google.inject.util.Modules;

public class CassandraRabbitMQJamesServerMain implements JamesServerMain {

    public static class Server {

        public interface RequiresConfiguration {
            RequiresBlobStore configuration(UnaryOperator<Configuration.Builder> builder);
        }

        public interface RequiresBlobStore {
            Builder blobStore(Function<BlobStoreConfiguration.Builder, BlobStoreConfiguration> configuration);
        }

        public static class Builder {

            private final CassandraRabbitMQJamesConfiguration configuration;
            private GuiceJamesServer server;

            Builder(CassandraRabbitMQJamesConfiguration configuration) {
                this.configuration = configuration;
                this.server = GuiceJamesServer.forConfiguration(configuration.basicConfiguration())
                    .combineWith(MODULES)
                    .combineWith(BlobStoreModulesChooser.chooseModules(configuration.blobStoreConfiguration()))
                    .combineWith(BlobStoreCacheModulesChooser.chooseModules(configuration.blobStoreConfiguration()));
            }

            public Builder customize(UnaryOperator<GuiceJamesServer> modifier) {
                server = modifier.apply(server);
                return this;
            }

            public GuiceJamesServer build() {
                return server;
            }
        }
    }

    public static Server.Builder builder(CassandraRabbitMQJamesConfiguration configuration) {
        return new Server.Builder(
            new CassandraRabbitMQJamesConfiguration(
                configuration.basicConfiguration(),
                configuration.blobStoreConfiguration()));
    }

    public static Server.Builder builder(Configuration configuration, BlobStoreConfiguration blobStoreConfiguration) {
        return new Server.Builder(new CassandraRabbitMQJamesConfiguration(configuration, blobStoreConfiguration));
    }

    public static Server.RequiresConfiguration builder() {
        return configurationBuilder -> blobStoreConfiguration ->
            builder(
                configurationBuilder.apply(Configuration.builder()).build(),
                blobStoreConfiguration.apply(BlobStoreConfiguration.builder()));
    }

    protected static final Module MODULES =
        Modules
            .override(Modules.combine(REQUIRE_TASK_MANAGER_MODULE, new DistributedTaskManagerModule()))
            .with(new RabbitMQModule(), new RabbitMQEventBusModule(), new DistributedTaskSerializationModule());

    public static void main(String[] args) throws Exception {
        Configuration configuration = Configuration.builder()
            .useWorkingDirectoryEnvProperty()
            .build();
        BlobStoreConfiguration blobStoreConfiguration = BlobStoreConfiguration.parse(configuration);

        LOGGER.info("Loading configuration {}", configuration.toString());
        GuiceJamesServer server = builder(configuration, blobStoreConfiguration)
            .customize(s -> s.combineWith(new JMXServerModule()))
            .build();

        JamesServerMain.main(server);
    }
}
