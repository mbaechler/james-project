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
package org.apache.james.mpt.imapmailbox.inmemory;

import java.io.IOException;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.james.SMTPJamesServerMain;
import org.apache.james.core.JamesServerResourceLoader;
import org.apache.james.filesystem.api.JamesDirectoriesProvider;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mpt.api.SmtpHostSystem;
import org.apache.james.mpt.host.JamesSmtpHostSystem;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.utils.ConfigurationsPerformer;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

public class SmtpTestModule extends AbstractModule {

    private TemporaryFolder folder = new TemporaryFolder();
    private String rootDirectory;
    
    public SmtpTestModule() throws IOException {
        folder.create();
        rootDirectory = folder.newFolder().getAbsolutePath();
    }
    
    @Override
    protected void configure() {
        install(Modules.override(SMTPJamesServerMain.defaultModule).with(new MyModule(rootDirectory)));
    }

    public static class MyModule extends AbstractModule {
        
        private String rootDirectory;

        public MyModule(String rootDirectory) {
            this.rootDirectory = rootDirectory;
        }
        
        @Override
        protected void configure() {
            
        }
        @Provides @Singleton
        public JamesDirectoriesProvider directories() throws MissingArgumentException {
            return new JamesDirectoriesProvider() {

                @Override
                public String getAbsoluteDirectory() {
                    return "/";
                }

                @Override
                public String getConfDirectory() {
                    return ClassLoader.getSystemResource("conf").getPath();
                }

                @Override
                public String getVarDirectory() {
                    return rootDirectory + "/var/";
                }

                @Override
                public String getRootDirectory() {
                    return rootDirectory;
                }
                
            };
        }
    }
    
    @Provides
    @Singleton
    public SmtpHostSystem provideHostSystem(final UsersRepository usersRepository, Injector injector) throws Exception {
        injector.getInstance(ConfigurationsPerformer.class).initModules();
        return new JamesSmtpHostSystem() {
            
            @Override
            public boolean addUser(String user, String password) throws Exception {
                usersRepository.addUser(user, password);
                return true;
            }
            
            @Override
            protected void resetData() throws Exception {
                folder.delete();
            }
            
            @Override
            public void createMailbox(MailboxPath mailboxPath) throws Exception {
                throw new IllegalStateException();
            }
        };
    }

}
