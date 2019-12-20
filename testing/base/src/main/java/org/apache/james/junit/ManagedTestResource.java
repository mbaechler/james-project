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

package org.apache.james.junit;

import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import com.github.fge.lambdas.Throwing;

public class ManagedTestResource implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback, ParameterResolver {

    public static <T> ManagedTestResourceBuilder<T> forResource(T resource) {
        return new ManagedTestResourceBuilder<>(resource);
    }

    @FunctionalInterface
    public interface LifecycleMethod<T> {
        void execute(T resource) throws Exception;
    }

    public static class ManagedTestResourceBuilder<T> {

        private final T resource;

        private Optional<LifecycleMethod<T>> beforeEachCallback;
        private Optional<LifecycleMethod<T>> afterEachCallback;
        private Optional<LifecycleMethod<T>> beforeAllCallback;
        private Optional<LifecycleMethod<T>> afterAllCallback;
        private Optional<ParameterResolver> resolver;

        private ManagedTestResourceBuilder(T resource) {
            this.resource = resource;
            this.beforeEachCallback = Optional.empty();
            this.afterEachCallback = Optional.empty();
            this.beforeAllCallback = Optional.empty();
            this.afterAllCallback = Optional.empty();
            this.resolver = Optional.empty();
        }

        public ManagedTestResourceBuilder<T> beforeEach(LifecycleMethod<T> method) {
            beforeEachCallback = Optional.of(method);
            return this;
        }

        public ManagedTestResourceBuilder<T> afterEach(LifecycleMethod<T> method) {
            afterEachCallback = Optional.of(method);
            return this;
        }

        public ManagedTestResourceBuilder<T> beforeAll(LifecycleMethod<T> method) {
            beforeAllCallback = Optional.of(method);
            return this;
        }

        public ManagedTestResourceBuilder<T> afterAll(LifecycleMethod<T> method) {
            afterAllCallback = Optional.of(method);
            return this;
        }

        public ManagedTestResourceBuilder<T> resolveAs(Class<? super T> type) {
            resolver = Optional.of(SingleParameterResolver.forType(type).resolve(resource));
            return this;
        }

        public ManagedTestResource build() {
            return new ManagedTestResource(
                beforeEachCallback.map(callback -> context -> callback.execute(resource)),
                afterEachCallback.map(callback -> context -> callback.execute(resource)),
                beforeAllCallback.map(callback -> context -> callback.execute(resource)),
                afterAllCallback.map(callback -> context -> callback.execute(resource)),
                resolver.orElse(nullResolver())
            );
        }

        private ParameterResolver nullResolver() {
            return new ParameterResolver() {
                @Override
                public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
                    return false;
                }

                @Override
                public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
                    return null;
                }
            };
        }

    }

    private final Optional<BeforeEachCallback> beforeEachCallback;
    private final Optional<AfterEachCallback> afterEachCallback;
    private final Optional<BeforeAllCallback> beforeAllCallback;
    private final Optional<AfterAllCallback> afterAllCallback;
    private final ParameterResolver resolver;

    private ManagedTestResource(Optional<BeforeEachCallback> beforeEachCallback,
                                Optional<AfterEachCallback> afterEachCallback,
                                Optional<BeforeAllCallback> beforeAllCallback,
                                Optional<AfterAllCallback> afterAllCallback,
                                ParameterResolver resolver) {
        this.beforeEachCallback = beforeEachCallback;
        this.afterEachCallback = afterEachCallback;
        this.beforeAllCallback = beforeAllCallback;
        this.afterAllCallback = afterAllCallback;
        this.resolver = resolver;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        beforeEachCallback.ifPresent(Throwing.<BeforeEachCallback>consumer(callback -> callback.beforeEach(context)).sneakyThrow());
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        afterEachCallback.ifPresent(Throwing.<AfterEachCallback>consumer(callback -> callback.afterEach(context)).sneakyThrow());
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        beforeAllCallback.ifPresent(Throwing.<BeforeAllCallback>consumer(callback -> callback.beforeAll(context)).sneakyThrow());
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        afterAllCallback.ifPresent(Throwing.<AfterAllCallback>consumer(callback -> callback.afterAll(context)).sneakyThrow());
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return resolver.supportsParameter(parameterContext, extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return resolver.resolveParameter(parameterContext, extensionContext);
    }
}
