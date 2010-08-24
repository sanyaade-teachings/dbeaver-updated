/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumn;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumn;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumnType;

/**
 * GenericTable
 */
public class MySQLProcedureColumn extends JDBCColumn implements DBSProcedureColumn
{
    private MySQLProcedure procedure;
    private DBSProcedureColumnType columnType;

    public MySQLProcedureColumn(
        MySQLProcedure procedure,
        String columnName,
        String typeName,
        int valueType,
        int ordinalPosition,
        int columnSize,
        int scale,
        int precision,
        int radix,
        boolean nullable,
        String remarks,
        DBSProcedureColumnType columnType)
    {
        super(columnName,
            typeName,
            valueType,
            ordinalPosition,
            columnSize,
            scale,
            radix,
            precision,
            nullable,
            remarks);
        this.procedure = procedure;
        this.columnType = columnType;
    }

    public DBSObject getParentObject()
    {
        return getProcedure();
    }

    public MySQLDataSource getDataSource()
    {
        return procedure.getDataSource();
    }

    public MySQLProcedure getProcedure()
    {
        return procedure;
    }

    @Property(name = "Column Type", viewable = true, order = 10)
    public DBSProcedureColumnType getColumnType()
    {
        return columnType;
    }

}
