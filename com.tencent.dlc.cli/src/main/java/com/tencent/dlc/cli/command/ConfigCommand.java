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
package com.tencent.dlc.cli.command;

import com.tencent.dlc.cli.TdlcCli;
import com.tencent.dlc.cli.config.CliConfig;
import com.tencent.dlc.cli.config.ConfigManager;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Manages persisted CLI defaults.
 */
@CommandLine.Command(
    name = "config",
    description = "Manage default configuration values",
    subcommands = {
        ConfigCommand.SetCommand.class,
        ConfigCommand.GetCommand.class,
        ConfigCommand.ListCommand.class,
        ConfigCommand.UnsetCommand.class
    }
)
public class ConfigCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private TdlcCli parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    ConfigManager createConfigManager() {
        String configPath = parent != null ? parent.getConfigPath() : null;
        if (configPath != null && !configPath.isEmpty()) {
            return new ConfigManager(Paths.get(configPath));
        }
        return new ConfigManager();
    }

    boolean isQuiet() {
        return parent != null && parent.isQuiet();
    }

    PrintWriter getOut() {
        return spec.commandLine().getOut();
    }

    String getProfile() {
        return parent != null ? parent.getProfile() : null;
    }

    String resolveProfile(String localProfile) {
        return (localProfile != null && !localProfile.isEmpty()) ? localProfile : getProfile();
    }

    @CommandLine.Command(name = "set", description = "Set a configuration value")
    static class SetCommand implements Callable<Integer> {

        @CommandLine.ParentCommand
        private ConfigCommand parent;

        @CommandLine.Option(
            names = {"-p", "--profile"},
            description = "Profile to update"
        )
        private String profile;

        @CommandLine.Parameters(index = "0", description = "Configuration key")
        private String key;

        @CommandLine.Parameters(index = "1", description = "Configuration value")
        private String value;

        @Override
        public Integer call() {
            ConfigManager manager = parent.createConfigManager();
            try {
                String[] parsed = ConfigManager.parseProfileKey(key);
                if (parsed[0] != null) {
                    profile = parsed[0];
                    key = parsed[1];
                } else {
                    key = parsed[1];
                }
                profile = parent.resolveProfile(profile);
                CliConfig config = manager.load(profile);
                apply(config, key, value);
                manager.save(config, profile);
                if (!parent.isQuiet()) {
                    String displayProfile = profile != null && !profile.isEmpty() ? " (profile: " + profile + ")" : "";
                    parent.getOut().println("Set " + key + displayProfile + " = " + maskIfSecret(key, value));
                }
                return 0;
            } catch (IOException e) {
                parent.spec.commandLine().getErr().println("Failed to save config: " + e.getMessage());
                return 1;
            }
        }
    }

    @CommandLine.Command(name = "get", description = "Get a configuration value")
    static class GetCommand implements Callable<Integer> {

        @CommandLine.ParentCommand
        private ConfigCommand parent;

        @CommandLine.Option(
            names = {"-p", "--profile"},
            description = "Profile to read"
        )
        private String profile;

        @CommandLine.Parameters(index = "0", description = "Configuration key")
        private String key;

        @Override
        public Integer call() {
            ConfigManager manager = parent.createConfigManager();
            try {
                String[] parsed = ConfigManager.parseProfileKey(key);
                if (parsed[0] != null) {
                    profile = parsed[0];
                    key = parsed[1];
                } else {
                    key = parsed[1];
                }
                profile = parent.resolveProfile(profile);
                CliConfig config = manager.load(profile);
                String value = getValue(config, key);
                if (value == null || value.isEmpty()) {
                    return 1;
                }
                parent.getOut().println(value);
                return 0;
            } catch (IOException e) {
                parent.spec.commandLine().getErr().println("Failed to load config: " + e.getMessage());
                return 1;
            }
        }
    }

    @CommandLine.Command(name = "list", description = "List all configuration values")
    static class ListCommand implements Callable<Integer> {

        @CommandLine.ParentCommand
        private ConfigCommand parent;

        @CommandLine.Option(
            names = {"-p", "--profile"},
            description = "Profile to list"
        )
        private String profile;

        @Override
        public Integer call() {
            ConfigManager manager = parent.createConfigManager();
            try {
                String activeProfile = parent.resolveProfile(profile);
                CliConfig config = manager.load(activeProfile);
                printIfNotEmpty("secretId", maskIfSecret("secretId", config.getSecretId()));
                printIfNotEmpty("secretKey", maskIfSecret("secretKey", config.getSecretKey()));
                printIfNotEmpty("token", maskIfSecret("token", config.getToken()));
                printIfNotEmpty("endpoint", config.getHost());
                printIfNotEmpty("region", config.getRegion());
                printIfNotEmpty("taskType", config.getTaskType());
                printIfNotEmpty("engine", config.getDataEngineName());
                printIfNotEmpty("datasource", config.getDatasourceConnectionName());
                printIfNotEmpty("resultType", config.getResultType());
                printIfNotEmpty("readType", config.getReadType());
                printIfNotEmpty("output-format", config.getOutputFormat());
                return 0;
            } catch (IOException e) {
                parent.spec.commandLine().getErr().println("Failed to load config: " + e.getMessage());
                return 1;
            }
        }

        private void printIfNotEmpty(String key, String value) {
            if (value != null && !value.isEmpty()) {
                parent.getOut().println(key + " = " + value);
            }
        }
    }

    @CommandLine.Command(name = "unset", description = "Remove a configuration value")
    static class UnsetCommand implements Callable<Integer> {

        @CommandLine.ParentCommand
        private ConfigCommand parent;

        @CommandLine.Option(
            names = {"-p", "--profile"},
            description = "Profile to update"
        )
        private String profile;

        @CommandLine.Parameters(index = "0", description = "Configuration key")
        private String key;

        @Override
        public Integer call() {
            ConfigManager manager = parent.createConfigManager();
            try {
                String[] parsed = ConfigManager.parseProfileKey(key);
                if (parsed[0] != null) {
                    profile = parsed[0];
                    key = parsed[1];
                } else {
                    key = parsed[1];
                }
                profile = parent.resolveProfile(profile);
                manager.unset(key, profile);
                if (!parent.isQuiet()) {
                    String displayProfile = profile != null && !profile.isEmpty() ? " (profile: " + profile + ")" : "";
                    parent.getOut().println("Unset " + key + displayProfile);
                }
                return 0;
            } catch (IOException e) {
                parent.spec.commandLine().getErr().println("Failed to save config: " + e.getMessage());
                return 1;
            }
        }
    }

    private static void apply(CliConfig config, String key, String value) {
        switch (key) {
            case "secretId" -> config.setSecretId(value);
            case "secretKey" -> config.setSecretKey(value);
            case "token" -> config.setToken(value);
            case "host" -> config.setHost(value);
            case "region" -> config.setRegion(value);
            case "taskType" -> config.setTaskType(value);
            case "dataEngineName" -> config.setDataEngineName(value);
            case "datasourceConnectionName" -> config.setDatasourceConnectionName(value);
            case "resultType" -> config.setResultType(value);
            case "readType" -> config.setReadType(value);
            case "outputFormat" -> config.setOutputFormat(value);
            default -> throw new IllegalArgumentException("Unknown configuration key: " + key);
        }
    }

    private static String getValue(CliConfig config, String key) {
        return switch (key) {
            case "secretId" -> config.getSecretId();
            case "secretKey" -> config.getSecretKey();
            case "token" -> config.getToken();
            case "host" -> config.getHost();
            case "region" -> config.getRegion();
            case "taskType" -> config.getTaskType();
            case "dataEngineName" -> config.getDataEngineName();
            case "datasourceConnectionName" -> config.getDatasourceConnectionName();
            case "resultType" -> config.getResultType();
            case "readType" -> config.getReadType();
            case "outputFormat" -> config.getOutputFormat();
            default -> null;
        };
    }

    private static String maskIfSecret(String key, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (key.equalsIgnoreCase("secretKey") || key.equalsIgnoreCase("secret-key")
            || key.equalsIgnoreCase("token")) {
            return "********";
        }
        return value;
    }
}
