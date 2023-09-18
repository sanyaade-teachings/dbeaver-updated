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
package org.jkiss.dbeaver.erd.ui.notations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

public class ERDNotationRegistry {

    private Log log = Log.getLog(ERDNotationRegistry.class);
    private Map<String, ERDNotationDescriptor> notations = new LinkedHashMap<>();
    private ERDNotationDescriptor defaultNotation;
    private static ERDNotationRegistry instance;
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.erd.ui.notation.style";

    private ERDNotationRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] cfgElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        Stream.of(cfgElements).forEach(c -> {
            try {
                addNotation(new ERDNotationDescriptor(c));
            } catch (CoreException e) {
                log.error(e.getStatus());
            }
        });

    }

    public static synchronized ERDNotationRegistry getInstance() {
        if (instance == null) {
            instance = new ERDNotationRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public List<ERDNotationDescriptor> getERDNotations() {
        return notations.values().stream().collect(Collectors.toList());
    }

    public void addNotation(ERDNotationDescriptor descriptor) {
        if (notations.containsKey(descriptor.getId())) {
            log.error("ER Diagram Notation already defined for id:" + descriptor.getId());
            return;
        }
        notations.put(descriptor.getId(), descriptor);
        if (descriptor.isDefault()) {
            if (defaultNotation == null) {
                defaultNotation = descriptor;
            } else {
                log.error("The default ERD Notation already defined for id:" + defaultNotation.getId());
            }
        }
    }

    public ERDNotationDescriptor getNotation(String id) {
        if (!notations.containsKey(id)) {
            log.error("ERD Notation not defined for key:" + id);
            return null;
        }
        return notations.get(id);

    }

    public ERDNotationDescriptor getDefaultNotation() {
        return this.defaultNotation;
    }

    public Optional<ERDNotationDescriptor> getERDNotationByName(String text) {
        return notations.values().stream().filter(p->p.getName().equals(text)).findFirst();
    }
}
