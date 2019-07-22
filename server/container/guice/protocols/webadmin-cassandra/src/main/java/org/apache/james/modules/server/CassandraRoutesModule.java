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

package org.apache.james.modules.server;

import org.apache.james.backends.cassandra.migration.CassandraMigrationService;
import org.apache.james.backends.cassandra.migration.Migration;
import org.apache.james.backends.cassandra.migration.MigrationTask;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionManager;
import org.apache.james.backends.cassandra.versions.SchemaTransition;
import org.apache.james.backends.cassandra.versions.SchemaVersion;
import org.apache.james.mailbox.cassandra.mail.migration.AttachmentMessageIdCreation;
import org.apache.james.mailbox.cassandra.mail.migration.AttachmentV2Migration;
import org.apache.james.mailbox.cassandra.mail.migration.MailboxPathV2Migration;
import org.apache.james.rrt.cassandra.migration.MappingsSourcesMigration;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.routes.CassandraMailboxMergingRoutes;
import org.apache.james.webadmin.routes.CassandraMigrationRoutes;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class CassandraRoutesModule extends AbstractModule {
    private static final SchemaTransition FROM_V2_TO_V3 = SchemaTransition.to(new SchemaVersion(3));
    private static final SchemaTransition FROM_V3_TO_V4 = SchemaTransition.to(new SchemaVersion(4));
    private static final SchemaTransition FROM_V4_TO_V5 = SchemaTransition.to(new SchemaVersion(5));
    private static final SchemaTransition FROM_V5_TO_V6 = SchemaTransition.to(new SchemaVersion(6));
    private static final SchemaTransition FROM_V6_TO_V7 = SchemaTransition.to(new SchemaVersion(7));

    @Override
    protected void configure() {
        bind(MigrationTask.Impl.class).in(Scopes.SINGLETON);
        bind(CassandraRoutesModule.class).in(Scopes.SINGLETON);
        bind(CassandraMailboxMergingRoutes.class).in(Scopes.SINGLETON);
        bind(CassandraMigrationService.class).in(Scopes.SINGLETON);

        bind(MigrationTask.Factory.class).to(MigrationTask.Impl.class);

        Multibinder<Routes> routesMultibinder = Multibinder.newSetBinder(binder(), Routes.class);
        routesMultibinder.addBinding().to(CassandraMigrationRoutes.class);
        routesMultibinder.addBinding().to(CassandraMailboxMergingRoutes.class);

        MapBinder<SchemaTransition, Migration> allMigrationClazzBinder = MapBinder.newMapBinder(binder(), SchemaTransition.class, Migration.class);
        allMigrationClazzBinder.addBinding(FROM_V2_TO_V3).toInstance(() -> { });
        allMigrationClazzBinder.addBinding(FROM_V3_TO_V4).to(AttachmentV2Migration.class);
        allMigrationClazzBinder.addBinding(FROM_V4_TO_V5).to(AttachmentMessageIdCreation.class);
        allMigrationClazzBinder.addBinding(FROM_V5_TO_V6).to(MailboxPathV2Migration.class);
        allMigrationClazzBinder.addBinding(FROM_V6_TO_V7).to(MappingsSourcesMigration.class);

        bind(SchemaVersion.class)
            .annotatedWith(Names.named(CassandraMigrationService.LATEST_VERSION))
            .toInstance(CassandraSchemaVersionManager.MAX_VERSION);
    }
}
