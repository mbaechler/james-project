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

package org.apache.james.mailbox.quota.model;

import static org.apache.james.mailbox.quota.model.QuotaThresholdFixture._75;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class QuotaThresholdChangeTest {


    private static final Clock CLOCK = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    private static final Instant NOW = Instant.now(CLOCK);
    private static final Instant ONE_HOUR_AGO = NOW.minus(Duration.ofHours(1));
    private static final Instant TWO_HOURS_AGO = NOW.minus(Duration.ofHours(2));
    private static final Instant THREE_HOURS_AGO = NOW.minus(Duration.ofHours(3));

    @Test
    void shouldMatchBeanContract() {
        EqualsVerifier.forClass(QuotaThresholdChange.class)
            .allFieldsShouldBeUsed()
            .verify();
    }

    @Test
    void isNotOlderThanShouldReturnTrueWhenRecent() {
        QuotaThresholdChange change = new QuotaThresholdChange(_75, TWO_HOURS_AGO);
        assertThat(change.isAfter(THREE_HOURS_AGO)).isTrue();
    }

    @Test
    void isNotOlderThanShouldReturnFalseWhenOld() {
        QuotaThresholdChange change = new QuotaThresholdChange(_75, TWO_HOURS_AGO);

        assertThat(change.isAfter(ONE_HOUR_AGO)).isFalse();
    }

}