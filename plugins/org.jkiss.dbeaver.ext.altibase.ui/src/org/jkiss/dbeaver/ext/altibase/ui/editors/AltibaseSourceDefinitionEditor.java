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
package org.jkiss.dbeaver.ext.altibase.ui.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseSourceObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

public class AltibaseSourceDefinitionEditor extends SQLSourceViewer<AltibaseSourceObject> {

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
        return ((DBPScriptObjectExt) getSourceObject()).getExtendedDefinitionText(monitor);
    }

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText) {
        getInputPropertySource().setPropertyValue(
                monitor,
                AltibaseConstants.PROP_OBJECT_BODY_DEFINITION,
                sourceText);
    }

    @Override
    protected boolean isReadOnly() {
        return false;
    }
}
