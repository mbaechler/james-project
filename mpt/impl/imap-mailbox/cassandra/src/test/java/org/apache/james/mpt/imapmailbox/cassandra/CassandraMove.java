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

package org.apache.james.mpt.imapmailbox.cassandra;

import org.apache.james.backends.cassandra.DockerCassandraRule;
import org.apache.james.mpt.api.ImapHostSystem;
import org.apache.james.mpt.imapmailbox.suite.Move;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class CassandraMove extends Move {

    @ClassRule public static DockerCassandraRule cassandraServer = new DockerCassandraRule();

    private ImapHostSystem system;
    
    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new CassandraMailboxTestModule(cassandraServer.getIp(), cassandraServer.getBindingPort()));
        system = injector.getInstance(ImapHostSystem.class);
        system.beforeTest();
        super.setUp();
    }
    
    @Override
    protected ImapHostSystem createImapHostSystem() {
        return system;
    }

    @After
    public void tearDown() throws Exception {
        system.afterTest();
    }
    
}
