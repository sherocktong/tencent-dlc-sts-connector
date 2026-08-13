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
package com.tencent.dlc.core;

/**
 * Tencent DLC constants.
 *
 * <p>Shared between the DBeaver plugin and the standalone CLI.</p>
 */
public final class TencentDLCConstants {

    private TencentDLCConstants() {
        // utility class
    }

    public static final String PROVIDER_ID = "tencent_dlc";
    public static final String DRIVER_ID = "tencent_dlc_spark";

    // Provider properties
    public static final String PROP_REGION = "region";
    public static final String PROP_TASK_TYPE = "task_type";
    public static final String PROP_DATA_ENGINE_NAME = "data_engine_name";
    public static final String PROP_DATASOURCE_CONNECTION_NAME = "datasource_connection_name";
    public static final String PROP_RESULT_TYPE = "result_type";
    public static final String PROP_READ_TYPE = "read_type";
    public static final String PROP_TIMEZONE = "timezone";

    // Auth properties
    public static final String PROP_AUTH_TOKEN = "token";

    // JDBC URL prefix
    public static final String JDBC_URL_PREFIX = "jdbc:dlc:";

    // Default values
    public static final String DEFAULT_HOST = "dlc.tencentcloudapi.com";
    public static final String DEFAULT_REGION = "ap-singapore";
    public static final String DEFAULT_TASK_TYPE = "SparkSQLTask";
    public static final String DEFAULT_DATASOURCE_CONNECTION_NAME = "DataLakeCatalog";
    public static final String DEFAULT_RESULT_TYPE = "COS";
    public static final String DEFAULT_READ_TYPE = "Stream";

    // Driver class
    public static final String DRIVER_CLASS_NAME = "com.tencent.cloud.dlc.jdbc.DlcDriver";
}
