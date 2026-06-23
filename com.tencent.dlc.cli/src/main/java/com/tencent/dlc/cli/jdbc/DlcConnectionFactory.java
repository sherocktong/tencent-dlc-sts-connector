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
package com.tencent.dlc.cli.jdbc;

import com.tencent.dlc.cli.config.CliConfig;
import com.tencent.dlc.core.DlcUrlBuilder;
import com.tencent.dlc.core.TencentDLCConstants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Opens JDBC connections to Tencent DLC using the bundled DLC driver.
 */
public class DlcConnectionFactory {

    static {
        try {
            Class.forName(TencentDLCConstants.DRIVER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("DLC JDBC driver not found on classpath: " + TencentDLCConstants.DRIVER_CLASS_NAME, e);
        }
    }

    /**
     * Builds a JDBC URL and opens a connection with the provided credentials.
     */
    public Connection open(CliConfig config) throws SQLException {
        String url = buildUrl(config);
        Properties properties = new Properties();

        if (!isEmpty(config.getSecretId())) {
            properties.put("user", config.getSecretId());
        }
        if (!isEmpty(config.getSecretKey())) {
            properties.put("password", config.getSecretKey());
        }
        if (!isEmpty(config.getToken())) {
            properties.put("token", config.getToken().trim());
        }

        return DriverManager.getConnection(url, properties);
    }

    public static String buildUrl(CliConfig config) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put(TencentDLCConstants.PROP_TASK_TYPE, config.getTaskType());
        params.put(TencentDLCConstants.PROP_REGION, config.getRegion());
        params.put(TencentDLCConstants.PROP_DATA_ENGINE_NAME, config.getDataEngineName());
        params.put(TencentDLCConstants.PROP_DATASOURCE_CONNECTION_NAME, config.getDatasourceConnectionName());
        params.put(TencentDLCConstants.PROP_RESULT_TYPE, config.getResultType());
        params.put(TencentDLCConstants.PROP_READ_TYPE, config.getReadType());
        return DlcUrlBuilder.buildUrl(config.getHost(), params);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
