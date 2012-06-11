/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * DBNNode
 */
public abstract class DBNNode implements DBPNamedObject, DBPPersistedObject
{
    static final Log log = LogFactory.getLog(DBNNode.class);

    private DBNNode parentNode;

    protected DBNNode()
    {
        this.parentNode = null;
    }

    protected DBNNode(DBNNode parentNode)
    {
        this.parentNode = parentNode;
    }

    public boolean isDisposed()
    {
        return false;
    }

    void dispose(boolean reflect)
    {
        this.parentNode = null;
    }

    public final DBNNode getParentNode()
    {
        return parentNode;
    }

    public boolean isLocked()
    {
        return getParentNode() != null && getParentNode().isLocked();
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    public boolean isManagable()
    {
        return false;
    }

    @Override
    public String getName()
    {
        return getNodeName();
    }

    public abstract String getNodeType();

    public abstract String getNodeName();

    public abstract String getNodeDescription();

    public abstract Image getNodeIcon();

    public final Image getNodeIconDefault()
    {
        Image image = getNodeIcon();
        if (image == null) {
            if (this.allowsChildren()) {
                return DBIcon.TREE_FOLDER.getImage();
            } else {
                return DBIcon.TREE_PAGE.getImage();
            }
        } else {
            return image;
        }
    }

    public String getNodeFullName()
    {
        StringBuilder pathName = new StringBuilder();
        pathName.append(getNodeName());

        for (DBNNode parent = getParentNode(); parent != null && !(parent instanceof DBNDataSource); parent = parent.getParentNode()) {
            if (parent instanceof DBNDatabaseFolder) {
                // skip folders
                continue;
            }
            String parentName = parent.getNodeName();
            if (!CommonUtils.isEmpty(parentName)) {
                pathName.insert(0, '.').insert(0, parentName);
            }
        }
        return pathName.toString();
    }

    public abstract boolean allowsChildren();

    public abstract boolean allowsNavigableChildren();
    
    public abstract List<? extends DBNNode> getChildren(DBRProgressMonitor monitor) throws DBException;

    void clearNode(boolean reflect)
    {

    }

    public boolean supportsRename()
    {
        return false;
    }

    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
        throw new DBException("Rename is not supported");
    }

    public boolean supportsDrop(DBNNode otherNode)
    {
        return false;
    }

    public void dropNode(DBNNode otherNode) throws DBException
    {
        throw new DBException("Drop is not supported");
    }

    /**
     * Refreshes node.
     * If refresh cannot be done in this level then refreshes parent node.
     * Do not actually changes navigation tree. If some underlying object is refreshed it must fire DB model
     * event which will cause actual tree nodes refresh. Underlying object could present multiple times in
     * navigation model - each occurrence will be refreshed then.
     *
     * @param monitor progress monitor
     * @param source event source
     * @return real refreshed node or null if nothing was refreshed
     * @throws DBException on any internal exception
     */
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException
    {
        if (this.getParentNode() != null) {
            return this.getParentNode().refreshNode(monitor, source);
        } else {
            return null;
        }
    }
    
    public boolean allowsOpen()
    {
        return true;
    }

    public final boolean isChildOf(DBNNode node)
    {
        for (DBNNode parent = getParentNode(); parent != null; parent = parent.getParentNode()) {
            if (parent == node) {
                return true;
            }
        }
        return false;
    }

    public boolean isFiltered()
    {
        return false;
    }
}
