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
package org.jkiss.dbeaver.model.dashboard.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.dashboard.DBDashboard;
import org.jkiss.dbeaver.model.dashboard.DBDashboardContext;
import org.jkiss.dbeaver.model.dashboard.DBDashboardProvider;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * DashboardProviderDescriptor
 */
public class DashboardProviderDescriptor extends AbstractContextDescriptor implements DBDashboardProvider {
    private static final Log log = Log.getLog(DashboardProviderDescriptor.class);

    private final String id;
    private final ObjectType implType;
    private final boolean supportsCustomDashboards;

    public DashboardProviderDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute("id");
        this.supportsCustomDashboards = CommonUtils.toBoolean(config.getAttribute("supportsCustomization"));
        this.implType = new ObjectType(config.getAttribute("class"));
    }

    public String getId() {
        return id;
    }

    @Override
    public List<DBDashboard> loadDashboards(@NotNull DBRProgressMonitor monitor, @NotNull DBDashboardContext context) throws DBException {
        return null;
    }

    public ObjectType getImplType() {
        return implType;
    }

}
