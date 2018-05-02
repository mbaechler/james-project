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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.mailbox.quota.mailing.QuotaMailingListenerConfiguration;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMailContext;

import com.google.common.collect.ImmutableList;

public interface QuotaThresholdFixture {
    QuotaThreshold _50 = new QuotaThreshold(0.50);
    QuotaThreshold _75 = new QuotaThreshold(0.75);
    QuotaThreshold _759 = new QuotaThreshold(0.759);
    QuotaThreshold _80 = new QuotaThreshold(0.8);
    QuotaThreshold _90 = new QuotaThreshold(0.9);
    QuotaThreshold _95 = new QuotaThreshold(0.95);
    QuotaThreshold _99 = new QuotaThreshold(0.99);

    interface Quotas {
        interface Counts {
            Quota<QuotaCount> _32_PERCENT = Quota.<QuotaCount>builder()
                .used(QuotaCount.count(32))
                .computedLimit(QuotaCount.count(100))
                .build();

            Quota<QuotaCount> _40_PERCENT = Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build();

            Quota<QuotaCount> _52_PERCENT = Quota.<QuotaCount>builder()
                .used(QuotaCount.count(52))
                .computedLimit(QuotaCount.count(100))
                .build();

            Quota<QuotaCount> _85_PERCENT = Quota.<QuotaCount>builder()
                .used(QuotaCount.count(85))
                .computedLimit(QuotaCount.count(100))
                .build();
        }
        interface Sizes {
            Quota<QuotaSize> _30_PERCENT = Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build();
            Quota<QuotaSize> _42_PERCENT = Quota.<QuotaSize>builder()
                .used(QuotaSize.size(42))
                .computedLimit(QuotaSize.size(100))
                .build();

            Quota<QuotaSize> _55_PERCENT = Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build();

            Quota<QuotaSize> _60_PERCENT = Quota.<QuotaSize>builder()
                .used(QuotaSize.size(60))
                .computedLimit(QuotaSize.size(100))
                .build();

        }
    }

    interface TestConstants {
        Duration GRACE_PERIOD = Duration.ofDays(1);
        QuotaThresholds SINGLE_THRESHOLD = new QuotaThresholds(ImmutableList.of(_50));
        QuotaMailingListenerConfiguration DEFAULT_CONFIGURATION = new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD);
        String BOB = "bob@domain";
        MockMailboxSession BOB_SESSION = new MockMailboxSession(BOB);
        Instant NOW = Instant.now();
        QuotaRoot QUOTAROOT = QuotaRoot.quotaRoot("any", Optional.empty());
        Instant ONE_HOUR_AGO = NOW.minus(Duration.ofHours(1));
        Instant SIX_HOURS_AGO = NOW.minus(Duration.ofHours(6));
        Instant TWELVE_HOURS_AGO = NOW.minus(Duration.ofHours(12));
        Instant TWO_DAYS_AGO = NOW.minus(Duration.ofDays(2));
        Instant SIX_DAYS_AGO = NOW.minus(Duration.ofDays(6));
        Instant TWELVE_DAYS_AGO = NOW.minus(Duration.ofDays(12));
        Instant ONE_DAY_AGO = NOW.minus(Duration.ofDays(1));;
    }

    static FakeMailContext mailetContext() {
        return FakeMailContext.builder()
            .postmaster(MailAddressFixture.POSTMASTER_AT_JAMES)
            .build();
    }

}
