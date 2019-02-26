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

package org.apache.james.vault.search;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

public interface ValueMatcher<T> {

    boolean matches(T referenceValue);

    class SingleValueMatcher<T> implements ValueMatcher<T> {

        private static final BiFunction<String, String, Boolean> CONTAINS = (testedValue, referenceValue) -> referenceValue.contains(testedValue);
        private static final BiFunction<String, String, Boolean> CONTAINS_IGNORE_CASE =
            (testedValue, referenceValue) -> referenceValue.toLowerCase(Locale.US).contains(testedValue.toLowerCase(Locale.US));
        private static final BiFunction<ZonedDateTime, ZonedDateTime, Boolean> BEFORE_OR_EQUALS =
            (testedValue, referenceValue) -> referenceValue.isBefore(testedValue) || referenceValue.isEqual(testedValue);
        private static final BiFunction<ZonedDateTime, ZonedDateTime, Boolean> AFTER_OR_EQUALS =
            (testedValue, referenceValue) -> referenceValue.isAfter(testedValue) || referenceValue.isEqual(testedValue);

        static SingleValueMatcher<String> contains(String testedValue) {
            return new SingleValueMatcher<>(testedValue, CONTAINS);
        }

        static SingleValueMatcher<String> containsIgnoreCase(String testedValue) {
            return new SingleValueMatcher<>(testedValue, CONTAINS_IGNORE_CASE);
        }

        static SingleValueMatcher<ZonedDateTime> beforeOrEquals(ZonedDateTime testedValue) {
            return new SingleValueMatcher<>(testedValue, BEFORE_OR_EQUALS);
        }

        static SingleValueMatcher<ZonedDateTime> afterOrEquals(ZonedDateTime testedValue) {
            return new SingleValueMatcher<>(testedValue, AFTER_OR_EQUALS);
        }

        static <T> SingleValueMatcher<T> isEquals(T testedValue) {
            return new SingleValueMatcher<>(testedValue, Object::equals);
        }

        private final T testedValue;
        private final BiFunction<T, T, Boolean> matchesFunction;

        private SingleValueMatcher(T testedValue, BiFunction<T, T, Boolean> matchesFunction) {
            this.testedValue = testedValue;
            this.matchesFunction = matchesFunction;
        }

        @Override
        public boolean matches(T referenceValue) {
            return matchesFunction.apply(testedValue, referenceValue);
        }
    }

    class ListContains<T> implements ValueMatcher<List<T>> {

        static <T> ListContains<T> of(T testedValue) {
            return new ListContains<>(testedValue);
        }

        private final T testedValue;

        private ListContains(T testedValue) {
            this.testedValue = testedValue;
        }

        @Override
        public boolean matches(List<T> referenceValue) {
            return referenceValue.contains(testedValue);
        }
    }
}
