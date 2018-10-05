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

package org.apache.james.quota.search;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public class Offset {
    public static Offset of(int value) {
        return new Offset(value);
    }

    public static Offset none() {
        return new Offset(0);
    }

    private final int value;

    private Offset(int value) {
        Preconditions.checkArgument(value >= 0, "An offset can not be negative");
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof Offset) {
            Offset offset = (Offset) o;

            return Objects.equals(this.value, offset.value);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("value", value)
            .toString();
    }
}
