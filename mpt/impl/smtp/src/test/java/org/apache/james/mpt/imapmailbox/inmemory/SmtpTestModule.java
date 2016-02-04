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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.james.SMTPJamesServerMain;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.dnsservice.api.TemporaryResolutionException;
import org.apache.james.filesystem.api.JamesDirectoriesProvider;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mpt.api.SmtpHostSystem;
import org.apache.james.mpt.host.JamesSmtpHostSystem;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.model.JamesUser;
import org.apache.james.user.api.model.User;
import org.apache.james.utils.ConfigurationsPerformer;
import org.apache.mailet.MailAddress;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
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
            bind(MockDNSService.class).in(Scopes.SINGLETON);
            bind(DNSService.class).to(MockDNSService.class);
        }
        
        public static class MockDNSService implements DNSService{
            private Map<String,DNSRecord> records=new HashMap<String, DNSRecord>();

            public MockDNSService() {
                records.put("0.0.0.0", new DNSRecord(new InetAddress[]{InetAddresses.forString("0.0.0.0")}, ImmutableList.of(), ImmutableList.of()));
                records.put("127.0.0.1", new DNSRecord(new InetAddress[]{InetAddresses.forString("127.0.0.1")}, ImmutableList.of(), ImmutableList.of()));
            }
            
            public void registerRecord(String hostname, InetAddress[] addresses,Collection<String> MXRecords, Collection<String> TXTRecords ){
                records.put(hostname,new DNSRecord(addresses, MXRecords, TXTRecords));
            }
            public void dropRecord(String hostname){
                records.remove(hostname);
            }

            @Override
            public Collection<String> findMXRecords(final String hostname) throws TemporaryResolutionException {
                return hostRecord(hostname).MXRecords;
            }

            @Override
            public Collection<String> findTXTRecords(String hostname) {
                return hostRecord(hostname).TXTRecords;
            }

            @Override
            public InetAddress[] getAllByName(String host) throws UnknownHostException {
                return hostRecord(host).addresses;
            }

            @Override
            public InetAddress getByName(String host) throws UnknownHostException {
                return hostRecord(host).addresses[0];
            }

            private DNSRecord hostRecord(String host) {
                System.out.println("hostRecord : " + host);
                return records.entrySet().stream().filter((entry)->entry.getKey().equals(host)).findFirst().get().getValue();
            }

            @Override
            public InetAddress getLocalHost() throws UnknownHostException {
                return InetAddress.getLocalHost();
            }

            @Override
            public String getHostName(InetAddress addr) {
                return records.entrySet().stream().filter((entry)->entry.getValue().contains(addr)).findFirst().get().getKey();
            }
        }
        static class DNSRecord{
            InetAddress[] addresses;
            Collection<String> MXRecords;
            Collection<String> TXTRecords;
            private List<InetAddress> addressList;
            DNSRecord(InetAddress[] adresses, Collection<String> mxRecords, Collection<String> txtRecords) {
                this.addresses = adresses;
                MXRecords = mxRecords;
                TXTRecords = txtRecords;
                addressList = Arrays.asList(addresses);
            }
            boolean contains(InetAddress address){
                return addressList.contains(address);
            }
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
                JamesUser userObj = (JamesUser) usersRepository.getUserByName(user);
                userObj.setForwarding(true);
                userObj.setForwardingDestination(new MailAddress("ray@yopmail.com"));
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
