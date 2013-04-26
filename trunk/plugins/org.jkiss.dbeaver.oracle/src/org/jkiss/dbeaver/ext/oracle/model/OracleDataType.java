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
package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObjectEx;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Oracle data type
 */
public class OracleDataType extends OracleObject<DBSObject>
    implements DBSDataType, DBSEntity, DBPQualifiedObject, OracleSourceObjectEx {

    static final Log log = LogFactory.getLog(OracleTableForeignKey.class);

    static class TypeDesc {
        final DBSDataKind dataKind;
        final int valueType;
        final int precision;
        final int minScale;
        final int maxScale;
        private TypeDesc(DBSDataKind dataKind, int valueType, int precision, int minScale, int maxScale)
        {
            this.dataKind = dataKind;
            this.valueType = valueType;
            this.precision = precision;
            this.minScale = minScale;
            this.maxScale = maxScale;
        }
    }

    static final Map<String, TypeDesc> PREDEFINED_TYPES = new HashMap<String, TypeDesc>();
    static final Map<Integer, TypeDesc> PREDEFINED_TYPE_IDS = new HashMap<Integer, TypeDesc>();
    static  {
        PREDEFINED_TYPES.put("BFILE", new TypeDesc(DBSDataKind.LOB, java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("BINARY ROWID", new TypeDesc(DBSDataKind.ROWID, java.sql.Types.ROWID, 0, 0, 0));
        PREDEFINED_TYPES.put("BINARY_DOUBLE", new TypeDesc(DBSDataKind.NUMERIC, java.sql.Types.DOUBLE, 63, 127, -84));
        PREDEFINED_TYPES.put("BINARY_FLOAT", new TypeDesc(DBSDataKind.NUMERIC, java.sql.Types.FLOAT, 63, 127, -84));
        PREDEFINED_TYPES.put("BLOB", new TypeDesc(DBSDataKind.LOB, java.sql.Types.BLOB, 0, 0, 0));
        PREDEFINED_TYPES.put("CANONICAL", new TypeDesc(DBSDataKind.UNKNOWN, java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("CFILE", new TypeDesc(DBSDataKind.LOB, java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("CHAR", new TypeDesc(DBSDataKind.STRING, java.sql.Types.CHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("CLOB", new TypeDesc(DBSDataKind.LOB, java.sql.Types.CLOB, 0, 0, 0));
        PREDEFINED_TYPES.put("CONTIGUOUS ARRAY", new TypeDesc(DBSDataKind.ARRAY, java.sql.Types.ARRAY, 0, 0, 0));
        PREDEFINED_TYPES.put("DATE", new TypeDesc(DBSDataKind.DATETIME, Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("DECIMAL", new TypeDesc(DBSDataKind.NUMERIC, java.sql.Types.DECIMAL, 63, 127, -84));
        PREDEFINED_TYPES.put("DOUBLE PRECISION", new TypeDesc(DBSDataKind.NUMERIC, java.sql.Types.DOUBLE, 63, 127, -84));
        PREDEFINED_TYPES.put("FLOAT", new TypeDesc(DBSDataKind.NUMERIC, java.sql.Types.FLOAT, 63, 127, -84));
        PREDEFINED_TYPES.put("INTEGER", new TypeDesc(DBSDataKind.NUMERIC, java.sql.Types.INTEGER, 63, 127, -84));
        PREDEFINED_TYPES.put("INTERVAL DAY TO SECOND", new TypeDesc(DBSDataKind.UNKNOWN, java.sql.Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("INTERVAL YEAR TO MONTH", new TypeDesc(DBSDataKind.UNKNOWN, java.sql.Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("LOB POINTER", new TypeDesc(DBSDataKind.LOB, java.sql.Types.BLOB, 0, 0, 0));
        PREDEFINED_TYPES.put("NAMED COLLECTION", new TypeDesc(DBSDataKind.ARRAY, java.sql.Types.ARRAY, 0, 0, 0));
        PREDEFINED_TYPES.put("NAMED OBJECT", new TypeDesc(DBSDataKind.OBJECT, java.sql.Types.STRUCT, 0, 0, 0));
        PREDEFINED_TYPES.put("NUMBER", new TypeDesc(DBSDataKind.NUMERIC, java.sql.Types.NUMERIC, 63, 127, -84));
        PREDEFINED_TYPES.put("OCTET", new TypeDesc(DBSDataKind.BINARY, java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("OID", new TypeDesc(DBSDataKind.UNKNOWN, java.sql.Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("POINTER", new TypeDesc(DBSDataKind.UNKNOWN, java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("RAW", new TypeDesc(DBSDataKind.BINARY, java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("REAL", new TypeDesc(DBSDataKind.NUMERIC, java.sql.Types.REAL, 63, 127, -84));
        PREDEFINED_TYPES.put("REF", new TypeDesc(DBSDataKind.REFERENCE, java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("SIGNED BINARY INTEGER", new TypeDesc(DBSDataKind.NUMERIC, java.sql.Types.INTEGER, 63, 127, -84));
        PREDEFINED_TYPES.put("SMALLINT", new TypeDesc(DBSDataKind.NUMERIC, java.sql.Types.SMALLINT, 63, 127, -84));
        PREDEFINED_TYPES.put("TABLE", new TypeDesc(DBSDataKind.OBJECT, java.sql.Types.OTHER, 0, 0, 0));
        PREDEFINED_TYPES.put("TIME", new TypeDesc(DBSDataKind.DATETIME, java.sql.Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("TIME WITH TZ", new TypeDesc(DBSDataKind.DATETIME, java.sql.Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP", new TypeDesc(DBSDataKind.DATETIME, java.sql.Types.TIMESTAMP, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP WITH LOCAL TZ", new TypeDesc(DBSDataKind.DATETIME, -102, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP WITH TZ", new TypeDesc(DBSDataKind.DATETIME, -101, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP WITH LOCAL TIME ZONE", new TypeDesc(DBSDataKind.DATETIME, -102, 0, 0, 0));
        PREDEFINED_TYPES.put("TIMESTAMP WITH TIME ZONE", new TypeDesc(DBSDataKind.DATETIME, -101, 0, 0, 0));
        PREDEFINED_TYPES.put("UNSIGNED BINARY INTEGER", new TypeDesc(DBSDataKind.NUMERIC, java.sql.Types.BIGINT, 63, 127, -84));
        PREDEFINED_TYPES.put("UROWID", new TypeDesc(DBSDataKind.ROWID, java.sql.Types.ROWID, 0, 0, 0));
        PREDEFINED_TYPES.put("VARCHAR", new TypeDesc(DBSDataKind.STRING, java.sql.Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("VARCHAR2", new TypeDesc(DBSDataKind.STRING, java.sql.Types.VARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("VARYING ARRAY", new TypeDesc(DBSDataKind.ARRAY, java.sql.Types.ARRAY, 0, 0, 0));

        PREDEFINED_TYPES.put("VARRAY", new TypeDesc(DBSDataKind.ARRAY, java.sql.Types.ARRAY, 0, 0, 0));
        PREDEFINED_TYPES.put("ROWID", new TypeDesc(DBSDataKind.ROWID, java.sql.Types.ROWID, 0, 0, 0));
        PREDEFINED_TYPES.put("LONG", new TypeDesc(DBSDataKind.BINARY, java.sql.Types.LONGVARBINARY, 0, 0, 0));
        PREDEFINED_TYPES.put("RAW", new TypeDesc(DBSDataKind.BINARY, java.sql.Types.LONGVARBINARY, 0, 0, 0));
        PREDEFINED_TYPES.put("LONG RAW", new TypeDesc(DBSDataKind.BINARY, java.sql.Types.LONGVARBINARY, 0, 0, 0));
        PREDEFINED_TYPES.put("NVARCHAR2", new TypeDesc(DBSDataKind.STRING, java.sql.Types.NVARCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("NCHAR", new TypeDesc(DBSDataKind.STRING, java.sql.Types.NCHAR, 0, 0, 0));
        PREDEFINED_TYPES.put("NCLOB", new TypeDesc(DBSDataKind.LOB, java.sql.Types.NCLOB, 0, 0, 0));

        PREDEFINED_TYPES.put("REF CURSOR", new TypeDesc(DBSDataKind.REFERENCE, -10, 0, 0, 0));

        for (TypeDesc type : PREDEFINED_TYPES.values()) {
            PREDEFINED_TYPE_IDS.put(type.valueType, type);
        }
    }
    
    private String typeCode;
    private byte[] typeOID;
    private Object superType;
    private final AttributeCache attributeCache;
    private final MethodCache methodCache;
    private boolean flagPredefined;
    private boolean flagIncomplete;
    private boolean flagFinal;
    private boolean flagInstantiable;
    private TypeDesc typeDesc;
    private int valueType = java.sql.Types.OTHER;
    private String sourceDeclaration;
    private String sourceDefinition;

    public OracleDataType(DBSObject owner, String typeName, boolean persisted)
    {
        super(owner, typeName, persisted);
        this.attributeCache = new AttributeCache();
        this.methodCache = new MethodCache();
        if (owner instanceof OracleDataSource) {
            flagPredefined = true;
            findTypeDesc(typeName);
        }
    }

    public OracleDataType(DBSObject owner, ResultSet dbResult)
    {
        super(owner, JDBCUtils.safeGetString(dbResult, "TYPE_NAME"), true);
        this.typeCode = JDBCUtils.safeGetString(dbResult, "TYPECODE");
        this.typeOID = JDBCUtils.safeGetBytes(dbResult, "TYPE_OID");
        this.flagPredefined = JDBCUtils.safeGetBoolean(dbResult, "PREDEFINED", OracleConstants.YES);
        this.flagIncomplete = JDBCUtils.safeGetBoolean(dbResult, "INCOMPLETE", OracleConstants.YES);
        this.flagFinal = JDBCUtils.safeGetBoolean(dbResult, "FINAL", OracleConstants.YES);
        this.flagInstantiable = JDBCUtils.safeGetBoolean(dbResult, "INSTANTIABLE", OracleConstants.YES);
        String superTypeOwner = JDBCUtils.safeGetString(dbResult, "SUPERTYPE_OWNER");
        boolean hasAttributes;
        boolean hasMethods;
        if (!CommonUtils.isEmpty(superTypeOwner)) {
            this.superType = new OracleLazyReference(
                superTypeOwner,
                JDBCUtils.safeGetString(dbResult, "SUPERTYPE_NAME"));
            hasAttributes = JDBCUtils.safeGetInt(dbResult, "LOCAL_ATTRIBUTES") > 0;
            hasMethods = JDBCUtils.safeGetInt(dbResult, "LOCAL_METHODS") > 0;
        } else {
            hasAttributes = JDBCUtils.safeGetInt(dbResult, "ATTRIBUTES") > 0;
            hasMethods = JDBCUtils.safeGetInt(dbResult, "METHODS") > 0;
        }
        attributeCache = hasAttributes ? new AttributeCache() : null;
        methodCache = hasMethods ? new MethodCache() : null;
        
        if (owner instanceof OracleDataSource && flagPredefined) {
            // Determine value type for predefined types
            findTypeDesc(name);
        } else {
            if ("COLLECTION".equals(this.typeCode)) {
                this.valueType = java.sql.Types.ARRAY;
            } else if ("OBJECT".equals(this.typeCode)) {
                this.valueType = java.sql.Types.STRUCT;
            }
        }
    }

    // Use by tree navigator thru reflection
    public boolean hasMethods()
    {
        return methodCache != null;
    }
    // Use by tree navigator thru reflection
    public boolean hasAttributes()
    {
        return attributeCache != null;
    }

    private boolean findTypeDesc(String typeName)
    {
        if (typeName.startsWith("PL/SQL")) {
            // Don't care about PL/SQL types
            return true;
        }
        typeName = normalizeTypeName(typeName);
        this.typeDesc = PREDEFINED_TYPES.get(typeName);
        if (this.typeDesc == null) {
            log.warn("Unknown predefined type: " + typeName);
            return false;
        } else {
            this.valueType = this.typeDesc.valueType;
            return true;
        }
    }

    public static DBSDataKind getDataKind(int typeID)
    {
        TypeDesc desc = PREDEFINED_TYPE_IDS.get(typeID);
        return desc != null ? desc.dataKind : null;
    }

    @Override
    public OracleSchema getSchema()
    {
        return parent instanceof OracleSchema ? (OracleSchema)parent : null;
    }

    @Override
    public OracleSourceType getSourceType()
    {
        return OracleSourceType.TYPE;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getSourceDeclaration(DBRProgressMonitor monitor) throws DBCException
    {
        if (sourceDeclaration == null && monitor != null) {
            sourceDeclaration = OracleUtils.getSource(monitor, this, false);
        }
        return sourceDeclaration;
    }

    @Override
    public void setSourceDeclaration(String sourceDeclaration)
    {
        this.sourceDeclaration = sourceDeclaration;
    }

    @Override
    public IDatabasePersistAction[] getCompileActions()
    {
        return new IDatabasePersistAction[] {
            new OracleObjectPersistAction(
                OracleObjectType.VIEW,
                "Compile type",
                "ALTER TYPE " + getFullQualifiedName() + " COMPILE"
            )};
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getSourceDefinition(DBRProgressMonitor monitor) throws DBException
    {
        if (sourceDefinition == null && monitor != null) {
            sourceDefinition = OracleUtils.getSource(monitor, this, true);
        }
        return sourceDefinition;
    }

    @Override
    public void setSourceDefinition(String source)
    {
        this.sourceDefinition = source;
    }

    @Override
    public String getTypeName()
    {
        return getFullQualifiedName();
    }

    @Override
    public int getTypeID()
    {
        return valueType;
    }

    @Override
    public DBSDataKind getDataKind()
    {
        return JDBCUtils.resolveDataKind(getDataSource(), getName(), valueType);
    }

    @Override
    public int getScale()
    {
        return typeDesc == null ? 0 : typeDesc.minScale;
    }

    @Override
    public int getPrecision()
    {
        return typeDesc == null ? 0 : typeDesc.precision;
    }

    @Override
    public long getMaxLength()
    {
        return getPrecision();
    }

    @Override
    public int getMinScale()
    {
        return typeDesc == null ? 0 : typeDesc.minScale;
    }

    @Override
    public int getMaxScale()
    {
        return typeDesc == null ? 0 : typeDesc.maxScale;
    }

    @Override
    public DBSObject getParentObject()
    {
        return parent instanceof OracleSchema ?
            parent :
            parent instanceof OracleDataSource ? ((OracleDataSource) parent).getContainer() : null;
    }

    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, editable = true, order = 2)
    public String getTypeCode()
    {
        return typeCode;
    }

    @Property(hidden = true, viewable = false, editable = false)
    public byte[] getTypeOID()
    {
        return typeOID;
    }

    @Property(viewable = true, editable = true, order = 3)
    public OracleDataType getSuperType(DBRProgressMonitor monitor)
    {
        if (superType  == null) {
            return null;
        } else if (superType instanceof OracleDataType) {
            return (OracleDataType)superType;
        } else {
            try {
                OracleLazyReference olr = (OracleLazyReference) superType;
                final OracleSchema superSchema = getDataSource().getSchema(monitor, olr.schemaName);
                if (superSchema == null) {
                    log.warn("Referenced schema '" + olr.schemaName + "' not found for super type '" + olr.objectName + "'");
                } else {
                    superType = superSchema.dataTypeCache.getObject(monitor, superSchema, olr.objectName);
                    if (superType == null) {
                        log.warn("Referenced type '" + olr.objectName + "' not found in schema '" + olr.schemaName + "'");
                    } else {
                        return (OracleDataType)superType;
                    }
                }
            } catch (DBException e) {
                log.error(e);
            }
            superType = null;
            return null;
        }
    }

    @Property(viewable = true, order = 4)
    public boolean isPredefined()
    {
        return flagPredefined;
    }

    @Property(viewable = true, order = 5)
    public boolean isIncomplete()
    {
        return flagIncomplete;
    }

    @Property(viewable = true, order = 6)
    public boolean isFinal()
    {
        return flagFinal;
    }

    @Property(viewable = true, order = 7)
    public boolean isInstantiable()
    {
        return flagInstantiable;
    }

    @Override
    public DBSEntityType getEntityType()
    {
        return DBSEntityType.TYPE;
    }

    @Override
    @Association
    public Collection<OracleDataTypeAttribute> getAttributes(DBRProgressMonitor monitor)
        throws DBException
    {
        return attributeCache != null ? attributeCache.getObjects(monitor, this) : null;
    }

    @Override
    public Collection<? extends DBSEntityConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public OracleDataTypeAttribute getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException
    {
        return attributeCache != null ? attributeCache.getObject(monitor, this, attributeName) : null;
    }

    @Association
    public Collection<OracleDataTypeMethod> getMethods(DBRProgressMonitor monitor)
        throws DBException
    {
        return methodCache != null ? methodCache.getObjects(monitor, this) : null;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public String getFullQualifiedName()
    {
        return parent instanceof OracleSchema ?
            DBUtils.getFullQualifiedName(getDataSource(), parent, this) :
            name;
    }

    @Override
    public String toString()
    {
        return getFullQualifiedName();
    }

    public static OracleDataType resolveDataType(DBRProgressMonitor monitor, OracleDataSource dataSource, String typeOwner, String typeName)
    {
        typeName = normalizeTypeName(typeName);
        OracleSchema typeSchema = null;
        OracleDataType type = null;
        if (typeOwner != null) {
            try {
                typeSchema = dataSource.getSchema(monitor, typeOwner);
                if (typeSchema == null) {
                    OracleUtils.log.error("Type attr schema '" + typeOwner + "' not found");
                } else {
                    type = typeSchema.getDataType(monitor, typeName);
                }
            } catch (DBException e) {
                log.error(e);
            }
        } else {
            type = (OracleDataType)dataSource.getDataType(typeName);
        }
        if (type == null) {
            log.debug("Data type '" + typeName + "' not found - declare new one");
            type = new OracleDataType(typeSchema == null ? dataSource : typeSchema, typeName, true);
            type.flagPredefined = true;
            if (typeSchema == null) {
                dataSource.dataTypeCache.cacheObject(type);
            } else {
                typeSchema.dataTypeCache.cacheObject(type);
            }
        }
        return type;
    }

    private static String normalizeTypeName(String typeName) {
        for (;;) {
            int modIndex = typeName.indexOf('(');
            if (modIndex == -1) {
                break;
            }
            int modEnd = typeName.indexOf(')', modIndex);
            if (modEnd == -1) {
                break;
            }
            typeName = typeName.substring(0, modIndex) +
                (modEnd == typeName.length() - 1 ? "" : typeName.substring(modEnd + 1));
        }
        return typeName;
    }

    @Override
    public DBSObjectState getObjectState()
    {
        return DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException
    {

    }

    private class AttributeCache extends JDBCObjectCache<OracleDataType, OracleDataTypeAttribute> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, OracleDataType owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM SYS.ALL_TYPE_ATTRS " +
                "WHERE OWNER=? AND TYPE_NAME=? ORDER BY ATTR_NO");
            dbStat.setString(1, OracleDataType.this.parent.getName());
            dbStat.setString(2, getName());
            return dbStat;
        }
        @Override
        protected OracleDataTypeAttribute fetchObject(JDBCExecutionContext context, OracleDataType owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataTypeAttribute(context.getProgressMonitor(), OracleDataType.this, resultSet);
        }
    }

    private class MethodCache extends JDBCObjectCache<OracleDataType, OracleDataTypeMethod> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, OracleDataType owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT m.*,r.RESULT_TYPE_OWNER,RESULT_TYPE_NAME,RESULT_TYPE_MOD\n" +
                "FROM SYS.ALL_TYPE_METHODS m\n" +
                "LEFT OUTER JOIN SYS.ALL_METHOD_RESULTS r ON r.OWNER=m.OWNER AND r.TYPE_NAME=m.TYPE_NAME AND r.METHOD_NAME=m.METHOD_NAME AND r.METHOD_NO=m.METHOD_NO\n" +
                "WHERE m.OWNER=? AND m.TYPE_NAME=?\n" +
                "ORDER BY m.METHOD_NO");
            dbStat.setString(1, OracleDataType.this.parent.getName());
            dbStat.setString(2, getName());
            return dbStat;
        }

        @Override
        protected OracleDataTypeMethod fetchObject(JDBCExecutionContext context, OracleDataType owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataTypeMethod(context.getProgressMonitor(), OracleDataType.this, resultSet);
        }
    }

}
