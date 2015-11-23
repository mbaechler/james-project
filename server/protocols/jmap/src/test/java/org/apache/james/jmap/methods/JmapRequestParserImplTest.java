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

package org.apache.james.jmap.methods;

import org.apache.james.jmap.model.ProtocolRequest;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableSet;

public class JmapRequestParserImplTest {

    @Test(expected=IllegalArgumentException.class)
    public void extractJmapRequestShouldThrowWhenNullRequestClass() throws Exception {
        JsonNode[] nodes = new JsonNode[] { new ObjectNode(new JsonNodeFactory(false)).textNode("unknwonMethod"),
                new ObjectNode(new JsonNodeFactory(false)).putObject("{\"id\": \"id\"}"),
                new ObjectNode(new JsonNodeFactory(false)).textNode("#1")} ;

        JmapRequestParserImpl jmapRequestParserImpl = new JmapRequestParserImpl(ImmutableSet.of(new Jdk8Module()));
        jmapRequestParserImpl.extractJmapRequest(ProtocolRequest.deserialize(nodes), null);
    }

    @Test
    public void extractJmapRequestShouldNotThrowWhenJsonContainsUnknownProperty() throws Exception {
        ObjectNode parameters = new ObjectNode(new JsonNodeFactory(false));
        parameters.put("id", "myId");
        JsonNode[] nodes = new JsonNode[] { new ObjectNode(new JsonNodeFactory(false)).textNode("unknwonMethod"),
                parameters,
                new ObjectNode(new JsonNodeFactory(false)).textNode("#1")} ;

        JmapRequestParserImpl jmapRequestParserImpl = new JmapRequestParserImpl(ImmutableSet.of(new Jdk8Module()));
        jmapRequestParserImpl.extractJmapRequest(ProtocolRequest.deserialize(nodes), RequestClass.class);
    }

    @Test
    public void extractJmapRequestShouldNotThrowWhenPropertyMissingInJson() throws Exception {
        ObjectNode parameters = new ObjectNode(new JsonNodeFactory(false));
        JsonNode[] nodes = new JsonNode[] { new ObjectNode(new JsonNodeFactory(false)).textNode("unknwonMethod"),
                parameters,
                new ObjectNode(new JsonNodeFactory(false)).textNode("#1")} ;

        JmapRequestParserImpl jmapRequestParserImpl = new JmapRequestParserImpl(ImmutableSet.of(new Jdk8Module()));
        jmapRequestParserImpl.extractJmapRequest(ProtocolRequest.deserialize(nodes), RequestClass.class);
    }

    private static class RequestClass implements JmapRequest {

        @SuppressWarnings("unused")
        public String parameter;
    
    }
}
