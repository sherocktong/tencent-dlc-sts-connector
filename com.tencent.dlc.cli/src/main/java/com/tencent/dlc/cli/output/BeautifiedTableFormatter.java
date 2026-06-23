/*
 * Tencent DLC CLI
 * Copyright (C) 2026 Bing Tong and contributors
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
package com.tencent.dlc.cli.output;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pretty-prints a result set as a compact, single-line-per-row table.
 *
 * <p>Each row is printed on exactly one line; newline and carriage-return
 * characters inside cell values are replaced with a single space so the output
 * never contains embedded return lines.</p>
 */
public class BeautifiedTableFormatter implements ResultFormatter {

    private static final String NULL_VALUE = "NULL";
    private static final String COLUMN_SEPARATOR = " | ";

    @Override
    public void format(ResultSet resultSet, PrintWriter output, int maxRows) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        if (metaData == null) {
            printRowsWithoutMetaData(resultSet, output, maxRows);
            return;
        }

        int columnCount = metaData.getColumnCount();

        List<String> headers = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            headers.add(metaData.getColumnLabel(i));
        }

        List<List<String>> rows = new ArrayList<>();
        int rowCount = 0;
        while (resultSet.next() && (maxRows < 0 || rowCount < maxRows)) {
            List<String> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(formatValue(resultSet.getObject(i)));
            }
            rows.add(row);
            rowCount++;
        }

        int[] widths = computeWidths(headers, rows);
        printRow(output, headers, widths);
        for (List<String> row : rows) {
            printRow(output, row, widths);
        }
        if (rows.isEmpty()) {
            output.println("(no rows)");
        }
        output.println(rowCount + " row(s)");
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return NULL_VALUE;
        }
        return sanitize(value.toString());
    }

    private static String sanitize(String value) {
        return value.replace("\r\n", " ")
            .replace("\r", " ")
            .replace("\n", " ");
    }

    private static int[] computeWidths(List<String> headers, List<List<String>> rows) {
        int[] widths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            widths[i] = headers.get(i).length();
        }
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                widths[i] = Math.max(widths[i], row.get(i).length());
            }
        }
        return widths;
    }

    private static void printRow(PrintWriter output, List<String> values, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                line.append(COLUMN_SEPARATOR);
            }
            line.append(padRight(values.get(i), widths[i]));
        }
        output.println(line);
    }

    private static String padRight(String value, int width) {
        if (value.length() >= width) {
            return value;
        }
        return value + " ".repeat(width - value.length());
    }

    private static void printRowsWithoutMetaData(ResultSet resultSet, PrintWriter output, int maxRows) throws SQLException {
        int rowCount = 0;
        while (resultSet.next() && (maxRows < 0 || rowCount < maxRows)) {
            output.println(formatValue(resultSet.getObject(1)));
            rowCount++;
        }
        if (rowCount == 0) {
            output.println("(no rows)");
        }
        output.println(rowCount + " row(s)");
    }
}
