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
package org.apache.james.mpt.imapmailbox.external.james;

import org.apache.james.mpt.api.ImapHostSystem;
import org.apache.james.mpt.imapmailbox.external.james.host.ProvisioningAPI;
import org.apache.james.mpt.imapmailbox.external.james.host.SmtpHostSystem;
import org.apache.james.mpt.imapmailbox.external.james.host.StaticJamesConfiguration;
import org.apache.james.mpt.imapmailbox.external.james.host.docker.CliProvisioningAPI;
import org.apache.james.mpt.imapmailbox.external.james.host.external.ExternalJamesConfiguration;
import org.apache.james.util.Port;
import org.apache.james.util.docker.DockerContainer;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class DockerJamesExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerJamesExtension.class);

    private static final int IMAP_PORT = 143;
    private static final int SMTP_PORT = 587;
    private static final int WEBADMIN_PORT = 8000;

    private final DockerContainer container;
    private final CliProvisioningAPI.CliType cliProvisioningType;

    private ImapHostSystem system;
    private SmtpHostSystem smtpHostSystem;
    private ProvisioningAPI provisioningAPI;
    private StaticJamesConfiguration configuration;

    public static RequireProvisioningAPI imageFromProperty(String property, Network network) {
        return cliType -> new DockerJamesExtension(retrieveDockerImageName(property), cliType, network);
    }

    interface RequireProvisioningAPI {
        DockerJamesExtension withProvisioning(CliProvisioningAPI.CliType cliType);
    }

    private static String retrieveDockerImageName(String property) {
        String imageName = System.getProperty(property);
        Assumptions.assumeThat(imageName)
            .describedAs("No property '" + property + "' defined to run integration-test")
            .isNotNull();
        return imageName;
    }

    private DockerJamesExtension(DockerContainer container, CliProvisioningAPI.CliType cliProvisioningType, Network network) {
        this.container = container.withNetwork(network);
        this.cliProvisioningType = cliProvisioningType;
    }

    private DockerJamesExtension(String image, CliProvisioningAPI.CliType cliProvisioningType, Network network) {
        this(DockerContainer.fromName(image)
            .withExposedPorts(SMTP_PORT, IMAP_PORT)
            .waitingFor(new HostPortWaitStrategy())
            .withLogConsumer(frame -> {
                switch (frame.getType()) {
                    case STDOUT:
                        LOGGER.info(frame.getUtf8String());
                        break;
                    case STDERR:
                        LOGGER.error(frame.getUtf8String());
                        break;
                    case END:
                        break; //Ignore
                }
            }), cliProvisioningType, network);
    }


    public void start() {
        container.start();
    }

    public void stop() {
        container.stop();
    }

    public void pause() {
        container.pause();
    }

    public void unpause() {
        container.unpause();
    }

    public Port getWebadminPort() {
        return getMappedPort(WEBADMIN_PORT);
    }

    private Port getMappedPort(int port) {
        return Port.of(container.getMappedPort(port));
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        start();
        provisioningAPI = new CliProvisioningAPI(cliProvisioningType, container);
        configuration = new StaticJamesConfiguration(container.getContainerIp(), Port.of(IMAP_PORT), Port.of(SMTP_PORT));
        Injector injector = Guice.createInjector(new ExternalJamesModule(configuration, provisioningAPI));
        system = injector.getInstance(ImapHostSystem.class);
        smtpHostSystem = injector.getInstance(SmtpHostSystem.class);
        system.beforeTest();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        stop();
        system.afterTest();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return ImmutableSet.of(
                ImapHostSystem.class,
                SmtpHostSystem.class,
                ExternalJamesConfiguration.class,
                ProvisioningAPI.class)
            .contains(parameterType);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        if (parameterType == ImapHostSystem.class) {
            return system;
        } else if (parameterType == SmtpHostSystem.class) {
            return smtpHostSystem;
        } else if (parameterType == ExternalJamesConfiguration.class) {
            return configuration;
        } else if (parameterType == ProvisioningAPI.class) {
            return provisioningAPI;
        }
        return null;
    }

}
