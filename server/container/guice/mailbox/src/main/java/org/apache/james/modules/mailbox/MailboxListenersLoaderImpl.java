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
package org.apache.james.modules.mailbox;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.event.MailboxListenerRegistry;
import org.apache.james.util.OptionalUtils;
import org.apache.james.utils.ExtendedClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class MailboxListenersLoaderImpl implements Configurable, MailboxListenersLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailboxListenersLoaderImpl.class);

    private final MailboxListenerFactory mailboxListenerFactory;
    private final MailboxListenerRegistry registry;
    private final ExtendedClassLoader classLoader;
    private final Set<MailboxListener> guiceDefinedListeners;

    @Inject
    public MailboxListenersLoaderImpl(MailboxListenerFactory mailboxListenerFactory, MailboxListenerRegistry registry,
                                  ExtendedClassLoader classLoader, Set<MailboxListener> guiceDefinedListeners) {
        this.mailboxListenerFactory = mailboxListenerFactory;
        this.registry = registry;
        this.classLoader = classLoader;
        this.guiceDefinedListeners = guiceDefinedListeners;
    }

    @Override
    public void configure(HierarchicalConfiguration configuration) {
        LOGGER.info("Loading user registered mailbox listeners");

        ListenersConfiguration listenersConfiguration = ListenersConfiguration.from(configuration);

        guiceDefinedListeners.forEach(this::register);

        listenersConfiguration.getListenersConfiguration().stream()
            .map(this::createListener)
            .forEach(this::register);
    }

    @Override
    public void register(MailboxListener listener) {
        try {
            registry.addGlobalListener(listener);
        } catch (MailboxException e) {
            LOGGER.error("Error while registering global listener {}", listener, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public MailboxListener createListener(ListenerConfiguration configuration) {
        return OptionalUtils
            .orSuppliers(
                () -> createFromClass(configuration),
                () -> createFromFactory(configuration))
            .orElseThrow(() -> new RuntimeException("unexpected error loading mailbox listener"));
    }

    private Optional<MailboxListener> createFromClass(ListenerConfiguration configuration) {
        Optional<String> listenerClass = configuration.getClazz();
        if (listenerClass.isPresent()) {
            LOGGER.info("Loading user registered mailbox listener {}", listenerClass);
            try {
                Class<? extends MailboxListener> clazz = classLoader.locateClass(listenerClass.get());
                return Optional.of(mailboxListenerFactory.createMailboxListener(clazz));
            } catch (ClassNotFoundException e) {
                LOGGER.error("Error while loading user registered mailbox listener {}", listenerClass, e);
                throw new RuntimeException(e);
            }
        }
        return Optional.empty();
    }

    private Optional<MailboxListener> createFromFactory(ListenerConfiguration configuration) {
        Optional<String> factoryClass = configuration.getFactoryClass();
        if (factoryClass.isPresent()) {
            LOGGER.info("Loading user registered mailbox listener {}", factoryClass);
            try {
                Class<? extends ConfigurableMailboxListener.Factory> factoryClazz = classLoader.locateClass(factoryClass.get());
                ConfigurableMailboxListener.Factory factory = mailboxListenerFactory.createFactory(factoryClazz);
                MailboxListener.ExecutionMode executionMode = configuration.isAsync().map(this::getExecutionMode).orElse(MailboxListener.ExecutionMode.SYNCHRONOUS);
                HierarchicalConfiguration listenerConfiguration = configuration.getConfiguration().orElseGet(HierarchicalConfiguration::new);
                return Optional.of(factory.create(listenerConfiguration, executionMode));
            } catch (ClassNotFoundException e) {
                LOGGER.error("Error while loading user registered mailbox listener with factory {}", factoryClass, e);
                throw new RuntimeException(e);
            }
        }
        return Optional.empty();
    }

    private MailboxListener.ExecutionMode getExecutionMode(boolean isAsync) {
        if (isAsync) {
            return MailboxListener.ExecutionMode.ASYNCHRONOUS;
        }
        return MailboxListener.ExecutionMode.SYNCHRONOUS;
    }
}
