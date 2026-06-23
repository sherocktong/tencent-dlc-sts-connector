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
import java.sql.SQLException;

/**
 * Formats a JDBC {@link ResultSet} for console output.
 */
public interface ResultFormatter {

    /**
     * Writes the result set to the given writer.
     *
     * @param resultSet the result set to format
     * @param output    the destination writer
     * @param maxRows   maximum number of rows to print; negative means unlimited
     * @throws SQLException if reading the result set fails
     */
    void format(ResultSet resultSet, PrintWriter output, int maxRows) throws SQLException;
}
