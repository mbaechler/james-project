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

package org.apache.james.modules;

import javax.inject.Inject;

import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.MaxQuotaManager;
import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.quota.QuotaRootResolver;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.mailbox.store.mail.model.SerializableQuota;
import org.apache.james.mailbox.store.mail.model.SerializableQuotaValue;
import org.apache.james.mailbox.store.probe.QuotaProbe;
import org.apache.james.utils.GuiceProbe;

import com.github.fge.lambdas.Throwing;

public class QuotaProbesImpl implements QuotaProbe, GuiceProbe {

    private final MaxQuotaManager maxQuotaManager;
    private final QuotaRootResolver quotaRootResolver;
    private final QuotaManager quotaManager;

    @Inject
    public QuotaProbesImpl(MaxQuotaManager maxQuotaManager, QuotaRootResolver quotaRootResolver, QuotaManager quotaManager) {
        this.maxQuotaManager = maxQuotaManager;
        this.quotaRootResolver = quotaRootResolver;
        this.quotaManager = quotaManager;
    }

    @Override
    public String getQuotaRoot(String namespace, String user, String name) throws MailboxException {
        return quotaRootResolver.getQuotaRoot(new MailboxPath(namespace, user, name)).getValue();
    }

    @Override
    public SerializableQuota<QuotaCount> getMessageCountQuota(String quotaRoot) throws MailboxException {
        return SerializableQuota.newInstance(quotaManager.getMessageQuota(QuotaRoot.quotaRoot(quotaRoot)));
    }

    @Override
    public SerializableQuota<QuotaSize> getStorageQuota(String quotaRoot) throws MailboxException {
        return SerializableQuota.newInstance(quotaManager.getStorageQuota(QuotaRoot.quotaRoot(quotaRoot)));
    }

    @Override
    public SerializableQuotaValue<QuotaCount> getMaxMessageCount(String quotaRoot) throws MailboxException {
        return SerializableQuotaValue.valueOf(maxQuotaManager.getMaxMessage(QuotaRoot.quotaRoot(quotaRoot)));
    }

    @Override
    public SerializableQuotaValue<QuotaSize> getMaxStorage(String quotaRoot) throws MailboxException {
        return SerializableQuotaValue.valueOf(maxQuotaManager.getMaxStorage(QuotaRoot.quotaRoot(quotaRoot)));
    }

    @Override
    public SerializableQuotaValue<QuotaCount> getDefaultMaxMessageCount() throws MailboxException {
        return SerializableQuotaValue.valueOf(maxQuotaManager.getDefaultMaxMessage());
    }

    @Override
    public SerializableQuotaValue<QuotaSize> getDefaultMaxStorage() throws MailboxException {
        return SerializableQuotaValue.valueOf(maxQuotaManager.getDefaultMaxStorage());
    }

    @Override
    public void setMaxMessageCount(String quotaRoot, SerializableQuotaValue<QuotaCount> maxMessageCount) throws MailboxException {
        maxMessageCount.toValue(QuotaCount::count, QuotaCount.unlimited())
            .ifPresent(
                Throwing.consumer(
                    (QuotaCount value) -> maxQuotaManager.setMaxMessage(QuotaRoot.quotaRoot(quotaRoot), value))
                    .sneakyThrow());
    }

    @Override
    public void setMaxStorage(String quotaRoot, SerializableQuotaValue<QuotaSize> maxSize) throws MailboxException {
        maxSize.toValue(QuotaSize::size, QuotaSize.unlimited())
            .ifPresent(
                Throwing.consumer(
                    (QuotaSize value) -> maxQuotaManager.setMaxStorage(QuotaRoot.quotaRoot(quotaRoot), value))
                    .sneakyThrow());
    }

    @Override
    public void setDefaultMaxMessageCount(SerializableQuotaValue<QuotaCount> maxDefaultMessageCount) throws MailboxException {
        maxDefaultMessageCount.toValue(QuotaCount::count, QuotaCount.unlimited())
            .ifPresent(Throwing.consumer(maxQuotaManager::setDefaultMaxMessage).sneakyThrow());
    }

    @Override
    public void setDefaultMaxStorage(SerializableQuotaValue<QuotaSize> maxDefaultSize) throws MailboxException {
        maxDefaultSize.toValue(QuotaSize::size, QuotaSize.unlimited())
            .ifPresent(Throwing.consumer(maxQuotaManager::setDefaultMaxStorage).sneakyThrow());
    }
}
