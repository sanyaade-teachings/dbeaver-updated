package org.jkiss.dbeaver.ext.gaussdb.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBFunction;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBSchema;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

public class GaussDBFunctionManager extends SQLObjectEditor<GaussDBFunction, GaussDBSchema> implements
                                    DBEObjectRenamer<GaussDBFunction> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, GaussDBFunction> getObjectsCache(GaussDBFunction object) {
        GaussDBSchema schema = (GaussDBSchema) object.getContainer();
        return schema.getGaussDBFunctionsCache();
    }

    @Override
    public boolean canCreateObject(Object container) {
        return container instanceof PostgreSchema
                    && ((PostgreSchema) container).getDataSource().getServerType().supportsFunctionCreate();
    }

    @Override
    public boolean canDeleteObject(GaussDBFunction object) {
        return object.getDataSource().getServerType().supportsFunctionCreate();
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor,
                                            ObjectChangeCommand command,
                                            Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Function name cannot be empty");
        }
    }

    @Override
    protected GaussDBFunction createDatabaseObject(DBRProgressMonitor monitor,
                                                   DBECommandContext context,
                                                   Object container,
                                                   Object copyFrom,
                                                   Map<String, Object> options) throws DBException {
        return new GaussDBFunction((PostgreSchema) container);
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor,
                                          DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions,
                                          SQLObjectEditor<GaussDBFunction, GaussDBSchema>.ObjectCreateCommand command,
                                          Map<String, Object> options) throws DBException {
        createOrReplaceProcedureQuery(actions, command.getObject());
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor,
                                          DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actionList,
                                          ObjectChangeCommand command,
                                          Map<String, Object> options) {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            createOrReplaceProcedureQuery(actionList, command.getObject());
        }
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor,
                                          DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions,
                                          SQLObjectEditor<GaussDBFunction, GaussDBSchema>.ObjectDeleteCommand command,
                                          Map<String, Object> options) throws DBException {
        String objectType = command.getObject().getProcedureTypeName();
        actions.add(new SQLDatabasePersistAction("Drop function",
                                                 "DROP " + objectType + " " + command.getObject().getFullQualifiedSignature()) //$NON-NLS-1$
        );

    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actions, PostgreProcedure procedure) {
        actions.add(new SQLDatabasePersistAction("Create function", procedure.getBody(), true));
    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor,
                                         DBCExecutionContext executionContext,
                                         List<DBEPersistAction> actions,
                                         NestedObjectCommand<GaussDBFunction, PropertyHandler> command,
                                         Map<String, Object> options) {
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            actions.add(new SQLDatabasePersistAction("Comment function",
                                                     "COMMENT ON " + command.getObject().getProcedureTypeName() + " "
                                                                 + command.getObject().getFullQualifiedSignature() + " IS "
                                                                 + SQLUtils.quoteString(command.getObject(),
                                                                                        command.getObject().getDescription())));
        }
        boolean isDDL = CommonUtils.getOption(options, DBPScriptObject.OPTION_DDL_SOURCE);
        if (isDDL) {
            try {
                PostgreUtils.getObjectGrantPermissionActions(monitor, command.getObject(), actions, options);
            } catch (DBException e) {
                log.error(e);
            }
        }

    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext,
                             @NotNull GaussDBFunction object,
                             @NotNull Map<String, Object> options,
                             @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor,
                                          DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions,
                                          ObjectRenameCommand command,
                                          Map<String, Object> options) {
        GaussDBFunction procedure = command.getObject();
        actions.add(new SQLDatabasePersistAction("Rename function",
                                                 "ALTER " + command.getObject().getProcedureTypeName() + " "
                                                             + DBUtils.getQuotedIdentifier(procedure.getSchema()) + "."
                                                             + PostgreProcedure.makeOverloadedName(procedure.getSchema(),
                                                                                                   command.getOldName(),
                                                                                                   procedure.getParameters(monitor),
                                                                                                   true, false, false)
                                                             + " RENAME TO "
                                                             + DBUtils.getQuotedIdentifier(procedure.getDataSource(),
                                                                                           command.getNewName())));
    }

}