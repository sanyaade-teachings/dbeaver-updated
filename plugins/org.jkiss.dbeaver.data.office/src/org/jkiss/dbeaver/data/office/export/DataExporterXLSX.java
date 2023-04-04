/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
 * Copyright (C) 2017 Adolfo Suarez  (agustavo@gmail.com)
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
package org.jkiss.dbeaver.data.office.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.tools.transfer.stream.IAppendableDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.tools.transfer.stream.exporter.StreamExporterAbstract;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Export XLSX with Apache POI
 */
public class DataExporterXLSX extends StreamExporterAbstract implements IAppendableDataExporter {

    private static final Log log = Log.getLog(DataExporterXLSX.class);

    private static final String PROP_HEADER = "header";
    private static final String PROP_NULL_STRING = "nullString";

    private static final String PROP_ROWNUMBER = "rownumber";
    private static final String PROP_BORDER = "border";
    private static final String PROP_HEADER_FONT = "headerfont";

    private static final String BINARY_FIXED = "[BINARY]";

    private static final String PROP_TRUESTRING = "trueString";
    private static final String PROP_FALSESTRING = "falseString";

    private static final String PROP_EXPORT_SQL = "exportSql";
    private static final String PROP_SPLIT_SQLTEXT = "splitSqlText";

    private static final String PROP_SPLIT_BYROWCOUNT = "splitByRowCount";
    private static final String PROP_SPLIT_BYCOL = "splitByColNum";

    private static final String PROP_DATE_FORMAT = "dateFormat";
    private static final String PROP_APPEND_STRATEGY = "appendStrategy";

    private static final int EXCEL2007MAXROWS = 1048575;
    private static final int EXCEL_MAX_CELL_CHARACTERS = 32767; // Total number of characters that a cell can contain - 32,767 characters

    enum FontStyleProp {NONE, BOLD, ITALIC, STRIKEOUT, UNDERLINE}

    private static final int ROW_WINDOW = 100;

    private String nullString;

    private DBDAttributeBinding[] columns;
    private DBDAttributeDecorator decorator;

    private SXSSFWorkbook wb;

    private HeaderFormat headerFormat = HeaderFormat.LABEL;
    private boolean rowNumber = false;
    private String boolTrue = "true";
    private String boolFalse = "false";
    private boolean booleRedefined;
    private boolean exportSql = false;
    private boolean splitSqlText = false;
    private String dateFormat = "";
    private AppendStrategy appendStrategy = AppendStrategy.CREATE_NEW_SHEETS;

    private int splitByRowCount = EXCEL2007MAXROWS;
    private int splitByCol = 0;
    private int rowCount = 0;
    private int sheetIndex = 0;

    private XSSFCellStyle style;
    private XSSFCellStyle styleDate;
    private XSSFCellStyle styleHeader;

    private HashMap<Object, Worksheet> worksheets;

