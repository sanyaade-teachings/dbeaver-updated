/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mysql.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * GenericCatalog
 */
public class MySQLCatalog implements DBSCatalog, DBPSaveableObject, DBPRefreshableObject
{
    static final Log log = LogFactory.getLog(MySQLCatalog.class);

    final TableCache tableCache = new TableCache();
    final ProceduresCache proceduresCache = new ProceduresCache();
    final TriggerCache triggerCache = new TriggerCache();
    final ConstraintCache constraintCache = new ConstraintCache();
    final IndexCache indexCache = new IndexCache();

    private MySQLDataSource dataSource;
    private String name;
    private MySQLCharset defaultCharset;
    private MySQLCollation defaultCollation;
    private String sqlPath;
    private boolean persisted;

    public MySQLCatalog(MySQLDataSource dataSource, ResultSet dbResult)
    {
        this.dataSource = dataSource;
        if (dbResult != null) {
            this.name = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_SCHEMA_NAME);
            defaultCharset = dataSource.getCharset(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFAULT_CHARACTER_SET_NAME));
            defaultCollation = dataSource.getCollation(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFAULT_COLLATION_NAME));
            sqlPath = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_SQL_PATH);
            persisted = true;
        } else {
            persisted = false;
        }
    }

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @Override
    public MySQLDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    @Property(name = "Schema Name", viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Property(name = "Default Charset", viewable = true, order = 2)
    public MySQLCharset getDefaultCharset()
    {
        return defaultCharset;
    }

    void setDefaultCharset(MySQLCharset defaultCharset)
    {
        this.defaultCharset = defaultCharset;
    }

    @Property(name = "Default Collation", viewable = true, order = 3)
    public MySQLCollation getDefaultCollation()
    {
        return defaultCollation;
    }

    void setDefaultCollation(MySQLCollation defaultCollation)
    {
        this.defaultCollation = defaultCollation;
    }

    @Property(name = "SQL Path", viewable = true, order = 3)
    public String getSqlPath()
    {
        return sqlPath;
    }

    void setSqlPath(String sqlPath)
    {
        this.sqlPath = sqlPath;
    }

    public TableCache getTableCache()
    {
        return tableCache;
    }

    public ProceduresCache getProceduresCache()
    {
        return proceduresCache;
    }

    public TriggerCache getTriggerCache()
    {
        return triggerCache;
    }

    public ConstraintCache getConstraintCache()
    {
        return constraintCache;
    }

    public IndexCache getIndexCache()
    {
        return indexCache;
    }

    @Association
    public Collection<MySQLTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return indexCache.getObjects(monitor, this, null);
    }

    @Association
    public Collection<MySQLTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, MySQLTable.class);
    }

    public MySQLTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, this, name, MySQLTable.class);
    }

    @Association
    public Collection<MySQLView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, MySQLView.class);
    }

    @Association
    public Collection<MySQLProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return proceduresCache.getObjects(monitor, this);
    }

    public MySQLProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException
    {
        return proceduresCache.getObject(monitor, this, procName);
    }

    @Association
    public Collection<MySQLTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getObjects(monitor, this);
    }

    public MySQLTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache.getObject(monitor, this, name);
    }

    @Override
    public Collection<MySQLTableBase> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, this);
    }

    @Override
    public MySQLTableBase getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return tableCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return MySQLTable.class;
    }

    @Override
    public synchronized void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        monitor.subTask("Cache tables");
        tableCache.loadObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            tableCache.loadChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache table constraints");
            constraintCache.getObjects(monitor, this);
        }
    }

    @Override
    public synchronized boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        tableCache.clearCache();
        indexCache.clearCache();
        constraintCache.clearCache();
        proceduresCache.clearCache();
        triggerCache.clearCache();
        return true;
    }

    public boolean isSystem()
    {
        return MySQLConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(getName()) || MySQLConstants.MYSQL_SCHEMA_NAME.equalsIgnoreCase(getName());
    }

    @Override
    public String toString()
    {
        return name + " [" + dataSource.getContainer().getName() + "]";
    }

    class TableCache extends JDBCStructCache<MySQLCatalog, MySQLTableBase, MySQLTableColumn> {
        
        protected TableCache()
        {
            super(JDBCConstants.TABLE_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, MySQLCatalog owner)
            throws SQLException
        {
            return context.prepareStatement("SHOW FULL TABLES FROM " + DBUtils.getQuotedIdentifier(getDataSource(), getName()));
        }

        @Override
        protected MySQLTableBase fetchObject(JDBCExecutionContext context, MySQLCatalog owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            final String tableType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_TYPE);
            if (tableType.indexOf("VIEW") != -1) {
                return new MySQLView(MySQLCatalog.this, dbResult);
            } else {
                return new MySQLTable(MySQLCatalog.this, dbResult);
            }
        }

        @Override
        protected boolean isChildrenCached(MySQLTableBase table)
        {
            return table.isColumnsCached();
        }

        @Override
        protected void cacheChildren(MySQLTableBase table, List<MySQLTableColumn> columns)
        {
            table.setColumns(columns);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(JDBCExecutionContext context, MySQLCatalog owner, MySQLTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(MySQLConstants.META_TABLE_COLUMNS)
                .append(" WHERE ").append(MySQLConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(MySQLConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(MySQLConstants.COL_ORDINAL_POSITION);

            JDBCPreparedStatement dbStat = context.prepareStatement(sql.toString());
            dbStat.setString(1, MySQLCatalog.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected MySQLTableColumn fetchChild(JDBCExecutionContext context, MySQLCatalog owner, MySQLTableBase table, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLTableColumn(table, dbResult);
        }
    }

    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<MySQLCatalog, MySQLTable, MySQLTableIndex, MySQLTableIndexColumn> {
        protected IndexCache()
        {
            super(tableCache, MySQLTable.class, MySQLConstants.COL_TABLE_NAME, MySQLConstants.COL_INDEX_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, MySQLCatalog owner, MySQLTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(MySQLConstants.META_TABLE_STATISTICS)
                .append(" WHERE ").append(MySQLConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(MySQLConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(MySQLConstants.COL_INDEX_NAME).append(",").append(MySQLConstants.COL_SEQ_IN_INDEX);

            JDBCPreparedStatement dbStat = context.prepareStatement(sql.toString());
            dbStat.setString(1, MySQLCatalog.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected MySQLTableIndex fetchObject(JDBCExecutionContext context, MySQLCatalog owner, MySQLTable parent, String indexName, ResultSet dbResult)
            throws SQLException, DBException
        {
            boolean isNonUnique = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_NON_UNIQUE) != 0;
            String indexTypeName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_INDEX_TYPE);
            DBSIndexType indexType;
            if (MySQLConstants.INDEX_TYPE_BTREE.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_BTREE;
            } else if (MySQLConstants.INDEX_TYPE_FULLTEXT.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_FULLTEXT;
            } else if (MySQLConstants.INDEX_TYPE_HASH.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_HASH;
            } else if (MySQLConstants.INDEX_TYPE_RTREE.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_RTREE;
            } else {
                indexType = DBSIndexType.OTHER;
            }
            final String comment = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COMMENT);

            return new MySQLTableIndex(
                parent,
                isNonUnique,
                indexName,
                indexType,
                comment);
        }

        @Override
        protected MySQLTableIndexColumn fetchObjectRow(
            JDBCExecutionContext context,
            MySQLTable parent, MySQLTableIndex object, ResultSet dbResult)
            throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_SEQ_IN_INDEX);
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_COLLATION);
            boolean nullable = "YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_NULLABLE));

            MySQLTableColumn tableColumn = parent.getColumn(context.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                return null;
            }

            return new MySQLTableIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                "A".equalsIgnoreCase(ascOrDesc),
                nullable);
        }

        @Override
        protected Collection<MySQLTableIndex> getObjectsCache(MySQLTable parent)
        {
            return parent.getIndexesCache();
        }

        @Override
        protected void cacheObjects(MySQLTable parent, List<MySQLTableIndex> indexes)
        {
            parent.setIndexes(indexes);
        }

        @Override
        protected void cacheChildren(MySQLTableIndex index, List<MySQLTableIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * Constraint cache implementation
     */
    class ConstraintCache extends JDBCCompositeCache<MySQLCatalog, MySQLTable, MySQLTableConstraint, MySQLTableConstraintColumn> {
        protected ConstraintCache()
        {
            super(tableCache, MySQLTable.class, MySQLConstants.COL_TABLE_NAME, MySQLConstants.COL_CONSTRAINT_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, MySQLCatalog owner, MySQLTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder(500);
            sql.append(
                "SELECT kc.CONSTRAINT_NAME,kc.TABLE_NAME,kc.COLUMN_NAME,kc.ORDINAL_POSITION\n" +
                "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kc WHERE kc.TABLE_SCHEMA=? AND kc.REFERENCED_TABLE_NAME IS NULL");
            if (forTable != null) {
                sql.append(" AND kc.TABLE_NAME=?");
            }
            sql.append("\nORDER BY kc.CONSTRAINT_NAME,kc.ORDINAL_POSITION");

            JDBCPreparedStatement dbStat = context.prepareStatement(sql.toString());
            dbStat.setString(1, MySQLCatalog.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected MySQLTableConstraint fetchObject(JDBCExecutionContext context, MySQLCatalog owner, MySQLTable parent, String constraintName, ResultSet dbResult)
            throws SQLException, DBException
        {
            if (constraintName.equals("PRIMARY")) {
                return new MySQLTableConstraint(
                    parent, constraintName, null, DBSEntityConstraintType.PRIMARY_KEY, true);
            } else {
                return new MySQLTableConstraint(
                    parent, constraintName, null, DBSEntityConstraintType.UNIQUE_KEY, true);
            }
        }

        @Override
        protected MySQLTableConstraintColumn fetchObjectRow(
            JDBCExecutionContext context,
            MySQLTable parent, MySQLTableConstraint object, ResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_NAME);
            MySQLTableColumn column = parent.getColumn(context.getProgressMonitor(), columnName);
            if (column == null) {
                log.warn("Column '" + columnName + "' not found in table '" + parent.getFullQualifiedName() + "'");
                return null;
            }
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_ORDINAL_POSITION);

            return new MySQLTableConstraintColumn(
                object,
                column,
                ordinalPosition);
        }

        @Override
        protected Collection<MySQLTableConstraint> getObjectsCache(MySQLTable parent)
        {
            return parent.getUniqueKeysCache();
        }

        @Override
        protected void cacheObjects(MySQLTable parent, List<MySQLTableConstraint> constraints)
        {
            parent.cacheUniqueKeys(constraints);
        }

        @Override
        protected void cacheChildren(MySQLTableConstraint constraint, List<MySQLTableConstraintColumn> rows)
        {
            constraint.setColumns(rows);
        }
    }

    /**
     * Procedures cache implementation
     */
    class ProceduresCache extends JDBCStructCache<MySQLCatalog, MySQLProcedure, MySQLProcedureColumn> {

        ProceduresCache()
        {
            super(JDBCConstants.PROCEDURE_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, MySQLCatalog owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM " + MySQLConstants.META_TABLE_ROUTINES +
                " WHERE " + MySQLConstants.COL_ROUTINE_SCHEMA + "=?" +
                " ORDER BY " + MySQLConstants.COL_ROUTINE_NAME
            );
            dbStat.setString(1, getName());
            return dbStat;
        }

        @Override
        protected MySQLProcedure fetchObject(JDBCExecutionContext context, MySQLCatalog owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLProcedure(MySQLCatalog.this, dbResult);
        }

        @Override
        protected boolean isChildrenCached(MySQLProcedure parent)
        {
            return parent.isColumnsCached();
        }

        @Override
        protected void cacheChildren(MySQLProcedure parent, List<MySQLProcedureColumn> columns)
        {
            parent.cacheColumns(columns);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(JDBCExecutionContext context, MySQLCatalog owner, MySQLProcedure procedure)
            throws SQLException
        {
            // Load procedure columns thru MySQL metadata
            // There is no metadata table about proc/func columns -
            // it should be parsed from SHOW CREATE PROCEDURE/FUNCTION query
            // Lets driver do it instead of me
            return context.getMetaData().getProcedureColumns(
                getName(),
                null,
                procedure.getName(),
                null).getSource();
        }

        @Override
        protected MySQLProcedureColumn fetchChild(JDBCExecutionContext context, MySQLCatalog owner, MySQLProcedure parent, ResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            int columnTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
            int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
            String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
            int position = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            long columnSize = JDBCUtils.safeGetLong(dbResult, JDBCConstants.LENGTH);
            boolean notNull = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.procedureNoNulls;
            int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE);
            int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
            //int radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.RADIX);
            //DBSDataType dataType = getDataSourceContainer().getInfo().getSupportedDataType(typeName);
            DBSProcedureColumnType columnType;
            switch (columnTypeNum) {
                case DatabaseMetaData.procedureColumnIn: columnType = DBSProcedureColumnType.IN; break;
                case DatabaseMetaData.procedureColumnInOut: columnType = DBSProcedureColumnType.INOUT; break;
                case DatabaseMetaData.procedureColumnOut: columnType = DBSProcedureColumnType.OUT; break;
                case DatabaseMetaData.procedureColumnReturn: columnType = DBSProcedureColumnType.RETURN; break;
                case DatabaseMetaData.procedureColumnResult: columnType = DBSProcedureColumnType.RESULTSET; break;
                default: columnType = DBSProcedureColumnType.UNKNOWN; break;
            }
            if (CommonUtils.isEmpty(columnName) && columnType == DBSProcedureColumnType.RETURN) {
                columnName = "RETURN";
            }
            return new MySQLProcedureColumn(
                parent,
                columnName,
                typeName,
                valueType,
                position,
                columnSize,
                scale, precision, notNull,
                columnType);
        }
    }

    class TriggerCache extends JDBCObjectCache<MySQLCatalog, MySQLTrigger> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, MySQLCatalog owner)
            throws SQLException
        {
            return context.prepareStatement(
                "SHOW FULL TRIGGERS FROM " + getName());
        }

        @Override
        protected MySQLTrigger fetchObject(JDBCExecutionContext context, MySQLCatalog owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            String tableName = JDBCUtils.safeGetString(dbResult, "TABLE");
            MySQLTable triggerTable = CommonUtils.isEmpty(tableName) ? null : getTable(context.getProgressMonitor(), tableName);
            return new MySQLTrigger(MySQLCatalog.this, triggerTable, dbResult);
        }

    }

}
