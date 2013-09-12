/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model.cache;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;

/**
 * Cache for DB2 Tables
 * 
 * @author Denis Forveille
 * 
 */
public final class DB2TableCache extends JDBCStructCache<DB2Schema, DB2Table, DB2TableColumn> {

   private static final String SQL_TABS     = "SELECT * FROM SYSCAT.TABLES WHERE TABSCHEMA = ? AND TYPE IN ('H','L','T','U') ORDER BY TABNAME WITH UR";
   private static final String SQL_COLS_TAB = "SELECT * FROM SYSCAT.COLUMNS WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY COLNO WITH UR";
   private static final String SQL_COLS_ALL = "SELECT * FROM SYSCAT.COLUMNS WHERE TABSCHEMA = ? ORDER BY TABNAME, COLNO WITH UR";

   public DB2TableCache() {
      super("TABNAME");
      setListOrderComparator(DBUtils.<DB2Table> nameComparator());
   }

   @Override
   protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, DB2Schema db2Schema) throws SQLException {
      final JDBCPreparedStatement dbStat = context.prepareStatement(SQL_TABS);
      dbStat.setString(1, db2Schema.getName());
      return dbStat;
   }

   @Override
   protected DB2Table fetchObject(JDBCExecutionContext context, DB2Schema db2Schema, ResultSet dbResult) throws SQLException,
                                                                                                        DBException {
      return new DB2Table(context.getProgressMonitor(), db2Schema, dbResult);
   }

   @Override
   protected JDBCStatement prepareChildrenStatement(JDBCExecutionContext context, DB2Schema db2Schema, DB2Table forTable) throws SQLException {

      String sql;
      if (forTable != null) {
         sql = SQL_COLS_TAB;
      } else {
         sql = SQL_COLS_ALL;
      }
      JDBCPreparedStatement dbStat = context.prepareStatement(sql);
      dbStat.setString(1, db2Schema.getName());
      if (forTable != null) {
         dbStat.setString(2, forTable.getName());
      }
      return dbStat;
   }

   @Override
   protected DB2TableColumn fetchChild(JDBCExecutionContext context, DB2Schema db2Schema, DB2Table db2Table, ResultSet dbResult) throws SQLException,
                                                                                                                                DBException {
      return new DB2TableColumn(context.getProgressMonitor(), db2Table, dbResult);
   }

}
