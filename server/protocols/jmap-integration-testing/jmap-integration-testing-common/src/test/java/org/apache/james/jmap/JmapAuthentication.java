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

package org.apache.james.jmap;

import static io.restassured.RestAssured.with;

import java.io.InputStream;

import org.apache.james.jmap.api.access.AccessToken;

import com.jayway.jsonpath.JsonPath;

import io.restassured.http.ContentType;

public class JmapAuthentication {

    public static AccessToken authenticateJamesUser(String username, String password) {
        String continuationToken = getContinuationToken(username);
        InputStream json = with()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"" + password + "\"}")
                .post("/authentication")
                .body()
                .asInputStream();
        return AccessToken.fromString(
            JsonPath.parse(json).read("accessToken")
        );
    }

    private static String getContinuationToken(String username) {
        InputStream json = with()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"username\": \"" + username + "\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
        .post("/authentication")
            .body().asInputStream();
        return JsonPath.parse(json).read("continuationToken");
    }
}
