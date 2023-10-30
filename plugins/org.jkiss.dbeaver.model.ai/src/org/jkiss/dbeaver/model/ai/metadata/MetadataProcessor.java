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
package org.jkiss.dbeaver.model.ai.metadata;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionContext;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionMessage;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionScope;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class MetadataProcessor {
    public static final MetadataProcessor INSTANCE = new MetadataProcessor();
    private static final Log log = Log.getLog(MetadataProcessor.class);

    private static final boolean SUPPORTS_ATTRS = true;
    private static final int MAX_RESPONSE_TOKENS = 2000;


    public String generateObjectDescription(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject object,
        @Nullable DBCExecutionContext context,
        @NotNull IAIFormatter formatter,
        int maxRequestLength,
        boolean useFullyQualifiedName
    ) throws DBException {
        if (DBNUtils.getNodeByObject(monitor, object, false) == null) {
            // Skip hidden objects
            return "";
        }
        StringBuilder description = new StringBuilder();
        if (object instanceof DBSEntity) {
            String name = useFullyQualifiedName && context != null ? DBUtils.getObjectFullName(
                context.getDataSource(),
                object,
                DBPEvaluationContext.DDL
            ) : DBUtils.getQuotedIdentifier(object);
            description.append('\n').append(name).append("(");
            boolean firstAttr = addPromptAttributes(monitor, (DBSEntity) object, description, true);
            formatter.addExtraDescription(monitor, (DBSEntity) object, description, firstAttr);
            description.append(");");
        } else if (object instanceof DBSObjectContainer) {
            monitor.subTask("Load cache of " + object.getName());
            ((DBSObjectContainer) object).cacheStructure(
                monitor,
                DBSObjectContainer.STRUCT_ENTITIES | DBSObjectContainer.STRUCT_ATTRIBUTES);
            for (DBSObject child : ((DBSObjectContainer) object).getChildren(monitor)) {
                if (DBUtils.isSystemObject(child) || DBUtils.isHiddenObject(child) || child instanceof DBSTablePartition) {
                    continue;
                }
                String childText = generateObjectDescription(
                    monitor,
                    child,
                    context,
                    formatter,
                    maxRequestLength,
                    isRequiresFullyQualifiedName(child, context)
                );
                if (description.length() + childText.length() > maxRequestLength * 3) {
                    log.debug("Trim GPT metadata prompt  at table '" + child.getName() + "' - too long request");
                    break;
                }
                description.append(childText);
            }
        }
        return description.toString();
    }

    /**
     * Creates a new message containing completion metadata for the request
     */
    @NotNull
    public DAICompletionMessage createMetadataMessage(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionContext context,
        @Nullable DBSObjectContainer mainObject,
        @NotNull IAIFormatter formatter,
        int maxTokens
    ) throws DBException {
        if (mainObject == null || mainObject.getDataSource() == null) {
            throw new DBException("Invalid completion request");
        }

        final DBCExecutionContext executionContext = context.getExecutionContext();

        final StringBuilder sb = new StringBuilder();
        sb.append("You need to perform SQL completion using the provided information. Start your response with SELECT keyword");

        final String extraInstructions = formatter.getExtraInstructions(monitor, mainObject, executionContext);
        if (CommonUtils.isNotEmpty(extraInstructions)) {
            sb.append(", ").append(extraInstructions);
        }

        sb.append("\nDialect is: ").append(mainObject.getDataSource().getSQLDialect().getDialectName());
        sb.append("\nTables with their properties:");

        if (context.getScope() == DAICompletionScope.CUSTOM) {
            for (DBSEntity entity : context.getCustomEntities()) {
                sb.append(generateObjectDescription(
                    monitor,
                    entity,
                    executionContext,
                    formatter,
                    maxTokens,
                    isRequiresFullyQualifiedName(entity, executionContext)
                ));
            }
        } else {
            sb.append(generateObjectDescription(
                monitor,
                mainObject,
                executionContext,
                formatter,
                maxTokens,
                false
            ));
        }

        return new DAICompletionMessage(
            DAICompletionMessage.Role.SYSTEM,
            sb.toString()
        );
    }

    // @NotNull
    // private static String generatePrompt(DAICompletionRequest request, @Nullable DAICompletionSession session, int maxRequestLength) {
    //     final StringJoiner sb = new StringJoiner("\n");
    //
    //     if (session != null && !session.getMessages().isEmpty()) {
    //         final List<DAICompletionRequest> requests = session.getMessages();
    //         int capacity = maxRequestLength - request.getPromptText().length();
    //         int index = requests.size() - 1;
    //
    //         for (; index > 0; index--) {
    //             final String prompt = requests.get(index).getPromptText();
    //             final int length = prompt.length();
    //
    //             if (capacity > length) {
    //                 capacity -= length;
    //             } else {
    //                 break;
    //             }
    //         }
    //
    //         for (int i = index; i < requests.size(); i++) {
    //             sb.add(requests.get(i).getPromptText());
    //         }
    //     }
    //
    //     sb.add(request.getPromptText());
    //
    //     return sb.toString();
    // }

    protected boolean addPromptAttributes(
        DBRProgressMonitor monitor,
        DBSEntity entity,
        StringBuilder prompt,
        boolean firstAttr
    ) throws DBException {
        if (SUPPORTS_ATTRS) {
            List<? extends DBSEntityAttribute> attributes = entity.getAttributes(monitor);
            if (attributes != null) {
                for (DBSEntityAttribute attribute : attributes) {
                    if (DBUtils.isHiddenObject(attribute)) {
                        continue;
                    }
                    if (!firstAttr) prompt.append(",");
                    firstAttr = false;
                    prompt.append(attribute.getName());
                }
            }
        }
        return firstAttr;
    }

    private boolean isRequiresFullyQualifiedName(@NotNull DBSObject object, @Nullable DBCExecutionContext context) {
        if (context == null || context.getContextDefaults() == null) {
            return false;
        }
        DBSObject parent = object.getParentObject();
        DBCExecutionContextDefaults contextDefaults = context.getContextDefaults();
        return parent != null && !(parent.equals(contextDefaults.getDefaultCatalog())
            || parent.equals(contextDefaults.getDefaultSchema()));
    }

    private MetadataProcessor() {

    }
}
