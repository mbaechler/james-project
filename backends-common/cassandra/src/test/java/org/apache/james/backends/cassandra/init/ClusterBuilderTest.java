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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;

class ClusterBuilderTest {

    @Test
    void consistencyLevelShouldBeEqualToQuorum() {
        Cluster cluster = ClusterBuilder.builder()
            .host("localhost")
            .port(ClusterBuilder.DEFAULT_CASSANDRA_PORT)
            .build();

        ConsistencyLevel consistencyLevel = cluster.getConfiguration()
                .getQueryOptions()
                .getConsistencyLevel();

        assertThat(consistencyLevel).isEqualTo(ConsistencyLevel.QUORUM);
    }

    @Test
    void refreshSchemaIntervalMillisShouldReturnDefaultValueWhenNotGiven() {
        Cluster cluster = ClusterBuilder.builder()
            .host("localhost")
            .port(ClusterBuilder.DEFAULT_CASSANDRA_PORT)
            .build();

        int refreshSchemaIntervalMillis = cluster.getConfiguration()
                .getQueryOptions()
                .getRefreshSchemaIntervalMillis();

        assertThat(refreshSchemaIntervalMillis).isEqualTo(1000);
    }

    @Test
    void refreshSchemaIntervalMillisShouldReturnCustomValueWhenGiven() {
        int expected = 123;
        Cluster cluster = ClusterBuilder.builder()
            .host("localhost")
            .port(ClusterBuilder.DEFAULT_CASSANDRA_PORT)
            .forTest()
            .refreshSchemaIntervalMillis(expected)
            .build();

        int refreshSchemaIntervalMillis = cluster.getConfiguration()
                .getQueryOptions()
                .getRefreshSchemaIntervalMillis();

        assertThat(refreshSchemaIntervalMillis).isEqualTo(expected);
    }
}
