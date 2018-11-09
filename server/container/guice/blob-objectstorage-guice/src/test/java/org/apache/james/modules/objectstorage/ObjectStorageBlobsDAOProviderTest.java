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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.blob.api.HashBlobId;
import org.apache.james.blob.objectstorage.ContainerName;
import org.apache.james.blob.objectstorage.DockerSwift;
import org.apache.james.blob.objectstorage.DockerSwiftExtension;
import org.apache.james.blob.objectstorage.ObjectStorageBlobsDAO;
import org.apache.james.blob.objectstorage.swift.Credentials;
import org.apache.james.blob.objectstorage.swift.DomainName;
import org.apache.james.blob.objectstorage.swift.IdentityV3;
import org.apache.james.blob.objectstorage.swift.PassHeaderName;
import org.apache.james.blob.objectstorage.swift.Project;
import org.apache.james.blob.objectstorage.swift.ProjectName;
import org.apache.james.blob.objectstorage.swift.SwiftKeystone2ObjectStorage;
import org.apache.james.blob.objectstorage.swift.SwiftKeystone3ObjectStorage;
import org.apache.james.blob.objectstorage.swift.SwiftTempAuthObjectStorage;
import org.apache.james.blob.objectstorage.swift.TenantName;
import org.apache.james.blob.objectstorage.swift.UserHeaderName;
import org.apache.james.blob.objectstorage.swift.UserName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DockerSwiftExtension.class)
class ObjectStorageBlobsDAOProviderTest {

    private ContainerName containerName;
    private DockerSwift dockerSwift;

    @BeforeEach
    void setUp(DockerSwift dockerSwift) throws Exception {
        this.dockerSwift = dockerSwift;
        containerName = ContainerName.of(UUID.randomUUID().toString());
    }

    public static final HashBlobId.Factory BLOB_ID_FACTORY = new HashBlobId.Factory();

/*
    @Test
    void providesTempauthBackedBlobstoreDao() throws ConfigurationException {
        ObjectStorageBlobsDAOProvider objectStorageBlobsDAOProvider =
            new ObjectStorageBlobsDAOProvider(
                ObjectStorageBlobConfiguration.builder()
                    .swift()
                    .container(containerName)
                    .tempAuth(SwiftTempAuthObjectStorage.configBuilder()
                        .endpoint(dockerSwift.swiftEndpoint())
                        .credentials(Credentials.of("testing"))
                        .userName(UserName.of("tester"))
                        .tenantName(TenantName.of("test"))
                        .tempAuthHeaderUserName(UserHeaderName.of("X-Storage-User"))
                        .tempAuthHeaderPassName(PassHeaderName.of("X-Storage-Pass"))
                        .build())
                    .build(),
                BLOB_ID_FACTORY);
        ObjectStorageBlobsDAO objectStorageBlobsDAO = objectStorageBlobsDAOProvider.get();
        assertThat(objectStorageBlobsDAO).isNotNull();
    }

    @Test
    void providesKeystone2BackedBlobstoreDao() throws ConfigurationException {
        ObjectStorageBlobsDAOProvider objectStorageBlobsDAOProvider =
            new ObjectStorageBlobsDAOProvider(
                ObjectStorageBlobConfiguration.builder()
                    .swift()
                    .container(containerName)
                    .keystone2(SwiftKeystone2ObjectStorage.configBuilder()
                        .endpoint(dockerSwift.keystoneV2Endpoint())
                        .credentials(Credentials.of("creds"))
                        .userName(UserName.of("demo"))
                        .tenantName(TenantName.of("test"))
                        .build())
                    .build(),
                BLOB_ID_FACTORY);
        ObjectStorageBlobsDAO objectStorageBlobsDAO = objectStorageBlobsDAOProvider.get();
        assertThat(objectStorageBlobsDAO).isNotNull();
    }

    @Test
    void providesKeystone3BackedBlobstoreDao() throws ConfigurationException {
        ObjectStorageBlobsDAOProvider objectStorageBlobsDAOProvider =
            new ObjectStorageBlobsDAOProvider(
                ObjectStorageBlobConfiguration.builder()
                    .swift()
                    .container(containerName)
                    .keystone3(SwiftKeystone3ObjectStorage.configBuilder()
                        .endpoint(dockerSwift.keystoneV3Endpoint())
                        .credentials(Credentials.of("creds"))
                        .project(Project.of(ProjectName.of("test")))
                        .identity(IdentityV3.of(DomainName.of("Default"), UserName.of("demo")))
                        .build())
                    .build(),
                BLOB_ID_FACTORY);
        ObjectStorageBlobsDAO objectStorageBlobsDAO = objectStorageBlobsDAOProvider.get();
        assertThat(objectStorageBlobsDAO).isNotNull();
    }
*/
}