    public static Map<String, Object> getDefaultProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(DataExporterXLSX.PROP_ROWNUMBER, false);
        properties.put(DataExporterXLSX.PROP_BORDER, "THIN");
        properties.put(DataExporterXLSX.PROP_HEADER, HeaderFormat.LABEL.value);
        properties.put(DataExporterXLSX.PROP_NULL_STRING, null);
        properties.put(DataExporterXLSX.PROP_HEADER_FONT, "BOLD");
        properties.put(DataExporterXLSX.PROP_TRUESTRING, "true");
        properties.put(DataExporterXLSX.PROP_FALSESTRING, "false");
        properties.put(DataExporterXLSX.PROP_EXPORT_SQL, false);
        properties.put(DataExporterXLSX.PROP_SPLIT_SQLTEXT, false);
        properties.put(DataExporterXLSX.PROP_SPLIT_BYROWCOUNT, EXCEL2007MAXROWS);
        properties.put(DataExporterXLSX.PROP_SPLIT_BYCOL, 0);
        properties.put(DataExporterXLSX.PROP_DATE_FORMAT, "");
        properties.put(DataExporterXLSX.PROP_APPEND_STRATEGY, AppendStrategy.CREATE_NEW_SHEETS.value);
        return properties;
    }

    @Override
    public void init(IStreamDataExporterSite site) throws DBException {
        Map<String, Object> properties = site.getProperties();
        Object nullStringProp = properties.get(PROP_NULL_STRING);
        nullString = nullStringProp == null ? null : nullStringProp.toString();
        headerFormat = HeaderFormat.of(CommonUtils.toString(properties.get(PROP_HEADER)));

        try {
            rowNumber = CommonUtils.getBoolean(properties.get(PROP_ROWNUMBER), false);
        } catch (Exception e) {
            rowNumber = false;
        }

        try {
            boolTrue = CommonUtils.toString(properties.get(PROP_TRUESTRING), "true");
        } catch (Exception e) {
            boolTrue = "true";
        }
        try {
            boolFalse = CommonUtils.toString(properties.get(PROP_FALSESTRING), "false");
        } catch (Exception e) {
            boolFalse = "false";
        }
        if (!"true".equals(boolTrue) || !"false".equals(boolFalse)) {
            booleRedefined = true;
        }

        try {
            exportSql = CommonUtils.getBoolean(properties.get(PROP_EXPORT_SQL), false);
        } catch (Exception e) {
            exportSql = false;
        }

        try {
            splitSqlText = CommonUtils.getBoolean(properties.get(PROP_SPLIT_SQLTEXT), false);
        } catch (Exception e) {
            splitSqlText = false;
        }

        try {
            splitByRowCount = CommonUtils.toInt(properties.get(PROP_SPLIT_BYROWCOUNT), EXCEL2007MAXROWS);
        } catch (Exception e) {
            splitByRowCount = EXCEL2007MAXROWS;
        }

        try {
            splitByCol = CommonUtils.toInt(properties.get(PROP_SPLIT_BYCOL), 0);
        } catch (Exception e) {
            splitByCol = -1;
        }

        try {
            dateFormat = CommonUtils.toString(properties.get(PROP_DATE_FORMAT), "");
        } catch (Exception e) {
            dateFormat = "";
        }

        appendStrategy = AppendStrategy.of(CommonUtils.toString(properties.get(PROP_APPEND_STRATEGY)));

        if (wb == null) {
            wb = new SXSSFWorkbook(ROW_WINDOW);
        }

        worksheets = new HashMap<>(1);

        styleHeader = (XSSFCellStyle) wb.createCellStyle();

        BorderStyle border;

        try {

            border = BorderStyle.valueOf(CommonUtils.toString(properties.get(PROP_BORDER), BorderStyle.THIN.name()));

        } catch (Exception e) {

            border = BorderStyle.NONE;

        }

        FontStyleProp fontStyle;

        try {

            fontStyle = FontStyleProp.valueOf(CommonUtils.toString(properties.get(PROP_HEADER_FONT), FontStyleProp.BOLD.name()));

        } catch (Exception e) {

            fontStyle = FontStyleProp.NONE;

        }

        styleHeader.setBorderTop(border);
        styleHeader.setBorderBottom(border);
        styleHeader.setBorderLeft(border);
        styleHeader.setBorderRight(border);

        XSSFFont fontBold = (XSSFFont) wb.createFont();

        switch (fontStyle) {

        case BOLD:
            fontBold.setBold(true);
            break;

        case ITALIC:
            fontBold.setItalic(true);
            break;

        case STRIKEOUT:
            fontBold.setStrikeout(true);
            break;

        case UNDERLINE:
            fontBold.setUnderline((byte) 3);
            break;

        default:
            break;
        }

        styleHeader.setFont(fontBold);

        style = (XSSFCellStyle) wb.createCellStyle();
        style.setBorderTop(border);
        style.setBorderBottom(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);

        styleDate = (XSSFCellStyle) wb.createCellStyle();
        styleDate.setBorderTop(border);
        styleDate.setBorderBottom(border);
        styleDate.setBorderLeft(border);
        styleDate.setBorderRight(border);

        if (dateFormat == null || dateFormat.length() == 0) {
            styleDate.setDataFormat((short) 14);
        } else {
           styleDate.setDataFormat(
                wb.getCreationHelper().createDataFormat().getFormat(dateFormat));
        }

        this.rowCount = 0;
        this.sheetIndex = 0;

        super.init(site);
    }

    @Override
    public void dispose() {
        try {
            if (exportSql && wb != null) {
                try {

                    Sheet sh = wb.createSheet();
                    if (splitSqlText) {
                        String[] sqlText = getSite().getSource().getName().split("\n",
                                wb.getSpreadsheetVersion().getMaxRows());

                        int sqlRownum = 0;

                        for (String s : sqlText) {
                            Row row = sh.createRow(sqlRownum);
                            Cell newcell = row.createCell(0);
                            newcell.setCellValue(s);
                            sqlRownum++;
                        }

                    } else {
                        Row row = sh.createRow(0);
                        Cell newcell = row.createCell(0);
                        newcell.setCellValue(getSite().getSource().getName());
                    }
                    sh = null;
                } catch (Exception e) {
                    log.error("Dispose error", e);
                }
            }
            if (wb != null) {
                wb.write(getSite().getOutputStream());
                wb.close();
                wb.dispose();
            }

        } catch (IOException e) {
            log.error("Dispose error", e);
        }
        wb = null;
        if (!CommonUtils.isEmpty(worksheets)) {
            for (Worksheet w : worksheets.values()) {
                w.dispose();
            }
            worksheets.clear();
        }

        super.dispose();
    }

    @Override
    public void exportHeader(DBCSession session) throws DBException {

        columns = getSite().getAttributes();
        if (headerFormat.hasDescription()) {
            DBSEntity srcEntity = DBUtils.getAdapter(DBSEntity.class, getSite().getSource());
            DBExecUtils.bindAttributes(session, srcEntity, null, columns, null);
        }
        decorator = GeneralUtils.adapt(getSite().getSource(), DBDAttributeDecorator.class);
    }

    private void printHeader(DBCResultSet resultSet, Worksheet wsh) throws DBException {
        final SXSSFSheet sh = (SXSSFSheet) wsh.getSh();

        if (appendStrategy == AppendStrategy.USE_EXISTING_SHEETS && getPhysicalNumberOfRows(sh) > 0) {
            return;
        }

        boolean hasDescription = false;
        if (headerFormat.hasDescription()) {
            // Read bindings to extract column descriptions
            boolean bindingsOk = true;
            DBDAttributeBindingMeta[] bindings = new DBDAttributeBindingMeta[columns.length];
            for (int i = 0; i < columns.length; i++) {
                if (columns[i] instanceof DBDAttributeBindingMeta) {
                    bindings[i] = (DBDAttributeBindingMeta) columns[i];
                } else {
                    bindingsOk = false;
                    break;
                }
            }
            if (bindingsOk) {
                final DBSEntity sourceEntity = GeneralUtils.adapt(getSite().getSource(), DBSEntity.class);
                if (sourceEntity != null) {
                    DBExecUtils.bindAttributes(resultSet.getSession(), sourceEntity, resultSet, bindings, null);
                }
            }

            for (DBDAttributeBinding column : columns) {
                if (!CommonUtils.isEmpty(column.getDescription())) {
                    hasDescription = true;
                    break;
                }
            }
        }

        if (!hasDescription && !headerFormat.hasLabel()) {
            return;
        }

        sh.trackAllColumnsForAutoSizing();

        int startCol = rowNumber ? 1 : 0;

        if (headerFormat.hasLabel()) {
            Row row = sh.createRow(wsh.getCurrentRow());
            for (int i = 0, columnsSize = columns.length; i < columnsSize; i++) {
                DBDAttributeBinding column = columns[i];

                String colName = column.getLabel();
                if (CommonUtils.isEmpty(colName)) {
                    colName = column.getName();
                }
                Cell cell = row.createCell(i + startCol, CellType.STRING);
                cell.setCellValue(colName);
                cell.setCellStyle(styleHeader);
            }
            wsh.incRow();
        }

        if (hasDescription) {
            Row descRow = sh.createRow(wsh.getCurrentRow());
            for (int i = 0, columnsSize = columns.length; i < columnsSize; i++) {
                Cell descCell = descRow.createCell(i + startCol, CellType.STRING);
                String description = columns[i].getDescription();
                descCell.setCellValue(CommonUtils.notEmpty(description));
                descCell.setCellStyle(styleHeader);

                if (CommonUtils.isNotEmpty(description)) {
                    sh.autoSizeColumn(i);
                }
            }
            wsh.incRow();
        }

        try {
            sh.flushRows();
        } catch (IOException e) {
            throw new DBException("Error processing header", e);
        }

        sh.untrackAllColumnsForAutoSizing();
    }

    private void writeCellValue(Cell cell, Reader reader) throws IOException {
        try {
            StringBuilder sb = new StringBuilder();
            char buffer[] = new char[2000];
            for (;;) {
                int count = reader.read(buffer);
                if (count <= 0) {
                    break;
                }
                sb.append(buffer, 0, count);
            }
            cell.setCellValue(getPreparedString(sb.toString()));
        } finally {
            ContentUtils.close(reader);
        }
    }

    private Worksheet createSheet(DBCResultSet resultSet, Object colValue) throws DBException {
        final Sheet sheet;
        final Worksheet worksheet;
        if (appendStrategy == AppendStrategy.USE_EXISTING_SHEETS && sheetIndex < wb.getNumberOfSheets()) {
            sheet = wb.getSheetAt(sheetIndex++);
            worksheet = new Worksheet(sheet, colValue, getPhysicalNumberOfRows(sheet));
        } else {
            sheet = wb.createSheet();
            worksheet = new Worksheet(sheet, colValue, 0);
        }
        printHeader(resultSet, worksheet);
        return worksheet;
    }

    private Worksheet getWsh(DBCResultSet resultSet, Object[] row) throws DBException {
        Object colValue = ((splitByCol <= 0) || (splitByCol >= columns.length)) ? "" : row[splitByCol];
        Worksheet w = worksheets.get(colValue);
        if (w == null) {
            w = createSheet(resultSet, colValue);
            worksheets.put(w.getColumnVal(), w);
        } else {
            if (w.getCurrentRow() >= splitByRowCount) {
                w = createSheet(resultSet, colValue);
                worksheets.put(w.getColumnVal(), w);
            }
        }
        return w;
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row)
        throws DBException, IOException {

        Worksheet wsh = getWsh(resultSet, row);

        Row rowX = wsh.getSh().createRow(wsh.getCurrentRow());

        int startCol = 0;

        if (rowNumber) {

            Cell cell = rowX.createCell(startCol, CellType.NUMERIC);
            cell.setCellStyle(style);
            cell.setCellValue(String.valueOf(wsh.getCurrentRow()));
            startCol++;
        }

        for (int i = 0; i < row.length; i++) {
            DBDAttributeBinding column = columns[i];
            Cell cell = rowX.createCell(i + startCol, getCellType(column));
            cell.setCellStyle(getCellStyle(column, rowCount));

            if (DBUtils.isNullValue(row[i])) {
                if (!CommonUtils.isEmpty(nullString)) {
                    cell.setCellValue(nullString);
                } else {
                    cell.setCellValue("");
                }
            } else if (row[i] instanceof DBDContent) {
                DBDContent content = (DBDContent) row[i];
                try {
                    DBDContentStorage cs = content.getContents(session.getProgressMonitor());
                    if (cs == null) {
                        cell.setCellValue(DBConstants.NULL_VALUE_LABEL);
                    } else if (ContentUtils.isTextContent(content)) {
                        writeCellValue(cell, cs.getContentReader());
                    } else {
                        cell.setCellValue(BINARY_FIXED);
                    }
                } finally {
                    content.release();
                }
            } else if (row[i] instanceof Boolean) {

                if (booleRedefined) {
                    cell.setCellValue((Boolean) row[i] ? boolTrue : boolFalse);
                } else {
                    cell.setCellValue((Boolean) row[i]);
                }

            } else if (row[i] instanceof Number) {

                cell.setCellValue(((Number) row[i]).doubleValue());

            } else if (row[i] instanceof Date) {

                cell.setCellValue((Date) row[i]);
                cell.setCellStyle(styleDate);

            } else {
                String stringValue = super.getValueDisplayString(column, row[i]);
                cell.setCellValue(getPreparedString(stringValue));
            }

        }
        wsh.incRow();
        rowCount++;
    }

    private CellType getCellType(DBDAttributeBinding column) {
        switch (column.getDataKind()) {
        case NUMERIC:
            return CellType.NUMERIC;
        case BOOLEAN:
            return CellType.BOOLEAN;
        case STRING:
            return CellType.STRING;
        default:
            return CellType.BLANK;
        }
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws DBException, IOException {
        if (rowCount == 0) {
            exportRow(null, null, new Object[columns.length]);
        }
    }

    @Override
    public void importData(@NotNull IStreamDataExporterSite site) throws DBException {
        final File file = site.getOutputFile();
        if (file == null || !file.exists()) {
            return;
        }
        try {
            wb = new SXSSFWorkbook(new XSSFWorkbook(new FileInputStream(file)));
        } catch (Exception e) {
            throw new DBException("Error opening workbook", e);
        }
    }

    @Override
    public boolean shouldTruncateOutputFileBeforeExport() {
        return true;
    }

    private int getPhysicalNumberOfRows(@NotNull Sheet sheet) {
        return wb.getXSSFWorkbook().getSheetAt(wb.getSheetIndex(sheet)).getPhysicalNumberOfRows();
    }

    private String getPreparedString(String cellValue) {
        if (cellValue.length() > EXCEL_MAX_CELL_CHARACTERS) {
            // We must truncate long strings from our side, otherwise we get the error of the insertion from the apache.poi library
            log.warn("The string value of the row " + (rowCount + 1) + " was more maximum length, so it was cropped.");
            return CommonUtils.truncateString(cellValue, EXCEL_MAX_CELL_CHARACTERS);
        }
        return cellValue;
    }

    @NotNull
    private CellStyle getCellStyle(@NotNull DBDAttributeBinding attribute, int row) {
        if (decorator != null) {
            final String bg = decorator.getCellBackground(attribute, row);

            if (bg != null) {
                // Setting the foreground color sets the background color. Is this a bug/feature of POI?
                final XSSFCellStyle style = (XSSFCellStyle) this.style.clone();
                style.setFillForegroundColor(new XSSFColor(asColor(bg), new DefaultIndexedColorMap()));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                return style;
            }
        }

        return style;
    }

    /**
     * A reimplementation of {@link org.eclipse.jface.resource.StringConverter#asRGB(String)}.
     * <p>
     * Let's keep it here until it will be required in other places too
     */
    @NotNull
    private static Color asColor(@NotNull String value) {
        final StringTokenizer tokenizer = new StringTokenizer(value, ",");
        final int r = Integer.parseInt(tokenizer.nextToken().trim());
        final int g = Integer.parseInt(tokenizer.nextToken().trim());
        final int b = Integer.parseInt(tokenizer.nextToken().trim());
        return new Color(r, g, b);
    }

    private enum AppendStrategy {
        CREATE_NEW_SHEETS("create new sheets"),
        USE_EXISTING_SHEETS("use existing sheets");

        private final String value;

        AppendStrategy(@NotNull String value) {
            this.value = value;
        }

        @NotNull
        public static AppendStrategy of(@NotNull String value) {
            for (AppendStrategy strategy : values()) {
                if (strategy.value.equals(value)) {
                    return strategy;
                }
            }

            return CREATE_NEW_SHEETS;
        }
    }

    private enum HeaderFormat {
        LABEL("label"),
        DESCRIPTION("description"),
        BOTH("both"),
        NONE("none");

        private final String value;

        HeaderFormat(@NotNull String value) {
            this.value = value;
        }

        @NotNull
        public static HeaderFormat of(@NotNull String value) {
            for (HeaderFormat format : values()) {
                if (format.value.equals(value)) {
                    return format;
                }
            }

            return LABEL;
        }

        public boolean hasLabel() {
            return this == LABEL || this == BOTH;
        }

        public boolean hasDescription() {
            return this == DESCRIPTION || this == BOTH;
        }
    }
}
