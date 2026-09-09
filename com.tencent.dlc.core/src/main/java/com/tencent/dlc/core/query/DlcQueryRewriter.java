/*
 * Tencent DLC Core
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
package com.tencent.dlc.core.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites SQL statements that the Tencent DLC JDBC driver does not return
 * results for into equivalent queries that do work.
 *
 * <p>Known driver limitation: {@code DESCRIBE TABLE db.table} (and related
 * DDL) executes on the server but the driver returns a {@code ResultSet} with
 * {@code null} metadata and zero rows. This rewriter substitutes a query
 * against {@code information_schema.columns}, which returns the expected
 * column list.</p>
 *
 * <p>The rewrite is conservative: it only matches simple {@code DESCRIBE}
 * and {@code DESC} statements against a single table, optionally qualified
 * as {@code schema.table} or {@code catalog.schema.table}. Anything more
 * complex is passed through unchanged.</p>
 */
public final class DlcQueryRewriter {

    private static final Pattern DESCRIBE_PATTERN = Pattern.compile(
        "^\\s*(?:DESCRIBE|DESC)\\s+(?:TABLE\\s+)?(?:(?:EXTENDED|FORMATTED)\\s+)?(?:`?([^`\\.]+)`?\\.)?(?:`?([^`\\.]+)`?\\.)?(?:`?([^`\\s]+)`?)\\s*;?\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private DlcQueryRewriter() {
        // utility class
    }

    /**
     * Returns a rewritten SQL string if the input matches a known workaround
     * pattern; otherwise returns the original SQL unchanged.
     *
     * @param sql the SQL statement to rewrite
     * @return the rewritten SQL, or the original if no rewrite applies
     */
    public static String rewrite(String sql) {
        if (sql == null) {
            return null;
        }

        Matcher matcher = DESCRIBE_PATTERN.matcher(sql);
        if (matcher.matches()) {
            String first = matcher.group(1);
            String schema = matcher.group(2);
            String table = matcher.group(3);
            String catalog = null;
            if (schema == null || schema.isEmpty()) {
                // Two-part (or unqualified) name: the first part is the schema, not a catalog.
                schema = first;
            } else {
                catalog = first;
            }
            StringBuilder where = new StringBuilder();
            if (catalog != null && !catalog.isEmpty()) {
                where.append("catalog_name = '").append(escape(catalog)).append("' AND ");
            }
            if (schema == null || schema.isEmpty()) {
                schema = "current_database()";
            } else {
                schema = "'" + escape(schema) + "'";
            }
            where.append("schema_name = ").append(schema)
                .append(" AND table_name = '").append(escape(table)).append("'");
            return "SELECT column_name AS col_name, column_type AS data_type, column_comment AS comment "
                + "FROM information_schema.columns "
                + "WHERE " + where + " "
                + "ORDER BY column_position";
        }

        return sql;
    }

    private static String escape(String value) {
        return value.replace("'", "''");
    }
}
