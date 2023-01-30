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

package org.jkiss.dbeaver.model.ai.translator;

import java.time.LocalDate;

/**
 * Natural language translator item
 */
public class DAIHistoryItem {

    private String naturalText;
    private String completionText;
    private LocalDate time;

    public DAIHistoryItem() {
    }

    public DAIHistoryItem(String naturalText, String completionText) {
        this.naturalText = naturalText;
        this.completionText = completionText;
    }

    public String getNaturalText() {
        return naturalText;
    }

    public void setNaturalText(String naturalText) {
        this.naturalText = naturalText;
    }

    public String getCompletionText() {
        return completionText;
    }

    public void setCompletionText(String completionText) {
        this.completionText = completionText;
    }

    public LocalDate getTime() {
        return time;
    }

    public void setTime(LocalDate time) {
        this.time = time;
    }
}
