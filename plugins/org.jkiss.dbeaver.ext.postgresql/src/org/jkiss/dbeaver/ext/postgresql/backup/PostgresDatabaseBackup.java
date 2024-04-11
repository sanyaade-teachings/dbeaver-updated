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
package org.jkiss.dbeaver.ext.postgresql.backup;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.InternalDatabaseConfig;
import org.jkiss.dbeaver.model.sql.backup.BackupConstant;
import org.jkiss.dbeaver.model.sql.backup.BackupDatabase;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

import static java.util.concurrent.TimeUnit.MINUTES;

public class PostgresDatabaseBackup implements BackupDatabase {

    private static final Log log = Log.getLog(PostgresDatabaseBackup.class);

    @Override
    public void doBackup(
            @NotNull Connection connection,
            int currentSchemaVersion,
            @NotNull InternalDatabaseConfig databaseConfig
    ) throws DBException {
        try {
            URI uri = new URI(databaseConfig.getUrl().replace("jdbc:", ""));
            Path workspace = DBWorkbench.getPlatform().getWorkspace().getAbsolutePath().resolve(BackupConstant.BACKUP_FOLDER);
            Path backupFile = workspace.resolve(uri.getPath().replace("/", "") + "_"
                    + BackupConstant.BACKUP_FILE_NAME + databaseConfig.getSchema()
                    + currentSchemaVersion + BackupConstant.BACKUP_FILE_TYPE);
            if (Files.notExists(backupFile)) {
                Files.createDirectories(workspace);
                ProcessBuilder processBuilder = getBuilder(databaseConfig, uri, backupFile);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                while (process.isAlive()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                int exitCode = process.exitValue();

                if (exitCode == 0) {
                    log.info("Postgres backup successful");
                } else {
                    log.error("Postgres backup failed");
                    throw new DBException("Postgres backup failed");
                }
            }
        } catch (Exception e) {
            log.error("Create backup is failed: " + e.getMessage());
            throw new DBException("Create backup is failed: " + e.getMessage());
        }
    }

    private static ProcessBuilder getBuilder(@NotNull InternalDatabaseConfig databaseConfig, URI uri, Path backupFile) {

        return new ProcessBuilder(
                "pg_dump",
                "-h", uri.getHost(),
                "-p", String.valueOf(uri.getPort()),
                "-U", databaseConfig.getUser(),
                "-W", databaseConfig.getPassword(),
                "--schema", databaseConfig.getSchema(),
                "-F", "c",
                "-b",
                "-v",
                "-f", backupFile.toAbsolutePath().toString()
        );
    }
}
