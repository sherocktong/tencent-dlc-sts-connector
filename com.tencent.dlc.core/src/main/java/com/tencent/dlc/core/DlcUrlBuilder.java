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
 * Builds JDBC URLs for Tencent DLC from a host and parameter map.
 *
 * <p>This is shared between the DBeaver plugin and the standalone CLI. Both callers
 * map their own configuration objects to the generic {@code Map<String, String>} used
 * here.</p>
 */
public final class DlcUrlBuilder {

    private DlcUrlBuilder() {
        // utility class
    }

    /**
     * Builds a JDBC URL of the form {@code jdbc:dlc:{host}?key=value&...}.
     *
     * @param host   the DLC endpoint host; falls back to {@link TencentDLCConstants#DEFAULT_HOST} if empty
     * @param params URL query parameters; empty or null values are omitted
     * @return the constructed JDBC URL
     */
    public static String buildUrl(String host, java.util.Map<String, String> params) {
        if (host == null || host.isEmpty()) {
            host = TencentDLCConstants.DEFAULT_HOST;
        }

        StringBuilder url = new StringBuilder();
        url.append(TencentDLCConstants.JDBC_URL_PREFIX).append(host);

        boolean first = true;
        for (java.util.Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                url.append(first ? '?' : '&')
                    .append(entry.getKey()).append('=').append(value);
                first = false;
            }
        }

        return url.toString();
    }
}
