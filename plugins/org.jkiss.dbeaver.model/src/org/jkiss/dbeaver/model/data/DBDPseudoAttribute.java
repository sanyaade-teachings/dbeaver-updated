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

package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

/**
 * Pseudo attribute
 */
public class DBDPseudoAttribute implements DBPNamedObject {

    public enum PropagationPolicy {
        NORMAL(true),
        LOCAL(false);

        public final boolean projected;

        PropagationPolicy(boolean projected) {
            this.projected = projected;
        }
    }

    private final DBDPseudoAttributeType type;
    private final String name;
    private final String queryExpression;
    private final String alias;
    private final String description;
    private final boolean autoGenerated;
    private final PropagationPolicy propagationPolicy;

    public DBDPseudoAttribute(DBDPseudoAttributeType type, String name, String queryExpression, @Nullable String alias, String description, boolean autoGenerated, PropagationPolicy propagationPolicy)
    {
        this.type = type;
        this.name = name;
        this.queryExpression = queryExpression;
        this.alias = alias;
        this.description = description;
        this.autoGenerated = autoGenerated;
        this.propagationPolicy = propagationPolicy;
    }

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    public DBDPseudoAttributeType getType()
    {
        return type;
    }

    public boolean isAutoGenerated() {
        return autoGenerated;
    }

    public PropagationPolicy getPropagationPolicy() {
        return propagationPolicy;
    }

    public String getQueryExpression()
    {
        return queryExpression;
    }

    @Nullable
    public String getAlias()
    {
        return alias;
    }

    public String getDescription()
    {
        return description;
    }

    public String translateExpression(String tableAlias) {
        return queryExpression.replace("$alias", tableAlias);
    }

    @Override
    public String toString()
    {
        return name + " (" + type + ")";
    }

    public DBSEntityAttribute createFakeAttribute(DBSEntity owner, DBCAttributeMetaData attribute)
    {
        return new FakeEntityAttribute(owner, attribute);
    }

    @Nullable
    public static DBDPseudoAttribute getAttribute(DBDPseudoAttribute[] attributes, DBDPseudoAttributeType type)
    {
        if (attributes == null || attributes.length == 0) {
            return null;
        }
        for (DBDPseudoAttribute attribute : attributes) {
            if (attribute.getType() == type) {
                return attribute;
            }
        }
        return null;
    }

    private class FakeEntityAttribute implements DBSEntityAttribute, DBPQualifiedObject {
        private DBSEntity owner;
        private DBCAttributeMetaData attribute;

        public FakeEntityAttribute(DBSEntity owner, DBCAttributeMetaData attribute)
        {
            this.owner = owner;
            this.attribute = attribute;
        }

        @Override
        public boolean isAutoGenerated()
        {
            return autoGenerated;
        }

        @Override
        public int getOrdinalPosition()
        {
            return attribute.getOrdinalPosition();
        }

        @Nullable
        @Override
        public String getDefaultValue()
        {
            return null;
        }

        @NotNull
        @Override
        public DBSEntity getParentObject()
        {
            return owner;
        }

        @Nullable
        @Override
        public String getDescription()
        {
            return description;
        }

        @NotNull
        @Override
        public DBPDataSource getDataSource()
        {
            return owner.getDataSource();
        }

        @NotNull
        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public boolean isPersisted()
        {
            return true;
        }

        @Override
        public boolean isRequired()
        {
            return attribute.isRequired();
        }

        @NotNull
        @Override
        public String getTypeName()
        {
            return attribute.getTypeName();
        }

        @NotNull
        @Override
        public String getFullTypeName() {
            return attribute.getFullTypeName();
        }

        @Override
        public int getTypeID()
        {
            return attribute.getTypeID();
        }

        @NotNull
        @Override
        public DBPDataKind getDataKind()
        {
            return attribute.getDataKind();
        }

        @Nullable
        @Override
        public Integer getScale()
        {
            return attribute.getScale();
        }

        @Override
        public Integer getPrecision()
        {
            return attribute.getPrecision();
        }

        @Override
        public long getMaxLength()
        {
            return attribute.getMaxLength();
        }

        @Override
        public long getTypeModifiers() {
            return attribute.getTypeModifiers();
        }

        /**
         * Implements qualified object to avoid attribute name quoting
         */
        @NotNull
        @Override
        public String getFullyQualifiedName(DBPEvaluationContext context) {
            return name;
        }
    }
}
