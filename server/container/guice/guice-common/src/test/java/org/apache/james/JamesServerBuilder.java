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

package org.apache.james;

import java.io.File;
import java.util.Optional;

import org.apache.james.server.core.configuration.Configuration;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

public class JamesServerBuilder<ConfigurationT> {
    private static final boolean DEFAULT_AUTO_START = true;

    @FunctionalInterface
    public interface ConfigurationProvider<ConfigurationT> {
        ConfigurationT buildConfiguration(File tempDirectory);
    }

    @FunctionalInterface
    public interface ServerProvider<ConfigurationT>  {
        GuiceJamesServer buildServer(ConfigurationT configuration);
    }

    private final ImmutableList.Builder<GuiceModuleTestExtension> extensions;
    private final TemporaryFolderRegistrableExtension folderRegistrableExtension;
    private final ImmutableList.Builder<Module> overrideModules;
    private final ImmutableList.Builder<Module> additionalModules;
    private final ConfigurationProvider<ConfigurationT> configuration;
    private final ServerProvider<ConfigurationT> server;
    private Optional<Boolean> autoStart;

    public JamesServerBuilder(ConfigurationProvider<ConfigurationT> configurationProvider, ServerProvider<ConfigurationT> server) {
        this.configuration = configurationProvider;
        this.extensions = ImmutableList.builder();
        this.folderRegistrableExtension = new TemporaryFolderRegistrableExtension();
        this.autoStart = Optional.empty();
        this.overrideModules = ImmutableList.builder();
        this.additionalModules = ImmutableList.builder();
        this.server = server;
    }

    public JamesServerBuilder extensions(GuiceModuleTestExtension... extensions) {
        this.extensions.add(extensions);
        return this;
    }

    public JamesServerBuilder extension(GuiceModuleTestExtension extension) {
        return this.extensions(extension);
    }

    public JamesServerBuilder overrideServerModule(Module module) {
        this.overrideModules.add(module);
        return this;
    }

    public JamesServerBuilder additionalModule(Module module) {
        this.additionalModules.add(module);
        return this;
    }

    public JamesServerBuilder disableAutoStart() {
        this.autoStart = Optional.of(false);
        return this;
    }

    public JamesServerExtension build() {
        Preconditions.checkNotNull(server);
        JamesServerExtension.AwaitCondition awaitCondition = () -> extensions.build().forEach(GuiceModuleTestExtension::await);

        JamesServerExtension.ThrowingFunction<File, GuiceJamesServer> serverBuilder = file -> server
            .buildServer(configuration.buildConfiguration(file))
            .combineWith(additionalModules.build())
            .overrideWith(extensionModules(file, configuration))
            .overrideWith(overrideModules.build())
            .overrideWith((binder -> binder.bind(CleanupTasksPerformer.class).asEagerSingleton()));

        return new JamesServerExtension(
            buildAggregateJunitExtension(),
            serverBuilder,
            awaitCondition,
            autoStart.orElse(DEFAULT_AUTO_START));
    }

    private AggregateJunitExtension buildAggregateJunitExtension() {
        ImmutableList<GuiceModuleTestExtension> extensions = this.extensions.build();
        return new AggregateJunitExtension(
            ImmutableList.<RegistrableExtension>builder()
                .addAll(extensions)
                .add(folderRegistrableExtension)
                .build());
    }

    private ImmutableList<Module> extensionModules(File file, ConfigurationProvider configurationProvider) {
        return extensions.build()
            .stream()
            .map(GuiceModuleTestExtension::getModule)
            .collect(Guavate.toImmutableList());

    }

}
