/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.database;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.IDBNListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;

public class DatabaseNavigatorTree extends Composite implements IDBNListener
{
    static final Log log = LogFactory.getLog(DatabaseNavigatorTree.class);

    private TreeViewer viewer;
    private DBNModel model;
    private TreeEditor treeEditor;

    public DatabaseNavigatorTree(Composite parent, DBNNode rootNode, int style)
    {
        super(parent, SWT.NONE);
        this.setLayout(new FillLayout());
        this.model = rootNode.getModel();
        this.model.addListener(this);
        addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                if (model != null) {
                    model.removeListener(DatabaseNavigatorTree.this);
                    model = null;
                }
            }
        });

        // Create tree
        // TODO: there are problems with this tree when we have a lot of items.
        // TODO: I may set SWT.SINGLE style and it'll solve the problem at least when traversing tree
        // TODO: But we need multiple selection (to copy, export, etc)
        // TODO: need to do something with it
        int treeStyle = SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | style;
        this.viewer = new TreeViewer(this, treeStyle);
        this.viewer.getTree().setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
        this.viewer.setUseHashlookup(true);
        //if (rootNode instanceof DBNProject) {
        //    this.viewer.setAutoExpandLevel(2);
        //}
        this.viewer.setLabelProvider(new DatabaseNavigatorLabelProvider(this.viewer));
        this.viewer.setContentProvider(new DatabaseNavigatorContentProvider(this.viewer));
        this.viewer.setInput(rootNode);

        initEditor();
    }

    private void initEditor()
    {
        Tree treeControl = this.viewer.getTree();

        treeEditor = new TreeEditor(treeControl);
        treeEditor.horizontalAlignment = SWT.LEFT;
        treeEditor.verticalAlignment = SWT.TOP;
        treeEditor.grabHorizontal = false;
        treeEditor.minimumWidth = 50;

        //treeControl.addSelectionListener(new TreeSelectionAdapter());
        treeControl.addMouseListener(new TreeSelectionAdapter());
    }

    public TreeViewer getViewer()
    {
        return viewer;
    }

    public void nodeChanged(final DBNEvent event)
    {
        switch (event.getAction()) {
            case ADD:
            case REMOVE:
                final DBNNode parentNode = event.getNode().getParentNode();
                if (parentNode != null) {
                    asyncExec(new Runnable() { public void run() {
                        if (!viewer.getControl().isDisposed()) {
                            if (!parentNode.isDisposed()) {
                                viewer.refresh(parentNode);
                            }
                        }
                    }});
                }
                break;
            case UPDATE:
                asyncExec(new Runnable() { public void run() {
                    if (!viewer.getControl().isDisposed()) {
                        if (event.getNode() != null) {
                            switch (event.getNodeChange()) {
                                case LOAD:
                                    viewer.expandToLevel(event.getNode(), 1);
                                    viewer.refresh(event.getNode());
                                    break;
                                case UNLOAD:
                                    viewer.collapseToLevel(event.getNode(), -1);
                                    viewer.refresh(event.getNode());
                                    break;
                                case REFRESH:
                                    viewer.update(event.getNode(), null);
                                    break;
                                case LOCK:
                                case UNLOCK:
                                    viewer.refresh(event.getNode());
                                    break;
                            }
                        } else {
                            log.warn("Null node object");
                        }
                    }
                }});
                break;
            default:
                break;
        }
    }

    private void asyncExec(Runnable runnable)
    {
        Display.getDefault().asyncExec(runnable);
    }

    public void showNode(DBNNode node) {
        viewer.reveal(node);
        viewer.setSelection(new StructuredSelection(node));
    }

    public void reloadTree(DBNNode rootNode)
    {
        this.viewer.setInput(rootNode);
    }

    private class TreeSelectionAdapter implements MouseListener {

        private volatile TreeItem curSelection;
        private volatile RenameJob renameJob = new RenameJob();

        private volatile boolean doubleClick = false;

        public synchronized void mouseDoubleClick(MouseEvent e)
        {
            curSelection = null;
            renameJob.canceled = true;
        }

        public void mouseDown(MouseEvent e)
        {
        }

        public void mouseUp(MouseEvent e)
        {
            if ((e.stateMask & SWT.BUTTON1) == 0) {
                curSelection = null;
                return;
            }
            changeSelection(e);
        }

        public void changeSelection(MouseEvent e) {
            disposeOldEditor();
            final TreeItem newSelection = viewer.getTree().getItem(new Point(e.x, e.y));
            if (newSelection == null) {
                //curSelection = null;
                return;
            }

            if (!(newSelection.getData() instanceof DBNNode) || !((DBNNode)newSelection.getData()).supportsRename()) {
                curSelection = null;
                return;
            }
            if (curSelection != null && curSelection == newSelection) {
                renameJob.schedule(1000);
            }
            curSelection = newSelection;
        }

        private class RenameJob extends AbstractUIJob {
            private volatile boolean canceled = false;
            public RenameJob()
            {
                super("Rename ");
            }

            protected IStatus runInUIThread(DBRProgressMonitor monitor)
            {
                try {
                    if (!viewer.getTree().isDisposed() && viewer.getTree().isFocusControl() && curSelection != null && !canceled) {
                        getDisplay().asyncExec(new Runnable() {
                            public void run()
                            {
                                renameItem(curSelection);
                            }
                        });
                    }
                } finally {
                    canceled = false;
                }
                return Status.OK_STATUS;
            }

        }
    }

    private void renameItem(final TreeItem item)
    {
        // Clean up any previous editor control
        disposeOldEditor();

        final DBNNode node = (DBNNode) item.getData();

        Text text = new Text(viewer.getTree(), SWT.BORDER);
        text.setText(node.getNodeName());
        text.selectAll();
        text.setFocus();
        text.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e)
            {
                disposeOldEditor();
            }
        });
        text.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == SWT.CR) {
                    Text text = (Text) treeEditor.getEditor();
                    final String newName = text.getText();
                    if (!CommonUtils.isEmpty(newName) && !newName.equals(node.getNodeName())) {
                        try {
                            DBeaverCore.getInstance().runAndWait2(new DBRRunnableWithProgress() {
                                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                                {
                                    try {
                                        node.rename(monitor, newName);
                                    } catch (DBException e1) {
                                        throw new InvocationTargetException(e1);
                                    }
                                }
                            });
                        } catch (InvocationTargetException e1) {
                            UIUtils.showErrorDialog(getShell(), "Rename failed", null, e1.getTargetException());
                        } catch (InterruptedException e1) {
                            // do nothing
                        }
                    }

                    disposeOldEditor();
                    viewer.getTree().setFocus();
                } else if (e.keyCode == SWT.ESC) {
                    disposeOldEditor();
                    viewer.getTree().setFocus();
                }
            }
        });
        final Rectangle itemBounds = item.getBounds(0);
        final Rectangle treeBounds = viewer.getTree().getBounds();
        treeEditor.minimumWidth = Math.max(itemBounds.width, 50);
        treeEditor.minimumWidth = Math.min(treeEditor.minimumWidth, treeBounds.width - (itemBounds.x - treeBounds.x) - item.getImageBounds(0).width - 4);

        treeEditor.setEditor(text, item, 0);
    }

    private void disposeOldEditor()
    {
        Control oldEditor = treeEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

}