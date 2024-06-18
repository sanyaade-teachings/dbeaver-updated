package org.jkiss.dbeaver.ext.gaussdb.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gaussdb.model.DBCompatibilityEnum;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBDataSource;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionAtomic;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

public class GaussDBDatabaseManager extends SQLObjectEditor<GaussDBDatabase, GaussDBDataSource> implements
                                    DBEObjectRenamer<GaussDBDatabase> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, GaussDBDatabase> getObjectsCache(GaussDBDatabase object) {
        return null;
    }

    @Override
    public void deleteObject(DBECommandContext commandContext,
                             GaussDBDatabase object,
                             Map<String, Object> options) throws DBException {
        if (object == object.getDataSource().getDefaultInstance()) {
            throw new DBException("Cannot drop the currently open database."
                        + "\nSwitch to another database and try again\n(Note: enable 'Show all databases' option to see them).");
        }
        super.deleteObject(commandContext, object, options);
    }

    @Override
    public void renameObject(DBECommandContext commandContext,
                             GaussDBDatabase database,
                             Map<String, Object> options,
                             String newName) throws DBException {
        processObjectRename(commandContext, database, options, newName);
    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor,
                                         DBCExecutionContext executionContext,
                                         List<DBEPersistAction> actions,
                                         NestedObjectCommand<GaussDBDatabase, PropertyHandler> command,
                                         Map<String, Object> options) throws DBException {
        if (command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            PostgreDatabase database = command.getObject();
            actions.add(new SQLDatabasePersistAction("COMMENT ON DATABASE " + DBUtils.getQuotedIdentifier(database) + " IS "
                        + SQLUtils.quoteString(database, CommonUtils.notEmpty(database.getDescription()))));
        }
    }

    @Override
    protected GaussDBDatabase createDatabaseObject(DBRProgressMonitor monitor,
                                                   DBECommandContext context,
                                                   Object container,
                                                   Object copyFrom,
                                                   Map<String, Object> options) throws DBException {
        return ((GaussDBDataSource) container).createDatabaseImpl(monitor, "NewDatabase", null, null, null, null);
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor,
                                          DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions,
                                          SQLObjectEditor<GaussDBDatabase, GaussDBDataSource>.ObjectCreateCommand command,
                                          Map<String, Object> options) throws DBException {
        final GaussDBDatabase database = command.getObject();
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE DATABASE ").append(DBUtils.getQuotedIdentifier(database));

        if (database.getInitialOwner() != null) {
            sql.append("\nOWNER = ").append(DBUtils.getQuotedIdentifier(database.getInitialOwner()));
        }
        if (database.getInitialEncoding() != null) {
            sql.append("\nENCODING = '").append(database.getInitialEncoding().getName()).append("'");
        }
        if (database.getInitialTablespace() != null) {
            sql.append("\nTABLESPACE = ")
                        .append(DBUtils.getQuotedIdentifier(database.getDataSource(), database.getInitialTablespace().getName()));
        }
        if (database.getDatabaseCompatibleMode() != null && "" != database.getDatabaseCompatibleMode()) {
            sql.append("\nDBCOMPATIBILITY = '").append(DBCompatibilityEnum.of(database.getDatabaseCompatibleMode()).getdValue())
                        .append("'");
        }
        actions.add(new CreateDatabaseAction(database, sql));
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor,
                                          DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions,
                                          SQLObjectEditor<GaussDBDatabase, GaussDBDataSource>.ObjectDeleteCommand command,
                                          Map<String, Object> options) throws DBException {
        actions.add(new DeleteDatabaseAction(command));
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor,
                                          DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions,
                                          ObjectRenameCommand command,
                                          Map<String, Object> options) {
        actions.add(new SQLDatabasePersistAction("Rename database",
                                                 "ALTER DATABASE " //$NON-NLS-1$
                                                             + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(),
                                                                                           command.getOldName())
                                                             + " RENAME TO "
                                                             + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(),
                                                                                           command.getNewName())));
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor,
                                          DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actionList,
                                          ObjectChangeCommand command,
                                          Map<String, Object> options) {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            try {
                generateAlterActions(monitor, actionList, command);
            } catch (DBException e) {
                log.error(e);
            }
        }
    }

    private void generateAlterActions(DBRProgressMonitor monitor,
                                      List<DBEPersistAction> actionList,
                                      ObjectChangeCommand command) throws DBException {
        final PostgreDatabase database = command.getObject();
        final String alterPrefix = "ALTER DATABASE " + DBUtils.getQuotedIdentifier(command.getObject()) + " ";
        if (command.hasProperty("defaultTablespace")) {
            actionList.add(new SQLDatabasePersistAction(alterPrefix + "SET TABLESPACE "
                        + DBUtils.getQuotedIdentifier(database.getDefaultTablespace(monitor))));
        }
        if (command.hasProperty("defaultEncoding")) {
            actionList.add(new SQLDatabasePersistAction(alterPrefix + "SET ENCODING "
                        + DBUtils.getQuotedIdentifier(database.getDefaultEncoding(monitor))));
        }
        if (command.hasProperty("dBA")) {
            actionList.add(new SQLDatabasePersistAction(alterPrefix + "OWNER TO "
                        + DBUtils.getQuotedIdentifier(database.getDBA(monitor))));
        }
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor,
                                            ObjectChangeCommand command,
                                            Map<String, Object> options) throws DBException {
        super.validateObjectProperties(monitor, command, options);
        options.put(DBECommandContext.OPTION_AVOID_TRANSACTIONS, true);
    }

    private static class DeleteDatabaseAction extends SQLDatabasePersistActionAtomic {

        private final PostgreDatabase database;

        public DeleteDatabaseAction(ObjectDeleteCommand command) {
            super("Drop database", "DROP DATABASE " + DBUtils.getQuotedIdentifier(command.getObject()));
            database = command.getObject();
        }

        @Override
        public void beforeExecute(DBCSession session) throws DBCException {
            super.beforeExecute(session);
            database.shutdown(session.getProgressMonitor());
        }
    }

    private static class CreateDatabaseAction extends SQLDatabasePersistActionAtomic {
        private final GaussDBDatabase database;

        public CreateDatabaseAction(PostgreDatabase database, StringBuilder sql) {
            super("Create database", sql.toString());
            this.database = (GaussDBDatabase) database;
        }

        @Override
        public void afterExecute(DBCSession session, Throwable error) throws DBCException {
            super.afterExecute(session, error);
            if (error == null) {
                try {
                    DBRProgressMonitor monitor = session.getProgressMonitor();
                    database.checkInstanceConnection(monitor);
                    database.readDatabaseInfo(monitor);
                    database.checkPackageSupport(monitor);
                } catch (DBException e) {
                    log.error("Can't connect to the new database");
                }
            }
        }
    }

}