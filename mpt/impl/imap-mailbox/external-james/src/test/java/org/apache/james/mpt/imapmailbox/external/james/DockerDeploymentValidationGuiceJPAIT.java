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

import static org.hamcrest.Matchers.notNullValue;

import org.apache.james.core.Username;
import org.apache.james.mpt.api.ImapHostSystem;
import org.apache.james.mpt.imapmailbox.external.james.host.ProvisioningAPI;
import org.apache.james.mpt.imapmailbox.external.james.host.SmtpHostSystem;
import org.apache.james.mpt.imapmailbox.external.james.host.external.ExternalJamesConfiguration;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class DockerDeploymentValidationGuiceJPAIT extends DeploymentValidation {

    private ImapHostSystem system;
    private SmtpHostSystem smtpHostSystem;

    private static String retrieveDockerImageName() {
        String imageName = System.getProperty("docker.image.jpa");
        Assume.assumeThat("No property docker.image.jpa defined to run integration-test", imageName, notNullValue());
        return imageName;
    }

    private DockerJamesRule dockerJamesRule;

    @Override
    @Before
    public void setUp() throws Exception {
        dockerJamesRule = new DockerJamesRule(retrieveDockerImageName());
        dockerJamesRule.start();

        ProvisioningAPI provisioningAPI = dockerJamesRule.cliJarDomainsAndUsersAdder();
        Injector injector = Guice.createInjector(new ExternalJamesModule(getConfiguration(), provisioningAPI));
        provisioningAPI.addDomain(DOMAIN);
        provisioningAPI.addUser(Username.of(USER_ADDRESS), PASSWORD);
        system = injector.getInstance(ImapHostSystem.class);
        smtpHostSystem = injector.getInstance(SmtpHostSystem.class);
        system.beforeTest();

        super.setUp();
    }

    @Override
    protected ImapHostSystem createImapHostSystem() {
        return system;
    }

    @Override
    protected SmtpHostSystem createSmtpHostSystem() {
        return smtpHostSystem;
    }

    @Override
    protected ExternalJamesConfiguration getConfiguration() {
        return dockerJamesRule.getConfiguration();
    }

    @After
    public void tearDown() throws Exception {
        system.afterTest();
        dockerJamesRule.stop();
    }

}