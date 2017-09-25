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

package org.apache.james.jmap.model.mailbox;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxACL.EntryKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class Rights {
    public enum Right {
        Administer('a'),
        Expunge('e'),
        Insert('i'),
        Lookup('l'),
        Read('r'),
        Seen('s'),
        T_Delete('t'),
        Write('w');

        private final char imapRight;

        Right(char imapRight) {
            this.imapRight = imapRight;
        }

        @JsonValue
        public char getImapRight() {
            return imapRight;
        }

        public static boolean exists(char c) {
            return Arrays.stream(values())
                .anyMatch(right -> right.getImapRight() == c);
        }

        public static Right forChar(char c) {
            return Arrays.stream(values())
                .filter(right -> right.getImapRight() == c)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No matching right for '" + c + "'"));
        }
    }

    public static class Username {
        private final String value;

        public Username(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof Username) {
                Username username = (Username) o;

                return Objects.equals(this.value, username.value);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class Builder {
        private Multimap<Username, Right> rights;

        public Builder() {
            rights = ArrayListMultimap.create();
        }

        public Builder delegateTo(Username username, Right... rights) {
            delegateTo(username, Arrays.asList(rights));
            return this;
        }

        public Builder delegateTo(Username username, Collection<Right> rights) {
            this.rights.putAll(username, rights);
            return this;
        }

        public Builder combine(Builder builder) {
            this.rights.putAll(builder.rights);
            return this;
        }

        public Rights build() {
            return new Rights(rights);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Rights fromACL(MailboxACL acl) {
        return acl.getEntries()
            .entrySet()
            .stream()
            .filter(entry -> isSupported(entry.getKey()))
            .map(Rights::toRightsBuilder)
            .reduce(builder(), Builder::combine)
            .build();
    }

    private static Builder toRightsBuilder(Map.Entry<EntryKey, MailboxACL.Rfc4314Rights> entry) {
        return builder().delegateTo(
            new Username(entry.getKey().getName()),
            fromACL(entry.getValue()));
    }

    private static List<Right> fromACL(MailboxACL.Rfc4314Rights rights) {
        return rights.list()
            .stream()
            .map(MailboxACL.Right::asCharacter)
            .filter(Rights::existingChar)
            .map(Right::forChar)
            .collect(Guavate.toImmutableList());
    }

    private static boolean existingChar(Character c) {
        if (!Right.exists(c)) {
            LOGGER.warn("Non handled right '" + c + "'");
            return false;
        }
        return true;
    }

    private static boolean isSupported(EntryKey key) {
        if (key.isNegative()) {
            LOGGER.warn("Negative keys are not supported");
            return false;
        }
        if (key.getNameType() != MailboxACL.NameType.user) {
            LOGGER.warn(key.getNameType() + " is not sopported. Onlu 'user' is.");
            return false;
        }
        return true;
    }

    public static final Rights EMPTY = new Rights(ArrayListMultimap.create());

    private static final Logger LOGGER = LoggerFactory.getLogger(Rights.class);

    private final Multimap<Username, Right> rights;

    public Rights(Multimap<Username, Right> rights) {
        this.rights = rights;
    }

    @JsonAnyGetter
    public Map<Username, Collection<Right>> getRights() {
        return rights.asMap();
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof Rights) {
            Rights that = (Rights) o;

            return Objects.equals(this.rights, that.rights);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(rights);
    }
}
