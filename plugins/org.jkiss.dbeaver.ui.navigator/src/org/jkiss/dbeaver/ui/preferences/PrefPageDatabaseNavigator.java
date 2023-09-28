/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorDescriptor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorsRegistry;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * PrefPageDatabaseNavigator
 */
public class PrefPageDatabaseNavigator extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.navigator"; //$NON-NLS-1$

    private Button expandOnConnectCheck;
    private Button restoreFilterCheck;
    private Text restoreStateDepthText;
    private Button sortCaseInsensitiveCheck;
    private Button sortFoldersFirstCheck;
    private Button showConnectionHostCheck;
    private Button showObjectsDescriptionCheck;
    private Button showStatisticsCheck;
    private Button showNodeActionsCheck;
    private Button colorAllNodesCheck;

    private Button showObjectTipsCheck;
    private Button showToolTipsCheck;
    private Button showContentsInToolTipsContents;

    private Button showResourceFolderPlaceholdersCheck;
    private Button groupByDriverCheck;
    private Text longListFetchSizeText;
    private Combo dsDoubleClickBehavior;
    private Combo objDoubleClickBehavior;
    private Combo defaultEditorPageCombo;

    public PrefPageDatabaseNavigator()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    public void init(IWorkbench workbench)
    {

    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        {
            Group navigatorGroup = UIUtils.createControlGroup(composite, UINavigatorMessages.pref_page_database_general_group_navigator, 2, SWT.NONE, 0);
            ((GridData)navigatorGroup.getLayoutData()).verticalSpan = 2;

            showConnectionHostCheck = UIUtils.createCheckbox(
                navigatorGroup,
                UINavigatorMessages.pref_page_database_general_label_show_host_name,
                UINavigatorMessages.pref_page_database_general_label_show_host_name_tip,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_CONNECTION_HOST_NAME),
                2);
            showObjectsDescriptionCheck = UIUtils.createCheckbox(
                navigatorGroup,
                UINavigatorMessages.pref_page_database_general_label_show_objects_description,
                UINavigatorMessages.pref_page_database_general_label_show_objects_description_tip,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_OBJECTS_DESCRIPTION),
                2);
            showStatisticsCheck = UIUtils.createCheckbox(
                navigatorGroup,
                UINavigatorMessages.pref_page_database_general_label_show_statistics,
                UINavigatorMessages.pref_page_database_general_label_show_statistics_tip,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_STATISTICS_INFO),
                2);
            showNodeActionsCheck = UIUtils.createCheckbox(
                navigatorGroup,
                UINavigatorMessages.pref_page_database_general_label_show_node_actions,
                UINavigatorMessages.pref_page_database_general_label_show_node_actions_tip,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS),
                2);
            showResourceFolderPlaceholdersCheck = UIUtils.createCheckbox(
                navigatorGroup,
                UINavigatorMessages.pref_page_database_general_label_show_folder_placeholders,
                UINavigatorMessages.pref_page_database_general_label_show_folder_placeholders_tip,
                store.getBoolean(ModelPreferences.NAVIGATOR_SHOW_FOLDER_PLACEHOLDERS),
                2);
            sortFoldersFirstCheck = UIUtils.createCheckbox(
                navigatorGroup,
                UINavigatorMessages.pref_page_database_general_label_folders_first,
                UINavigatorMessages.pref_page_database_general_label_folders_first_tip,
                store.getBoolean(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST),
                2);
            groupByDriverCheck = UIUtils.createCheckbox(
                navigatorGroup,
                UINavigatorMessages.pref_page_database_general_label_group_database_by_driver,
                "",
                store.getBoolean(NavigatorPreferences.NAVIGATOR_GROUP_BY_DRIVER),
                2);
            // TODO: remove or enable this setting
            groupByDriverCheck.setEnabled(false);

            sortCaseInsensitiveCheck = UIUtils.createCheckbox(
                navigatorGroup,
                UINavigatorMessages.pref_page_database_general_label_order_elements_alphabetically,
                UINavigatorMessages.pref_page_database_general_label_order_elements_alphabetically_tip,
                store.getBoolean(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY),
                2);

            colorAllNodesCheck = UIUtils.createCheckbox(
                navigatorGroup,
                UINavigatorMessages.pref_page_database_general_label_color_all_nodes,
                UINavigatorMessages.pref_page_database_general_label_color_all_nodes_tip,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_COLOR_ALL_NODES),
                2);

            showObjectTipsCheck = UIUtils.createCheckbox(
                navigatorGroup,
                UINavigatorMessages.pref_page_database_general_label_show_tips_in_tree,
                UINavigatorMessages.pref_page_database_general_label_show_tips_in_tree_tip,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_OBJECT_TIPS),
                2);
            showToolTipsCheck = UIUtils.createCheckbox(
                navigatorGroup,
                UINavigatorMessages.pref_page_database_general_label_show_tooltips,
                UINavigatorMessages.pref_page_database_general_label_show_tooltips_tip,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_TOOLTIPS),
                2);
            showContentsInToolTipsContents = UIUtils.createCheckbox(
                navigatorGroup,
                UINavigatorMessages.pref_page_database_general_label_show_contents_in_tooltips,
                UINavigatorMessages.pref_page_database_general_label_show_contents_in_tooltips_tip,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_CONTENTS_IN_TOOLTIP),
                2);
        }

        {
            Group behaviorGroup = UIUtils.createControlGroup(composite, UINavigatorMessages.pref_page_database_navigator_group_behavior, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            objDoubleClickBehavior = UIUtils.createLabelCombo(behaviorGroup, UINavigatorMessages.pref_page_database_general_label_double_click_node, SWT.DROP_DOWN | SWT.READ_ONLY);
            objDoubleClickBehavior.add(UINavigatorMessages.pref_page_database_general_label_double_click_node_open_properties, 0);
            objDoubleClickBehavior.add(UINavigatorMessages.pref_page_database_general_label_double_click_node_expand_collapse, 1);

            dsDoubleClickBehavior = UIUtils.createLabelCombo(behaviorGroup, UINavigatorMessages.pref_page_database_general_label_double_click_connection, SWT.DROP_DOWN | SWT.READ_ONLY);
            dsDoubleClickBehavior.add(UINavigatorMessages.pref_page_database_general_label_double_click_connection_open_properties, NavigatorPreferences.DoubleClickBehavior.EDIT.ordinal());
            dsDoubleClickBehavior.add(UINavigatorMessages.pref_page_database_general_label_double_click_connection_conn_disconn, NavigatorPreferences.DoubleClickBehavior.CONNECT.ordinal());
            dsDoubleClickBehavior.add(UINavigatorMessages.pref_page_database_general_label_double_click_connection_open_sqleditor, NavigatorPreferences.DoubleClickBehavior.SQL_EDITOR.ordinal());
            dsDoubleClickBehavior.add(UINavigatorMessages.pref_page_database_general_label_double_click_connection_expand_collapse, NavigatorPreferences.DoubleClickBehavior.EXPAND.ordinal());
            dsDoubleClickBehavior.add(UINavigatorMessages.pref_page_database_general_label_double_click_connection_open_new_sqleditor, NavigatorPreferences.DoubleClickBehavior.SQL_EDITOR_NEW.ordinal());

            defaultEditorPageCombo = UIUtils.createLabelCombo(behaviorGroup, UINavigatorMessages.pref_page_navigator_default_editor_page_label, UINavigatorMessages.pref_page_navigator_default_editor_page_tip, SWT.DROP_DOWN | SWT.READ_ONLY);
        }

        {
            Group miscGroup = UIUtils.createControlGroup(composite, UINavigatorMessages.pref_page_database_navigator_group_misc, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            expandOnConnectCheck = UIUtils.createCheckbox(
                miscGroup,
                UINavigatorMessages.pref_page_database_general_label_expand_navigator_tree,
                UINavigatorMessages.pref_page_database_general_label_expand_navigator_tree_tip,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_EXPAND_ON_CONNECT),
                2);
            restoreFilterCheck = UIUtils.createCheckbox(
                miscGroup,
                UINavigatorMessages.pref_page_database_general_label_restore_filter,
                UINavigatorMessages.pref_page_database_general_label_restore_filter_tip,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_RESTORE_FILTER),
                2);

            longListFetchSizeText = UIUtils.createLabelText(
                miscGroup,
                UINavigatorMessages.pref_page_database_general_label_long_list_fetch_size,
                store.getString(NavigatorPreferences.NAVIGATOR_LONG_LIST_FETCH_SIZE),
                SWT.BORDER);
            longListFetchSizeText.setToolTipText(UINavigatorMessages.pref_page_database_general_label_long_list_fetch_size_tip);
            longListFetchSizeText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));

            restoreStateDepthText = UIUtils.createLabelText(
                miscGroup,
                UINavigatorMessages.pref_page_database_general_label_restore_state_depth,
                store.getString(NavigatorPreferences.NAVIGATOR_RESTORE_STATE_DEPTH),
                SWT.BORDER);
            restoreStateDepthText.setToolTipText(UINavigatorMessages.pref_page_database_general_label_restore_state_depth_tip);
            restoreStateDepthText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        }

        setValue();

        return composite;
    }

    private void setValue() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        NavigatorPreferences.DoubleClickBehavior objDCB = CommonUtils.valueOf(
            NavigatorPreferences.DoubleClickBehavior.class,
            store.getString(NavigatorPreferences.NAVIGATOR_OBJECT_DOUBLE_CLICK));
        objDoubleClickBehavior.select(objDCB == NavigatorPreferences.DoubleClickBehavior.EXPAND ? 1 : 0);
        dsDoubleClickBehavior.select(
            CommonUtils.valueOf(
                NavigatorPreferences.DoubleClickBehavior.class,
                store.getString(NavigatorPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK),
                NavigatorPreferences.DoubleClickBehavior.EDIT)
                .ordinal());

        String defEditorPage = store.getString(NavigatorPreferences.NAVIGATOR_DEFAULT_EDITOR_PAGE);
        List<EntityEditorDescriptor> entityEditors = getAvailableEditorPages();
        defaultEditorPageCombo.removeAll();
        defaultEditorPageCombo.add(UINavigatorMessages.pref_page_navigator_default_editor_page_last);
        defaultEditorPageCombo.select(0);
        for (EntityEditorDescriptor eed : entityEditors) {
            defaultEditorPageCombo.add(eed.getName());
            if (eed.getId().equals(defEditorPage)) {
                defaultEditorPageCombo.select(defaultEditorPageCombo.getItemCount() - 1);
            }
        }
    }

    @Override
    protected void performDefaults() {
        expandOnConnectCheck.setSelection(false);
        restoreFilterCheck.setSelection(false);
        restoreStateDepthText.setText("0");
        showObjectTipsCheck.setSelection(true);
        showToolTipsCheck.setSelection(true);
        showContentsInToolTipsContents.setSelection(false);
        sortCaseInsensitiveCheck.setSelection(false);
        sortFoldersFirstCheck.setSelection(true);
        showConnectionHostCheck.setSelection(true);
        showObjectsDescriptionCheck.setSelection(false);
        showStatisticsCheck.setSelection(true);
        showNodeActionsCheck.setSelection(true);
        colorAllNodesCheck.setSelection(false);
        showResourceFolderPlaceholdersCheck.setSelection(true);
        groupByDriverCheck.setSelection(false);
        longListFetchSizeText.setText("5000");
        objDoubleClickBehavior.select(NavigatorPreferences.DoubleClickBehavior.EDIT.ordinal());
        dsDoubleClickBehavior.select(NavigatorPreferences.DoubleClickBehavior.EXPAND.ordinal());
        defaultEditorPageCombo.select(0);
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        store.setValue(NavigatorPreferences.NAVIGATOR_EXPAND_ON_CONNECT, expandOnConnectCheck.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_RESTORE_FILTER, restoreFilterCheck.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_RESTORE_STATE_DEPTH, restoreStateDepthText.getText());
        store.setValue(NavigatorPreferences.NAVIGATOR_SHOW_OBJECT_TIPS, showObjectTipsCheck.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_SHOW_TOOLTIPS, showToolTipsCheck.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_SHOW_CONTENTS_IN_TOOLTIP, showContentsInToolTipsContents.getSelection());
        store.setValue(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, sortCaseInsensitiveCheck.getSelection());
        store.setValue(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, sortFoldersFirstCheck.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_SHOW_CONNECTION_HOST_NAME, showConnectionHostCheck.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_SHOW_OBJECTS_DESCRIPTION, showObjectsDescriptionCheck.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_SHOW_STATISTICS_INFO, showStatisticsCheck.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS, showNodeActionsCheck.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_COLOR_ALL_NODES, colorAllNodesCheck.getSelection());
        store.setValue(ModelPreferences.NAVIGATOR_SHOW_FOLDER_PLACEHOLDERS, showResourceFolderPlaceholdersCheck.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_GROUP_BY_DRIVER, groupByDriverCheck.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_LONG_LIST_FETCH_SIZE, longListFetchSizeText.getText());
        NavigatorPreferences.DoubleClickBehavior objDCB = NavigatorPreferences.DoubleClickBehavior.EXPAND;
        if (objDoubleClickBehavior.getSelectionIndex() == 0) {
            objDCB = NavigatorPreferences.DoubleClickBehavior.EDIT;
        }
        store.setValue(NavigatorPreferences.NAVIGATOR_OBJECT_DOUBLE_CLICK, objDCB.name());
        store.setValue(NavigatorPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK,
            CommonUtils.fromOrdinal(NavigatorPreferences.DoubleClickBehavior.class, dsDoubleClickBehavior.getSelectionIndex()).name());

        List<EntityEditorDescriptor> entityEditors = getAvailableEditorPages();
        int defEditorIndex = defaultEditorPageCombo.getSelectionIndex();
        store.setValue(NavigatorPreferences.NAVIGATOR_DEFAULT_EDITOR_PAGE, defEditorIndex <= 0 ? "" : entityEditors.get(defEditorIndex - 1).getId());

        PrefUtils.savePreferenceStore(store);

        return true;
    }

    private List<EntityEditorDescriptor> getAvailableEditorPages() {
        final EntityEditorsRegistry editorsRegistry = EntityEditorsRegistry.getInstance();
        final List<EntityEditorDescriptor> editors = new ArrayList<>(editorsRegistry.getEntityEditors());
        editors.removeIf(editor -> {
            if (editor.getType() != EntityEditorDescriptor.Type.editor) return true;
            for (AbstractDescriptor.ObjectType ot : editor.getObjectTypes()) {
                if (!DBSDataContainer.class.getName().equals(ot.getImplName()) &&
                    !DBSObjectContainer.class.getName().equals(ot.getImplName()) &&
                    !DBSEntity.class.getName().equals(ot.getImplName()) &&
                    !DBSTable.class.getName().equals(ot.getImplName()))
                {
                    return true;
                }
            }
            return false;
        });
        editors.sort(Comparator.comparing(editor -> {
            switch (editor.getPosition()) {
                case EntityEditorDescriptor.POSITION_PROPS:
                    return -2;
                case EntityEditorDescriptor.POSITION_START:
                    return -1;
                case EntityEditorDescriptor.POSITION_END:
                    return 1;
                default:
                    return 0;
            }
        }));
        editors.add(0, editorsRegistry.getDefaultEditor());
        return editors;
    }

    @Override
    public void applyData(Object data)
    {
        super.applyData(data);
    }

    @Nullable
    @Override
    public IAdaptable getElement()
    {
        return null;
    }

    @Override
    public void setElement(IAdaptable element)
    {
    }

}