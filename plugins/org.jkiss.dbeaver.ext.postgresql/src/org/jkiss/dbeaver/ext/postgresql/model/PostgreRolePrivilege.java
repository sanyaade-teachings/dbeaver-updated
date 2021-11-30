/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * PostgreRolePrivilege
 */
public class PostgreRolePrivilege extends PostgrePrivilege {

    private static final Log log = Log.getLog(PostgreRolePrivilege.class);

    private PostgrePrivilegeGrant.Kind kind;
    private PostgreDatabase database;
    private String schemaName;
    private String objectName;

    public PostgreRolePrivilege(PostgrePrivilegeOwner owner, PostgrePrivilegeGrant.Kind kind, @NotNull PostgreSchema schema, String objectName, List<PostgrePrivilegeGrant> privileges) {
        super(owner, privileges);
        this.kind = kind;
        this.schemaName = schema.getName();
        this.database = schema.getDatabase();
        this.objectName = objectName;
    }

    @Property(viewable = true, order = 1)
    @NotNull
    public String getName() {
        return getFullObjectName();
    }

    @Override
    public PostgreObject getTargetObject(DBRProgressMonitor monitor) throws DBException
    {
        final PostgreSchema schema = owner.getDatabase().getSchema(monitor, schemaName);
        if (schema != null) {
            JDBCTable childTable = schema.getChild(monitor, objectName);
            return childTable instanceof PostgreObject ? (PostgreObject) childTable : null;
        }
        return null;
    }

    public PostgrePrivilegeGrant.Kind getKind() {
        return kind;
    }

    public void setKind(PostgrePrivilegeGrant.Kind kind) {
        this.kind = kind;
    }

    public PostgreDatabase getDatabase() {
        return database;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getFullObjectName() {
        return DBUtils.getQuotedIdentifier(database) + "." + DBUtils.getQuotedIdentifier(getDataSource(), schemaName) +
            (kind == PostgrePrivilegeGrant.Kind.SCHEMA ? "" :
                ("." + (kind == PostgrePrivilegeGrant.Kind.FUNCTION || kind == PostgrePrivilegeGrant.Kind.PROCEDURE ? objectName :
                        DBUtils.getQuotedIdentifier(getDataSource(), objectName))));
    }

    @Override
    public String toString() {
        return getFullObjectName();
    }

    @Override
    public int compareTo(@NotNull PostgrePrivilege o) {
        if (o instanceof PostgreRolePrivilege) {
            final int databaseRes = database.getName().compareTo(((PostgreRolePrivilege)o).getDatabase().getName());
            final int res = schemaName.compareTo(((PostgreRolePrivilege)o).schemaName);
            return databaseRes != 0 ? databaseRes : res != 0 ? res : CommonUtils.compare(objectName, ((PostgreRolePrivilege)o).objectName);
        }
        return 0;
    }

}

