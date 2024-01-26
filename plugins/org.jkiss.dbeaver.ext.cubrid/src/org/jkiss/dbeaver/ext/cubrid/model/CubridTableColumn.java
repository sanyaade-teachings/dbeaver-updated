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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.DatabaseMetaData;
import java.sql.Types;

public class CubridTableColumn extends GenericTableColumn
{

    private static final Log log = Log.getLog(CubridTableColumn.class);

    public CubridTableColumn(CubridTable table)
    {
        super(table);
    }

    public CubridTableColumn(CubridTable table, JDBCResultSet dbResult)
    {
        super(table);
        this.name = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
        this.valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
        this.typeName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TYPE_NAME);
        this.maxLength = JDBCUtils.safeGetLong(dbResult, JDBCConstants.COLUMN_SIZE);
        this.required = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.columnNoNulls;

        try {
            this.scale = JDBCUtils.safeGetInteger(dbResult, JDBCConstants.DECIMAL_DIGITS);
        } catch (Throwable e) {
            log.warn("Error getting column scale", e);
            this.scale = -1;
        }

        if (valueType == Types.NUMERIC || valueType == Types.DECIMAL) {
            this.precision = (int) this.maxLength;
        }
        int radix = 10;
        try {
            radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NUM_PREC_RADIX);
        } catch (Exception e) {
            log.warn("Error getting column radix", e);
        }

        String defaultValue = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_DEF);
        long charLength = JDBCUtils.safeGetLong(dbResult, JDBCConstants.CHAR_OCTET_LENGTH);
        this.ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
        this.autoGenerated = "YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.IS_GENERATEDCOLUMN));
        setAutoIncrement("YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.IS_AUTOINCREMENT)));
        setDescription(JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS));
        setRadix(radix);
        setDefaultValue(defaultValue);
        setMaxLength(charLength);
        setPersisted(true);
    }
}
