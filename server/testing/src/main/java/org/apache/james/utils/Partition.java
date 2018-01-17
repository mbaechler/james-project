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

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeMap;
import io.vavr.collection.Stream;

public class Partition<T> {


    public static <T> Partition<T> create(ImmutableMultimap<Integer, T> distribution) {
        Preconditions.checkArgument(distribution.keySet().stream().allMatch(i -> i != null && i >= 0));
        Preconditions.checkArgument(distribution.values().stream().allMatch(Objects::nonNull));
        Preconditions.checkArgument(distribution.keySet().stream().mapToInt(Integer::intValue).sum() > 0);
        return new Partition<>(distribution);
    }

    private final TreeRangeMap<Integer, T> rangeSet;

    private Partition(ImmutableMultimap<Integer, T> distribution) {
        rangeSet = generateRangeSet(distribution);
    }

    private TreeRangeMap<Integer, T> generateRangeSet(ImmutableMultimap<Integer, T> distribution) {
        List<Map.Entry<Integer, T>> entries = distribution.entries().stream().filter(e -> e.getKey() > 0).collect(Collectors.toList());

        TreeRangeMap<Integer, T> rangeSet = TreeRangeMap.create();
        int start = 0;
        for (Map.Entry<Integer, T> entry: entries) {
            rangeSet.put(Range.closedOpen(start, start + entry.getKey()), entry.getValue());
            start = rangeSet.span().upperEndpoint();
        }
        return rangeSet;
    }

    public Stream<T> generateRandomStream() {
        int max = rangeSet.span().upperEndpoint() - 1;
        Random random = new SecureRandom();

        return Stream.continually(() -> rangeSet.get(random.nextInt(max + 1)));
    }


}
