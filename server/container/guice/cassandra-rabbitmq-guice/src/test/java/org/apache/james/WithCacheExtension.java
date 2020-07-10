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

import org.apache.james.jmap.draft.JmapJamesServerContract;
import org.apache.james.modules.AwsS3BlobStoreExtension;
import org.apache.james.modules.RabbitMQExtension;
import org.apache.james.modules.TestJMAPServerModule;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class WithCacheExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private final JamesServerExtension jamesServerExtension;

    WithCacheExtension() {
        jamesServerExtension = CassandraRabbitMQServerExtension.builder()
            .defaultConfiguration()
            .blobStore(builder -> builder.objectStorage().enableCache())
            .withSpecificParameters(extension -> extension
                .extension(new DockerElasticSearchExtension())
                .extension(new CassandraExtension())
                .extension(new RabbitMQExtension())
                .extension(new AwsS3BlobStoreExtension())
                .overrideServerModule(new TestJMAPServerModule())
                .overrideServerModule(JmapJamesServerContract.DOMAIN_LIST_CONFIGURATION_MODULE)
                .disableAutoStart())
            .build();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        jamesServerExtension.beforeAll(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        jamesServerExtension.afterAll(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        jamesServerExtension.beforeEach(context);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        jamesServerExtension.afterEach(context);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return (parameterContext.getParameter().getType() == GuiceJamesServer.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return jamesServerExtension.getGuiceJamesServer();
    }
}
