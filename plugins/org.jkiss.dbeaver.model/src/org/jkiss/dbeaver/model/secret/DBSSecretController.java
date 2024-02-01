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

package org.jkiss.dbeaver.model.secret;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.auth.SMSessionSecretKeeper;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;

/**
 * Secret manager API
 */
public interface DBSSecretController {

    @Nullable
    String getPrivateSecretValue(@NotNull String secretId) throws DBException;

    void setPrivateSecretValue(@NotNull String secretId, @Nullable String secretValue) throws DBException;

    @NotNull
    default List<DBSSecretValue> discoverCurrentUserSecrets(
        @NotNull DBSSecretObject secretObject
    ) throws DBException {
        return List.of();
    }

    @NotNull
    default List<DBSSecretValue> listAllSharedSecrets(
        @NotNull DBSSecretObject secretObject
    ) throws DBException {
        return List.of();
    }

    default void setSubjectSecretValue(
        @NotNull String subjectId,
        @NotNull DBSSecretObject secretObject,
        @NotNull DBSSecretValue secretValue
    ) throws DBException {
    }

    default void deleteSubjectSecrets(@NotNull String subjectId) throws DBException {
    }

    default void deleteObjectSecrets(
        @NotNull DBSSecretObject secretObject
    ) throws DBException {
    }

    /**
     * Syncs any changes with file system/server
     */
    void flushChanges() throws DBException;

    @NotNull
    static DBSSecretController getProjectSecretController(DBPProject project) throws DBException {
        return getSessionSecretController(project.getWorkspaceSession());
    }

    @NotNull
    static DBSSecretController getGlobalSecretController() throws DBException {
        return getSessionSecretController(DBWorkbench.getPlatform().getWorkspace().getWorkspaceSession());
    }

    @NotNull
    static DBSSecretController getSessionSecretController(SMSession spaceSession) throws DBException {
        SMSessionSecretKeeper secretKeeper = DBUtils.getAdapter(SMSessionSecretKeeper.class, spaceSession);
        if (secretKeeper != null) {
            DBSSecretController secretController = secretKeeper.getSecretController();
            if (secretController != null) {
                return secretController;
            }
        }
        throw new IllegalStateException("Session secret controller not found");
    }

}
