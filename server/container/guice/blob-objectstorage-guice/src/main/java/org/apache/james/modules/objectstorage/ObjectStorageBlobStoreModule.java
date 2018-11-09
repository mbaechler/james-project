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

import java.io.FileNotFoundException;

import javax.inject.Singleton;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.blob.objectstorage.ObjectStorageBlobsDAO;
import org.apache.james.blob.objectstorage.PayloadCodec;
import org.apache.james.utils.PropertiesProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class ObjectStorageBlobStoreModule extends AbstractModule {

    private static final String OBJECTSTORAGE_CONFIGURATION_NAME = "objectstorage";

    @Override
    protected void configure() {
        bind(PayloadCodec.class).toProvider(PayloadCodecProvider.class).in(Scopes.SINGLETON);
        bind(ObjectStorageBlobsDAO.class).toProvider(ObjectStorageBlobsDAOProvider.class).in(Scopes.SINGLETON);
        bind(BlobStore.class).toProvider(ObjectStorageBlobsDAOProvider.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    private ObjectStorageBlobConfiguration getMailQueueConfiguration(PropertiesProvider propertiesProvider) throws ConfigurationException {
        try {
            Configuration configuration = propertiesProvider.getConfiguration(OBJECTSTORAGE_CONFIGURATION_NAME);
            return ObjectStorageBlobConfiguration.from(configuration);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException(OBJECTSTORAGE_CONFIGURATION_NAME + " configuration " + "was not found");
        }
    }

}
