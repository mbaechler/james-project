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

package org.apache.james.transport.matchers.dlp;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.core.Domain;
import org.apache.james.dlp.api.DLPConfigurationItem;
import org.apache.james.dlp.api.DLPConfigurationStore;

public interface DlpRulesLoader {

    DlpDomainRules load(Domain domain);

    class Impl implements DlpRulesLoader {

        private final DLPConfigurationStore configurationStore;

        @Inject
        public Impl(DLPConfigurationStore configurationStore) {
            this.configurationStore = configurationStore;
        }

        @Override
        public DlpDomainRules load(Domain domain) {
          return toRules(configurationStore.list(domain));
        }

        private DlpDomainRules toRules(Stream<DLPConfigurationItem> items) {
            DlpDomainRules.DlpDomainRulesBuilder builder = DlpDomainRules.builder();
            items.forEach(item -> {
                if (item.getTargets().isContentTargeted()) {
                    builder.contentRule(item.getId(), Pattern.compile(item.getRegexp()));
                }
                if (item.getTargets().isRecipientTargeted()) {
                    builder.recipientRule(item.getId(), Pattern.compile(item.getRegexp()));
                }
                if (item.getTargets().isSenderTargeted()) {
                    builder.senderRule(item.getId(), Pattern.compile(item.getRegexp()));
                }
            });
            return builder.build();
        }
    }
}
