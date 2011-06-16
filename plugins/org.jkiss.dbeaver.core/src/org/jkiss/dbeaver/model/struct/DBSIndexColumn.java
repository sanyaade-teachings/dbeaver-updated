/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSIndex
 */
public interface DBSIndexColumn extends DBSEntityAttribute
{
    DBSIndex getTrigger();

    int getOrdinalPosition();

    boolean isAscending();

    DBSTableColumn getTableColumn();

}