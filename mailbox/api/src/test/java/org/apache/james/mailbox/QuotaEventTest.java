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

package org.apache.james.mailbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import org.apache.james.core.User;
import org.apache.james.core.quota.QuotaCount;
import org.apache.james.core.quota.QuotaSize;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.scala.ScalaQuotaEvent$;
import org.junit.jupiter.api.Test;

public class QuotaEventTest {


    /*
    @Test
    void quotaNoopShouldBeEqualsToAnotherInstance() {
        QuotaNoop foo = new QuotaNoop(User.fromUsername("foo@bar.com"), QuotaRoot.quotaRoot("foo", Optional.empty()));
        QuotaNoop foo2 = new QuotaNoop(User.fromUsername("foo@bar.com"), QuotaRoot.quotaRoot("foo", Optional.empty()));
        assertThat(foo).isEqualTo(foo2);
    }

    @Test
    void quotaNoopShouldNotBeEqualToDifferentValue() {
        QuotaNoop foo = new QuotaNoop(User.fromUsername("foo@bar.com"), QuotaRoot.quotaRoot("foo", Optional.empty()));
        QuotaNoop foo2 = new QuotaNoop(User.fromUsername("joe@bar.com"), QuotaRoot.quotaRoot("foo", Optional.empty()));
        assertThat(foo).isNotEqualTo(foo2);
    }

    @Test
    void quotaNoopShouldNotBeEqualToDifferentCopy() {
        QuotaNoop foo = new QuotaNoop(User.fromUsername("foo@bar.com"), QuotaRoot.quotaRoot("foo", Optional.empty()));
        QuotaNoop foo2 = foo.copy(foo.copy$default$1(), QuotaRoot.quotaRoot("bar", Optional.empty()));;
        assertThat(foo).isNotEqualTo(foo2);
    }*/

    /*@Test
    void quotaNoopShouldBeSerializableToJson() {
        QuotaNoop foo = new QuotaNoop(User.fromUsername("foo@bar.com"), QuotaRoot.quotaRoot("foo", Optional.empty()));
        assertThat(QuotaEvent$.MODULE$.toJson(foo)).isEqualTo("{\"user\":\"foo@bar.com\",\"quotaRoot\":\"foo\"}");
    }*/


    private static final MailboxListener.QuotaUsageUpdatedEvent QUOTA_USAGE_UPDATED_EVENT =
        new MailboxListener.QuotaUsageUpdatedEvent(
            null,
            QuotaRoot.quotaRoot("foo", Optional.empty()),
            Quota.<QuotaCount>builder().used(QuotaCount.count(12)).computedLimit(QuotaCount.count(100)).build(),
            Quota.<QuotaSize>builder().used(QuotaSize.size(1234)).computedLimit(QuotaSize.size(10000)).build(),
            Instant.parse("2018-11-13T12:00:55Z"));

    private static final String QUOTA_USAGE_UPDATED_EVENT_JSON = "{\"QuotaUsageUpdatedEvent\":{\"quotaRoot\":\"foo\"," +
        "\"countQuota\":{\"used\":12,\"limit\":100,\"limits\":{}}," +
        "\"sizeQuota\":{\"used\":1234,\"limit\":10000,\"limits\":{}}," +
        "\"time\":\"2018-11-13T12:00:55Z\"}}";

    @Test
    void quotaUsageUpdatedShouldBeSerializableToJson() {
        assertThat(ScalaQuotaEvent$.MODULE$.toJson(QUOTA_USAGE_UPDATED_EVENT))
            .isEqualTo(QUOTA_USAGE_UPDATED_EVENT_JSON);
    }

    @Test
    void quotaUsageUpdatedShouldBeDeSerializableFromJson() {
        assertThat(ScalaQuotaEvent$.MODULE$.fromJson(QUOTA_USAGE_UPDATED_EVENT_JSON).get())
            .isEqualTo(QUOTA_USAGE_UPDATED_EVENT);
    }


}
