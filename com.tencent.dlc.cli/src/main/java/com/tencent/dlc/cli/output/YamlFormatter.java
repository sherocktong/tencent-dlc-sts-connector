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

/**
 * Formats a result set as a YAML list of maps.
 *
 * <p>This is a lightweight implementation that does not require an external YAML
 * library. Values are quoted when they contain YAML-sensitive characters.</p>
 */
public class YamlFormatter implements ResultFormatter {

    @Override
    public void format(ResultSet resultSet, PrintWriter output, int maxRows) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        if (metaData == null) {
            printRowsWithoutMetaData(resultSet, output, maxRows);
            return;
        }

        int columnCount = metaData.getColumnCount();

        String[] labels = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            labels[i] = metaData.getColumnLabel(i + 1);
        }

        int rowCount = 0;
        while (resultSet.next() && (maxRows < 0 || rowCount < maxRows)) {
            output.println("-");
            for (int i = 0; i < columnCount; i++) {
                Object value = resultSet.getObject(i + 1);
                output.print("  ");
                output.print(labels[i]);
                output.print(": ");
                output.println(formatValue(value));
            }
            rowCount++;
        }

        if (rowCount == 0) {
            output.println("[]");
        }
        output.println("# " + rowCount + " row(s)");
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        String text = value.toString();
        if (needsQuoting(text)) {
            return "\"" + escape(text) + "\"";
        }
        return text;
    }

    private static boolean needsQuoting(String value) {
        if (value.isEmpty()) {
            return true;
        }
        char first = value.charAt(0);
        if (first == '[' || first == '{' || first == '*' || first == '&' || first == '!' || first == '|' || first == '>') {
            return true;
        }
        if (value.contains(":") || value.contains("#") || value.contains("\"")
            || value.contains("'") || value.contains("\n") || value.contains("\r")
            || value.contains("\t")) {
            return true;
        }
        return false;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private static void printRowsWithoutMetaData(ResultSet resultSet, PrintWriter output, int maxRows) throws SQLException {
        int rowCount = 0;
        while (resultSet.next() && (maxRows < 0 || rowCount < maxRows)) {
            Object value = resultSet.getObject(1);
            output.println("- " + formatValue(value));
            rowCount++;
        }
        if (rowCount == 0) {
            output.println("[]");
        }
        output.println("# " + rowCount + " row(s)");
    }
}
