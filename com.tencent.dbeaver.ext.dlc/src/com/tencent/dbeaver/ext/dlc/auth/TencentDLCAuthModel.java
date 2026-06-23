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
package com.tencent.dbeaver.ext.dlc.auth;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import com.tencent.dlc.core.TencentDLCConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Properties;

/**
 * Tencent DLC auth model supporting SecretId, SecretKey and STS Token.
 */
public class TencentDLCAuthModel extends AuthModelDatabaseNative<TencentDLCCredentials> {

    public static final String ID = "tencent_dlc_native";

    @NotNull
    @Override
    public TencentDLCCredentials createCredentials() {
        return new TencentDLCCredentials();
    }

    @Override
    public Object initAuthentication(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull TencentDLCCredentials credentials,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull Properties connProperties
    ) throws DBException {
        if (!CommonUtils.isEmpty(credentials.getUserName())) {
            connProperties.put("user", credentials.getUserName());
        }
        if (!CommonUtils.isEmpty(credentials.getUserPassword())) {
            connProperties.put("password", credentials.getUserPassword());
        }
        if (!CommonUtils.isEmpty(credentials.getToken())) {
            connProperties.put("token", credentials.getToken().trim());
        }
        return super.initAuthentication(monitor, dataSource, credentials, configuration, connProperties);
    }

    @NotNull
    @Override
    public TencentDLCCredentials loadCredentials(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration
    ) {
        TencentDLCCredentials credentials = super.loadCredentials(dataSource, configuration);
        credentials.setToken(configuration.getAuthProperty(TencentDLCConstants.PROP_AUTH_TOKEN));
        return credentials;
    }

    @Override
    public void saveCredentials(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull TencentDLCCredentials credentials
    ) {
        configuration.setAuthProperty(TencentDLCConstants.PROP_AUTH_TOKEN, credentials.getToken());
        super.saveCredentials(dataSource, configuration, credentials);
    }
}
