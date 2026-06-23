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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads and persists CLI defaults from a properties file.
 *
 * <p>The default location is {@code ~/.tdlc/config.properties}. Profiles are supported
 * via {@code [profile:<name>]} sections. Values in the active profile override the default
 * section; command-line flags override both.</p>
 *
 * <p>On POSIX systems the file is created with owner-read/write-only permissions (0600)
 * because it may contain secrets.</p>
 */
public class ConfigManager {

    public static final String CONFIG_DIR_NAME = ".tdlc";
    public static final String CONFIG_FILE_NAME = "config.properties";
    public static final String DEFAULT_PROFILE = "default";
    public static final String PROFILE_PREFIX = "profile:";

    private static final Set<PosixFilePermission> SECRET_FILE_PERMISSIONS =
        PosixFilePermissions.fromString("rw-------");

    private static final Map<String, String> KEY_ALIASES = new HashMap<>();

    static {
        KEY_ALIASES.put("secret-id", "secretId");
        KEY_ALIASES.put("secret-id".replace("-", "_"), "secretId");
        KEY_ALIASES.put("secret-key", "secretKey");
        KEY_ALIASES.put("secret-key".replace("-", "_"), "secretKey");
        KEY_ALIASES.put("endpoint", "host");
        KEY_ALIASES.put("engine", "dataEngineName");
        KEY_ALIASES.put("data-engine-name", "dataEngineName");
        KEY_ALIASES.put("data-engine-name".replace("-", "_"), "dataEngineName");
        KEY_ALIASES.put("datasource", "datasourceConnectionName");
        KEY_ALIASES.put("datasource-connection-name", "datasourceConnectionName");
        KEY_ALIASES.put("datasource-connection-name".replace("-", "_"), "datasourceConnectionName");
        KEY_ALIASES.put("task-type", "taskType");
        KEY_ALIASES.put("task-type".replace("-", "_"), "taskType");
        KEY_ALIASES.put("result-type", "resultType");
        KEY_ALIASES.put("result-type".replace("-", "_"), "resultType");
        KEY_ALIASES.put("read-type", "readType");
        KEY_ALIASES.put("read-type".replace("-", "_"), "readType");
        KEY_ALIASES.put("output-format", "outputFormat");
        KEY_ALIASES.put("output-format".replace("-", "_"), "outputFormat");
    }

    private final Path configPath;

    public ConfigManager() {
        this(defaultConfigPath());
    }

    public ConfigManager(Path configPath) {
        this.configPath = configPath;
    }

    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Loads the default configuration from disk.
     */
    public CliConfig load() throws IOException {
        return load(null);
    }

    /**
     * Loads configuration from disk, applying the active profile on top of the default section.
     * A {@code null} or empty profile name loads only the default section.
     */
    public CliConfig load(String activeProfile) throws IOException {
        CliConfig config = new CliConfig();
        if (!Files.exists(configPath)) {
            return config;
        }

        ParsedConfig parsed = parseConfig();
        applyProperties(config, parsed.defaultProperties);

        if (activeProfile != null && !activeProfile.isEmpty()
            && !activeProfile.equals(DEFAULT_PROFILE)) {
            Map<String, String> profileProperties = parsed.profiles.get(activeProfile);
            if (profileProperties != null) {
                applyProperties(config, profileProperties);
            }
        }

        return config;
    }

    /**
     * Persists the given default configuration to disk, preserving any existing profiles.
     */
    public void save(CliConfig config) throws IOException {
        save(config, null);
    }

    /**
     * Persists configuration for the given profile. A {@code null} or empty profile
     * updates the default section.
     */
    public void save(CliConfig config, String profile) throws IOException {
        ensureParentDirectoryExists();

        ParsedConfig parsed = Files.exists(configPath) ? parseConfig() : new ParsedConfig();
        Map<String, String> target = (profile == null || profile.isEmpty() || profile.equals(DEFAULT_PROFILE))
            ? parsed.defaultProperties
            : parsed.profiles.computeIfAbsent(profile, k -> new LinkedHashMap<>());

        putConfig(target, config);

        writeConfig(parsed);
        restrictPermissions();
    }

