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
package org.apache.james.mailbox.inmemory.quota;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.MaxQuotaManager;
import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.util.OptionalUtils;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;

public class InMemoryPerUserMaxQuotaManager implements MaxQuotaManager {

    private Optional<QuotaCount> maxMessage = Optional.empty();
    private Optional<QuotaSize> maxStorage = Optional.empty();

    private final Map<String, QuotaCount> domainMaxMessage = new ConcurrentHashMap<>();
    private final Map<String, QuotaSize> domainMaxStorage = new ConcurrentHashMap<>();

    private final Map<String, QuotaSize> userMaxStorage = new ConcurrentHashMap<>();
    private final Map<String, QuotaCount> userMaxMessage = new ConcurrentHashMap<>();

    @Override
    public void setDefaultMaxStorage(QuotaSize maxStorage) {
        this.maxStorage = Optional.of(maxStorage);
    }

    @Override
    public void setDomainMaxMessage(String domain, QuotaCount count) {
        domainMaxMessage.put(domain, count);
    }

    @Override
    public void setDomainMaxStorage(String domain, QuotaSize size) {
        domainMaxStorage.put(domain, size);
    }

    @Override
    public void removeDomainMaxMessage(String domain) {
        domainMaxMessage.remove(domain);
    }

    @Override
    public void removeDomainMaxStorage(String domain) {
        domainMaxStorage.remove(domain);
    }

    @Override
    public Optional<QuotaSize> getMaxStorage(QuotaRoot quotaRoot) {
        return OptionalUtils.or(
            Optional.ofNullable(userMaxStorage.get(quotaRoot.getValue())),
            quotaRoot.getDomain().flatMap(this::getDomainMaxStorage),
            maxStorage);
    }

    @Override
    public Optional<QuotaCount> getMaxMessage(QuotaRoot quotaRoot) {
        return OptionalUtils.or(
            Optional.ofNullable(userMaxMessage.get(quotaRoot.getValue())),
            quotaRoot.getDomain().flatMap(this::getDomainMaxMessage),
            maxMessage);
    }

    @Override
    public Map<Quota.Scope, QuotaCount> listMaxMessagesDetails(QuotaRoot quotaRoot) {
        Function<String, Optional<QuotaCount>> domainQuotaFunction = Throwing.function(this::getDomainMaxMessage).sneakyThrow();
        return Stream.of(
                Pair.of(Quota.Scope.User, Optional.ofNullable(userMaxMessage.get(quotaRoot.getValue()))),
                Pair.of(Quota.Scope.Domain, quotaRoot.getDomain().flatMap(domainQuotaFunction)),
                Pair.of(Quota.Scope.Global, maxMessage))
            .filter(pair -> pair.getValue().isPresent())
            .collect(Guavate.toImmutableMap(Pair::getKey, value -> value.getValue().get()));
    }

    @Override
    public Map<Quota.Scope, QuotaSize> listMaxStorageDetails(QuotaRoot quotaRoot) {
        Function<String, Optional<QuotaSize>> domainQuotaFunction = Throwing.function(this::getDomainMaxStorage).sneakyThrow();
        return Stream.of(
                Pair.of(Quota.Scope.User, Optional.ofNullable(userMaxStorage.get(quotaRoot.getValue()))),
                Pair.of(Quota.Scope.Domain, quotaRoot.getDomain().flatMap(domainQuotaFunction)),
                Pair.of(Quota.Scope.Global, maxStorage))
            .filter(pair -> pair.getValue().isPresent())
            .collect(Guavate.toImmutableMap(Pair::getKey, value -> value.getValue().get()));
    }

    @Override
    public void setDefaultMaxMessage(QuotaCount maxMessage) {
        this.maxMessage = Optional.of(maxMessage);
    }

    @Override
    public Optional<QuotaCount> getDomainMaxMessage(String domain) {
        return Optional.ofNullable(domainMaxMessage.get(domain));
    }

    @Override
    public Optional<QuotaSize> getDomainMaxStorage(String domain) {
        return Optional.ofNullable(domainMaxStorage.get(domain));
    }

    @Override
    public void setMaxStorage(QuotaRoot user, QuotaSize maxStorageQuota) {
        userMaxStorage.put(user.getValue(), maxStorageQuota);
    }

    @Override
    public void setMaxMessage(QuotaRoot quotaRoot, QuotaCount maxMessageCount) {
        userMaxMessage.put(quotaRoot.getValue(), maxMessageCount);
    }

    @Override
    public Optional<QuotaSize> getDefaultMaxStorage() {
        return maxStorage;
    }

    @Override
    public Optional<QuotaCount> getDefaultMaxMessage() {
        return maxMessage;
    }

    @Override
    public void removeMaxMessage(QuotaRoot quotaRoot) {
        userMaxMessage.remove(quotaRoot.getValue());
    }

    @Override
    public void removeMaxStorage(QuotaRoot quotaRoot) {
        userMaxStorage.remove(quotaRoot.getValue());
    }

    @Override
    public void removeDefaultMaxStorage() {
        maxStorage = Optional.empty();
    }

    @Override
    public void removeDefaultMaxMessage() {
        maxMessage = Optional.empty();
    }
}
