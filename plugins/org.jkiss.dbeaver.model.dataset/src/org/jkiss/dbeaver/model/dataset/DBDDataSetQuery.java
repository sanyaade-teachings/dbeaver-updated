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

package org.jkiss.dbeaver.model.dataset;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.virtual.DBVColorOverride;

import java.util.List;

/**
 * Dataset
 */
public class DBDDataSetQuery {

    // Query ID
    private String id;
    // Query datasource
    private DBPDataSourceContainer dataSourceContainer;
    // Query text
    private String queryText;
    // Data filters
    private DBDDataFilter dataFilters;
    // Custom colors
    private List<DBVColorOverride> colorOverrides;

}
