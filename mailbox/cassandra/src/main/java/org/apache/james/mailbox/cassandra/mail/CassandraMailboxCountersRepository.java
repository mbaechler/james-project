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
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Assignment;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.store.mail.model.Mailbox;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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

    private CompletableFuture<ResultSet> execute(Statement statement) {
        return FutureConverter.toCompletableFuture(session.executeAsync(statement));
    }

    public CompletableFuture<Long> countMessagesInMailbox(Mailbox<CassandraId> mailbox) {
        return execute(
            select(COUNT)
                .from(TABLE_NAME)
                .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid())))
            .thenApply(result ->
                Optional
                    .ofNullable(result.one())
                    .map(row -> row.getLong(COUNT))
                    .orElse(0L));
    }

    public CompletableFuture<Long> countUnseenMessagesInMailbox(Mailbox<CassandraId> mailbox) {
        return execute(
            select(UNSEEN)
                .from(TABLE_NAME)
                .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid())))
            .thenApply(result ->
                Optional
                    .ofNullable(result.one())
                    .map(row -> row.getLong(UNSEEN))
                    .orElse(0L));
    }

    public CompletableFuture<Void> decrementCount(Mailbox<CassandraId> mailbox) {
        return updateMailbox(mailbox, decr(COUNT));
    }

    public CompletableFuture<Void> incrementCount(Mailbox<CassandraId> mailbox) {
        return updateMailbox(mailbox, incr(COUNT));
    }

    public CompletableFuture<Void> decrementUnseen(Mailbox<CassandraId> mailbox) {
        return updateMailbox(mailbox, decr(UNSEEN));
    }

    public CompletableFuture<Void> incrementUnseen(Mailbox<CassandraId> mailbox) {
        return updateMailbox(mailbox, incr(UNSEEN));
    }

    private CompletableFuture<Void> updateMailbox(Mailbox<CassandraId> mailbox, Assignment operation) {
        return execute(
            update(TABLE_NAME)
                .with(operation)
                .where(eq(MAILBOX_ID, mailbox.getMailboxId().asUuid())))
            .thenAccept(this::consume);
    }

    private <U> void consume(U value) {}
    
}
