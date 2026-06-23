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
package com.tencent.dlc.cli.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds all connection and authentication settings for a DLC CLI session.
 *
 * <p>Values are populated from the config file and then overridden by command-line
 * arguments. Empty strings are treated as "not set" by the URL builder.</p>
 */
public class CliConfig {

    private String secretId = "";
    private String secretKey = "";
    private String token = "";

    private String host = "";
    private String region = "";
    private String taskType = "";
    private String dataEngineName = "";
    private String datasourceConnectionName = "";
    private String resultType = "";
    private String readType = "";
    private String outputFormat = "";

    private final Map<String, String> parameters = new HashMap<>();

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = nonNull(secretId);
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = nonNull(secretKey);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = nonNull(token);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = nonNull(host);
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = nonNull(region);
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = nonNull(taskType);
    }

    public String getDataEngineName() {
        return dataEngineName;
    }

    public void setDataEngineName(String dataEngineName) {
        this.dataEngineName = nonNull(dataEngineName);
    }

    public String getDatasourceConnectionName() {
        return datasourceConnectionName;
    }

    public void setDatasourceConnectionName(String datasourceConnectionName) {
        this.datasourceConnectionName = nonNull(datasourceConnectionName);
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = nonNull(resultType);
    }

    public String getReadType() {
        return readType;
    }

    public void setReadType(String readType) {
        this.readType = nonNull(readType);
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = nonNull(outputFormat);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameter(String key, String value) {
        this.parameters.put(key, nonNull(value));
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}
