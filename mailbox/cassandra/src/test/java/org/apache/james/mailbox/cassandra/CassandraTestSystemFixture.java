/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                 *
 * *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailbox.cassandra;

import static org.mockito.Mockito.mock;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.mailbox.cassandra.mail.CassandraDeletedMessageDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraFirstUnseenDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraApplicableFlagDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxCounterDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxRecentsDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdToImapUidDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProvider;
import org.apache.james.mailbox.cassandra.modules.CassandraAclModule;
import org.apache.james.mailbox.cassandra.modules.CassandraAnnotationModule;
import org.apache.james.mailbox.cassandra.modules.CassandraApplicableFlagsModule;
import org.apache.james.mailbox.cassandra.modules.CassandraAttachmentModule;
import org.apache.james.mailbox.cassandra.modules.CassandraDeletedMessageModule;
import org.apache.james.mailbox.cassandra.modules.CassandraFirstUnseenModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxCounterModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxRecentsModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMessageModule;
import org.apache.james.mailbox.cassandra.modules.CassandraModSeqModule;
import org.apache.james.mailbox.cassandra.modules.CassandraUidModule;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.Authorizator;
import org.apache.james.mailbox.store.NoMailboxPathLocker;
import org.apache.james.mailbox.store.StoreMessageIdManager;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.apache.james.mailbox.store.quota.DefaultQuotaRootResolver;

public class CassandraTestSystemFixture {
    
    public static final int MOD_SEQ = 452;
    public static final int MAX_ACL_RETRY = 10;
    public static CassandraCluster cassandra;
    
    public static CassandraMailboxSessionMapperFactory createMapperFactory() {
        cassandra.ensureAllTables();
        CassandraUidProvider uidProvider = new CassandraUidProvider(cassandra.getConf());
        CassandraModSeqProvider modSeqProvider = new CassandraModSeqProvider(cassandra.getConf());
        CassandraMessageId.Factory messageIdFactory = new CassandraMessageId.Factory();
        CassandraMessageIdDAO messageIdDAO = new CassandraMessageIdDAO(cassandra.getConf(), messageIdFactory);
        CassandraMessageIdToImapUidDAO imapUidDAO = new CassandraMessageIdToImapUidDAO(cassandra.getConf(), messageIdFactory);
        CassandraMessageDAO messageDAO = new CassandraMessageDAO(cassandra.getConf(), cassandra.getTypesProvider());
        CassandraMailboxCounterDAO mailboxCounterDAO = new CassandraMailboxCounterDAO(cassandra.getConf());
        CassandraMailboxRecentsDAO mailboxRecentsDAO = new CassandraMailboxRecentsDAO(cassandra.getConf());
        CassandraApplicableFlagDAO applicableFlagDAO = new CassandraApplicableFlagDAO(cassandra.getConf());

        CassandraMailboxDAO mailboxDAO = new CassandraMailboxDAO(cassandra.getConf(), cassandra.getTypesProvider(), MAX_ACL_RETRY);
        CassandraMailboxPathDAO mailboxPathDAO = new CassandraMailboxPathDAO(cassandra.getConf(), cassandra.getTypesProvider());
        CassandraFirstUnseenDAO firstUnseenDAO = new CassandraFirstUnseenDAO(cassandra.getConf());
        CassandraDeletedMessageDAO deletedMessageDAO = new CassandraDeletedMessageDAO(cassandra.getConf());
        return new CassandraMailboxSessionMapperFactory(uidProvider,
            modSeqProvider,
            cassandra.getConf(),
            messageDAO,
            messageIdDAO,
            imapUidDAO,
            mailboxCounterDAO,
            mailboxRecentsDAO,
            mailboxDAO,
            mailboxPathDAO,
            firstUnseenDAO,
            applicableFlagDAO,
            deletedMessageDAO);
    }

    public static CassandraMailboxManager createMailboxManager(CassandraMailboxSessionMapperFactory mapperFactory) throws Exception{
        CassandraMailboxManager cassandraMailboxManager = new CassandraMailboxManager(mapperFactory, mock(Authenticator.class), mock(Authorizator.class),
            new NoMailboxPathLocker(), new MessageParser(), new CassandraMessageId.Factory());
        cassandraMailboxManager.init();

        return cassandraMailboxManager;
    }

    public static StoreMessageIdManager createMessageIdManager(CassandraMailboxSessionMapperFactory mapperFactory, QuotaManager quotaManager, MailboxEventDispatcher dispatcher) {
        return new StoreMessageIdManager(mapperFactory,
            dispatcher,
            new CassandraMessageId.Factory(),
            quotaManager,
            new DefaultQuotaRootResolver(mapperFactory));
    }

    public static void clean() {
        cassandra.clearAllTables();
    }

    public static void init() {
        cassandra = CassandraCluster.create(
                new CassandraModuleComposite(
                    new CassandraAclModule(),
                    new CassandraMailboxModule(),
                    new CassandraMessageModule(),
                    new CassandraMailboxCounterModule(),
                    new CassandraMailboxRecentsModule(),
                    new CassandraFirstUnseenModule(),
                    new CassandraDeletedMessageModule(),
                    new CassandraUidModule(),
                    new CassandraModSeqModule(),
                    new CassandraAttachmentModule(),
                    new CassandraAnnotationModule(),
                    new CassandraApplicableFlagsModule()));
    }

    public static void stop() {
        cassandra.close();
    }
}
