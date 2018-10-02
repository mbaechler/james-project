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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/** 
 * Serializer
 * 
 * @since Mailet API v3.2
 */
public interface Serializer<T> {
    JsonNode serialize(T object);

    class BooleanSerializer implements Serializer<Boolean> {
        @Override
        public JsonNode serialize(Boolean object) {
            return BooleanNode.valueOf(object);
        }

        @Override
        public boolean equals(Object other) {
            return this.getClass() == other.getClass();
        }
    }

    Serializer<Boolean> BOOLEAN_SERIALIZER = new BooleanSerializer();

    class StringSerializer implements Serializer<String> {
        @Override
        public JsonNode serialize(String object) {
            return TextNode.valueOf(object);
        }

        @Override
        public boolean equals(Object other) {
            return this.getClass() == other.getClass();
        }
    }

    Serializer<String> STRING_SERIALIZER = new StringSerializer();

    class IntSerializer implements Serializer<Integer> {
        @Override
        public JsonNode serialize(Integer object) {
            return IntNode.valueOf(object);
        }

        @Override
        public boolean equals(Object other) {
            return this.getClass() == other.getClass();
        }
    }

    Serializer<Integer> INT_SERIALIZER = new IntSerializer();

    class CollectionSerializer<U> implements Serializer<Collection<AttributeValue<U>>> {
        @Override
        public JsonNode serialize(Collection<AttributeValue<U>> object) {
            List<JsonNode> jsons = object.stream()
                .map(AttributeValue::toJson)
                .collect(ImmutableList.toImmutableList());
            return new ArrayNode(JsonNodeFactory.instance, jsons);
        }

        @Override
        public boolean equals(Object other) {
            return this.getClass() == other.getClass();
        }
    }

    class MapSerializer<U> implements Serializer<Map<String, AttributeValue<U>>> {
        @Override
        public JsonNode serialize(Map<String, AttributeValue<U>> object) {
            Map<String, JsonNode> jsonMap = object.entrySet().stream()
                .collect(ImmutableMap.toImmutableMap(Entry::getKey, entry -> entry.getValue().toJson()));
            return new ObjectNode(JsonNodeFactory.instance, jsonMap);
        }

        @Override
        public boolean equals(Object other) {
            return this.getClass() == other.getClass();
        }
    }
}
