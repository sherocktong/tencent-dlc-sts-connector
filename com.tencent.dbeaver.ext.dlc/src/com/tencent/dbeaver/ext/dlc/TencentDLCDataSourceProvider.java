/*
 * Tencent DLC DBeaver Plugin
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
package com.tencent.dbeaver.ext.dlc;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Tencent DLC data source provider.
 */
public class TencentDLCDataSourceProvider extends GenericDataSourceProvider {

    public TencentDLCDataSourceProvider() {
        super();
    }

    @NotNull
    @Override
    public TencentDLCDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container
    ) throws DBException {
        return new TencentDLCDataSource(monitor, container, new TencentDLCMetaModel());
    }

    @NotNull
    @Override
    public String getConnectionURL(@NotNull DBPDriver driver, @NotNull DBPConnectionConfiguration connectionInfo) {
        // If user provided a raw URL, use it directly
        if (!CommonUtils.isEmpty(connectionInfo.getUrl()) &&
            CommonUtils.isEmpty(connectionInfo.getHostName()) &&
            CommonUtils.isEmpty(connectionInfo.getHostPort()) &&
            CommonUtils.isEmpty(connectionInfo.getServerName()) &&
            CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            return connectionInfo.getUrl();
        }

        String host = connectionInfo.getHostName();
        if (CommonUtils.isEmpty(host)) {
            host = TencentDLCConstants.DEFAULT_HOST;
        }

        StringBuilder url = new StringBuilder();
        url.append(TencentDLCConstants.JDBC_URL_PREFIX).append(host);

        List<String> params = new ArrayList<>();
        addParam(params, connectionInfo, TencentDLCConstants.PROP_TASK_TYPE);
        addParam(params, connectionInfo, TencentDLCConstants.PROP_REGION);
        addParam(params, connectionInfo, TencentDLCConstants.PROP_DATA_ENGINE_NAME);
        addParam(params, connectionInfo, TencentDLCConstants.PROP_DATASOURCE_CONNECTION_NAME);
        addParam(params, connectionInfo, TencentDLCConstants.PROP_RESULT_TYPE);
        addParam(params, connectionInfo, TencentDLCConstants.PROP_READ_TYPE);

        for (int i = 0; i < params.size(); i++) {
            url.append(i == 0 ? '?' : '&').append(params.get(i));
        }

        return url.toString();
    }

    private static void addParam(List<String> params, DBPConnectionConfiguration connectionInfo, String key) {
        String value = connectionInfo.getProviderProperty(key);
        if (CommonUtils.isEmpty(value)) {
            value = connectionInfo.getProperty(key);
        }
        if (!CommonUtils.isEmpty(value)) {
            params.add(key + "=" + value);
        }
    }
}
