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

package org.apache.james.dlp.api;

import static org.apache.james.dlp.api.DLPFixture.RULE;
import static org.apache.james.dlp.api.DLPFixture.RULE_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.apache.james.core.Domain;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

public interface DLPRuleStoreContract {

    @Test
    default void listShouldReturnEmptyWhenNone(DLPRulesStore dlpRulesStore) {
        assertThat(dlpRulesStore.list(Domain.LOCALHOST))
            .isEmpty();
    }

    @Test
    default void listShouldReturnExistingEntries(DLPRulesStore dlpRulesStore) {
        dlpRulesStore.store(Domain.LOCALHOST, RULE);
        dlpRulesStore.store(Domain.LOCALHOST, RULE_2);

        assertThat(dlpRulesStore.list(Domain.LOCALHOST)).containsOnly(RULE, RULE_2);
    }

    @Test
    default void listShouldNotReturnEntriesOfOtherDomains(DLPRulesStore dlpRulesStore) {
        dlpRulesStore.store(Domain.LOCALHOST, RULE);
        dlpRulesStore.store(Domain.of("any.com"), RULE_2);

        assertThat(dlpRulesStore.list(Domain.LOCALHOST)).containsOnly(RULE);
    }

    @Test
    default void clearShouldRemoveAllEnriesOfADomain(DLPRulesStore dlpRulesStore) {
        dlpRulesStore.store(Domain.LOCALHOST, RULE);
        dlpRulesStore.store(Domain.LOCALHOST, RULE_2);

        dlpRulesStore.clear(Domain.LOCALHOST);

        assertThat(dlpRulesStore.list(Domain.LOCALHOST)).isEmpty();
    }

    @Test
    default void clearShouldNotFailWhenDomainDoesNotExist(DLPRulesStore dlpRulesStore) {
        assertThatCode(() -> dlpRulesStore.clear(Domain.LOCALHOST))
            .doesNotThrowAnyException();
    }

    @Test
    default void clearShouldOnlyRemoveEntriesOfADomain(DLPRulesStore dlpRulesStore) {
        Domain otherDomain = Domain.of("any.com");
        dlpRulesStore.store(Domain.LOCALHOST, RULE);
        dlpRulesStore.store(otherDomain, RULE_2);

        dlpRulesStore.clear(Domain.LOCALHOST);

        assertThat(dlpRulesStore.list(otherDomain)).containsOnly(RULE_2);
    }
    @Test
    default void clearShouldOnlyRemovePreviouslyExistingEntries(DLPRulesStore dlpRulesStore) {
        dlpRulesStore.store(Domain.LOCALHOST, ImmutableList.of(RULE, RULE_2));

        dlpRulesStore.clear(Domain.LOCALHOST);

        dlpRulesStore.store(Domain.LOCALHOST, ImmutableList.of(RULE));

        assertThat(dlpRulesStore.list(Domain.LOCALHOST)).containsOnly(RULE);
    }

    @Test
    default void storeShouldNotStoreDuplicates(DLPRulesStore dlpRulesStore) {
        dlpRulesStore.store(Domain.LOCALHOST, ImmutableList.of(RULE, RULE_2));
        dlpRulesStore.store(Domain.LOCALHOST, ImmutableList.of(RULE));

        assertThat(dlpRulesStore.list(Domain.LOCALHOST)).containsOnly(RULE, RULE_2);
    }
}
