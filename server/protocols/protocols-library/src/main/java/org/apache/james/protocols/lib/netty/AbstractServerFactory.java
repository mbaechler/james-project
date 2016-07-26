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

package org.apache.james.protocols.lib.netty;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

/**
 * Abstract base class for Factories that need to create {@link AbstractConfigurableAsyncServer}'s via configuration files
 */
public abstract class AbstractServerFactory implements Configurable, LogEnabled {

    private Logger log;
    private List<AbstractConfigurableAsyncServer> servers;
    private HierarchicalConfiguration config;

    /**
     * Create {@link AbstractConfigurableAsyncServer} servers, inject dependencies and configure them before return all fo them in a {@link List}
     * 
     * @param log
     * @param config
     * @return servers
     * @throws Exception
     */
    protected abstract List<AbstractConfigurableAsyncServer> createServers(Logger log, HierarchicalConfiguration config) throws Exception;
    
    @Override
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        this.config = config;
    }

    @Override
    public void setLog(Logger log) {
        this.log = log;
    }

    @PostConstruct
    public void init() throws Exception {
        servers = createServers(log, config);
        List<Future<?>> initTasks = Lists.newArrayList();
        ExecutorService executor = Executors.newCachedThreadPool();
        for (final AbstractConfigurableAsyncServer server: servers) {
            initTasks.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        server.init();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }));
        }
        for (Future<?> task: initTasks) {
            task.get();
        }
        executor.shutdown();
    }
    
    /**
     * Return all {@link AbstractConfigurableAsyncServer} instances that was create via this Factory
     * @return
     */
    public List<AbstractConfigurableAsyncServer> getServers() {
        return servers;
    }
    
    @PreDestroy
    public void destroy() throws InterruptedException, ExecutionException {
        List<Future<?>> destroyTasks = Lists.newArrayList();
        ExecutorService executor = Executors.newCachedThreadPool();
        for (final AbstractConfigurableAsyncServer server: servers) {
            destroyTasks.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        server.destroy();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }));
        }
        for (Future<?> task: destroyTasks) {
            task.get();
        }
        executor.shutdown();
    }
 
}
