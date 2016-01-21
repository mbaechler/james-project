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
package org.apache.james.jmap.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.NotImplementedException;
import org.apache.james.jmap.methods.JmapRequest;
import org.apache.james.util.streams.Collectors;

import java.util.List;
import java.util.Optional;

@JsonDeserialize(builder = GetMailboxesRequest.Builder.class)
public class GetMailboxesRequest implements JmapRequest {

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private String accountId;
        private ImmutableList.Builder<String> ids;
        private Optional<ImmutableSet<MailboxProperty>> properties;

        private Builder() {
            ids = ImmutableList.builder();
            properties = Optional.empty();
        }

        public Builder accountId(String accountId) {
            if (accountId != null) {
                throw new NotImplementedException();
            }
            return this;
        }

        public Builder ids(List<String> ids) {
            if (ids != null) {
                throw new NotImplementedException();
            }
            return this;
        }

        public Builder properties(List<String> properties) {
            this.properties = Optional.of(
                properties.stream()
                    .map(MailboxProperty::findProperty)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toImmutableSet()));
            return this;
        }
        
        public GetMailboxesRequest build() {
            return new GetMailboxesRequest(Optional.ofNullable(accountId), ids.build(), properties);
        }
    }

    private final Optional<String> accountId;
    private final List<String> ids;
    private final Optional<ImmutableSet<MailboxProperty>> properties;

    private GetMailboxesRequest(Optional<String> accountId, List<String> ids, Optional<ImmutableSet<MailboxProperty>> properties) {
        this.accountId = accountId;
        this.ids = ids;
        this.properties = properties;
    }

    public Optional<String> getAccountId() {
        return accountId;
    }

    public List<String> getIds() {
        return ids;
    }

    public Optional<ImmutableSet<MailboxProperty>> getProperties() {
        return properties;
    }
}
