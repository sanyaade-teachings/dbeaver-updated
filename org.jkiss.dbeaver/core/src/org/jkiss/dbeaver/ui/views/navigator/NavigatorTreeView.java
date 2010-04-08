package org.jkiss.dbeaver.ui.views.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.ext.ui.IRefreshableView;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.RefreshTreeAction;
import org.jkiss.dbeaver.utils.ViewUtils;

public class NavigatorTreeView extends ViewPart implements IDBMListener, IMetaModelView, IRefreshableView, IDoubleClickListener
{
    static Log log = LogFactory.getLog(NavigatorTreeView.class);

    public static final String ID = "org.jkiss.dbeaver.core.navigationView";

    private TreeViewer viewer;
    private DBMModel model;
    private RefreshTreeAction refreshAction;

    public NavigatorTreeView()
    {
        super();
        model = DBeaverCore.getInstance().getMetaModel();
        model.addListener(this);
    }

    /**
     * We will set up a dummy model to initialize tree heararchy. In real
     * code, you will connect to a real model and expose its hierarchy.
     */
    public DBMModel getMetaModel()
    {
        return model;
    }

    public TreeViewer getViewer()
    {
        return viewer;
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return this;
    }

    public IAction getRefreshAction()
    {
        return refreshAction;
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    public void createPartControl(Composite parent)
    {
        this.viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        this.viewer.setContentProvider(new NavigatorTreeContentProvider(this));
        this.viewer.setLabelProvider(new NavigatorTreeLabelProvider(this));
        this.viewer.setInput(getMetaModel().getRoot());
        this.viewer.addSelectionChangedListener(
            new ISelectionChangedListener()
            {
                public void selectionChanged(SelectionChangedEvent event)
                {
                    IStructuredSelection structSel = (IStructuredSelection)event.getSelection();
                    if (!structSel.isEmpty()) {
                        Object object = structSel.getFirstElement();
                        if (object instanceof DBSObject) {
                            String desc = ((DBSObject)object).getDescription();
                            if (CommonUtils.isEmpty(desc)) {
                                desc = ((DBSObject)object).getName();
                            }
                            getViewSite().getActionBars().getStatusLineManager().setMessage(desc);
                        }
                    }
                }
            }
        );
        this.viewer.addDoubleClickListener(this);
        // Hook context menu
        ViewUtils.addContextMenu(this);
        // Add drag and drop support
        ViewUtils.addDragAndDropSupport(this);

        getViewSite().setSelectionProvider(viewer);

        // Add refresh action binding
        refreshAction = new RefreshTreeAction(this);
        refreshAction.setEnabled(true);

        getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);

        getViewSite().getActionBars().updateActionBars();
    }

    public void dispose()
    {
        if (model != null) {
            model.removeListener(this);
            model = null;
        }
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus()
    {
        viewer.getControl().setFocus();
    }

    public void nodeChanged(final DBMEvent event)
    {
        switch (event.getAction()) {
            case ADD:
            case REMOVE:
                if (event.getNode() instanceof DBMDataSource) {
                    asyncExec(new Runnable() { public void run() {
                        if (!viewer.getControl().isDisposed()) {
                            viewer.refresh();
                        }
                    }});
                }
                break;
            case REFRESH:
                asyncExec(new Runnable() { public void run() {
                    if (!viewer.getControl().isDisposed()) {
                        switch (event.getNodeChange()) {
                            case LOADED:
                                viewer.expandToLevel(event.getNode().getObject(), 1);
                                break;
                            case UNLOADED:
                                viewer.collapseToLevel(event.getNode().getObject(), -1);
                                break;
                            case CHANGED:
                                getViewer().refresh(event.getNode().getObject());
                                break;
                        }
                        viewer.refresh(event.getNode().getObject());
                    }
                }});
                break;
            default:
                break;
        }
    }

    public void doubleClick(DoubleClickEvent event)
    {
        DBMNode dbmNode = getSelectedNode();
        if (dbmNode == null) {
            return;
        }
        ViewUtils.runAction(dbmNode.getDefaultAction(), this, this.viewer.getSelection());
    }

    private DBMNode getSelectedNode()
    {
        return ViewUtils.getSelectedNode(this);
    }

    public DBSDataSourceContainer getSelectedDataSourceContainer()
    {
        DBMNode selectedNode = getSelectedNode();
        if (selectedNode == null) {
            return null;
        }

        for (DBMNode curNode = selectedNode; curNode != null; curNode = curNode.getParentNode()) {
            if (curNode.getObject() instanceof DBSDataSourceContainer) {
                return (DBSDataSourceContainer)curNode.getObject();
            }
        }
        return null;
    }

    private void asyncExec(Runnable runnable)
    {
        if (!getSite().getShell().isDisposed() && !getSite().getShell().getDisplay().isDisposed()) {
            getSite().getShell().getDisplay().asyncExec(runnable);
        }
    }
}
