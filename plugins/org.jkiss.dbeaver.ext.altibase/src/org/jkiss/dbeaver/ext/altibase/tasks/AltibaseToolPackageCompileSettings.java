/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.altibase.model.AltibasePackage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;

import java.util.Map;

public class AltibaseToolPackageCompileSettings extends SQLToolExecuteSettings<AltibasePackage> {

    @Override
    public void loadConfiguration(@NotNull DBRRunnableContext runnableContext, 
            @NotNull Map<String, Object> config, @NotNull DBPProject project) {
        super.loadConfiguration(runnableContext, config, project);
    }

    @Override
    public void saveConfiguration(Map<String, Object> config) {
        super.saveConfiguration(config);
    }
}