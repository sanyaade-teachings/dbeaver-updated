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
package org.jkiss.dbeaver.model.rm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

/**
 * Resource manager administration API.
 */
public interface RMAdminController {
    void saveProjectConfiguration(
        @NotNull String projectId,
        @NotNull String configurationPath,
        @NotNull String configuration
    ) throws DBException;

    @Nullable
    String readProjectConfiguration(@NotNull String projectId, @NotNull String configurationPath) throws DBException;

    /**
     * Move all project resources to a backup folder
     *
     * @return - backup id
     */
    String backupProject(@NotNull String projectId) throws DBException;

    /**
     * Copy all project resources from a backup to project folder
     */
    void restoreBackup(@NotNull String projectId, @NotNull String backupId) throws DBException;
}
