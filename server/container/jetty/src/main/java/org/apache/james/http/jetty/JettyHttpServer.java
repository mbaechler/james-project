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

package org.apache.james.http.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class JettyHttpServer {
    
    public static JettyHttpServer start(Configuration configuration) throws Exception {
        return new JettyHttpServer(configuration).start();
    }

    private Server server;
    private ServerConnector serverConnector;

    private JettyHttpServer(Configuration configuration) {
        server = new Server();
        serverConnector = new ServerConnector(server);
        server.addConnector(serverConnector);
        ServletHandler servletHandler = buildServletHandler(configuration);
        server.setHandler(servletHandler);
    }

    private ServletHandler buildServletHandler(Configuration configuration) {
        ServletHandler servletHandler = new ServletHandler();
        configuration.getMappings().forEach((path, servlet) -> servletHandler.addServletWithMapping(new ServletHolder(servlet), path));
        return servletHandler;
    }
    
    private JettyHttpServer start() throws Exception {
        server.start();
        return this;
    }
    
    public void stop() throws Exception {
        server.stop();
    }

    public int getPort() {
        return serverConnector.getLocalPort();
    }
    
}
