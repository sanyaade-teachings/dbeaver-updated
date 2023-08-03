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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;

/**
 * Datasource folder
 */
public interface DBPDataSourceFolder extends DBPNamedObject2 {

    String getFolderPath();

    String getDescription();

    DBPDataSourceFolder getParent();

    void setParent(DBPDataSourceFolder parent);

    DBPDataSourceFolder[] getChildren();

    DBPDataSourceRegistry getDataSourceRegistry();

    boolean canMoveTo(DBPDataSourceFolder folder);

    /**
     * Specifies if the folder is not present on the file system, but should not be removed on data sources configuration reload
     */
    void setProtected(boolean value);

    /**
     * Is folder protected from the removing on configuration reload
     */
    boolean isProtected();
}
