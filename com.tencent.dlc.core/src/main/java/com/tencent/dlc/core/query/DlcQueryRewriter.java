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
 *
 * <p>When a {@code defaultCatalog} is supplied, two-part names
 * ({@code schema.table}) and unqualified names ({@code table}) are resolved
 * against it so the resulting query carries an explicit
 * {@code catalog_name = ...} predicate. This matches the behaviour of the
 * DLC connection's {@code datasource_connection_name} setting and avoids
 * relying on whichever catalog happens to be current in the session.</p>
 *
 * <p>Limitation: DLC pins {@code information_schema} to the connection's
 * default catalog and does not list views in {@code information_schema.columns}
 * at all, so the rewrite alone returns no rows for other catalogs or for
 * views. Callers can combine {@link #parseDescribe(String)} with
 * {@link #describeProbeSql(DescribeTarget)} and
 * {@code com.tencent.dlc.core.result.DlcDescribeFallback} to fall back to a
 * {@code SELECT * FROM <name> LIMIT 0} probe, which resolves the schema of
 * tables and views in any catalog.</p>
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
     * A table (or view) name parsed from a simple {@code DESCRIBE} statement.
     * Any of {@code catalog} and {@code schema} may be {@code null} when the
     * original statement did not qualify them.
     */
    public static final class DescribeTarget {
        private final String catalog;
        private final String schema;
        private final String table;

        private DescribeTarget(String catalog, String schema, String table) {
            this.catalog = catalog;
            this.schema = schema;
            this.table = table;
        }

        public String getCatalog() {
            return catalog;
        }

        public String getSchema() {
            return schema;
        }

        public String getTable() {
            return table;
        }

        /**
         * Returns the name as a backtick-quoted dotted identifier, omitting
         * absent parts so resolution follows the session's current
         * catalog/schema for unqualified segments.
         */
        public String qualifiedName() {
            StringBuilder name = new StringBuilder();
            if (catalog != null && !catalog.isEmpty()) {
                name.append('`').append(escapeIdentifier(catalog)).append("`.");
            }
            if (schema != null && !schema.isEmpty()) {
                name.append('`').append(escapeIdentifier(schema)).append("`.");
            }
            name.append('`').append(escapeIdentifier(table)).append('`');
            return name.toString();
        }
    }

    /**
     * Parses a simple {@code DESCRIBE}/{@code DESC} statement and returns its
     * target, or {@code null} if the SQL does not match the supported shape.
     *
     * @param sql the SQL statement to parse
     * @return the describe target, or {@code null} when the SQL is not a
     *         simple DESCRIBE statement
     */
    public static DescribeTarget parseDescribe(String sql) {
        if (sql == null) {
            return null;
        }
        Matcher matcher = DESCRIBE_PATTERN.matcher(sql);
        if (!matcher.matches()) {
            return null;
        }
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
        return new DescribeTarget(catalog, schema, table);
    }

    /**
     * Returns the probe query used to resolve a describe target's schema when
     * the {@code information_schema.columns} rewrite yields no rows: a
     * zero-row select that works for tables and views in any catalog.
     *
     * @param target the parsed describe target
     * @return {@code SELECT * FROM <name> LIMIT 0} for the target
     */
    public static String describeProbeSql(DescribeTarget target) {
        return "SELECT * FROM " + target.qualifiedName() + " LIMIT 0";
    }

    /**
     * Returns a rewritten SQL string if the input matches a known workaround
     * pattern; otherwise returns the original SQL unchanged.
     *
     * @param sql the SQL statement to rewrite
     * @return the rewritten SQL, or the original if no rewrite applies
     */
    public static String rewrite(String sql) {
        return rewrite(sql, null);
    }

    /**
     * Returns a rewritten SQL string, falling back to {@code defaultCatalog}
     * when the statement does not name one explicitly.
     *
     * @param sql            the SQL statement to rewrite
     * @param defaultCatalog catalog name to apply when the SQL omits one; may
     *                       be {@code null} or empty to leave the SQL
     *                       catalog-unqualified
     * @return the rewritten SQL, or the original if no rewrite applies
     */
    public static String rewrite(String sql, String defaultCatalog) {
        if (sql == null) {
            return null;
        }

        DescribeTarget target = parseDescribe(sql);
        if (target != null) {
            String catalog = target.catalog;
            String schema = target.schema;
            String table = target.table;
            if ((catalog == null || catalog.isEmpty())
                && defaultCatalog != null && !defaultCatalog.isEmpty()) {
                catalog = defaultCatalog;
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

    private static String escapeIdentifier(String value) {
        return value.replace("`", "``");
    }
}
