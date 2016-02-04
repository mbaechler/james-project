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

package org.apache.james.mpt.host;

import java.util.HashSet;
import java.util.Set;

import org.apache.james.mailbox.MailboxSession.User;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mpt.api.SmtpHostSystem;
import org.apache.james.mpt.monitor.SystemLoggingMonitor;
import org.apache.james.mpt.session.ExternalSessionFactory;

public abstract class JamesSmtpHostSystem extends ExternalSessionFactory implements SmtpHostSystem {

    private final Set<User> users;

    public JamesSmtpHostSystem() {
        super("localhost", 1025, new SystemLoggingMonitor(), "220 mydomain.tld smtp");
        users = new HashSet<User>();
    }

    public void configure() {
    }

    public void beforeTest() throws Exception {
    }
    
    public void afterTest() throws Exception {
        users.clear();
        resetData();
    }
    
    protected abstract void resetData() throws Exception;

    public abstract void createMailbox(MailboxPath mailboxPath) throws Exception;


    public void afterTests() throws Exception {
        // default do nothing
    }

    public void beforeTests() throws Exception {
        // default do nothing
    }
    
}
