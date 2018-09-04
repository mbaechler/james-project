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

package org.apache.james.queue.rabbitmq;

import java.util.Objects;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

final class MailQueueName {

    private static final String PREFIX = "JamesMailQueue";
    private static final String EXCHANGE_PREFIX = PREFIX + "-exchange-";
    @VisibleForTesting static final String WORKQUEUE_PREFIX = PREFIX + "-workqueue-";

    static MailQueueName fromString(String name) {
        Preconditions.checkNotNull(name);
        return new MailQueueName(name);
    }

    static Optional<MailQueueName> fromRabbitWorkQueueName(String workQueueName) {
        return Optional.of(workQueueName)
            .filter(MailQueueName::isMailQueueName)
            .map(name -> new MailQueueName(toMailqueueName(workQueueName)));
    }

    private static boolean isMailQueueName(String name) {
        return name.startsWith(WORKQUEUE_PREFIX);
    }

    private static String toMailqueueName(String name) {
        return name.substring(WORKQUEUE_PREFIX.length());
    }

    private final String name;

    private MailQueueName(String name) {
        this.name = name;
    }

    String asString() {
        return name;
    }

    String toRabbitWorkQueueName() {
        return WORKQUEUE_PREFIX + name;
    }

    String toRabbitExchangeName() {
        return EXCHANGE_PREFIX + name;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MailQueueName) {
            MailQueueName that = (MailQueueName) o;
            return Objects.equals(name, that.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .toString();
    }
}
