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

package org.apache.james.mailbox.cassandra.mail;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Assignment;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.mail.model.Mailbox;

import javax.inject.Inject;

import static com.datastax.driver.core.querybuilder.QueryBuilder.decr;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.incr;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxCountersTable.COUNT;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxCountersTable.MAILBOX_ID;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxCountersTable.TABLE_NAME;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxCountersTable.UNSEEN;

public class CassandraMailboxCountersRepository {

    private final Session session;

    @Inject
    public CassandraMailboxCountersRepository(Session session) {
        this.session = session;
    }

    public long countMessagesInMailbox(Mailbox<CassandraId> mailbox) {
        ResultSet results = session.execute(
            select(COUNT)
                .from(TABLE_NAME)
                .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid())));
        return results.isExhausted() ? 0 : results.one().getLong(COUNT);
    }

    public long countUnseenMessagesInMailbox(Mailbox<CassandraId> mailbox) throws MailboxException {
        ResultSet results = session.execute(
            select(UNSEEN)
                .from(TABLE_NAME)
                .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid())));
        return results.isExhausted() ? 0 : results.one().getLong(UNSEEN);
    }

    public void decrementCount(Mailbox<CassandraId> mailbox) {
        updateMailbox(mailbox, decr(COUNT));
    }

    public void incrementCount(Mailbox<CassandraId> mailbox) {
        updateMailbox(mailbox, incr(COUNT));
    }

    public void decrementUnseen(Mailbox<CassandraId> mailbox) {
        updateMailbox(mailbox, decr(UNSEEN));
    }

    public void incrementUnseen(Mailbox<CassandraId> mailbox) {
        updateMailbox(mailbox, incr(UNSEEN));
    }

    private void updateMailbox(Mailbox<CassandraId> mailbox, Assignment operation) {
        session.execute(update(TABLE_NAME)
            .with(operation)
            .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid())));
    }

}
