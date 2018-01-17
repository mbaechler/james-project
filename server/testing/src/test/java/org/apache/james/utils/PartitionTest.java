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

package org.apache.james.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMultimap;

class PartitionTest {

    @Test
    void createShouldNotSupportNegativeDistribution() {
        assertThatThrownBy(() -> Partition.create(ImmutableMultimap.of(-1, "a")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createShouldSupportZeroDistribution() {
        Partition<String> testee = Partition.create(ImmutableMultimap.of(0, "a", 1, "b"));

        assertThat(testee.generateRandomStream().take(10)).containsOnly("b");
    }

    @Test
    void createShouldNotSupportEmptyDistribution() {
        assertThatThrownBy(() -> Partition.create(ImmutableMultimap.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createShouldNotSupportEffectivelyEmptyDistribution() {
        assertThatThrownBy(() -> Partition.create(ImmutableMultimap.of(0, "a", 0, "b")))
            .isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    void streamOfSingleDistributionMapShouldAlwaysReturnSameElement() {
        Partition<Integer> testee = Partition.create(ImmutableMultimap.of(10, 1));

        assertThat(testee.generateRandomStream().take(10)).containsOnly(1);
    }

    @Test
    void streamOfEvenDistributionMapShouldReturnSameNumberOfEachElement() {
        Partition<String> testee = Partition.create(ImmutableMultimap.of(10, "a", 10, "b"));

        Map<String, Long> distribution = testee.generateRandomStream().take(1_000_000)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertThat(distribution.get("a")).isCloseTo(distribution.get("b"), Offset.offset(5_000L));
    }

    @Test
    void streamOfSpecificDistributionMapShouldReturnTwiceMoreA() {
        Partition<String> testee = Partition.create(ImmutableMultimap.of(20, "a", 10, "b"));

        Map<String, Long> distribution = testee.generateRandomStream().take(1_000_000)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertThat(distribution.get("a")).isCloseTo(distribution.get("b") * 2, Offset.offset(5_000L));
    }

    @Test
    void partitionShouldSupportDuplicatedDistributionEntry() {
        Partition<String> testee = Partition.create(ImmutableMultimap.of(10, "a", 10, "b", 10, "a"));

        Map<String, Long> distribution = testee.generateRandomStream().take(1_000_000)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertThat(distribution.get("a")).isCloseTo(distribution.get("b") * 2, Offset.offset(5_000L));
    }


}