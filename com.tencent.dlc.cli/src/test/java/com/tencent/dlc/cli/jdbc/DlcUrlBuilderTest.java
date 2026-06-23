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

import com.tencent.dlc.core.DlcUrlBuilder;
import com.tencent.dlc.core.TencentDLCConstants;
import com.tencent.dlc.cli.config.CliConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DlcUrlBuilderTest {

    @Test
    void buildUrlWithDefaults() {
        CliConfig config = new CliConfig();

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put(TencentDLCConstants.PROP_TASK_TYPE, config.getTaskType());
        params.put(TencentDLCConstants.PROP_REGION, config.getRegion());
        params.put(TencentDLCConstants.PROP_DATA_ENGINE_NAME, config.getDataEngineName());
        params.put(TencentDLCConstants.PROP_DATASOURCE_CONNECTION_NAME, config.getDatasourceConnectionName());
        params.put(TencentDLCConstants.PROP_RESULT_TYPE, config.getResultType());
        params.put(TencentDLCConstants.PROP_READ_TYPE, config.getReadType());

        String url = DlcUrlBuilder.buildUrl(config.getHost(), params);

        assertEquals(TencentDLCConstants.JDBC_URL_PREFIX + TencentDLCConstants.DEFAULT_HOST, url);
    }

    @Test
    void buildUrlWithAllParameters() {
        CliConfig config = new CliConfig();
        config.setHost("dlc.tencentcloudapi.com");
        config.setTaskType("SparkSQLTask");
        config.setRegion("ap-beijing");
        config.setDataEngineName("my-engine");
        config.setDatasourceConnectionName("DataLakeCatalog");
        config.setResultType("COS");
        config.setReadType("Stream");

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put(TencentDLCConstants.PROP_TASK_TYPE, config.getTaskType());
        params.put(TencentDLCConstants.PROP_REGION, config.getRegion());
        params.put(TencentDLCConstants.PROP_DATA_ENGINE_NAME, config.getDataEngineName());
        params.put(TencentDLCConstants.PROP_DATASOURCE_CONNECTION_NAME, config.getDatasourceConnectionName());
        params.put(TencentDLCConstants.PROP_RESULT_TYPE, config.getResultType());
        params.put(TencentDLCConstants.PROP_READ_TYPE, config.getReadType());

        String url = DlcUrlBuilder.buildUrl(config.getHost(), params);

        assertEquals(
            "jdbc:dlc:dlc.tencentcloudapi.com?task_type=SparkSQLTask&region=ap-beijing" +
                "&data_engine_name=my-engine&datasource_connection_name=DataLakeCatalog" +
                "&result_type=COS&read_type=Stream",
            url
        );
    }

    @Test
    void emptyValuesAreOmitted() {
        CliConfig config = new CliConfig();
        config.setRegion("ap-shanghai");

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put(TencentDLCConstants.PROP_TASK_TYPE, config.getTaskType());
        params.put(TencentDLCConstants.PROP_REGION, config.getRegion());
        params.put(TencentDLCConstants.PROP_DATA_ENGINE_NAME, config.getDataEngineName());
        params.put(TencentDLCConstants.PROP_DATASOURCE_CONNECTION_NAME, config.getDatasourceConnectionName());
        params.put(TencentDLCConstants.PROP_RESULT_TYPE, config.getResultType());
        params.put(TencentDLCConstants.PROP_READ_TYPE, config.getReadType());

        String url = DlcUrlBuilder.buildUrl(config.getHost(), params);

        assertEquals("jdbc:dlc:dlc.tencentcloudapi.com?region=ap-shanghai", url);
    }
}
