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

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.init.CassandraTypesProvider;
import org.apache.james.mailbox.AbstractSubscriptionManagerTest;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxCountersRepository;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageRepository;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProvider;
import org.apache.james.mailbox.cassandra.modules.CassandraSubscriptionModule;

import com.datastax.driver.core.Session;

/**
 * Test Cassandra subscription against some general purpose written code.
 */
public class CassandraSubscriptionManagerTest extends AbstractSubscriptionManagerTest {

    private static final CassandraCluster cassandra = CassandraCluster.create(new CassandraSubscriptionModule());
    
    @Override
    public SubscriptionManager createSubscriptionManager() {
        Session session = cassandra.getConf();
        CassandraTypesProvider typesProvider = cassandra.getTypesProvider();
        CassandraMessageRepository messageRepository = new CassandraMessageRepository(session, typesProvider);
        CassandraMailboxCountersRepository mailboxCountersRepository = new CassandraMailboxCountersRepository(session);
        return new CassandraSubscriptionManager(
            new CassandraMailboxSessionMapperFactory(
                new CassandraUidProvider(session),
                new CassandraModSeqProvider(session),
                session,
                typesProvider,
                messageRepository,
                mailboxCountersRepository)
        );
    }
}
