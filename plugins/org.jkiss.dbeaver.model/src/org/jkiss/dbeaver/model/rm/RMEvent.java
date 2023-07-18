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

import java.util.List;
import java.util.Map;

public class RMEvent {

    public enum Action {
        RESOURCE_DELETE,
        RESOURCE_ADD,
        PROJECT_DELETE,
        PROJECT_ADD
    }

    @NotNull
    private final Action action;
    @NotNull
    private final RMProject project;
    @Nullable
    private final String resourcePath;
    @NotNull
    private final Map<String, Object> parameters;

    public RMEvent(@NotNull Action action, @NotNull RMProject project, @Nullable String resourcePath, @NotNull Map<String, Object> parameters) {
        this.action = action;
        this.project = project;
        this.resourcePath = resourcePath;
        this.parameters = parameters;
    }

    public RMEvent(@NotNull Action action, @NotNull RMProject project) {
        this(action, project, null, Map.of());
    }

    public RMEvent(@NotNull Action action, @NotNull RMProject project, @NotNull String resourcePath) {
        this(action, project, resourcePath, Map.of());
    }

    @NotNull
    public Action getAction() {
        return action;
    }

    @NotNull
    public RMProject getProject() {
        return project;
    }

    @Nullable
    public String getResourcePath() {
        return resourcePath;
    }

    public Map<String, Object> getParameters() {
        return Map.copyOf(parameters);
    }
}
