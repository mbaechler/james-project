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

import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.objectstorage.ObjectStorageBlobsDAO;
import org.apache.james.blob.objectstorage.ObjectStorageBlobsDAOBuilder;
import org.apache.james.blob.objectstorage.swift.SwiftKeystone2ObjectStorage;
import org.apache.james.blob.objectstorage.swift.SwiftKeystone3ObjectStorage;
import org.apache.james.blob.objectstorage.swift.SwiftTempAuthObjectStorage;

import com.google.common.collect.ImmutableMap;

public class ObjectStorageBlobsDAOProvider implements Provider<ObjectStorageBlobsDAO> {

    public static final String OBJECTSTORAGE_PROVIDER_SWIFT = "swift";

    private final ObjectStorageBlobConfiguration configuration;
    private final BlobId.Factory blobIdFactory;
    private final ImmutableMap<String, Supplier<ObjectStorageBlobsDAOBuilder.RequireContainerName>> providersByName;
    private final ImmutableMap<String, Supplier<ObjectStorageBlobsDAOBuilder.RequireContainerName>> swiftAuthApiByName;

    @Inject
    public ObjectStorageBlobsDAOProvider(ObjectStorageBlobConfiguration configuration,
                                         BlobId.Factory blobIdFactory) throws ConfigurationException {
        //This provider map will allow to implement S3 provider
        providersByName = ImmutableMap.of(OBJECTSTORAGE_PROVIDER_SWIFT, this::selectSwiftAuthApi);
        swiftAuthApiByName = ImmutableMap.of(
            SwiftTempAuthObjectStorage.AUTH_API_NAME, this::swiftTempAuth,
            SwiftKeystone2ObjectStorage.AUTH_API_NAME, this::swiftKeystone2Auth,
            SwiftKeystone3ObjectStorage.AUTH_API_NAME, this::swiftKeystone3Auth);

        this.configuration = configuration;
        this.blobIdFactory = blobIdFactory;
    }

    @Override
    public ObjectStorageBlobsDAO get() {
        return providersByName.get(configuration.getProvider()).get().container(configuration.getNamespace()).blobIdFactory(blobIdFactory).build();
    }

    private ObjectStorageBlobsDAOBuilder.RequireContainerName selectSwiftAuthApi() {
        return swiftAuthApiByName.get(configuration.getAuthApi()).get();
    }

    private ObjectStorageBlobsDAOBuilder.RequireContainerName swiftTempAuth() {
        return ObjectStorageBlobsDAO.builder(configuration.getTempAuthConfiguration());
    }

    private ObjectStorageBlobsDAOBuilder.RequireContainerName swiftKeystone2Auth() {
        return ObjectStorageBlobsDAO.builder(configuration.getKeystone2Configuration());
    }

    private ObjectStorageBlobsDAOBuilder.RequireContainerName swiftKeystone3Auth() {
        return ObjectStorageBlobsDAO.builder(configuration.getKeystone3Configuration());
    }
}
