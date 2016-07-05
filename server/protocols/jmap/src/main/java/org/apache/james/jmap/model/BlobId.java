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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;

public class BlobId {

    public static BlobId of(String rawValue) {
        Preconditions.checkNotNull(rawValue);
        Preconditions.checkArgument(!rawValue.isEmpty());
        return new BlobId(rawValue);
    }

    private final String rawValue;
    
    private BlobId(String rawValue) {
        this.rawValue = rawValue;
    }
    
    @JsonValue
    public String getRawValue() {
        return rawValue;
    }
    
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof BlobId) {
            BlobId other = (BlobId) obj;
            return Objects.equals(this.rawValue, other.rawValue);
        }
        return false;
    }
    
    @Override
    public final int hashCode() {
        return Objects.hashCode(this.rawValue);
    }
}
