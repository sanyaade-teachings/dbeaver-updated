/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.dpi.app;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.impl.app.DefaultCertificateStorage;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMRegistry;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.registry.formatter.DataFormatterRegistry;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.runtime.qm.QMRegistryImpl;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * DPIPlatform
 */
public class DPIPlatform extends BasePlatformImpl implements DBPPlatformDesktop {

    public static final String PLUGIN_ID = "org.jkiss.dbeaver.model.dpi"; //$NON-NLS-1$

    private static final Log log = Log.getLog(DPIPlatform.class);

    static DPIPlatform instance;

    private static volatile boolean isClosing = false;

    private Path tempFolder;
    private DesktopWorkspaceImpl workspace;

    private static boolean disposed = false;
    private QMRegistryImpl qmController;
    private DefaultCertificateStorage defaultCertificateStorage;

    public static DPIPlatform getInstance() {
        return instance;
    }

    static DPIPlatform createInstance() {
        log.debug("Initializing " + GeneralUtils.getProductTitle());
        try {
            instance = new DPIPlatform();
            instance.initialize();
            return instance;
        } catch (Throwable e) {
            log.error("Error initializing DPI platform", e);
            throw new IllegalStateException("Error initializing DPI platform", e);
        }
    }

    public static String getCorePluginID() {
        return PLUGIN_ID;
    }

    public static boolean isStandalone() {
        return BaseApplicationImpl.getInstance().isStandalone();
    }

    public static boolean isClosing() {
        return isClosing;
    }

    private static void setClosing(boolean closing) {
        isClosing = closing;
    }

    private DPIPlatform() {
    }

    protected void initialize() {
        long startTime = System.currentTimeMillis();
        log.debug("Initialize DPI Platform...");

        try {
            Path installPath = RuntimeUtils.getLocalPathFromURL(Platform.getInstallLocation().getURL());

            this.tempFolder = installPath.resolve("temp");
            this.defaultCertificateStorage = new DefaultCertificateStorage(installPath.resolve("security"));
        } catch (IOException e) {
            log.debug(e);
        }

/*
        // Register properties adapter
        this.workspace = new DesktopWorkspaceImpl(this, ResourcesPlugin.getWorkspace());
        this.workspace.initializeProjects();
*/

        QMUtils.initApplication(this);
        this.qmController = new QMRegistryImpl();

        //super.initialize();

        log.debug("DPI Platform initialized (" + (System.currentTimeMillis() - startTime) + "ms)");
    }

    public synchronized void dispose() {
        log.debug("Shutdown DPI...");
        DPIPlatform.setClosing(true);
        super.dispose();
        workspace.dispose();

        DataSourceProviderRegistry.getInstance().dispose();
        // Remove temp folder
        if (tempFolder != null) {
            if (!ContentUtils.deleteFileRecursive(tempFolder)) {
                log.warn("Can't delete temp folder '" + tempFolder + "'");
            }
            tempFolder = null;
        }

        DPIPlatform.instance = null;
        DPIPlatform.disposed = true;
    }

    @NotNull
    @Override
    public DBPWorkspaceDesktop getWorkspace() {
        return workspace;
    }

    @NotNull
    @Override
    public DBPPlatformLanguage getLanguage() {
        return PlatformLanguageRegistry.getInstance().getLanguage(Locale.ENGLISH);
    }

    @NotNull
    @Override
    public DBPApplication getApplication() {
        return BaseApplicationImpl.getInstance();
    }

    @NotNull
    public QMRegistry getQueryManager() {
        return qmController;
    }

    @Override
    public DBPGlobalEventManager getGlobalEventManager() {
        return GlobalEventManagerImpl.getInstance();
    }

    @NotNull
    @Override
    public DBPDataFormatterRegistry getDataFormatterRegistry() {
        return DataFormatterRegistry.getInstance();
    }

    @NotNull
    @Override
    public DBPPreferenceStore getPreferenceStore() {
        return new BundlePreferenceStore(PLUGIN_ID);
    }

    @NotNull
    @Override
    public DBACertificateStorage getCertificateStorage() {
        return defaultCertificateStorage;
    }

    @NotNull
    @Override
    public DBPExternalFileManager getExternalFileManager() {
        return workspace;
    }

    @NotNull
    public Path getTempFolder(DBRProgressMonitor monitor, String name) {
        return tempFolder.resolve(name);
    }

    @Override
    protected Plugin getProductPlugin() {
        return null;//Platform.getBundle(PLUGIN_ID);
    }

    @Override
    public boolean isShuttingDown() {
        return isClosing();
    }

}
