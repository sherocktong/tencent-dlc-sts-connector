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

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.tencent.dlc.core.query.DlcQueryRewriter;
import com.tencent.dlc.core.query.DlcQueryRewriter.DescribeTarget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DlcDescribeFallbackTest {

    private static final DescribeTarget TARGET =
        DlcQueryRewriter.parseDescribe("DESCRIBE cat.sch.tab");

    @Test
    void nonEmptyTierOneIsPassedThroughWithoutProbing() throws SQLException {
        List<Object[]> rows = List.of(
            new Object[] {"id", "bigint", "primary key"},
            new Object[] {"name", "string", null}
        );
        Connection failOnProbe = (Connection) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
                throw new AssertionError("probe must not run when tier one returned rows");
            });

        ResultSet result = DlcDescribeFallback.materialize(TARGET, failOnProbe, tierOne(rows));

        assertEquals(List.of(
            Arrays.asList("id", "bigint", "primary key"),
            Arrays.asList("name", "string", null)
        ), drain(result));
    }

    @Test
    void emptyTierOneFallsBackToProbeMetadata() throws SQLException {
        AtomicReference<String> probedSql = new AtomicReference<>();
        Connection connection = probeConnection(probedSql,
            probeResultSet(new String[] {"comp_item_store", "comp_item_no"},
                new String[] {"string", "decimal(20,2)"}));

        ResultSet result = DlcDescribeFallback.materialize(TARGET, connection, tierOne(List.of()));

        assertEquals("SELECT * FROM `cat`.`sch`.`tab` LIMIT 0", probedSql.get());
        assertEquals(List.of(
            Arrays.asList("comp_item_store", "string", null),
            Arrays.asList("comp_item_no", "decimal(20,2)", null)
        ), drain(result));
    }

    @Test
    void probeFailureKeepsEmptyDescribeResult() throws SQLException {
        Connection failing = (Connection) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
                if ("createStatement".equals(method.getName())) {
                    return failingStatement();
                }
                throw new UnsupportedOperationException(method.getName());
            });

        ResultSet result = DlcDescribeFallback.materialize(TARGET, failing, tierOne(List.of()));

        assertEquals(List.of(), drain(result));
        ResultSetMetaData metaData = result.getMetaData();
        assertEquals(3, metaData.getColumnCount());
        assertEquals("col_name", metaData.getColumnName(1));
        assertEquals("data_type", metaData.getColumnName(2));
        assertEquals("comment", metaData.getColumnName(3));
    }

    @Test
    void nullTierOneIsTreatedAsEmpty() throws SQLException {
        AtomicReference<String> probedSql = new AtomicReference<>();
        Connection connection = probeConnection(probedSql,
            probeResultSet(new String[] {"id"}, new String[] {"bigint"}));

        ResultSet result = DlcDescribeFallback.materialize(TARGET, connection, null);

        assertEquals(List.of(Arrays.asList("id", "bigint", null)), drain(result));
    }

    @Test
    void materializeDoesNotHideTierOneDrainFailures() {
        ResultSet broken = (ResultSet) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] {ResultSet.class},
            (proxy, method, args) -> {
                if ("next".equals(method.getName())) {
                    throw new SQLException("read failed");
                }
                throw new UnsupportedOperationException(method.getName());
            });

        assertThrows(SQLException.class,
            () -> DlcDescribeFallback.materialize(TARGET, (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, method, args) -> null), broken));
    }

    private static List<List<Object>> drain(ResultSet resultSet) throws SQLException {
        List<List<Object>> rows = new ArrayList<>();
        while (resultSet.next()) {
            rows.add(Arrays.asList(
                resultSet.getObject(1), resultSet.getObject(2), resultSet.getObject(3)));
        }
        return rows;
    }

    /** A tier-one result set yielding the given rows through getObject. */
    private static ResultSet tierOne(List<Object[]> rows) {
        Iterator<Object[]> iterator = rows.iterator();
        AtomicReference<Object[]> current = new AtomicReference<>();
        return (ResultSet) Proxy.newProxyInstance(
            DlcDescribeFallbackTest.class.getClassLoader(), new Class<?>[] {ResultSet.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "next" -> {
                    if (iterator.hasNext()) {
                        current.set(iterator.next());
                        yield true;
                    }
                    yield false;
                }
                case "getObject" -> current.get()[(Integer) args[0] - 1];
                case "close" -> null;
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    /** A probe result set exposing only metadata for the given columns. */
    private static ResultSet probeResultSet(String[] names, String[] typeNames) {
        ResultSetMetaData metaData = (ResultSetMetaData) Proxy.newProxyInstance(
            DlcDescribeFallbackTest.class.getClassLoader(), new Class<?>[] {ResultSetMetaData.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getColumnCount" -> names.length;
                case "getColumnName" -> names[(Integer) args[0] - 1];
                case "getColumnLabel" -> names[(Integer) args[0] - 1];
                case "getColumnTypeName" -> typeNames[(Integer) args[0] - 1];
                case "getColumnType" -> Types.VARCHAR;
                default -> throw new UnsupportedOperationException(method.getName());
            });
        return (ResultSet) Proxy.newProxyInstance(
            DlcDescribeFallbackTest.class.getClassLoader(), new Class<?>[] {ResultSet.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getMetaData" -> metaData;
                case "next" -> false;
                case "close" -> null;
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    /** A connection stub whose single statement runs the probe query. */
    private static Connection probeConnection(AtomicReference<String> executedSql, ResultSet probeResultSet) {
        return (Connection) Proxy.newProxyInstance(
            DlcDescribeFallbackTest.class.getClassLoader(), new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
                if ("createStatement".equals(method.getName())) {
                    return (Statement) Proxy.newProxyInstance(
                        DlcDescribeFallbackTest.class.getClassLoader(), new Class<?>[] {Statement.class},
                        (stmtProxy, stmtMethod, stmtArgs) -> switch (stmtMethod.getName()) {
                            case "executeQuery" -> {
                                executedSql.set((String) stmtArgs[0]);
                                yield probeResultSet;
                            }
                            case "close" -> null;
                            default -> throw new UnsupportedOperationException(stmtMethod.getName());
                        });
                }
                throw new UnsupportedOperationException(method.getName());
            });
    }

    private static Statement failingStatement() {
        return (Statement) Proxy.newProxyInstance(
            DlcDescribeFallbackTest.class.getClassLoader(), new Class<?>[] {Statement.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "executeQuery" -> throw new SQLException("table or view not found");
                case "close" -> null;
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }
}
