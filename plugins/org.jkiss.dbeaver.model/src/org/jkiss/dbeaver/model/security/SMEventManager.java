/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.security;

import org.jkiss.dbeaver.DBException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Security manager event manager
 */
public class SMEventManager {
    public enum SMEvent {
        BEFORE_USER_ACTIVATED
    }

    private static final List<SMEventListener> listeners = new CopyOnWriteArrayList<>();

    public static synchronized void addEventListener(SMEventListener listener) {
        listeners.add(listener);
    }

    public static synchronized void removeEventListener(SMEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Fire security manager event
     */
    public static void fireEvent(SMEvent event) throws DBException {
        switch (event) {
            case BEFORE_USER_ACTIVATED:
                for (SMEventListener listener : listeners) {
                    listener.beforeUserActivated();
                }
                break;
            default:
                throw new DBException("Unknown sm event type:" + event);
        }
    }
}
