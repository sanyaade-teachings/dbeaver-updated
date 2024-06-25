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
package org.jkiss.dbeaver.ui.editors.sql.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.features.DBRFeatureRegistry;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorFeatures;
import org.osgi.framework.BundleContext;

public class SQLEditorActivator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.jkiss.dbeaver.ui.editors.sql";

    // The shared instance
    private static SQLEditorActivator plugin;
    private DBPPreferenceStore preferences;
    private SQLEditor.TransactionStatusUpdateJob transactionStatusUpdateJob;

    public SQLEditorActivator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        preferences = new BundlePreferenceStore(getBundle());

        transactionStatusUpdateJob = new SQLEditor.TransactionStatusUpdateJob();
        transactionStatusUpdateJob.schedule();

        DBRFeatureRegistry.getInstance().registerFeatures(SQLEditorFeatures.class);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (transactionStatusUpdateJob != null) {
            transactionStatusUpdateJob.cancel();
            transactionStatusUpdateJob = null;
        }

        plugin = null;
        super.stop(context);
    }

    public static SQLEditorActivator getDefault() {
        return plugin;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public DBPPreferenceStore getPreferences() {
        return preferences;
    }
}
