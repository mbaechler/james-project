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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

public class ProtocolRequest {

    public static ProtocolRequest deserialize(JsonNode[] json) {
        Preconditions.checkState(json.length == 3, "should have three elements");
        Preconditions.checkState(json[0].isTextual(), "first element should be a String");
        Preconditions.checkState(json[1].isObject(), "second element should be a Json");
        Preconditions.checkState(json[2].isTextual(), "third element should be a String");
        return new ProtocolRequest(json[0].textValue(), (ObjectNode) json[1], json[2].textValue());
    }

    private final String method;
    private final ObjectNode parameters;
    private final String clientId;

    protected ProtocolRequest(String method, ObjectNode parameters, String clientId) {
        this.method = method;
        this.parameters = parameters;
        this.clientId = clientId;
    }

    public String getMethod() {
        return method;
    }

    public ObjectNode getParameters() {
        return parameters;
    }

    public String getClientId() {
        return clientId;
    }
}