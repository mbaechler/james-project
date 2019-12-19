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

package org.apache.james.backends.es;

import org.apache.james.util.docker.DockerContainer;
import org.apache.james.util.docker.Images;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.testcontainers.containers.Network;

public class DockerElasticSearchExtension implements AfterEachCallback, ParameterResolver {

    private final DockerElasticSearch elasticSearch;

    public DockerElasticSearchExtension() {
        elasticSearch = DockerElasticSearchSingleton.INSTANCE;
    }
    public DockerElasticSearchExtension(Network network, String networkAlias) {
        DockerContainer eSContainer = DockerElasticSearch.NoAuth.defaultContainer(Images.ELASTICSEARCH_6)
            .withNetwork(network)
            .withNetworkAliases(networkAlias);
        elasticSearch = new DockerElasticSearch.NoAuth(eSContainer);
    }


    @Override
    public void afterEach(ExtensionContext context) {
        elasticSearch.cleanUpData();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return (parameterContext.getParameter().getType() == DockerElasticSearch.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return elasticSearch;
    }

    public void awaitForElasticSearch() {
        elasticSearch.flushIndices();
    }

    public DockerElasticSearch getDockerElasticSearch() {
        return elasticSearch;
    }

    public void start() {
        elasticSearch.start();
    }

    public void stop() {
        elasticSearch.stop();
    }
}
