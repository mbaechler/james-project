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
package org.apache.james.queue.activemq;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueueContract;
import org.apache.james.queue.api.MailQueueName;
import org.apache.james.queue.reactiveActivemq.ReactiveActiveMQMailQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ActiveMQMailQueueTest implements MailQueueContract {

    @Container
    public GenericContainer artemis = new GenericContainer("vromero/activemq-artemis:2.12.0").withExposedPorts(61616);

    ReactiveActiveMQMailQueue mailQueue;
    private String url;

    @BeforeEach
    public void setUp() throws Exception {
        Integer mappedPort = artemis.getMappedPort(61616);
        url = "tcp://localhost:" + mappedPort;
        ServerLocator serverLocator = ActiveMQClient.createServerLocator(url);
        ClientSessionFactory factory =  serverLocator.createSessionFactory();
        ClientSession session = factory.createSession("artemis","simetraehcapa", false, true, true,  serverLocator.isPreAcknowledge(), serverLocator.getAckBatchSize());

        MailQueueName queueName = MailQueueName.of(RandomStringUtils.random(10));
        mailQueue = new ReactiveActiveMQMailQueue(session, queueName);
    }


    @AfterEach
    public void tearDown() {
        //mailQueue.dispose();
    }

    @Override
    public MailQueue getMailQueue() {
        return mailQueue;
    }

}
