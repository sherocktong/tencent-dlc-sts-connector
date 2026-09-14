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
package com.tencent.dlc.core.result;

import com.tencent.dlc.core.query.DlcQueryRewriter;
import com.tencent.dlc.core.query.DlcQueryRewriter.DescribeTarget;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;

/**
 * Runtime fallback for {@code DESCRIBE} statements whose static rewrite to
 * {@code information_schema.columns} returned no rows.
 *
 * <p>DLC pins {@code information_schema} to the connection's default catalog
 * and never lists views in {@code information_schema.columns}, so the static
 * rewrite ({@code DlcQueryRewriter}) cannot describe views or objects in
 * other catalogs. This class probes the target directly with
 * {@code SELECT * FROM <name> LIMIT 0}, which resolves the schema of tables
 * and views in any catalog without scanning data, and synthesizes a
 * DESCRIBE-shaped result set ({@code col_name}, {@code data_type},
 * {@code comment}).</p>
 *
 * <p>The returned {@link CachedRowSet} is scrollable and self-contained, so
 * callers may close the underlying statement immediately. If the probe fails
 * (e.g. the object genuinely does not exist), the empty tier-one result is
 * returned instead of raising an error, preserving the pre-fallback
 * behaviour.</p>
 */
public final class DlcDescribeFallback {

    private static final String[] DESCRIBE_COLUMNS = {"col_name", "data_type", "comment"};

    private DlcDescribeFallback() {
        // utility class
    }

    /**
     * Drains the tier-one result set; if it contains no rows, probes the
     * describe target and builds a DESCRIBE-shaped result from the probe's
     * metadata.
     *
     * @param target  the parsed describe target; must not be {@code null}
     * @param connection connection used to run the probe; must not be {@code null}
     * @param tierOne the result of the {@code information_schema.columns} rewrite
     * @return a scrollable, DESCRIBE-shaped result set, never {@code null}
     * @throws SQLException if draining the tier-one result set fails
     */
    public static ResultSet materialize(DescribeTarget target, Connection connection, ResultSet tierOne)
        throws SQLException {
        List<Object[]> rows = drain(tierOne);
        if (rows.isEmpty()) {
            rows = probe(target, connection);
        }
        return toCachedRowSet(rows);
    }

    private static List<Object[]> drain(ResultSet resultSet) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        if (resultSet == null) {
            return rows;
        }
        while (resultSet.next()) {
            Object[] row = new Object[DESCRIBE_COLUMNS.length];
            for (int i = 0; i < row.length; i++) {
                row[i] = resultSet.getObject(i + 1);
            }
            rows.add(row);
        }
        return rows;
    }

    private static List<Object[]> probe(DescribeTarget target, Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(DlcQueryRewriter.describeProbeSql(target))) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            List<Object[]> rows = new ArrayList<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String typeName = metaData.getColumnTypeName(i);
                if (typeName == null || typeName.isEmpty()) {
                    typeName = JDBCType.valueOf(metaData.getColumnType(i)).getName();
                }
                rows.add(new Object[] {metaData.getColumnName(i), typeName, null});
            }
            return rows;
        } catch (SQLException e) {
            // Object does not exist or the probe is unsupported: keep the empty result.
            return new ArrayList<>();
        }
    }

    private static CachedRowSet toCachedRowSet(List<Object[]> rows) throws SQLException {
        CachedRowSet cached = RowSetProvider.newFactory().createCachedRowSet();
        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        metaData.setColumnCount(DESCRIBE_COLUMNS.length);
        for (int i = 0; i < DESCRIBE_COLUMNS.length; i++) {
            metaData.setColumnName(i + 1, DESCRIBE_COLUMNS[i]);
            metaData.setColumnLabel(i + 1, DESCRIBE_COLUMNS[i]);
            metaData.setColumnType(i + 1, Types.VARCHAR);
        }
        cached.setMetaData(metaData);
        for (Object[] row : rows) {
            cached.moveToInsertRow();
            for (int i = 0; i < row.length; i++) {
                if (row[i] == null) {
                    cached.updateNull(i + 1);
                } else {
                    cached.updateObject(i + 1, row[i]);
                }
            }
            cached.insertRow();
        }
        cached.moveToCurrentRow();
        cached.beforeFirst();
        return cached;
    }
}
