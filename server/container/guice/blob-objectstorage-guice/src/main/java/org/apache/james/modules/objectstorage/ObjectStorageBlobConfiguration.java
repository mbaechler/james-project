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

package org.apache.james.modules.objectstorage;

import org.apache.commons.configuration.Configuration;
import org.apache.james.blob.objectstorage.ContainerName;
import org.apache.james.blob.objectstorage.swift.SwiftKeystone2ObjectStorage;
import org.apache.james.blob.objectstorage.swift.SwiftKeystone3ObjectStorage;
import org.apache.james.blob.objectstorage.swift.SwiftTempAuthObjectStorage;

import com.google.common.base.Preconditions;

class ObjectStorageBlobConfiguration {
    private static final String OBJECTSTORAGE_CONFIGURATION_NAME = "objectstorage";
    private static final String OBJECTSTORAGE_NAMESPACE = "objectstorage.namespace";
    private static final String OBJECTSTORAGE_PROVIDER = "objectstorage.provider";
    private static final String OBJECTSTORAGE_SWIFT_AUTH_API = "objectstorage.swift.authapi";

    static ObjectStorageBlobConfiguration from(Configuration configuration) {
        String provider = configuration.getString(OBJECTSTORAGE_PROVIDER, null);
        String namespace = configuration.getString(OBJECTSTORAGE_NAMESPACE, null);
        String authApi = configuration.getString(OBJECTSTORAGE_SWIFT_AUTH_API, null);
        SwiftTempAuthObjectStorage.Configuration tempAuth = SwiftTmpAuthConfigurationReader.readSwiftConfiguration(configuration);
        SwiftKeystone2ObjectStorage.Configuration keystone2Configuration = SwiftKeystone2ConfigurationReader.readSwiftConfiguration(configuration);
        SwiftKeystone3ObjectStorage.Configuration keystone3Configuration = SwiftKeystone3ConfigurationReader.readSwiftConfiguration(configuration);

        Preconditions.checkArgument(authApi != null,
            "Mandatory configuration value " + OBJECTSTORAGE_SWIFT_AUTH_API + " is missing from " + OBJECTSTORAGE_CONFIGURATION_NAME + " configuration");
        Preconditions.checkArgument(namespace != null,
            "Mandatory configuration value " + OBJECTSTORAGE_NAMESPACE + " is missing from " + OBJECTSTORAGE_CONFIGURATION_NAME + " configuration");
        Preconditions.checkArgument(provider != null,
            "Mandatory configuration value " + OBJECTSTORAGE_PROVIDER + " is missing from " + OBJECTSTORAGE_CONFIGURATION_NAME + " configuration");

        return new ObjectStorageBlobConfiguration(provider, ContainerName.of(namespace), authApi, tempAuth, keystone2Configuration, keystone3Configuration);
    }

    private final String authApi;
    private final ContainerName namespace;
    private final String provider;
    private final SwiftTempAuthObjectStorage.Configuration tempAuth;
    private final SwiftKeystone2ObjectStorage.Configuration keystone2Configuration;
    private final SwiftKeystone3ObjectStorage.Configuration keystone3Configuration;

    private ObjectStorageBlobConfiguration(String provider, ContainerName namespace, String authApi, SwiftTempAuthObjectStorage.Configuration tempAuth, SwiftKeystone2ObjectStorage.Configuration keystone2Configuration, SwiftKeystone3ObjectStorage.Configuration keystone3Configuration) {
        this.authApi = authApi;
        this.namespace = namespace;
        this.provider = provider;
        this.tempAuth = tempAuth;
        this.keystone2Configuration = keystone2Configuration;
        this.keystone3Configuration = keystone3Configuration;
    }

    public String getAuthApi() {
        return authApi;
    }

    public ContainerName getNamespace() {
        return namespace;
    }

    public String getProvider() {
        return provider;
    }

    public SwiftTempAuthObjectStorage.Configuration getTempAuthConfiguration() {
        return tempAuth;
    }

    public SwiftKeystone2ObjectStorage.Configuration getKeystone2Configuration() {
        return keystone2Configuration;
    }

    public SwiftKeystone3ObjectStorage.Configuration getKeystone3Configuration() {
        return keystone3Configuration;
    }
}
