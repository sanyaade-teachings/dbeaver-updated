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

package org.jkiss.dbeaver.ext.altibase.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableColumnManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;

import java.util.Map;

/**
 * FireBirdTableColumnManager
 */
public class AltibaseTableColumnManager extends GenericTableColumnManager implements DBEObjectRenamer<GenericTableColumn> {

    @Override
    public void renameObject(DBECommandContext commandContext, GenericTableColumn object, 
            Map<String, Object> options, String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    /*
    private final ColumnModifier<GenericTableColumn> ComputedModifier = (monitor, column, sql, command) -> {
        final String definition = ((AltibaseTableColumn) column).getComputedDefinition();
        if (CommonUtils.isNotEmpty(definition)) {
            sql.append(" GENERATED ALWAYS AS ").append(definition);
        }
    };

    private final ColumnModifier<GenericTableColumn> IncrementModifier = (monitor, column, sql, command) -> {
        if (column.isAutoIncrement()) {
            sql.append(" GENERATED BY DEFAULT AS IDENTITY"); //$NON-NLS-1$
        }
    };

    @Override
    public void addIncrementClauseToNestedDeclaration(DBECommandAbstract<GenericTableColumn> command, StringBuilder decl) {
        // Increment clause already append with the IncrementModifier
    }

    @Override
    protected ColumnModifier[] getSupportedModifiers(GenericTableColumn column, Map<String, Object> options) {
        if (CommonUtils.isNotEmpty(((AltibaseTableColumn) column).getComputedDefinition())) {
            // Only GENERATED ALWAYS AS is supported when field is computed
            return new ColumnModifier[] {DataTypeModifier, ComputedModifier};
        }
        if (column.isAutoIncrement()) {
            // DefaultModifier and ComputedModifier not supported in this case, IncrementModifier must be before NotNullModifier
            return new ColumnModifier[] {DataTypeModifier, IncrementModifier, NotNullModifier};
        }
        // According to SQL92 DEFAULT comes before constraints
        return new ColumnModifier[] {DataTypeModifier, DefaultModifier, NotNullModifier};
    }

     */
    /**
     * Is is pretty standard
     */
    /*
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        final AltibaseTableColumn column = (AltibaseTableColumn) command.getObject();

        String prefix = "ALTER TABLE " + DBUtils.getObjectFullName(column.getTable(), DBPEvaluationContext.DDL) + " ALTER COLUMN " + DBUtils.getQuotedIdentifier(column) + " ";
        String typeClause = column.getFullTypeName();
        String computedDefinition = column.getComputedDefinition(monitor);
        if (command.getProperty(DBConstants.PROP_ID_TYPE_NAME) != null || command.getProperty("maxLength") != null || command.getProperty("precision") != null || command.getProperty("scale") != null) {
            final StringBuilder ddl = new StringBuilder()
                .append(prefix).append("TYPE ").append(typeClause);
            if (CommonUtils.isNotEmpty(computedDefinition)) {
                // Can't change type for computed columns without this clause.
                ddl.append(" GENERATED ALWAYS AS ").append(computedDefinition);
            }
            actionList.add(new SQLDatabasePersistAction("Set column type", ddl.toString()));
        }
        if (command.getProperty(DBConstants.PROP_ID_DEFAULT_VALUE) != null) {
            if (CommonUtils.isEmpty(column.getDefaultValue())) {
                actionList.add(new SQLDatabasePersistAction("Drop column default", prefix + "DROP DEFAULT"));
            } else {
                actionList.add(new SQLDatabasePersistAction("Set column default", prefix + "SET DEFAULT " + column.getDefaultValue()));
            }
        }
        if (command.getProperty("computedDefinition") != null) {
            actionList.add(new SQLDatabasePersistAction(
                "Set column computed definition",
                prefix + "TYPE " + typeClause + " GENERATED ALWAYS AS (" + computedDefinition + ")"
            ));
        }
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            actionList.add(new SQLDatabasePersistAction("Set column comment", "COMMENT ON COLUMN " +
                DBUtils.getObjectFullName(column.getTable(), DBPEvaluationContext.DDL) + "." + DBUtils.getQuotedIdentifier(column) +
                " IS " + SQLUtils.quoteString(column, CommonUtils.notEmpty(column.getDescription()))));
        }
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        final GenericTableColumn column = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                "Rename column",
                "ALTER TABLE " + DBUtils.getQuotedIdentifier(column.getTable()) + " ALTER COLUMN " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) +
                    " TO " + DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName())));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull GenericTableColumn object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    ///////////////////////////////////////////////
    // Reorder

    @Override
    protected void addObjectReorderActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectReorderCommand command, Map<String, Object> options) {
        final GenericTableColumn column = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Reorder column",
                "ALTER TABLE " + DBUtils.getQuotedIdentifier(command.getObject().getTable()) + " ALTER COLUMN " +
                    DBUtils.getQuotedIdentifier(command.getObject()) + " POSITION " + column.getOrdinalPosition()));
    }

    @Override
    public int getMinimumOrdinalPosition(GenericTableColumn object) {
        return 1;
    }

    @Override
    public int getMaximumOrdinalPosition(GenericTableColumn object) {
        try {
            return object.getTable().getAttributes(new VoidProgressMonitor()).size();
        } catch (DBException e) {
            log.error("Error reading columns for maximum order position", e);
            return 0;
        }
    }

    @Override
    public void setObjectOrdinalPosition(DBECommandContext commandContext, GenericTableColumn object, List<GenericTableColumn> siblingObjects, int newPosition) throws DBException {
        processObjectReorder(commandContext, object, siblingObjects, newPosition);
    }
     */
}
