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

package org.apache.james.modules.mailrepository;

import javax.mail.internet.MimeMessage;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStoreDAO;
import org.apache.james.blob.api.BucketName;
import org.apache.james.blob.api.Store;
import org.apache.james.blob.mail.MimeMessagePartsId;
import org.apache.james.blob.mail.MimeMessageStore;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.mailrepository.api.MailRepositoryUrl;
import org.apache.james.mailrepository.api.Protocol;
import org.apache.james.mailrepository.blob.BlobMailRepository;
import org.apache.james.mailrepository.memory.MailRepositoryStoreConfiguration;
import org.apache.james.mailrepository.memory.MemoryMailRepositoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.ProvidesIntoSet;

public class BlobstoreMailRepositoryModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(MailRepositoryStoreConfiguration.Item.class)
                .toInstance(
                        new MailRepositoryStoreConfiguration.Item(
                                ImmutableList.of(new Protocol("blob")),
                                BlobMailRepository.class.getName(),
                                new BaseHierarchicalConfiguration())
                );
        bind(MailRepositoryStore.class).to(MemoryMailRepositoryStore.class);

    }

    @ProvidesIntoSet()
    public MailRepositoryFactory blobMailRepository(BlobStoreDAO blobStore,
                                                    BlobId.Factory blobIdFactory,
                                                    MimeMessageStore.Factory mimeFactory) {
        return new MailRepositoryFactory() {
            @Override
            public Class<? extends MailRepository> fqdn() {
                return BlobMailRepository.class;
            }

            @Override
            public MailRepository create(MailRepositoryUrl url) {
                BucketName metadataBucketName = BucketName.of(url.getPath() + "/mailMetadata");
                BucketName mailDataBucketName = BucketName.of(url.getPath() + "/mimeMessageData");
                Store<MimeMessage, MimeMessagePartsId> mimeMessageStore = mimeFactory.mimeMessageStore(mailDataBucketName);

                return new BlobMailRepository(blobStore, blobIdFactory, mimeMessageStore, metadataBucketName);
            }
        };
    }

}
