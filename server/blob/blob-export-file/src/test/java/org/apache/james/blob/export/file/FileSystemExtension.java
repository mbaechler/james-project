/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.james.blob.export.file;

import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.junit.ManagedTestResource;
import org.apache.james.server.core.JamesServerResourceLoader;
import org.apache.james.server.core.filesystem.FileSystemImpl;

public class FileSystemExtension {

    public static ManagedTestResource defaultFilesystem() {
        return ManagedTestResource
            .forResource(new FileSystemImpl(new JamesServerResourceLoader("../testsFileSystemExtension/" + UUID.randomUUID())))
            .afterAll(fileSystem -> {
                if (fileSystem.getBasedir().exists()) {
                    FileUtils.forceDelete(fileSystem.getBasedir());
                }
            })
            .resolveAs(FileSystem.class)
            .build();
    }

}
