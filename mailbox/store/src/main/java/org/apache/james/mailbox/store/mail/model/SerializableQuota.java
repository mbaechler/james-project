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

package org.apache.james.mailbox.store.mail.model;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Function;

import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.quota.QuotaValue;

public class SerializableQuota<T extends QuotaValue<T>> implements Serializable {

    public static final long UNLIMITED = -1;

    public static <U extends QuotaValue<U>> SerializableQuota<U> newInstance(Quota<U> quota) {
        return new SerializableQuota<>(new SerializableQuotaValue<>(quota.getMax()), getUsed(quota.getUsed(), SerializableQuotaValue::new));
    }


    private static <U extends QuotaValue<U>> SerializableQuotaValue<U> getUsed(Optional<U> quota, Function<U, SerializableQuotaValue<U>> factory) {
        return quota.map(factory).orElse(null);
    }

    private final SerializableQuotaValue<T> max;
    private final SerializableQuotaValue<T> used;

    public SerializableQuota(SerializableQuotaValue<T> max, SerializableQuotaValue<T> used) {
        this.max = max;
        this.used = used;
    }


    public Long encodeAsLong() {
        return max.encodeAsLong();
    }

    public Long getUsed() {
        return used.encodeAsLong();
    }

}
