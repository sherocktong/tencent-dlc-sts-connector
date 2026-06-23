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
package com.tencent.dlc.core.jdbc;

import com.tencent.dlc.core.TencentDLCConstants;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * JDBC {@link Driver} wrapper for the Tencent DLC driver.
 *
 * <p>This driver delegates all connection requests to the real
 * {@code com.tencent.cloud.dlc.jdbc.DlcDriver} but wraps the returned
 * {@link Connection} so that every {@link java.sql.ResultSet} exposes safe
 * metadata. This works around a driver quirk where DDL statements such as
 * {@code DESCRIBE TABLE} return {@code null} metadata, which causes
 * {@link NullPointerException} in generic JDBC consumers such as DBeaver.</p>
 *
 * <p>The wrapper is registered automatically when the class is loaded. It
 * accepts the same URL format ({@code jdbc:dlc:...}) as the underlying driver.</p>
 */
public class DlcDriverWrapper implements Driver {

    static {
        try {
            DriverManager.registerDriver(new DlcDriverWrapper());
        } catch (SQLException e) {
            throw new ExceptionInInitializerError("Failed to register DlcDriverWrapper: " + e.getMessage());
        }
    }

    private static final String REAL_DRIVER_CLASS = TencentDLCConstants.DRIVER_CLASS_NAME;
    private static volatile Driver realDriver;

    /**
     * Loads and returns the real DLC driver, caching the instance after first use.
     *
     * <p>The driver is loaded reflectively so that {@code com.tencent.dlc.core}
     * does not need a compile-time dependency on the DLC JDBC driver JAR.</p>
     */
    private Driver resolveRealDriver() throws SQLException {
        Driver driver = realDriver;
        if (driver != null) {
            return driver;
        }
        try {
            Class<?> driverClass = Class.forName(REAL_DRIVER_CLASS);
            driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            realDriver = driver;
            return driver;
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new SQLException("Failed to load real DLC driver " + REAL_DRIVER_CLASS, e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        Connection connection = resolveRealDriver().connect(url, info);
        if (connection == null) {
            return null;
        }
        return new SafeMetaDataConnection(connection);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.startsWith(TencentDLCConstants.JDBC_URL_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return resolveRealDriver().getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        try {
            return resolveRealDriver().getMajorVersion();
        } catch (SQLException e) {
            return 1;
        }
    }

    @Override
    public int getMinorVersion() {
        try {
            return resolveRealDriver().getMinorVersion();
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public boolean jdbcCompliant() {
        try {
            return resolveRealDriver().jdbcCompliant();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        try {
            return resolveRealDriver().getParentLogger();
        } catch (SQLException e) {
            throw new SQLFeatureNotSupportedException(e.getMessage(), e);
        }
    }

    /**
     * No-op helper to force class loading and driver registration.
     */
    public static void init() {
        // static initializer registers the driver
    }
}
