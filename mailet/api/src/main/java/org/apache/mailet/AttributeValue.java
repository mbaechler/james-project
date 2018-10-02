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

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.james.util.streams.Iterators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;

/** 
 * Strong typing for attribute value, which represent the value of an attribute stored in a mail.
 * 
 * @since Mailet API v3.2
 */
public class AttributeValue<T> {
    private final T value;
    private final Serializer<T> serializer;

    public static AttributeValue<Boolean> of(Boolean value) {
        return new AttributeValue<>(value, Serializer.BOOLEAN_SERIALIZER);
    }

    public static AttributeValue<String> of(String value) {
        return new AttributeValue<>(value, Serializer.STRING_SERIALIZER);
    }

    public static AttributeValue<Integer> of(Integer value) {
        return new AttributeValue<>(value, Serializer.INT_SERIALIZER);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static AttributeValue<Collection<AttributeValue<?>>> of(Collection<AttributeValue<?>> value) {
        return new AttributeValue<>(value, new Serializer.CollectionSerializer());
    }

    public static AttributeValue<Map<String, AttributeValue<?>>> of(Map<String, AttributeValue<?>> value) {
        return new AttributeValue<>(value, new Serializer.MapSerializer());
    }

    public static AttributeValue<Serializable> ofSerializable(Serializable value) {
        return new AttributeValue<>(value, new Serializer.FSTSerializer());
    }

    @SuppressWarnings("unchecked")
    public static AttributeValue<?> of(Object value) {
        if (value instanceof Boolean) {
            return of((Boolean) value);
        }
        if (value instanceof String) {
            return of((String) value);
        }
        if (value instanceof Integer) {
            return of((Integer) value);
        }
        if (value instanceof Collection<?>) {
            return of(((Collection<AttributeValue<?>>) value));
        }
        if (value instanceof Map<?,?>) {
            return of(((Map<String, AttributeValue<?>>) value));
        }
        if (value instanceof Serializable) {
            return ofSerializable((Serializable) value);
        }
        throw new IllegalArgumentException("input should at least be Serializable");
    }

    private AttributeValue(T value, Serializer<T> serializer) {
        this.value = value;
        this.serializer = serializer;
    }

    public T value() {
        return value;
    }

    //FIXME : poor performance
    public AttributeValue<T> duplicate() {
        return (AttributeValue<T>) fromJson(toJson());
    }

    public JsonNode toJson() {
        return serializer.serialize(value);
    }

    public static AttributeValue<?> fromJsonString(String json) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode tree = objectMapper.readTree(json);
        return fromJson(tree);
    }

    public static AttributeValue<?> fromJson(JsonNode input) {
        if (input instanceof BooleanNode) {
            return fromJson((BooleanNode) input);
        }
        if (input instanceof TextNode) {
            return fromJson((TextNode) input);
        }
        if (input instanceof IntNode) {
            return fromJson((IntNode) input);
        }
        if (input instanceof ArrayNode) {
            return fromJson((ArrayNode) input);
        }
        //FIXME how are we supposed to choose between a Map and an Object Graph from FST
        if (input instanceof ObjectNode) {
            return fromJson((ObjectNode) input);
        }
        throw new IllegalStateException("unable to deserialize type " + input.getNodeType());
    }

    public static AttributeValue<Boolean> fromJson(BooleanNode booleanAsJson) {
        return AttributeValue.of(booleanAsJson.asBoolean());
    }

    public static AttributeValue<String> fromJson(TextNode stringAsJson) {
        return AttributeValue.of(stringAsJson.asText());
    }

    public static AttributeValue<Integer> fromJson(IntNode intAsJson) {
        return AttributeValue.of(intAsJson.asInt());
    }

    public static AttributeValue<? extends Collection<?>> fromJson(ArrayNode arrayAsJson) {
        return AttributeValue.of(
            Iterators.toStream(arrayAsJson.elements())
                .map(AttributeValue::fromJson)
                .collect(ImmutableList.toImmutableList()));
    }

    public static AttributeValue<? extends Map<String, ?>> fromJson(ObjectNode mapAsJson) {
        return AttributeValue.of(
            Iterators.toStream(mapAsJson.fields())
                .collect(Guavate.toImmutableMap(
                    Map.Entry::getKey,
                    entry -> fromJson(entry.getValue())
                )));
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof AttributeValue) {
            AttributeValue<?> that = (AttributeValue<?>) o;

            return Objects.equals(this.value, that.value)
                && Objects.equals(this.serializer, that.serializer);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(value, serializer);
    }
}
