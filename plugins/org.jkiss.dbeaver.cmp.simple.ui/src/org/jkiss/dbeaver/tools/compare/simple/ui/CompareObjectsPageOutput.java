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
package org.jkiss.dbeaver.tools.compare.simple.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.tools.compare.simple.CompareObjectsSettings;
import org.jkiss.dbeaver.tools.compare.simple.ui.internal.CompareUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

class CompareObjectsPageOutput extends ActiveWizardPage<CompareObjectsWizard> {

    private Button showOnlyDifference;
    private Combo reportTypeCombo;
    private Text outputFolderText;
    private Button useExternalTool;
    private Text externalToolPath;

    CompareObjectsPageOutput() {
        super(CompareUIMessages.compare_objects_page_settings_page);
        setTitle(CompareUIMessages.compare_objects_page_settings_title);
        setDescription(CompareUIMessages.compare_objects_page_settings_configuration_output_report);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final CompareObjectsSettings settings = getWizard().getSettings();

        {
            Group reportSettings = new Group(composite, SWT.NONE);
            reportSettings.setText(CompareUIMessages.compare_objects_page_report_settings);
            gl = new GridLayout(1, false);
            reportSettings.setLayout(gl);
            reportSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            showOnlyDifference = UIUtils.createCheckbox(reportSettings,
                CompareUIMessages.compare_objects_page_checkbox_show_only_differences,
                settings.isShowOnlyDifferences());
            showOnlyDifference.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    settings.setShowOnlyDifferences(showOnlyDifference.getSelection());
                }
            });
        }

        {
            Group outputSettings = new Group(composite, SWT.NONE);
            outputSettings.setText(CompareUIMessages.compare_objects_page_settings_configuration_output);
            gl = new GridLayout(2, false);
            outputSettings.setLayout(gl);
            outputSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(outputSettings,
                CompareUIMessages.compare_objects_page_settings_configuration_output_type);
            reportTypeCombo = new Combo(outputSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
            for (CompareObjectsSettings.OutputType outputType : CompareObjectsSettings.OutputType.values()) {
                reportTypeCombo.add(outputType.getTitle());
            }
            reportTypeCombo.select(settings.getOutputType().ordinal());
            reportTypeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    for (CompareObjectsSettings.OutputType outputType : CompareObjectsSettings.OutputType.values()) {
                        if (outputType.ordinal() == reportTypeCombo.getSelectionIndex()) {
                            settings.setOutputType(outputType);
                            UIUtils.enableWithChildren(outputFolderText.getParent(), outputType == CompareObjectsSettings.OutputType.FILE);
                            break;
                        }
                    }
                }
            });

            outputFolderText = DialogUtils.createOutputFolderChooser(outputSettings, null, null, false, null);
            outputFolderText.setText(settings.getOutputFolder());
            UIUtils.enableWithChildren(outputFolderText.getParent(), settings.getOutputType() == CompareObjectsSettings.OutputType.FILE);
            outputFolderText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    settings.setOutputFolder(outputFolderText.getText());
                }
            });
            Group additionalSettings = new Group(composite, SWT.NONE);
            additionalSettings.setText(CompareUIMessages.compare_objects_page_additional_settings);
            gl = new GridLayout(1, false);
            additionalSettings.setLayout(gl);
            additionalSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            useExternalTool = UIUtils.createCheckbox(additionalSettings, CompareUIMessages.compare_objects_page_settings_checkbox_external_tool, settings.isUseExternalTool());
            useExternalTool.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    settings.setUseExternalTool(useExternalTool.getSelection());
                }
            });
            
            UIUtils.createControlLabel(additionalSettings, CompareUIMessages.compare_objects_page_settings_configuration_compare_tool);
            externalToolPath = new Text(additionalSettings, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            gd.grabExcessHorizontalSpace = true;
            gd.widthHint = 355;
            externalToolPath.setLayoutData(gd);
            externalToolPath.setText(settings.getExternalToolPath());
            externalToolPath.addModifyListener(new ModifyListener() {                
                @Override
                public void modifyText(ModifyEvent e) {
                    settings.setExternalToolPath(externalToolPath.getText());                    
                }
            });
        }

        setControl(composite);
    }

    @Override
    public void activatePage() {
        updatePageCompletion();
    }

    @Override
    public void deactivatePage()
    {
        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        return true;
    }
}