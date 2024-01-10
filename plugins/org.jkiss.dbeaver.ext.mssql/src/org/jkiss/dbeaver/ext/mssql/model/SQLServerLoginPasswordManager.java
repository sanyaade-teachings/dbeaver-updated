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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAUserPasswordManager;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

public class SQLServerLoginPasswordManager implements DBAUserPasswordManager {

    private final SQLServerDataSource dataSource;

    SQLServerLoginPasswordManager(SQLServerDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void changeUserPassword(DBRProgressMonitor monitor, String loginName, String newPassword, String oldPassword) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Change user login password")) {
            session.enableLogging(false);
            JDBCUtils.executeSQL(session, "ALTER LOGIN " + DBUtils.getQuotedIdentifier(dataSource, loginName) + " WITH PASSWORD =" + SQLUtils.quoteString(dataSource, CommonUtils.notEmpty(newPassword)) +
                " OLD_PASSWORD =" + SQLUtils.quoteString(dataSource, CommonUtils.notEmpty(oldPassword)));
        } catch (SQLException e) {
            throw new DBCException("Error changing user password", e);
        }
    }
}
