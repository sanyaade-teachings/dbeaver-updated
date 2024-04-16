/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.dashboard;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardProviderDescriptor;

/**
 * Dashboard
 */
public interface DBDashboardItem extends DBPNamedObject {

    @Nullable
    String getPath();

    @NotNull
    String getId();

    @NotNull
    String getName();

    /**
     * Custom name displayed in chart composite
     */
    @Nullable
    String getDisplayName();

    String getTitle();

    @Nullable
    String getDescription();

    boolean isCustom();

    @NotNull
    DashboardProviderDescriptor getDashboardProvider();

    @NotNull
    String getDashboardRenderer();

}