    /**
     * Removes a key from the default section or a named profile.
     */
    public void unset(String key, String profile) throws IOException {
        ensureParentDirectoryExists();

        ParsedConfig parsed = Files.exists(configPath) ? parseConfig() : new ParsedConfig();
        Map<String, String> target = (profile == null || profile.isEmpty() || profile.equals(DEFAULT_PROFILE))
            ? parsed.defaultProperties
            : parsed.profiles.get(profile);

        if (target != null) {
            target.remove(key);
        }

        writeConfig(parsed);
        restrictPermissions();
    }

    /**
     * Merges command-line overrides on top of a loaded config. Empty CLI values are ignored.
     */
    public CliConfig merge(CliConfig base, CliConfig overrides) {
        CliConfig merged = new CliConfig();
        merged.setSecretId(overridden(base.getSecretId(), overrides.getSecretId()));
        merged.setSecretKey(overridden(base.getSecretKey(), overrides.getSecretKey()));
        merged.setToken(overridden(base.getToken(), overrides.getToken()));
        merged.setHost(overridden(base.getHost(), overrides.getHost()));
        merged.setRegion(overridden(base.getRegion(), overrides.getRegion()));
        merged.setTaskType(overridden(base.getTaskType(), overrides.getTaskType()));
        merged.setDataEngineName(overridden(base.getDataEngineName(), overrides.getDataEngineName()));
        merged.setDatasourceConnectionName(overridden(base.getDatasourceConnectionName(), overrides.getDatasourceConnectionName()));
        merged.setResultType(overridden(base.getResultType(), overrides.getResultType()));
        merged.setReadType(overridden(base.getReadType(), overrides.getReadType()));
        merged.setOutputFormat(overridden(base.getOutputFormat(), overrides.getOutputFormat()));
        merged.getParameters().putAll(base.getParameters());
        merged.getParameters().putAll(overrides.getParameters());
        return merged;
    }

