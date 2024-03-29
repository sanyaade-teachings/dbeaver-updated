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
package org.jkiss.dbeaver.ui.controls.resultset.panel.valueviewer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPanel;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.SpreadsheetPresentation;

/**
 * ValueViewCommandHandler
 */
public class ValueViewCommandHandler extends AbstractHandler {

    public static final String CMD_SAVE_VALUE = "org.jkiss.dbeaver.core.resultset.cell.save";

    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ResultSetViewer rsv = (ResultSetViewer) ResultSetHandlerMain.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (rsv == null) {
            return null;
        }
        String actionId = event.getCommand().getId();
        IResultSetPanel resultPanel = rsv.getVisiblePanel();
        if (resultPanel instanceof ValueViewerPanel valuePanel) {
            switch (actionId) {
                case ITextEditorActionDefinitionIds.SMART_ENTER:
                //case CoreCommands.CMD_EXECUTE_STATEMENT:
                case CMD_SAVE_VALUE:
                    // 1 close active inline editor
                    IResultSetPresentation activePresentation = rsv.getActivePresentation();
                    if (activePresentation instanceof SpreadsheetPresentation spsPresentation) {
                        spsPresentation.closeInlineEditor();
                    }
                    // 2 save value from panel
                    valuePanel.saveValue();
                    // 3 refresh panel to show actual
                    valuePanel.refresh(true);
                    break;
            }
        }


        return null;
    }

}