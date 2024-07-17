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
package org.jkiss.dbeaver.ext.import_config.dbvis;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportData;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DbvisConfigurationImporter {

    private static final Map<String, String> version2configuration = new LinkedHashMap<>();
    private static final Map<String, DbvisConfigurationCreator> version2creator = new LinkedHashMap<>();

    static {
        // configurations
        version2configuration.put(DbvisConfigurationCreatorv7.VERSION, DbvisConfigurationCreatorv7.CONFIG_FOLDER);
        version2configuration.put(DbvisConfigurationCreatorv233.VERSION, DbvisConfigurationCreatorv233.CONFIG_FOLDER);
        // creators
        version2creator.put(DbvisConfigurationCreatorv7.VERSION, new DbvisConfigurationCreatorv7());
        version2creator.put(DbvisConfigurationCreatorv233.VERSION, new DbvisConfigurationCreatorv233());
    }

    public ImportData importConfiguration(
        @NotNull ImportData data,
        @NotNull File folder
    ) throws DBException {
        for (Entry<String, String> configuration : version2configuration.entrySet()) {
            File configFile = new File(folder, configuration.getValue());
            if (configFile.exists()) {
                DbvisConfigurationCreator dbvisConfigurationCreator = version2creator.get(configuration.getKey());
                File configurationAsset = dbvisConfigurationCreator.getConfigurationAsset(folder);
                if (configurationAsset.exists()) {
                    data = dbvisConfigurationCreator.create(data, configurationAsset);
                }
            }
        }
        return data;
    }
}
