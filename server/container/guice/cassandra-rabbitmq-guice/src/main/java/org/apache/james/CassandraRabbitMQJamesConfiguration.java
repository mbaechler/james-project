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

import java.io.File;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.filesystem.api.JamesDirectoriesProvider;
import org.apache.james.modules.blobstore.BlobStoreConfiguration;
import org.apache.james.server.core.JamesServerResourceLoader;
import org.apache.james.server.core.MissingArgumentException;
import org.apache.james.server.core.configuration.Configuration;
import org.apache.james.server.core.filesystem.FileSystemImpl;
import org.apache.james.utils.PropertiesProvider;

import com.github.fge.lambdas.Throwing;

public class CassandraRabbitMQJamesConfiguration {

    private final Configuration basic;
    private final BlobStoreConfiguration blobStore;

    @FunctionalInterface
    public interface RequiresBasicConfiguration {
        RequireBlobStore configuration(UnaryOperator<Configuration.Builder> basicConfiguration);
    }

    @FunctionalInterface
    public interface RequireBlobStore {
        CassandraRabbitMQJamesConfiguration blobStore(Function<BlobStoreConfiguration.Builder, BlobStoreConfiguration> configure);
    }

    public static RequiresBasicConfiguration builder() {
        return basic -> blobstore ->
            new CassandraRabbitMQJamesConfiguration(
                basic.apply(Configuration.builder()).build(),
                blobstore.apply(BlobStoreConfiguration.builder()));
    }

    public CassandraRabbitMQJamesConfiguration(Configuration basic, BlobStoreConfiguration blobStore) {
        this.basic = basic;
        this.blobStore = blobStore;
    }

    public BlobStoreConfiguration blobStoreConfiguration() {
        return blobStore;
    }

    public Configuration basicConfiguration() {
        return basic;
    }
}
