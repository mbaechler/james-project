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
package org.apache.james.mailbox.cassandra;

import java.util.Locale;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.EmbeddedCassandra;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.mailbox.AbstractMailboxManagerTest;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProvider;
import org.apache.james.mailbox.cassandra.modules.CassandraAclModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxCounterModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMessageModule;
import org.apache.james.mailbox.cassandra.modules.CassandraSubscriptionModule;
import org.apache.james.mailbox.cassandra.modules.CassandraUidAndModSeqModule;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CassandraMailboxManagerTest that extends the StoreMailboxManagerTest.
 * 
 */
public class CassandraMailboxManagerTest extends AbstractMailboxManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("TIMINGS");
    
    private static final EmbeddedCassandra CASSANDRA = EmbeddedCassandra.createStartServer();
    
    private CassandraCluster cluster;

    @Before
    public void setup() throws Exception {
        LOGGER.warn("starting setup");
        CassandraModuleComposite dataDefinition = new CassandraModuleComposite(
                new CassandraAclModule(),
                new CassandraMailboxModule(),
                new CassandraMessageModule(),
                new CassandraMailboxCounterModule(),
                new CassandraUidAndModSeqModule(),
                new CassandraSubscriptionModule());
        
        cluster = CassandraCluster.create(dataDefinition, CASSANDRA, RandomStringUtils.randomAlphabetic(5).toLowerCase(Locale.US));
        LOGGER.warn("cluster created");
        createMailboxManager();
        LOGGER.warn("ending setup");
    }

    @After
    public void tearDown() throws Exception {
        MailboxSession session = getMailboxManager().createSystemSession("test", LoggerFactory.getLogger("Test"));
        session.close();
    }

    @Override
    protected void createMailboxManager() throws MailboxException {
        final CassandraUidProvider uidProvider = new CassandraUidProvider(cluster.getConf());
        final CassandraModSeqProvider modSeqProvider = new CassandraModSeqProvider(cluster.getConf());
        final CassandraMailboxSessionMapperFactory mapperFactory = new CassandraMailboxSessionMapperFactory(uidProvider,
            modSeqProvider,
            cluster.getConf(),
            cluster.getTypesProvider());

        final CassandraMailboxManager manager = new CassandraMailboxManager(mapperFactory, null, new JVMMailboxPathLocker());
        manager.init();

        setMailboxManager(manager);
    }

}
