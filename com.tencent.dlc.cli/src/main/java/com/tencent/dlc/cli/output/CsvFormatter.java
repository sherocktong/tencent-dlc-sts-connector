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
 * Formats a result set as RFC 4180-style CSV.
 */
public class CsvFormatter implements ResultFormatter {

    private static final char DELIMITER = ',';
    private static final char QUOTE = '"';

    @Override
    public void format(ResultSet resultSet, PrintWriter output, int maxRows) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        if (metaData == null) {
            printRowsWithoutMetaData(resultSet, output, maxRows);
            return;
        }

        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                output.print(DELIMITER);
            }
            output.print(escape(metaData.getColumnLabel(i)));
        }
        output.println();

        int rowCount = 0;
        while (resultSet.next() && (maxRows < 0 || rowCount < maxRows)) {
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    output.print(DELIMITER);
                }
                Object value = resultSet.getObject(i);
                output.print(value == null ? "" : escape(value.toString()));
            }
            output.println();
            rowCount++;
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains("\"") || value.contains(",") || value.contains("\n") || value.contains("\r")) {
            return QUOTE + value.replace("\"", "\"\"") + QUOTE;
        }
        return value;
    }

    private static void printRowsWithoutMetaData(ResultSet resultSet, PrintWriter output, int maxRows) throws SQLException {
        int rowCount = 0;
        while (resultSet.next() && (maxRows < 0 || rowCount < maxRows)) {
            Object value = resultSet.getObject(1);
            output.println(value == null ? "" : escape(value.toString()));
            rowCount++;
        }
    }
}
