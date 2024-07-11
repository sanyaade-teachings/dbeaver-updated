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
package org.jkiss.dbeaver.ext.cubrid.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTrigger;
import org.jkiss.dbeaver.ext.generic.edit.GenericTriggerManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;

public class CubridTriggerManager extends GenericTriggerManager {

    public static final String BASE_TRIGGER_NAME = "new_trigger";

    @Override
    public boolean canCreateObject(Object container) {
        return container instanceof GenericTableBase;
    }

    @Override
    protected CubridTrigger createDatabaseObject(
            DBRProgressMonitor monitor,
            DBECommandContext context,
            Object container,
            Object copyFrom,
            Map<String, Object> options) throws DBException {
        return new CubridTrigger((GenericTableBase) container, BASE_TRIGGER_NAME, monitor);
    }

    @Override
    protected void createOrReplaceTriggerQuery(
            DBRProgressMonitor monitor,
            DBCExecutionContext executionContext,
            List<DBEPersistAction> actions,
            GenericTrigger genericTrigger,
            boolean create) {
    	CubridTrigger trigger = (CubridTrigger) genericTrigger;
        StringBuilder sb = new StringBuilder();
        if (create) {
            sb.append("CREATE TRIGGER ");
            sb.append(trigger.getFullyQualifiedName(DBPEvaluationContext.DDL));
            sb.append(trigger.getActive() ? "" : "\nSTATUS INACTIVE");
            if (trigger.getPriority() != 0.0) {
                sb.append("\nPRIORITY ").append(trigger.getPriority());
            }
            sb.append("\n" + trigger.getActionTime() + " ");
            if (trigger.getEvent().equals("COMMIT") || trigger.getEvent().equals("ROLLBACK")) {
                sb.append(trigger.getEvent());
            } else {
                sb.append(trigger.getEvent());
                sb.append(" ON ").append(trigger.getTable().getUniqueName());
                if (trigger.getEvent().equals("UPDATE") || trigger.getEvent().equals("UPDATE STATEMENT")) {
                    if (trigger.getTargetColumn() != null) {
                        sb.append("(" + trigger.getTargetColumn() + ")");
                    }
                }
                if (trigger.getCondition() != null) {
                    sb.append("\nIF ").append(trigger.getCondition());
                }
            }
            sb.append("\nEXECUTE ");
            if (trigger.getActionType().equals("REJECT") || trigger.getActionType().equals("INVALIDATE_TRANSACTION")) {
                sb.append(trigger.getActionType());
            } else if (trigger.getActionType().equals("PRINT")) {
                sb.append(trigger.getActionType() + " ");
                sb.append(trigger.getActionDefinition() == null ? "" : SQLUtils.quoteString(trigger, trigger.getActionDefinition()));
            }
            else {
                sb.append(trigger.getActionDefinition() == null ? "" : trigger.getActionDefinition());
            }
        } else {
            sb.append("ALTER TRIGGER ");
            sb.append(trigger.getFullyQualifiedName(DBPEvaluationContext.DDL));
            sb.append(trigger.getActive() == true ? "\nSTATUS ACTIVE": "\nSTATUS INACTIVE");
            sb.append("\nPRIORITY ").append(trigger.getPriority());
        }
        if (trigger.getDescription() != null) {
            sb.append("\nCOMMENT ").append(SQLUtils.quoteString(trigger, trigger.getDescription()));
        }
        actions.add(new SQLDatabasePersistAction("Create and Alter Trigger", sb.toString()));
    }

}
