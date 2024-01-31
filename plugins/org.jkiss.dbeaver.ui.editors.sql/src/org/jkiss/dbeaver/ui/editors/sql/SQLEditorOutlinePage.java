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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerToggleOutlineView;
import org.jkiss.dbeaver.ui.editors.sql.semantics.*;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLDocumentSyntaxContext.SQLDocumentSyntaxContextListener;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDummyDataSourceContext.DummyTableRowsSource;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.*;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryRowsCteModel.SQLQueryRowsCteSubqueryModel;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQuerySelectionResultModel.ColumnSpec;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQuerySelectionResultModel.CompleteTupleSpec;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQuerySelectionResultModel.TupleSpec;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class SQLEditorOutlinePage extends ContentOutlinePage implements IContentOutlinePage {

    private static final int SQL_QUERY_ORIGINAL_TEXT_PREVIEW_LENGTH = 100;
    private final SQLEditorBase editor;
    private TreeViewer treeViewer;
    private OutlineScriptNode scriptNode;
    private List<OutlineNode> rootNodes;
    private SelectionSyncOperation currentSelectionSyncOp = SelectionSyncOperation.NONE;
    private SQLOutlineNodeBuilder currentNodeBuilder = new SQLOutlineNodeFullBuilder();

    private final CaretListener caretListener = event -> {
        if (currentSelectionSyncOp == SelectionSyncOperation.NONE) {
            LinkedList<OutlineNode> path = new LinkedList<>();
            int offset = event.caretOffset;
            OutlineNode node = scriptNode;
            OutlineNode nextNode = scriptNode;
            path.add(node);
            while (nextNode != null) {
                node = nextNode;
                nextNode = null;
                for (int i = 0; i < node.getChildrenCount(); i++) {
                    OutlineNode child = node.getChild(i);
                    IRegion range = child.getTextRange();
                    if (range != null && range.getOffset() <= offset) {
                        nextNode = child;
                        path.add(child);
                    } else {
                        break;
                    }
                }
            }

            if (node != null) {
                currentSelectionSyncOp = SelectionSyncOperation.FROM_EDITOR;
                treeViewer.getTree().setRedraw(false);
                treeViewer.reveal(new TreePath(path.toArray(OutlineNode[]::new)));
                treeViewer.setSelection(new StructuredSelection(node), true);
                treeViewer.getTree().setRedraw(true);
                currentSelectionSyncOp = SelectionSyncOperation.NONE;
            }
        }
    };

    private final ITextInputListener textInputListener = new ITextInputListener() {

        @Override
        public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
            if (newInput != null) {
                treeViewer.setInput(editor.getEditorInput());
            }
        }

        @Override
        public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
        }
    };

    public SQLEditorOutlinePage(@NotNull SQLEditorBase editor) {
        this.editor = editor;
        this.rootNodes = List.of(this.scriptNode = new OutlineScriptNode());
    }

    @NotNull
    private SQLOutlineNodeBuilder getNodeBuilder() {
        return this.currentNodeBuilder;
    }

    @Override
    protected int getTreeStyle() {
        return super.getTreeStyle() | SWT.FULL_SELECTION | SWT.VIRTUAL;
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        super.createControl(parent);
        this.treeViewer = super.getTreeViewer();
        this.treeViewer.setContentProvider(new ILazyTreeContentProvider() {
            @Override
            public void updateElement(@NotNull Object parent, int index) {
                if (parent == editor.getEditorInput()) {
                    this.updateChildNode(parent, index, rootNodes.get(index));
                } else if (parent instanceof OutlineNode node) {
                    this.updateChildNode(parent, index, node.getChild(index));
                } else {
                    throw new IllegalStateException(); // should never happen
                }
            }

            private void updateChildNode(@NotNull Object parent, int index, OutlineNode childNode) {
                treeViewer.replace(parent, index, childNode);
                treeViewer.setChildCount(childNode, childNode.getChildrenCount());
            }

            @Override
            public void updateChildCount(@NotNull Object element, int currentChildCount) {
                if (element == editor.getEditorInput()) {
                    treeViewer.setChildCount(element, rootNodes.size());
                } else if (element instanceof OutlineNode node) {
                    treeViewer.setChildCount(element, node.getChildrenCount());
                } else {
                    throw new IllegalStateException(); // should never happen
                }
            }

            @Nullable
            @Override
            public Object getParent(@NotNull Object element) {
                return element instanceof OutlineNode node ? node.getParent() : null;
            }
        });

        this.treeViewer.setUseHashlookup(true);
        this.treeViewer.setLabelProvider(new DecoratingStyledCellLabelProvider(new SQLOutlineLabelProvider(), null, null));
        this.treeViewer.setInput(editor.getEditorInput());
        this.treeViewer.setAutoExpandLevel(3);

        TextViewer textViewer = this.editor.getTextViewer();
        if (textViewer != null) {
            textViewer.getTextWidget().addCaretListener(this.caretListener);
            textViewer.addTextInputListener(this.textInputListener);
        }

        SQLEditorHandlerToggleOutlineView.refreshCommandState(editor.getSite());
        scheduleRefresh();
    }

    @Override
    public void selectionChanged(@NotNull SelectionChangedEvent event) {
        if (currentSelectionSyncOp == SelectionSyncOperation.NONE) {
            if (event.getSelection() instanceof IStructuredSelection selection && selection.getFirstElement() instanceof OutlineNode node) {
                IRegion textRange = node.getTextRange();
                if (textRange != null) {
                    currentSelectionSyncOp = SelectionSyncOperation.TO_EDITOR;
                    this.editor.selectAndReveal(textRange.getOffset(), textRange.getLength());
                    currentSelectionSyncOp = SelectionSyncOperation.NONE;
                }
            }
        }

        super.selectionChanged(event);
    }

    private void scheduleRefresh() {
        UIUtils.asyncExec(() -> {
            if (!treeViewer.getTree().isDisposed()) {
                treeViewer.refresh();
            }
        });
    }

    @NotNull
    private String prepareQueryPreview(@NotNull SQLDocumentScriptItemSyntaxContext scriptElement) {
        return this.prepareQueryPreview(scriptElement.getOriginalText());
    }

    @NotNull
    private String prepareQueryPreview(@NotNull String queryOriginalText) {
        int queryOriginalLength = queryOriginalText.length();
        int queryPreviewLength = Math.min(queryOriginalLength, SQL_QUERY_ORIGINAL_TEXT_PREVIEW_LENGTH);
        StringBuilder result = new StringBuilder(queryPreviewLength);
        try (Scanner scanner = new Scanner(queryOriginalText)) {
            while (scanner.hasNextLine() && result.length() < queryPreviewLength) {
                String line = scanner.nextLine().trim();
                int fragmentLength = Math.min(line.length(), queryPreviewLength - result.length());
                result.append(line, 0, fragmentLength).append(" ");
            }
        }
        if (result.length() < queryOriginalLength) {
            result.append(" ...");
        }
        return result.toString();
    }

    @Override
    public void dispose() {
        TextViewer textViewer = this.editor.getTextViewer();
        if (textViewer != null) {
            textViewer.getTextWidget().removeCaretListener(this.caretListener);
        }
        
        this.scriptNode.dispose();
        super.dispose();
        
        SQLEditorHandlerToggleOutlineView.refreshCommandState(editor.getSite());
    }

    private enum SelectionSyncOperation {
        NONE, FROM_EDITOR, TO_EDITOR
    }

    private interface SQLOutlineNodeBuilder extends SQLQueryNodeModelVisitor<OutlineQueryNode, Object> {
    }

    private static class SQLOutlineLabelProvider implements ILabelProvider, IFontProvider, IStyledLabelProvider {
        private final Styler extraTextStyler = StyledString.createColorRegistryStyler(JFacePreferences.DECORATIONS_COLOR, null);

        @Override
        public void addListener(@Nullable ILabelProviderListener listener) {
            // no listeners
        }

        @Override
        public boolean isLabelProperty(@Nullable Object element, @Nullable String property) {
            return false;
        }

        @Override
        public void removeListener(@Nullable ILabelProviderListener listener) {
            // no listeners
        }

        @Nullable
        @Override
        public Font getFont(@Nullable Object element) {
            // default font
            return null;
        }

        @Nullable
        @Override
        public Image getImage(@NotNull Object element) {
            return element instanceof OutlineNode node ? node.getImage() : DBeaverIcons.getImage(DBIcon.TYPE_UNKNOWN, false);
        }

        @NotNull
        @Override
        public String getText(@NotNull Object element) {
            return element instanceof OutlineNode node ? node.getText() : element.toString();
        }

        @NotNull
        @Override
        public StyledString getStyledText(@NotNull Object element) {
            StyledString result = new StyledString();
            if (element instanceof OutlineNode node) {
                String text = node.getText();
                result.append(text);
                
                String extra = node.getExtraText(); 
                if (extra != null) {
                    result.append(extra);
                    result.setStyle(text.length(), extra.length(), extraTextStyler);
                }
            } else {
                result.append(element.toString());
            }
            return result;
        }

        @Override
        public void dispose() {
        }
    }

    private abstract class OutlineNode {

        private OutlineNode parentNode;

        public OutlineNode(@Nullable OutlineNode parentNode) {
            this.parentNode = parentNode;
        }

        public abstract IRegion getTextRange();

        @Nullable
        public final OutlineNode getParent() {
            return this.parentNode;
        }

        public abstract String getText();

        @Nullable
        public String getExtraText() {
            return null;
        }

        @Nullable
        public Image getImage() {
            DBPImage icon = this.getIcon();
            return icon == null ? null : DBeaverIcons.getImage(icon, false);
        }

        protected abstract DBPImage getIcon();

        public abstract int getChildrenCount();

        public abstract OutlineNode getChild(int index);
    }

    private class OutlineInfoNode extends OutlineNode {
        private final String title;
        private final DBIcon icon;

        public OutlineInfoNode(@NotNull OutlineNode parentNode, @NotNull String title, @NotNull DBIcon icon) {
            super(parentNode);
            this.title = title;
            this.icon = icon;
        }

        @NotNull
        @Override
        public String getText() {
            return this.title;
        }

        @NotNull
        @Override
        public DBIcon getIcon() {
            return this.icon;
        }

        @Nullable
        @Override
        public OutlineNode getChild(int index) {
            return null;
        }

        @Override
        public int getChildrenCount() {
            return 0;
        }

        @Nullable
        @Override
        public IRegion getTextRange() {
            return null;
        }
    }

    private class OutlineScriptNode extends OutlineNode {
        final SQLDocumentSyntaxContext documentContext;

        OutlineNode noElementsNode = new OutlineInfoNode(this, "No elements detected", DBIcon.SMALL_INFO);

        Map<SQLDocumentScriptItemSyntaxContext, OutlineNode> elements = new HashMap<>();
        List<OutlineNode> children = Collections.emptyList();
        final SQLDocumentSyntaxContextListener syntaxContextListener = new SQLDocumentSyntaxContextListener() {
            @Override
            public void onScriptItemInvalidated(@Nullable SQLDocumentScriptItemSyntaxContext item) {
                UIUtils.syncExec(() -> {
                    updateElements();
                    updateChildren();
                    scheduleRefresh();
                });
            }

            @Override
            public void onScriptItemIntroduced(@Nullable SQLDocumentScriptItemSyntaxContext item) {
                UIUtils.syncExec(() -> {
                    updateElements();
                    updateChildren();
                    scheduleRefresh();
                });
            }

            @Override
            public void onAllScriptItemsInvalidated() {
                UIUtils.syncExec(() -> {
                    updateElements();
                    updateChildren();
                    scheduleRefresh();
                });
            }
        };

        public OutlineScriptNode() {
            super(null);
            this.documentContext = editor.getSyntaxContext();
            this.updateElements();
            this.updateChildren();
            this.documentContext.addListener(syntaxContextListener);
        }

        @NotNull
        @Override
        public IRegion getTextRange() {
            return new Region(0, 0);
        }

        private void updateElements() {
            this.elements = documentContext.getScriptItems().stream()
                .collect(Collectors.toMap(
                    e -> e.item,
                    e -> new OutlineScriptElementNode(this, e.offset, e.item),
                    (a, b) -> a, LinkedHashMap::new)
                );
        }

        private void updateChildren() {
            if (this.elements.isEmpty()) {
                this.children = List.of(noElementsNode);
            } else {
                this.children = List.copyOf(this.elements.values());
            }
        }

        @Override
        public int getChildrenCount() {
            return this.children.size();
        }

        @NotNull
        @Override
        public OutlineNode getChild(int index) {
            return this.children.get(index);
        }

        @NotNull
        @Override
        public String getText() {
            return editor.getTitle();
        }

        @NotNull
        @Override
        public Image getImage() {
            Image image = editor.getTitleImage();
            return new Image(image.getDevice(), image, SWT.IMAGE_COPY);
        }

        @NotNull
        @Override
        protected DBIcon getIcon() {
            return DBIcon.TREE_SCRIPT;
        }

        public void dispose() {
            this.documentContext.removeListener(syntaxContextListener);
        }
    }

    private class OutlineScriptElementNode extends OutlineQueryNode {
        private final int offset;
        private final SQLDocumentScriptItemSyntaxContext scriptElement;

        public OutlineScriptElementNode(
            @NotNull OutlineScriptNode parent,
            int offset,
            @NotNull SQLDocumentScriptItemSyntaxContext scriptElement
        ) {
            super(
                parent,
                scriptElement.getQueryModel(),
                prepareQueryPreview(scriptElement),
                null,
                UIIcon.SQL_EXECUTE,
                scriptElement.getQueryModel()
            );
            this.offset = offset;
            this.scriptElement = scriptElement;
        }

        public int getOffset() {
            return this.offset;
        }

        @NotNull
        @Override
        public IRegion getTextRange() {
            return new Region(this.offset, 0);
        }
    }

    private class OutlineQueryNode extends OutlineNode {
        private final SQLQueryNodeModel model;
        private final String text;
        private final String extraText;
        private final DBPImage icon;
        private final SQLQueryNodeModel[] childModels;
        private final IRegion textRange;
        private List<OutlineNode> children;

        public OutlineQueryNode(
            @NotNull OutlineNode parentNode,
            @NotNull SQLQueryNodeModel model,
            @NotNull String text,
            @Nullable String extraText,
            @NotNull DBPImage icon,
            @NotNull SQLQueryNodeModel... childModels
        ) {
            super(parentNode);
            this.model = model;
            this.text = text;
            this.extraText = extraText;
            this.icon = icon;
            this.childModels = childModels;

            int offset = parentNode instanceof OutlineScriptElementNode element ?
                element.getOffset() + this.model.getInterval().a :
                parentNode instanceof OutlineQueryNode queryNode ?
                    queryNode.getTextRange().getOffset() - queryNode.model.getInterval().a + this.model.getInterval().a :
                    parentNode.getTextRange().getOffset();

            this.textRange = new Region(offset, this.model.getInterval().length());
            this.children = null;
        }

        @NotNull
        @Override
        public IRegion getTextRange() {
            return this.textRange;
        }

        @NotNull
        private List<OutlineNode> obtainChildren() {
            if (this.children == null) {
                if (this.childModels.length == 0) {
                    this.children = Collections.emptyList();
                } else {
                    this.children = new ArrayList<>(childModels.length);
                    for (SQLQueryNodeModel childModel : childModels) {
                        if (childModel != null) {
                            childModel.apply(getNodeBuilder(), this);
                        }
                    }
                }
            }
            return this.children;
        }

        @NotNull
        @Override
        public String getText() {
            return this.text;
        }

        @Nullable
        @Override
        public String getExtraText() {
            return this.extraText;
        }
        
        @NotNull
        @Override
        protected DBPImage getIcon() {
            return this.icon;
        }

        @Override
        public int getChildrenCount() {
            return this.obtainChildren().size();
        }

        @NotNull
        @Override
        public OutlineNode getChild(int index) {
            return this.obtainChildren().get(index);
        }
    }

    private class SQLOutlineNodeFullBuilder implements SQLOutlineNodeBuilder {

        @Nullable
        @Override
        public Object visitRowsCte(@NotNull SQLQueryRowsCteModel cte, @NotNull OutlineQueryNode node) {
            this.makeNode(node, cte, "CTE", DBIcon.TREE_FOLDER_LINK, cte.getAllQueries().toArray(SQLQueryRowsSourceModel[]::new));
            return null;
        }

        @Nullable
        public Object visitRowsCteSubquery(@NotNull SQLQueryRowsCteSubqueryModel cteSubquery, @NotNull OutlineQueryNode node) {
            this.makeNode(node, cteSubquery, cteSubquery.subqueryName.getRawName(), DBIcon.TREE_TABLE_LINK, cteSubquery.source);
            return null;
        }

        @Nullable
        @Override
        public Object visitValueSubqueryExpr(@NotNull SQLQueryValueSubqueryExpression subqueryExpr, @NotNull OutlineQueryNode node) {
            this.makeNode(node, subqueryExpr, "Scalar subquery", UIIcon.FILTER_VALUE, subqueryExpr.getSource());
            return null;
        }

        @Nullable
        @Override
        public Object visitValueFlatExpr(@NotNull SQLQueryValueFlattenedExpression flattenedExpr, @NotNull OutlineQueryNode node) {
            switch (flattenedExpr.getOperands().size()) {
                case 0 -> {
                } /* do nothing */
                case 1 -> flattenedExpr.getOperands().get(0).apply(this, node);
                default ->
                    this.makeNode(
                        node,
                        flattenedExpr,
                        prepareQueryPreview(flattenedExpr.getContent()),
                        DBIcon.TREE_FUNCTION,
                        flattenedExpr.getOperands().toArray(SQLQueryNodeModel[]::new)
                    );
            }
            return null;
        }

        @Nullable
        @Override
        public Object visitValueColumnRefExpr(
            @NotNull SQLQueryValueColumnReferenceExpression columnRefExpr,
            @NotNull OutlineQueryNode node
        ) {
            SQLQueryQualifiedName tableName = columnRefExpr.getTableName();
            String tableRefString = tableName == null ? "" : (tableName.toIdentifierString() + ".");

            SQLQuerySymbolDefinition def = columnRefExpr.getColumnNameIfTrivialExpression().getDefinition();
            while (def instanceof SQLQuerySymbolEntry s && s != s.getDefinition()) {
                def = s.getDefinition();
            }

            String text = tableRefString + columnRefExpr.getColumnNameIfTrivialExpression().getName();
            String extraText = this.obtainExprTypeNameString(columnRefExpr);
            DBPImage icon = this.obtainExprTypeIcon(columnRefExpr);
            
            this.makeNode(node, columnRefExpr, text, extraText, icon);
            return null;
        }

        @Nullable
        @Override
        public Object visitValueMemberReferenceExpr(@NotNull SQLQueryValueMemberExpression memberRefExpr, @NotNull OutlineQueryNode node) {
            String text = prepareQueryPreview(memberRefExpr.getExprContent());
            String extraText = this.obtainExprTypeNameString(memberRefExpr);
            DBPImage icon = this.obtainExprTypeIcon(memberRefExpr);
            
            this.makeNode(node, memberRefExpr, text, extraText, icon);
            return null;
        }

        @Nullable
        @Override
        public Object visitValueIndexingExpr(@NotNull SQLQueryValueIndexingExpression indexingExpr, @NotNull OutlineQueryNode node) {
            String text = prepareQueryPreview(indexingExpr.getExprContent());
            String extraText = this.obtainExprTypeNameString(indexingExpr);
            DBPImage icon = this.obtainExprTypeIcon(indexingExpr);
            
            this.makeNode(node, indexingExpr, text, extraText, icon);
            return null;
        }

        @Nullable
        private String obtainExprTypeNameString(@NotNull SQLQueryValueExpression expr) {
            SQLQueryExprType type = expr.getValueType();
            String typeName = type == null || type == SQLQueryExprType.UNKNOWN ? null : type.getDisplayName();
            return typeName == null ? null : (" : " + typeName);
        }

        @NotNull
        private DBPImage obtainExprTypeIcon(@NotNull SQLQueryValueExpression expr) {
            SQLQueryExprType type = expr.getValueType();
            return type == null || type == SQLQueryExprType.UNKNOWN ? DBIcon.TYPE_UNKNOWN
                : type == SQLQueryExprType.STRING ? DBIcon.TYPE_STRING
                : type.getTypedDbObject() == null ? DBIcon.TYPE_UNKNOWN
                : DBValueFormatting.getTypeImage(type.getTypedDbObject());
        }
        
        @Nullable
        @Override
        public Object visitSelectionResult(@NotNull SQLQuerySelectionResultModel selectionResult, @NotNull OutlineQueryNode node) {
            selectionResult.getSublists().forEach(s -> s.apply(this, node));
            return null;
        }

        @Nullable
        @Override
        public Object visitSelectionModel(@NotNull SQLQuerySelectionModel selection, @NotNull OutlineQueryNode node) {
            if (selection.getResultSource() != null) {
                selection.getResultSource().apply(this, node);
            }
            return null;
        }

        @Nullable
        @Override
        public Object visitRowsTableData(@NotNull SQLQueryRowsTableDataModel tableData, @NotNull OutlineQueryNode node) {
            DBSEntity table = tableData.getTable();
            DBPImage icon = DBValueFormatting.getObjectImage(table);
            String text = table == null ?
                tableData.getName().toIdentifierString() :
                DBUtils.getObjectFullName(table, DBPEvaluationContext.DML);
            this.makeNode(node, tableData, text, icon);
            return null;
        }

        @Nullable
        @Override
        public Object visitRowsTableValue(SQLQueryRowsTableValueModel tableValue, OutlineQueryNode node) {
            this.makeNode(node, tableValue, "Table value", DBIcon.TYPE_UNKNOWN); // TODO
            return null;
        }


        @Nullable
        @Override
        public Object visitRowsCrossJoin(@NotNull SQLQueryRowsCrossJoinModel crossJoin, @NotNull OutlineQueryNode node) {
            List<SQLQueryNodeModel> children = this.flattenRowSetsCombination(crossJoin, x -> true, (x, l) -> { /* do nothing*/ });
            this.makeNode(node, crossJoin, "CROSS JOIN", DBIcon.TREE_TABLE_LINK, children.toArray(SQLQueryNodeModel[]::new));
            return null;
        }

        @Nullable
        @Override
        public Object visitRowsCorrelatedSource(@NotNull SQLQueryRowsCorrelatedSourceModel correlated, @NotNull OutlineQueryNode node) {
            List<SQLQuerySymbolEntry> columnNames = correlated.getCorrelationColumNames();
            String suffix = columnNames.isEmpty() ? "" : columnNames.stream()
                .map(SQLQuerySymbolEntry::getRawName)
                .collect(Collectors.joining(", ", " (", ")"));
            String prefix = correlated.getSource() instanceof SQLQueryRowsTableDataModel t ? t.getName().toIdentifierString() + " AS " : "";
            this.makeNode(
                node,
                correlated,
                prefix + correlated.getAlias().getRawName() + suffix,
                DBIcon.TREE_TABLE_ALIAS,
                correlated.getSource()
            );
            return null;
        }

        @Nullable
        @Override
        public Object visitRowsNaturalJoin(@NotNull SQLQueryRowsNaturalJoinModel naturalJoin, @NotNull OutlineQueryNode node) {
            // TODO bring join kind here
            if (node.model instanceof SQLQueryRowsNaturalJoinModel) {
                if (naturalJoin.getCondition() != null) {
                    // TODO add expression text to the ON node and remove its immediate and only child with the same text
                    this.makeNode(node, naturalJoin.getCondition(), "ON ", DBIcon.TREE_UNIQUE_KEY, naturalJoin.getCondition());
                } else {
                    String suffix = naturalJoin.getColumsToJoin().stream()
                        .map(SQLQuerySymbolEntry::getRawName)
                        .collect(Collectors.joining(", ", "(", ")"));
                    this.makeNode(node, naturalJoin, "USING " + suffix, DBIcon.TREE_UNIQUE_KEY);
                }
            } else {
                List<SQLQueryNodeModel> children = this.flattenRowSetsCombination(naturalJoin, x -> true, (x, l) -> l.add(x));
                this.makeNode(node, naturalJoin, "NATURAL JOIN ", DBIcon.TREE_TABLE_LINK, children.toArray(SQLQueryNodeModel[]::new));
            }
            return null;
        }

        private <T extends SQLQueryRowsSetOperationModel> List<SQLQueryNodeModel> flattenRowSetsCombination(
            @NotNull T op,
            @NotNull Predicate<T> predicate,
            @NotNull BiConsumer<T, List<SQLQueryNodeModel>> action
        ) {
            List<SQLQueryNodeModel> result = new LinkedList<>();
            this.flattenRowSetsCombinationImpl(op.getClass(), op, predicate, result, action);
            return result;
        }

        @SuppressWarnings("unchecked")
        private <T extends SQLQueryRowsSetOperationModel> void flattenRowSetsCombinationImpl(
            @NotNull Class<? extends SQLQueryRowsSetOperationModel> type,
            @NotNull T op,
            @NotNull Predicate<T> predicate,
            @NotNull List<SQLQueryNodeModel> result,
            @NotNull BiConsumer<T, List<SQLQueryNodeModel>> action
        ) {
            SQLQueryRowsSourceModel left = op.getLeft();
            if (type.isInstance(left) && predicate.test((T) left)) {
                this.flattenRowSetsCombinationImpl(type, (T) left, predicate, result, action);
            } else {
                result.add(left);
            }
            result.add(op.getRight());
            action.accept(op, result);
        }

        @Nullable
        @Override
        public Object visitRowsProjection(@NotNull SQLQueryRowsProjectionModel projection, @NotNull OutlineQueryNode node) {
            String suffix = projection.getDataContext().getColumnsList().stream()
                .map(c -> c.symbol.getName())
                .collect(Collectors.joining(", ", "(", ")"));
            this.makeNode(node, projection.getResult(), "SELECT " + suffix, DBIcon.TREE_COLUMNS, projection.getResult());
            this.makeNode(node, projection.getFromSource(), "FROM", DBIcon.TREE_FOLDER_TABLE, projection.getFromSource());

            if (projection.getWhereClause() != null) {
                this.makeNode(node, projection.getWhereClause(), "WHERE", UIIcon.FILTER, projection.getWhereClause());
            }
            if (projection.getGroupByClause() != null) {
                this.makeNode(
                    node,
                    projection.getGroupByClause(),
                    "GROUP BY",
                    UIIcon.GROUP_BY_ATTR,
                    projection.getGroupByClause()
                );
            }
            if (projection.getHavingClause() != null) {
                this.makeNode(node, projection.getHavingClause(), "HAVING", UIIcon.FILTER, projection.getHavingClause());
            }
            if (projection.getOrderByClause() != null) {
                this.makeNode(node, projection.getOrderByClause(), "ORDER BY", UIIcon.SORT, projection.getOrderByClause());
            }
            return null;
        }

        @Override
        public Object visitRowsSetCorrespondingOp(
            @NotNull SQLQueryRowsSetCorrespondingOperationModel correspondingOp,
            @NotNull OutlineQueryNode arg
        ) {
            List<SQLQueryNodeModel> children = this.flattenRowSetsCombination(
                correspondingOp,
                x -> x.getKind().equals(correspondingOp.getKind()), (x, l) -> { /*do nothing*/ });
            this.makeNode(
                arg,
                correspondingOp,
                correspondingOp.getKind().toString(),
                DBIcon.TREE_TABLE_LINK,
                children.toArray(SQLQueryNodeModel[]::new)
            );
            return null;
        }

        @Nullable
        @Override
        public Object visitDummyTableRowsSource(@NotNull DummyTableRowsSource dummyTable, @NotNull OutlineQueryNode arg) {
            this.makeNode(arg, dummyTable, "?", DBIcon.TYPE_UNKNOWN);
            return null;
        }

        @Nullable
        @Override
        public Object visitSelectCompleteTupleSpec(@NotNull CompleteTupleSpec completeTupleSpec, @NotNull OutlineQueryNode arg) {
            this.makeNode(arg, completeTupleSpec, "*", UIIcon.ASTERISK);
            return null;
        }

        @Nullable
        @Override
        public Object visitSelectTupleSpec(@NotNull TupleSpec tupleSpec, @NotNull OutlineQueryNode arg) {
            this.makeNode(arg, tupleSpec, tupleSpec.getTableName().toIdentifierString(), UIIcon.ASTERISK);
            return null;
        }

        @Nullable
        @Override
        public Object visitSelectColumnSpec(@NotNull ColumnSpec columnSpec, @NotNull OutlineQueryNode arg) {
            SQLQuerySymbolEntry alias = columnSpec.getAlias();
            SQLQuerySymbol mayBeColumnName = columnSpec.getValueExpression().getColumnNameIfTrivialExpression();
            
            String text = alias != null ? alias.getRawName() : (mayBeColumnName == null ? "?" : mayBeColumnName.getName());
            String extraText = this.obtainExprTypeNameString(columnSpec.getValueExpression());
            
            this.makeNode(arg, columnSpec, text, extraText, DBIcon.TREE_COLUMN, columnSpec.getValueExpression());
            return null;
        }

        private void makeNode(
            @NotNull OutlineQueryNode parent,
            @NotNull SQLQueryNodeModel model,
            @NotNull String text,
            @NotNull DBPImage icon,
            @NotNull SQLQueryNodeModel... childModels
        ) {
            parent.children.add(new OutlineQueryNode(parent, model, text, null, icon, childModels));
        }
        
        private void makeNode(
            @NotNull OutlineQueryNode parent,
            @NotNull SQLQueryNodeModel model,
            @NotNull String text,
            @NotNull String extraText,
            @NotNull DBPImage icon,
            @NotNull SQLQueryNodeModel... childModels
        ) {
            parent.children.add(new OutlineQueryNode(parent, model, text, extraText, icon, childModels));
        }
    }
}
