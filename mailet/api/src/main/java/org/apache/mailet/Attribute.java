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

package org.apache.mailet;

import java.util.Objects;

/** 
 * Attribute
 * 
 * @since Mailet API v3.2
 */
public class Attribute {
    private final AttributeName name;
    private final Object /*for the moment*/ value;

    public Attribute(AttributeName name, Object value) {
        this.name = name;
        this.value = value;
    }

    public AttributeName getName() {
        return name;
    }

    public Object /*for the moment*/ getValue() {
        return value;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof Attribute) {
            Attribute that = (Attribute) o;

            return Objects.equals(this.name, that.name)
                && Objects.equals(this.value, that.value);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(name, value);
    }
}
