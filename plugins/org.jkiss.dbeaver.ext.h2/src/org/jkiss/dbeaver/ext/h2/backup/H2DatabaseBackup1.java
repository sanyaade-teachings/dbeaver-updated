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
package org.jkiss.dbeaver.ext.h2.backup;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.backup.BackupDatabase;
import org.jkiss.dbeaver.model.sql.backup.BackupRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

public class H2DatabaseBackup1 implements BackupDatabase {
    private static final Log log = Log.getLog(H2DatabaseBackup1.class);

    public void doBackup(Connection connection, int currentSchemaVersion) {
        BackupRegistry.getInstance().getSettings();
            try {
                Path workspace = DBWorkbench.getPlatform().getWorkspace().getAbsolutePath().resolve("backup");
                Path backupFile = workspace.resolve("backupVersion" + currentSchemaVersion + ".zip");
                if (Files.notExists(backupFile)) {
                    Files.createDirectories(workspace);
                    String backupCommand = "BACKUP TO '" + backupFile + "'";
                    connection.createStatement().execute(backupCommand);

                    connection.close();

                    System.out.println("Reserve backup created to path: " + "backup");
                }
            } catch (Exception e) {
                log.error("Create backup is failed: " + e.getMessage());
            }
        }
}
