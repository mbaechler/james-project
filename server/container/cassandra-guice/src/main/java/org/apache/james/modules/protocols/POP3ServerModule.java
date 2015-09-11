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

package org.apache.james.modules.protocols;

import org.apache.james.pop3server.netty.POP3ServerFactory;
import org.apache.james.utils.ConfigurationPerformer;
import org.apache.james.utils.ConfigurationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class POP3ServerModule extends AbstractModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(POP3ServerModule.class);

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(POP3ModuleConfigurationPerformer.class);
    }

    @Singleton
    public static class POP3ModuleConfigurationPerformer implements ConfigurationPerformer {

        private final ConfigurationProvider configurationProvider;
        private final POP3ServerFactory pop3ServerFactory;

        @Inject
        public POP3ModuleConfigurationPerformer(ConfigurationProvider configurationProvider, POP3ServerFactory pop3ServerFactory) {
            this.configurationProvider = configurationProvider;
            this.pop3ServerFactory = pop3ServerFactory;
        }

        @Override
        public void initModule() throws Exception {
            pop3ServerFactory.setLog(LOGGER);
            pop3ServerFactory.configure(configurationProvider.getConfiguration("pop3server"));
            pop3ServerFactory.init();
        }
    }

}
