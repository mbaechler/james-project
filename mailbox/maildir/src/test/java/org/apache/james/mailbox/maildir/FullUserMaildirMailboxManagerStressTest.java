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

package org.apache.james.mailbox.maildir;

import java.io.File;

import org.apache.james.mailbox.MailboxManagerStressContract;
import org.apache.james.mailbox.events.EventBus;
import org.apache.james.mailbox.exception.MailboxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

class FullUserMaildirMailboxManagerStressTest implements MailboxManagerStressContract<MaildirMailboxManagerProvider.MaildirMailboxManager> {
    @TempDir
    File tmpFolder;

    MaildirMailboxManagerProvider.MaildirMailboxManager mailboxManager;

    @Override
    public MaildirMailboxManagerProvider.MaildirMailboxManager getManager() {
        return mailboxManager;
    }

    @Override
    public EventBus retrieveEventBus() {
        return mailboxManager.getEventBus();
    }

    @BeforeEach
    void setUp() throws MailboxException {
        this.mailboxManager = MaildirMailboxManagerProvider.createMailboxManager("/%fulluser", tmpFolder);
    }
}
