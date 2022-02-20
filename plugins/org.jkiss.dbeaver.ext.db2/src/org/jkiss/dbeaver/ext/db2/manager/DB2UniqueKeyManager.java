/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2017 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableUniqueKey;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.Map;

/**
 * DB2 Unique Keys Manager
 * 
 * @author Denis Forveille
 */
public class DB2UniqueKeyManager extends SQLConstraintManager<DB2TableUniqueKey, DB2Table> {

    private static final String                    SQL_DROP_PK = "ALTER TABLE %s DROP PRIMARY KEY ";
    private static final String                    SQL_DROP_UK = "ALTER TABLE %s DROP UNIQUE %s";

    // -----------------
    // Business Contract
    // -----------------

    @Override
    public boolean canEditObject(DB2TableUniqueKey object)
    {
        return false;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, DB2TableUniqueKey> getObjectsCache(DB2TableUniqueKey object)
    {
        return object.getParentObject().getSchema().getConstraintCache();
    }

    // ------
    // Create
    // ------

    @Override
    public DB2TableUniqueKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object table,
                                                  Object from, Map<String, Object> options)
    {
        return new DB2TableUniqueKey((DB2Table) table, DBSEntityConstraintType.UNIQUE_KEY);
    }

    // ------
    // DROP
    // ------

    @Override
    public String getDropConstraintPattern(DB2TableUniqueKey constraint)
    {
        String tablename = constraint.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL);
        if (constraint.getConstraintType().equals(DBSEntityConstraintType.PRIMARY_KEY)) {
            return String.format(SQL_DROP_PK, tablename);
        } else {
            return String.format(SQL_DROP_UK, tablename, constraint.getName());
        }
    }

}
