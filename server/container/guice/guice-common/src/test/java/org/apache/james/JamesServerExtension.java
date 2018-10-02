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
import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class JamesServerExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {

    interface ThrowingSupplier<T> {
        T get(File file) throws Exception;
    }

    private final TemporaryFolderRegistrableExtension folderRegistrableExtension;
    private final ThrowingSupplier<GuiceJamesServer> serverSupplier;
    private final RegistrableExtension registrableExtension;
    private GuiceJamesServer guiceJamesServer;

    JamesServerExtension(RegistrableExtension registrableExtension, ThrowingSupplier<GuiceJamesServer> serverSupplier) {
        this.registrableExtension = registrableExtension;
        this.serverSupplier = serverSupplier;
        this.folderRegistrableExtension = new TemporaryFolderRegistrableExtension();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        registrableExtension.beforeAll(extensionContext);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        folderRegistrableExtension.beforeEach(extensionContext);
        registrableExtension.beforeEach(extensionContext);
        guiceJamesServer = serverSupplier.get(createTmpDir());
        guiceJamesServer.start();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        guiceJamesServer.stop();
        registrableExtension.afterEach(extensionContext);
        folderRegistrableExtension.afterEach(extensionContext);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        registrableExtension.afterAll(extensionContext);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return (parameterContext.getParameter().getType() == GuiceJamesServer.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return guiceJamesServer;
    }

    private File createTmpDir() {
        try {
            return folderRegistrableExtension.getTemporaryFolder().newFolder();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
