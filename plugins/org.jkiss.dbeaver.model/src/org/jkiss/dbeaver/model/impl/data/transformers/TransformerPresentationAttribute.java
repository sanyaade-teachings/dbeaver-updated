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
package org.jkiss.dbeaver.model.impl.data.transformers;

import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;

/**
 * TransformerPresentationAttribute
 */
public class TransformerPresentationAttribute extends AbstractAttribute {

    private final DBPDataKind dataKind;

    public TransformerPresentationAttribute(
        DBDAttributeBinding attribute,
        String typeName,
        int typeId,
        DBPDataKind dataKind)
    {
        super(
            attribute.getName(),
            typeName,
            typeId,
            attribute.getOrdinalPosition(),
            attribute.getMaxLength(),
            attribute.getScale(),
            attribute.getPrecision(),
            attribute.isRequired(),
            attribute.isAutoGenerated());
        this.dataKind = dataKind;
    }

    @Override
    public DBPDataKind getDataKind() {
        return dataKind;
    }
}
