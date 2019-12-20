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

package org.apache.james.queue.jms;

import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.james.junit.ManagedTestResource;

import com.google.common.collect.ImmutableList;

public class BrokerExtension {

    public static String generateRandomQueueName(BrokerService broker) {
        String queueName = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(10);
        BrokerExtension.enablePrioritySupport(broker, queueName);
        return queueName;
    }

    private static void enablePrioritySupport(BrokerService aBroker, String queueName) {
        PolicyMap pMap = new PolicyMap();
        PolicyEntry entry = new PolicyEntry();
        entry.setPrioritizedMessages(true);
        entry.setQueue(queueName);
        pMap.setPolicyEntries(ImmutableList.of(entry));
        aBroker.setDestinationPolicy(pMap);
    }

    public static BrokerService buildBroker() {
        BrokerService broker = new BrokerService();
        broker.setPersistent(false);
        broker.setUseJmx(false);
        try {
            broker.addConnector("tcp://127.0.0.1:61616");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return broker;
    }

    public static ManagedTestResource defaultBroker() {
        return managed(buildBroker());
    }

    public static ManagedTestResource brokerWithStatistics() {
        BrokerService broker = buildBroker();
        enableStatistics(broker);

        return managed(broker);
    }

    public static ManagedTestResource managed(BrokerService broker) {
        return ManagedTestResource.forResource(broker)
            .beforeAll(BrokerService::start)
            .afterAll(BrokerService::stop)
            .resolveAs(BrokerService.class)
            .build();
    }


    private static void enableStatistics(BrokerService broker) {
        broker.setPlugins(new BrokerPlugin[]{new StatisticsBrokerPlugin()});
        broker.setEnableStatistics(true);
    }

}
