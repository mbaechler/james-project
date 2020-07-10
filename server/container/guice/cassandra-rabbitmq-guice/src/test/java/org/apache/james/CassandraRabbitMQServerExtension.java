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

import java.util.Optional;
import java.util.function.Function;

import org.apache.james.modules.blobstore.BlobStoreConfiguration;
import org.apache.james.server.core.configuration.Configuration;

public class CassandraRabbitMQServerExtension {

    @FunctionalInterface
    public interface RequiresBasicConfiguration {
        RequiresBlobConfiguration configure(JamesServerBuilder.ConfigurationProvider provider);

        default RequiresBlobConfiguration defaultConfiguration() {
            return configure(tmpDir -> Configuration.builder()
                .workingDirectory(tmpDir)
                .configurationFromClasspath()
                .build());
        }
    }

    @FunctionalInterface
    public interface RequiresBlobConfiguration {
        Builder blobStore(Function<BlobStoreConfiguration.Builder, BlobStoreConfiguration> configure);
    }

    @FunctionalInterface
    public interface ExtensionCustomizer {
        JamesServerBuilder customize(JamesServerBuilder builder);
    }

    public static class Builder {

        private final JamesServerBuilder extensionBuilder;
        private final BlobStoreConfiguration blobStore;

        private Builder(JamesServerBuilder.ConfigurationProvider configuration, BlobStoreConfiguration blobStore) {
            this.extensionBuilder = new JamesServerBuilder<CassandraRabbitMQJamesConfiguration>(configuration,
                c -> CassandraRabbitMQJamesServerMain.builder(c.basicConfiguration(), c.blobStoreConfiguration()).build());
            this.blobStore = blobStore;
        }

        public Builder withSpecificParameters(ExtensionCustomizer customizer) {
            customizer.customize(extensionBuilder);
            return this;
        }

        public JamesServerExtension build() {
            return extensionBuilder.build();
        }
    }

    public static RequiresBasicConfiguration builder() {
        return basic -> blobStore -> new Builder(basic, blobStore.apply(BlobStoreConfiguration.builder()));
    }
}
