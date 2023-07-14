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
package org.jkiss.dbeaver.model.dpi.api;

import org.jkiss.dbeaver.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DPI context
 */
public class DPIContext {
    private static final Log log = Log.getLog(DPIContext.class);

    private final Map<String, Object> objectIdCache = new HashMap<>();
    private final Map<Object, String> objectValueCache = new HashMap<>();
    private final AtomicLong objectCount = new AtomicLong();

    private final Object rootObject;
    private DPIController dpiController;

    public DPIContext(Object rootObject) {
        this.rootObject = rootObject;
    }

    public DPIController getDpiController() {
        return dpiController;
    }

    public void setController(DPIController dpiController) {
        this.dpiController = dpiController;
    }

    public Object getObject(String id) {
        return objectIdCache.get(id);
    }

    public void addObject(String id, Object object) {
        objectIdCache.put(id, object);
        objectValueCache.put(object, id);
    }

    public void pruneObject(String id) {
        Object removed = objectIdCache.remove(id);
        if (removed != null) {
            String removedId = objectValueCache.remove(removed);
            if (removedId == null) {
                log.warn("Pruned DPI object value wasn't found in value cache");
            } else if (!removedId.equals(id)) {
                log.warn("Pruned DPI object ID doesn't match (" + removedId + "<>" + id);
            }
        } else {
            log.warn("Cannot find object '" + id + "' for prune");
        }
    }

    public String getOrCreateObjectId(Object object) {
        String id = objectValueCache.get(object);
        if (id == null) {
            id = String.valueOf(objectCount.incrementAndGet());
            addObject(id, object);
        }
        return id;
    }

    public String getObjectId(Object object) {
        return objectValueCache.get(object);
    }

    public boolean hasObject(Object object) {
        return objectValueCache.containsKey(object);
    }

    public Object getRootObject() {
        return rootObject;
    }

}
