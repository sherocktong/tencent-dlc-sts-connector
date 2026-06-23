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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

    @Test
    void loadReturnsEmptyConfigWhenFileMissing(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("missing.properties");
        ConfigManager manager = new ConfigManager(configFile);

        CliConfig config = manager.load();

        assertEquals("", config.getSecretId());
        assertEquals("", config.getRegion());
    }

    @Test
    void saveAndLoadRoundTrip(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        ConfigManager manager = new ConfigManager(configFile);

        CliConfig config = new CliConfig();
        config.setSecretId("AKID");
        config.setSecretKey("secret");
        config.setRegion("ap-singapore");
        config.setDataEngineName("my-engine");
        manager.save(config);

        CliConfig loaded = manager.load();
        assertEquals("AKID", loaded.getSecretId());
        assertEquals("secret", loaded.getSecretKey());
        assertEquals("ap-singapore", loaded.getRegion());
        assertEquals("my-engine", loaded.getDataEngineName());

        String content = Files.readString(configFile);
        assertTrue(content.contains("secretId=AKID"));
    }

    @Test
    void mergePrefersOverrides(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        ConfigManager manager = new ConfigManager(configFile);

        CliConfig base = new CliConfig();
        base.setRegion("ap-singapore");
        base.setSecretId("AKID-base");
        manager.save(base);

        CliConfig overrides = new CliConfig();
        overrides.setRegion("ap-beijing");

        CliConfig merged = manager.merge(manager.load(), overrides);
        assertEquals("ap-beijing", merged.getRegion());
        assertEquals("AKID-base", merged.getSecretId());
    }

    @Test
    void saveAndLoadOutputFormat(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        ConfigManager manager = new ConfigManager(configFile);

        CliConfig config = new CliConfig();
        config.setOutputFormat("yaml");
        manager.save(config);

        CliConfig loaded = manager.load();
        assertEquals("yaml", loaded.getOutputFormat());

        String content = Files.readString(configFile);
        assertTrue(content.contains("outputFormat=yaml"));
    }

    @Test
    void normalizeKeySupportsOutputFormatAlias() {
        assertEquals("outputFormat", ConfigManager.normalizeKey("output-format"));
        assertEquals("outputFormat", ConfigManager.normalizeKey("output_format"));
    }

    @Test
    void loadWithProfileOverridesDefault(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, """
            region=ap-singapore
            engine=default-engine
            [profile:prod]
            region=ap-beijing
            engine=prod-engine
            """);
        ConfigManager manager = new ConfigManager(configFile);

        CliConfig defaultConfig = manager.load();
        assertEquals("ap-singapore", defaultConfig.getRegion());
        assertEquals("default-engine", defaultConfig.getDataEngineName());

        CliConfig prodConfig = manager.load("prod");
        assertEquals("ap-beijing", prodConfig.getRegion());
        assertEquals("prod-engine", prodConfig.getDataEngineName());
    }

    @Test
    void saveToProfilePreservesDefaultAndOtherProfiles(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        ConfigManager manager = new ConfigManager(configFile);

        CliConfig defaultConfig = new CliConfig();
        defaultConfig.setRegion("ap-singapore");
        manager.save(defaultConfig);

        CliConfig prodConfig = new CliConfig();
        prodConfig.setRegion("ap-beijing");
        prodConfig.setDataEngineName("prod-engine");
        manager.save(prodConfig, "prod");

        String content = Files.readString(configFile);
        assertTrue(content.contains("[profile:prod]"));
        assertTrue(content.contains("region=ap-singapore"));
        assertTrue(content.contains("region=ap-beijing"));

        CliConfig loaded = manager.load("prod");
        assertEquals("ap-beijing", loaded.getRegion());
        assertEquals("prod-engine", loaded.getDataEngineName());
    }

    @Test
    void parseProfileKeySplitsProfileAndKey() {
        String[] parsed = ConfigManager.parseProfileKey("profile.prod.region");
        assertEquals("prod", parsed[0]);
        assertEquals("region", parsed[1]);

        String[] noProfile = ConfigManager.parseProfileKey("region");
        assertNull(noProfile[0]);
        assertEquals("region", noProfile[1]);

        String[] kebab = ConfigManager.parseProfileKey("profile.prod.secret-id");
        assertEquals("prod", kebab[0]);
        assertEquals("secretId", kebab[1]);
    }

    @Test
    void unsetRemovesKeyFromProfile(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        ConfigManager manager = new ConfigManager(configFile);

        CliConfig prodConfig = new CliConfig();
        prodConfig.setRegion("ap-beijing");
        manager.save(prodConfig, "prod");

        manager.unset("region", "prod");

        CliConfig loaded = manager.load("prod");
        assertEquals("", loaded.getRegion());
    }

    @Test
    void normalizeKeySupportsAliases() {
        assertEquals("secretId", ConfigManager.normalizeKey("secret-id"));
        assertEquals("secretId", ConfigManager.normalizeKey("secret_id"));
        assertEquals("secretKey", ConfigManager.normalizeKey("secret-key"));
        assertEquals("taskType", ConfigManager.normalizeKey("task-type"));
        assertEquals("dataEngineName", ConfigManager.normalizeKey("data-engine-name"));
        assertEquals("datasourceConnectionName", ConfigManager.normalizeKey("datasource-connection-name"));
    }
}
