/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * DBPDriver
 */
public interface DBPDriver extends DBPObject
{
    DBPDataSourceProvider getDataSourceProvider()
        throws DBException;

    String getId();

    String getName();

    String getDescription();

    Image getIcon();

    String getDriverClassName();

    Object getDriverInstance() throws DBException;

    String getDefaultPort();

    String getSampleURL();

    String getWebURL();

    boolean supportsDriverProperties();

    boolean isAnonymousAccess();

    boolean isCustomDriverLoader();

    Collection<IPropertyDescriptor> getConnectionPropertyDescriptors();

    Map<Object, Object> getDefaultConnectionProperties();

    Map<Object, Object> getConnectionProperties();

    Map<Object, Object> getDriverParameters();

    Object getDriverParameter(String name);

    Collection<? extends DBPDriverLocalPath> getPathList();

    ClassLoader getClassLoader();

    void loadDriver()
        throws DBException;

}
