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

package org.apache.james.imap.message.response;

import org.apache.james.core.quota.QuotaCount;
import org.apache.james.mailbox.model.Quota;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class QuotaResponseTest {
    @Test
    public void shouldMatchBeanContract() {
        Quota<QuotaCount> red = Quota.<QuotaCount>builder()
            .computedLimit(QuotaCount.count(36))
            .used(QuotaCount.count(22))
            .build();
        Quota<QuotaCount> black = Quota.<QuotaCount>builder()
            .computedLimit(QuotaCount.count(32))
            .used(QuotaCount.count(24))
            .build();

        EqualsVerifier.forClass(QuotaResponse.class)
            .withPrefabValues(Quota.class, red, black)
            .verify();
    }
}