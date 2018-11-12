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

package org.apache.james.modules.objectstorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.commons.configuration.MapConfiguration;
import org.apache.james.blob.objectstorage.AESPayloadCodec;
import org.apache.james.blob.objectstorage.DefaultPayloadCodec;
import org.apache.james.blob.objectstorage.PayloadCodec;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

class PayloadCodecProviderTest {


    private static MapConfigurationBuilder newConfigBuilder() {
        return new MapConfigurationBuilder();
    }

    private static class MapConfigurationBuilder {
        private ImmutableMap.Builder<String, Object> config;

        public MapConfigurationBuilder() {
            this.config = new ImmutableMap.Builder<>();
        }

        public MapConfigurationBuilder put(String key, Object value) {
            config.put(key, value);
            return this;
        }

        public MapConfiguration build() {
            return new MapConfiguration(config.build());
        }
    }
}

