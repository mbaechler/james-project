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
package org.apache.james.jmap.cassandra;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Date;

import javax.mail.Flags;

import org.apache.james.backends.cassandra.EmbeddedCassandra;
import org.apache.james.jmap.FixedDateZonedDateTimeProvider;
import org.apache.james.jmap.JmapServer;
import org.apache.james.jmap.utils.ZonedDateTimeProvider;
import org.apache.james.mailbox.MailboxListener.Added;
import org.apache.james.mailbox.MailboxListener.MailboxAdded;
import org.apache.james.mailbox.elasticsearch.EmbeddedElasticSearch;
import org.apache.james.mailbox.exception.BadCredentialsException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.event.DefaultDelegatingMailboxListener;
import org.apache.james.mailbox.store.event.DelegatingMailboxListener;
import org.apache.james.mailbox.util.EventCollector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import com.google.inject.util.Modules;

public class OutboxMailDeliveryTest {

    private TemporaryFolder temporaryFolder = new TemporaryFolder();
    private EmbeddedElasticSearch embeddedElasticSearch = new EmbeddedElasticSearch(temporaryFolder);
    private EmbeddedCassandra cassandra = EmbeddedCassandra.createStartServer();
    private FixedDateZonedDateTimeProvider zonedDateTimeProvider = new FixedDateZonedDateTimeProvider();
    private DefaultDelegatingMailboxListener delegatingMailboxListener = new DefaultDelegatingMailboxListener();
    private JmapServer jmapServer = new CassandraJmapServer(
            Modules.combine(
                    CassandraJmapServer.defaultOverrideModule(temporaryFolder, embeddedElasticSearch, cassandra),
                    (binder) -> binder.bind(ZonedDateTimeProvider.class).toInstance(zonedDateTimeProvider),
                    (binder) -> binder.bind(DelegatingMailboxListener.class).toInstance(delegatingMailboxListener)));
    
    @Rule
    public RuleChain chain = RuleChain
        .outerRule(temporaryFolder)
        .around(embeddedElasticSearch)
        .around(jmapServer);
    
    private EventCollector listener;
    private String username;
    
    @Before
    public void setup() throws Exception {
        String domain = "domain.tld";
        this.username = "username@" + domain;
        String password = "password";
        jmapServer.serverProbe().addDomain(domain);
        jmapServer.serverProbe().addUser(username, password);
        
        listener = new EventCollector();
        delegatingMailboxListener.addGlobalListener(listener, null);
    }
    
    @Test
    public void mailDeliveryShouldNotifyMailboxListener() throws BadCredentialsException, MailboxException {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "INBOX");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "INBOX"), 
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        
        assertThat(listener.getEvents()).hasSize(2);
        assertThat(listener.getEvents().get(0)).isInstanceOf(MailboxAdded.class);
        int messageAdditionIndex = 1;
        assertThat(listener.getEvents().get(messageAdditionIndex)).isInstanceOf(Added.class);
        Added event = (Added) listener.getEvents().get(messageAdditionIndex);
        assertThat(event)
            .extracting(Added::getMailboxPath).contains(new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "INBOX"));
        
    }
}
