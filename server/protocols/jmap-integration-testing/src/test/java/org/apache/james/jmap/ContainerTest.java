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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

public class ContainerTest {

    @Rule public GenericContainer container = new GenericContainer("nginx:1.7.1")
            .withExposedPorts(80);
    
    @Test
    public void containerShouldBeReachable() throws ClientProtocolException, IOException, URISyntaxException {
        String containerIpAddress = container.getContainerInfo().getNetworkSettings().getIpAddress();
        Response response = Request.Get(new URIBuilder().setScheme("http").setHost(containerIpAddress).setPort(80).build()).execute();
        assertThat(response.returnResponse().getStatusLine().getStatusCode()).isEqualTo(200);
    }
    
    @Test
    public void containerShouldBeReachableOnExposedPort() throws ClientProtocolException, IOException, URISyntaxException {
        Integer containerPort = container.getMappedPort(80);
        Response response = Request.Get(new URIBuilder().setScheme("http").setHost("localhost").setPort(containerPort).build()).execute();
        assertThat(response.returnResponse().getStatusLine().getStatusCode()).isEqualTo(200);
    }
    
    
}
