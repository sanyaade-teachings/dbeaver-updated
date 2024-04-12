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
package org.jkiss.dbeaver.dpi.model.adapters;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.dpi.model.DPIContext;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.impl.dpi.DPIServerSmartProxyDataReceiver;

import java.io.IOException;

public class SQLDataReceiverAdapter extends AbstractTypeAdapter<DBDDataReceiver> {
    private final Gson gson;

    public SQLDataReceiverAdapter(@NotNull DPIContext context, Gson gson) {
        super(context);
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter jsonWriter, DBDDataReceiver dataReceiver) throws IOException {
        jsonWriter.beginObject();

//        if (dataReceiver instanceof DPICServerSmartProxyDataReceiver dpiReceiver) {
//            jsonWriter.name("maxRows");
//            jsonWriter.value(dpiReceiver.getMaxRows());
//            jsonWriter.name("offset");
//            jsonWriter.value(dpiReceiver.getOffset());
//            jsonWriter.name("resultSet");
//            jsonWriter.value(gson.toJson(dpiReceiver.getDpiResultSet()));
//            jsonWriter.name("session");
//            jsonWriter.value(gson.toJson(dpiReceiver.getSession()));
//
//        }

        //"{}"
        jsonWriter.endObject();
    }

    @Override
    public DBDDataReceiver read(JsonReader jsonReader) throws IOException {
        var proxy = new DPIServerSmartProxyDataReceiver();
        jsonReader.beginObject();
//        while (jsonReader.peek() == JsonToken.NAME) {
//            String attrName = jsonReader.nextName();
//            switch (attrName) {
//                case "maxRows":
//                    proxy.setMaxRows(jsonReader.nextLong());
//                    break;
//                case "offset":
//                    proxy.setOffset(jsonReader.nextLong());
//                    break;
//                case "resultSet":
//                    var resultSet = gson.fromJson(jsonReader.nextString(), DPIResultSet.class);
//                    proxy.setDpiResultSet(resultSet);
//                    break;
//                case "session":
//                    var session = gson.fromJson(jsonReader.nextString(), DBCSession.class);
//                    proxy.setSession(session);
//                    break;
//            }
//        }
        jsonReader.endObject();
        return proxy;
    }
}
