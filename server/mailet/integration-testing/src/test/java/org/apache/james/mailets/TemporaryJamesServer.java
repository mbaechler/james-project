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

package org.apache.james.mailets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.james.GuiceJamesServer;
import org.apache.james.MemoryJamesServerMain;
import org.apache.james.mailets.configuration.MailetContainer;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.utils.GuiceProbe;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

public class TemporaryJamesServer {

    private static final String MAILETCONTAINER_CONFIGURATION_FILENAME = "mailetcontainer.xml";

    private static final int LIMIT_TO_3_MESSAGES = 3;

    private final GuiceJamesServer jamesServer;


    public TemporaryJamesServer(TemporaryFolder temporaryFolder, MailetContainer mailetContainer, Module... additionalModules) throws Exception {
        appendMailetConfigurations(temporaryFolder, mailetContainer);

        jamesServer = new GuiceJamesServer()
            .combineWith(MemoryJamesServerMain.inMemoryServerModule)
            .overrideWith(ImmutableList.<Module>builder().addAll(Arrays.asList(additionalModules))
                .add(new TestJMAPServerModule(LIMIT_TO_3_MESSAGES))
                .add(new TemporaryFilesystemModule(temporaryFolder))
                .build()
                .toArray(new Module[additionalModules.length + 2]));

        jamesServer.start();
    }

    private void appendMailetConfigurations(TemporaryFolder temporaryFolder, MailetContainer mailetContainer) throws ConfigurationException, IOException {
        try (OutputStream outputStream = createMailetConfigurationFile(temporaryFolder)) {
            IOUtils.write(mailetContainer.serializeAsXml(), outputStream);
        }
    }

    private FileOutputStream createMailetConfigurationFile(TemporaryFolder temporaryFolder) throws IOException {
        File configurationFolder = temporaryFolder.newFolder("conf");
        return new FileOutputStream(Paths.get(configurationFolder.getAbsolutePath(), MAILETCONTAINER_CONFIGURATION_FILENAME).toFile());
    }

    public void shutdown() {
        jamesServer.stop();
    }
    
    public <T extends GuiceProbe> T getProbe(Class<T> probe) {
        return jamesServer.getProbe(probe);
    }

}
