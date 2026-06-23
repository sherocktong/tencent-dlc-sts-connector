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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * A minimal one-column {@link ResultSetMetaData} implementation used as a
 * fallback when the JDBC driver returns {@code null} metadata.
 *
 * <p>This is shared between the CLI result formatters and the DBeaver plugin
 * connection wrapper.</p>
 */
public final class DlcSingleColumnMetaData implements ResultSetMetaData {

    private static final String FALLBACK_COLUMN_LABEL = "result";

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public boolean isAutoIncrement(int column) {
        return false;
    }

    @Override
    public boolean isCaseSensitive(int column) {
        return true;
    }

    @Override
    public boolean isSearchable(int column) {
        return false;
    }

    @Override
    public boolean isCurrency(int column) {
        return false;
    }

    @Override
    public int isNullable(int column) {
        return columnNullableUnknown;
    }

    @Override
    public boolean isSigned(int column) {
        return false;
    }

    @Override
    public int getColumnDisplaySize(int column) {
        return 80;
    }

    @Override
    public String getColumnLabel(int column) {
        return FALLBACK_COLUMN_LABEL;
    }

    @Override
    public String getColumnName(int column) {
        return FALLBACK_COLUMN_LABEL;
    }

    @Override
    public String getSchemaName(int column) {
        return "";
    }

    @Override
    public int getPrecision(int column) {
        return 0;
    }

    @Override
    public int getScale(int column) {
        return 0;
    }

    @Override
    public String getTableName(int column) {
        return "";
    }

    @Override
    public String getCatalogName(int column) {
        return "";
    }

    @Override
    public int getColumnType(int column) {
        return java.sql.Types.VARCHAR;
    }

    @Override
    public String getColumnTypeName(int column) {
        return "VARCHAR";
    }

    @Override
    public boolean isReadOnly(int column) {
        return true;
    }

    @Override
    public boolean isWritable(int column) {
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(int column) {
        return false;
    }

    @Override
    public String getColumnClassName(int column) {
        return String.class.getName();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("No wrapper available");
    }
}
