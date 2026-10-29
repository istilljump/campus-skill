package com.campus.runner.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 轻量 Excel 导出工具（POI SXSSF 流式写，导出大表不占内存）。
 * 统一表头样式：深蓝底白字加粗；值单元格按类型写入（数字用数值型，时间转字符串）。
 */
public final class ExcelWriterUtil {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ExcelWriterUtil() {
    }

    /**
     * 写一个工作表：headers 为表头，rows 为数据行（与表头一一对应）
     */
    public static SXSSFWorkbook writeSheet(SXSSFWorkbook workbook, String sheetName,
                                           List<String> headers, List<List<Object>> rows) {
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle headStyle = headStyle(workbook);
        Row headRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headStyle);
            sheet.setColumnWidth(i, 20 * 256);
        }
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            List<Object> rowData = rows.get(r);
            for (int c = 0; c < rowData.size(); c++) {
                writeCell(row.createCell(c), rowData.get(c));
            }
        }
        return workbook;
    }

    private static void writeCell(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number && !(value instanceof BigDecimal)) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(TIME_FORMATTER.format((LocalDateTime) value));
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private static CellStyle headStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