    /**
     * Normalizes a user-supplied config key (e.g. {@code secret-id}, {@code secret_id})
     * to the camelCase property key.
     */
    public static String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        String normalized = KEY_ALIASES.get(key.toLowerCase());
        return normalized != null ? normalized : key;
    }

    /**
     * Parses a key that may be prefixed with a profile, e.g. {@code profile.prod.region}.
     * Returns an array where index 0 is the profile name (or null) and index 1 is the key.
     */
    public static String[] parseProfileKey(String input) {
        if (input == null) {
            return new String[]{null, null};
        }
        String normalized = input.trim();
        if (normalized.regionMatches(true, 0, PROFILE_PREFIX, 0, PROFILE_PREFIX.length() - 1)
            && normalized.length() > PROFILE_PREFIX.length()
            && normalized.charAt(PROFILE_PREFIX.length() - 1) == '.') {
            String remainder = normalized.substring(PROFILE_PREFIX.length());
            int dotIndex = remainder.indexOf('.');
            if (dotIndex > 0) {
                String profile = remainder.substring(0, dotIndex);
                String key = remainder.substring(dotIndex + 1);
                return new String[]{profile, normalizeKey(key)};
            }
        }
        return new String[]{null, normalizeKey(normalized)};
    }

    private ParsedConfig parseConfig() throws IOException {
        ParsedConfig parsed = new ParsedConfig();
        Map<String, String> current = parsed.defaultProperties;

        try (InputStream input = Files.newInputStream(configPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    String section = trimmed.substring(1, trimmed.length() - 1);
                    if (section.regionMatches(true, 0, PROFILE_PREFIX, 0, PROFILE_PREFIX.length() - 1)
                        && section.charAt(PROFILE_PREFIX.length() - 1) == ':') {
                        String profileName = section.substring(PROFILE_PREFIX.length());
                        current = parsed.profiles.computeIfAbsent(profileName, k -> new LinkedHashMap<>());
                    } else {
                        current = parsed.defaultProperties;
                    }
                    continue;
                }
                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex > 0) {
                    String key = normalizeKey(trimmed.substring(0, equalsIndex).trim());
                    String value = trimmed.substring(equalsIndex + 1).trim();
                    current.put(key, value);
                }
            }
        }

        return parsed;
    }

    private void writeConfig(ParsedConfig parsed) throws IOException {
        try (OutputStream output = Files.newOutputStream(configPath)) {
            output.write("# Tencent DLC CLI configuration\n".getBytes(StandardCharsets.UTF_8));

            for (Map.Entry<String, String> entry : parsed.defaultProperties.entrySet()) {
                output.write((entry.getKey() + "=" + entry.getValue() + "\n").getBytes(StandardCharsets.UTF_8));
            }

            for (Map.Entry<String, Map<String, String>> profile : parsed.profiles.entrySet()) {
                output.write(("\n[profile:" + profile.getKey() + "]\n").getBytes(StandardCharsets.UTF_8));
                for (Map.Entry<String, String> entry : profile.getValue().entrySet()) {
                    output.write((entry.getKey() + "=" + entry.getValue() + "\n").getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    private static void applyProperties(CliConfig config, Map<String, String> properties) {
        if (properties.containsKey("secretId")) config.setSecretId(properties.get("secretId"));
        if (properties.containsKey("secretKey")) config.setSecretKey(properties.get("secretKey"));
        if (properties.containsKey("token")) config.setToken(properties.get("token"));
        if (properties.containsKey("host")) config.setHost(properties.get("host"));
        if (properties.containsKey("region")) config.setRegion(properties.get("region"));
        if (properties.containsKey("taskType")) config.setTaskType(properties.get("taskType"));
        if (properties.containsKey("engine") || properties.containsKey("dataEngineName")) {
            config.setDataEngineName(properties.getOrDefault("engine", properties.get("dataEngineName")));
        }
        if (properties.containsKey("datasource") || properties.containsKey("datasourceConnectionName")) {
            config.setDatasourceConnectionName(properties.getOrDefault("datasource", properties.get("datasourceConnectionName")));
        }
        if (properties.containsKey("resultType")) config.setResultType(properties.get("resultType"));
        if (properties.containsKey("readType")) config.setReadType(properties.get("readType"));
        if (properties.containsKey("outputFormat")) config.setOutputFormat(properties.get("outputFormat"));
    }

    private static void putConfig(Map<String, String> target, CliConfig config) {
        putIfNotEmpty(target, "secretId", config.getSecretId());
        putIfNotEmpty(target, "secretKey", config.getSecretKey());
        putIfNotEmpty(target, "token", config.getToken());
        putIfNotEmpty(target, "host", config.getHost());
        putIfNotEmpty(target, "region", config.getRegion());
        putIfNotEmpty(target, "taskType", config.getTaskType());
        putIfNotEmpty(target, "engine", config.getDataEngineName());
        putIfNotEmpty(target, "datasource", config.getDatasourceConnectionName());
        putIfNotEmpty(target, "resultType", config.getResultType());
        putIfNotEmpty(target, "readType", config.getReadType());
        putIfNotEmpty(target, "outputFormat", config.getOutputFormat());
    }

    private static Path defaultConfigPath() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isEmpty()) {
            userHome = ".";
        }
        return Paths.get(userHome, CONFIG_DIR_NAME, CONFIG_FILE_NAME);
    }

    private void ensureParentDirectoryExists() throws IOException {
        Path parent = configPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private void restrictPermissions() {
        try {
            if (Files.getFileStore(configPath).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(configPath, SECRET_FILE_PERMISSIONS);
            }
        } catch (IOException e) {
            // Best-effort: on non-POSIX systems this is expected to fail.
        }
    }

    private static void putIfNotEmpty(Map<String, String> target, String key, String value) {
        if (value != null && !value.isEmpty()) {
            target.put(key, value);
        }
    }

    private static String overridden(String base, String override) {
        return (override != null && !override.isEmpty()) ? override : base;
    }

    private static class ParsedConfig {
        final Map<String, String> defaultProperties = new LinkedHashMap<>();
        final Map<String, Map<String, String>> profiles = new LinkedHashMap<>();
    }
}
