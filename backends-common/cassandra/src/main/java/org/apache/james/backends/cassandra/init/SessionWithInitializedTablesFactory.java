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

package org.apache.james.backends.cassandra.init;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionWithInitializedTablesFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger("TIMINGS");
    
    private final static String DEFAULT_KEYSPACE_NAME = "apache_james";

    private CassandraModule module;

    public SessionWithInitializedTablesFactory(CassandraModule module) {
        this.module = module;
    }

    public Session createSession(Cluster cluster, String keyspace) {
        LOGGER.warn("getting connection");
        Session session = cluster.connect(keyspace);
        LOGGER.warn("creating types");
        new CassandraTypesCreator(module, session)
            .initializeTypes();
        LOGGER.warn("creating tables");
        new CassandraTableManager(module, session)
            .ensureAllTables();
        LOGGER.warn("tables created");
        return session;
    }

    public Session createSession(Cluster cluster) {
        return createSession(cluster, DEFAULT_KEYSPACE_NAME);
    }

}
