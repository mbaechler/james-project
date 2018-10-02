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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;

public class AttributeValueTest {
    @Test
    void shouldRespectBeanContract() {
        EqualsVerifier.forClass(AttributeValue.class).verify();
    }

    @Test
    void stringShouldBeSerializedAndBack() {
        AttributeValue<String> expected = AttributeValue.of("value");
        
        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);
        
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void booleanShouldBeSerializedAndBack() {
        AttributeValue<Boolean> expected = AttributeValue.of(true);

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }


    @Test
    void intShouldBeSerializedAndBack() {
        AttributeValue<Integer> expected = AttributeValue.of(42);
        
        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);
        
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void emptyStringListShouldBeSerializedAndBack() {
        AttributeValue<?> expected = AttributeValue.of(ImmutableList.<String>of());
        
        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);
        
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void listShouldBeSerializedAndBack() {
        AttributeValue<?> expected = AttributeValue.of(ImmutableList.of(AttributeValue.of("first"), AttributeValue.of("second")));
        
        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);
        
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void emptyMapShouldBeSerializedAndBack() {
        AttributeValue<?> expected = AttributeValue.of(ImmutableMap.of());
        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);
        
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void mapWithPrimitiveTypesShouldBeSerializedAndBack() {
        AttributeValue<?> expected = AttributeValue.of(ImmutableMap.of("a", AttributeValue.of("value"), "b", AttributeValue.of(12)));
        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnStringAttributeValueWhenString() throws Exception {
        AttributeValue<String> expected = AttributeValue.of("value");

        AttributeValue<?> actual = AttributeValue.fromJsonString("\"value\"");
        
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnIntAttributeValueWhenInt() throws Exception {
        AttributeValue<Integer> expected = AttributeValue.of(42);

        AttributeValue<?> actual = AttributeValue.fromJsonString("42");
        
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnEmptyListAttributeValueWhenEmptyArray() throws Exception {
        AttributeValue<?> expected = AttributeValue.of(ImmutableList.of());

        AttributeValue<?> actual = AttributeValue.fromJsonString("[]");
        
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnListAttributeValueWhenArray() throws Exception {
        AttributeValue<?> expected = AttributeValue.of(ImmutableList.of(AttributeValue.of("first"), AttributeValue.of("second")));

        AttributeValue<?> actual = AttributeValue.fromJsonString("[\"first\",\"second\"]");
        
        assertThat(actual).isEqualTo(expected);
    }
}
