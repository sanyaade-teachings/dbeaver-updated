/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.utils.CommonUtils;

/**
 * GlobalPropertyTester
 */
public class GlobalPropertyTester extends PropertyTester {
    //static final Log log = LogFactory.get vLog(ObjectPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.global";
    public static final String PROP_STANDALONE = "standalone";
    public static final String PROP_HAS_PERMISSION = "hasPermission";
    public static final String PROP_CAN_CREATE_CONNECTION = "canCreateConnection";
    public static final String PROP_HAS_ACTIVE_PROJECT = "hasActiveProject";
    public static final String PROP_HAS_MULTI_PROJECTS = "hasMultipleProjects";
    public static final String PROP_CAN_CREATE_PROJECT = "canCreateProject";
    public static final String PROP_CAN_EDIT_RESOURCE= "canEditResource";
    public static final String PROP_CAN_VIEW_RESOURCE= "canViewResource";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        switch (property) {
            case PROP_HAS_PERMISSION:
                return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(CommonUtils.toString(expectedValue));
            case PROP_HAS_MULTI_PROJECTS:
                return DBWorkbench.getPlatform().getWorkspace().getProjects().size() > 1;
            case PROP_HAS_ACTIVE_PROJECT:
                return DBWorkbench.getPlatform().getWorkspace().getActiveProject() != null;
            case PROP_STANDALONE:
                return DBWorkbench.getPlatform().getApplication().isStandalone();
            case PROP_CAN_CREATE_PROJECT:
                return !DBWorkbench.getPlatform().getApplication().isDistributed();
            case PROP_CAN_CREATE_CONNECTION:
            {
                for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
                    if (project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_CONNECTIONS_EDIT)) {
                        return true;
                    }
                }
                return false;
            }
            case PROP_CAN_EDIT_RESOURCE:
            {
                DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
                return project != null && project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT);
            }
            case PROP_CAN_VIEW_RESOURCE:
            {
                DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
                var x = project != null && project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_VIEW);
                return x;
            }
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

    public static class ResourceListener implements IPluginService, IResourceChangeListener {

        @Override
        public void activateService() {
            ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        }

        @Override
        public void deactivateService() {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        }

        @Override
        public void resourceChanged(IResourceChangeEvent event) {
            if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
                for (IResourceDelta childDelta : event.getDelta().getAffectedChildren()) {
                    if (childDelta.getResource() instanceof IProject) {
                        if (childDelta.getKind() == IResourceDelta.ADDED || childDelta.getKind() == IResourceDelta.REMOVED) {
                            firePropertyChange(GlobalPropertyTester.PROP_HAS_MULTI_PROJECTS);
                        }
                    }
                }
            }
        }
    }
}
