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
package org.jkiss.dbeaver.model.websocket.event;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public class WSProjectUpdateEvent extends WSAbstractProjectEvent {

    public WSProjectUpdateEvent(
        @NotNull WSEventType eventType,
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull String projectId
    ) {
        super(eventType, sessionId, userId, projectId);
    }

    public static WSProjectUpdateEvent create(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull String projectId
    ) {
        return new WSProjectUpdateEvent(
            WSEventType.RM_PROJECT_CREATED,
            sessionId,
            userId,
            projectId
        );
    }

    public static WSProjectUpdateEvent delete(
        @Nullable String sessionId,
        @Nullable String userId,
        @NotNull String projectId
    ) {
        return new WSProjectUpdateEvent(
            WSEventType.RM_PROJECT_REMOVED,
            sessionId,
            userId,
            projectId
        );
    }
}
