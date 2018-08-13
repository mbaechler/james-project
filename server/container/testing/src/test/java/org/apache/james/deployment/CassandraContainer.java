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

package org.apache.james.deployment;

import org.apache.james.backends.cassandra.CassandraWaitStrategy;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

public class CassandraContainer extends GenericContainer<CassandraContainer>{

    private static final int CASSANDRA_PORT = 9042;

    static final int cassandraMemory = 1000;
    static final long cassandraContainerMemory = Float.valueOf(cassandraMemory * 1.2f * 1024 * 1024L).longValue();
    
    public CassandraContainer() {
        super("cassandra:3.11.3");
        withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withTmpFs(ImmutableMap.of("/var/lib/cassandra", "rw,noexec,nosuid,size=200m")));
        withCreateContainerCmdModifier(cmd -> cmd.withMemory(cassandraContainerMemory));
        withExposedPorts(CASSANDRA_PORT);
        withEnv("MAX_HEAP_SIZE", "400M");
        withEnv("HEAP_NEWSIZE", "400M");
        
        waitingFor(new CassandraWaitStrategy(this));
    }
    

}
