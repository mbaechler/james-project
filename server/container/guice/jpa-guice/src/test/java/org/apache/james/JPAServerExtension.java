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

import java.util.Optional;

import org.apache.james.server.core.configuration.Configuration;

public class JPAServerExtension {

    @FunctionalInterface
    interface RequiresBasicConfiguration {
        Builder configure(JamesServerBuilder.ConfigurationProvider provider);

        default Builder defaultConfiguration() {
            return configure(tmpDir -> Configuration.builder()
                .workingDirectory(tmpDir)
                .configurationFromClasspath()
                .build());
        }
    }

    @FunctionalInterface
    interface ExtensionCustomizer {
        JamesServerBuilder customize(JamesServerBuilder builder);
    }

    public static class Builder {

        private final JamesServerBuilder extensionBuilder;
        private Optional<ExtensionCustomizer> customizer;

        private Builder(JamesServerBuilder.ConfigurationProvider configuration) {
            this.extensionBuilder = new JamesServerBuilder<Configuration>(configuration, c -> JPAJamesServerMain.builder(c).build());
        }

        public Builder withSpecificParameters(ExtensionCustomizer customizer) {
            customizer.customize(extensionBuilder);
            return this;
        }

        public JamesServerExtension build() {
            return extensionBuilder.build();
        }
    }

    public static RequiresBasicConfiguration builder() {
        return provider -> new Builder(provider);
    }
}
