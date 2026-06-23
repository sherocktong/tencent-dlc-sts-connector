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
package com.tencent.dlc.cli;

import com.tencent.dlc.cli.command.CompletionCommand;
import com.tencent.dlc.cli.command.ConfigCommand;
import com.tencent.dlc.cli.command.SqlCommand;
import com.tencent.dlc.cli.command.VersionCommand;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Entry point for the Tencent DLC CLI.
 */
@CommandLine.Command(
    name = "tdlc",
    mixinStandardHelpOptions = true,
    version = "tdlc 1.0.0-SNAPSHOT",
    description = "Command-line interface for Tencent Cloud Data Lake Compute (DLC).",
    subcommands = {
        SqlCommand.class,
        ConfigCommand.class,
        CompletionCommand.class,
        VersionCommand.class
    }
)
public class TdlcCli implements Callable<Integer> {

    @CommandLine.Option(
        names = {"-c", "--config"},
        description = "Path to the configuration file (default: ~/.tdlc/config.properties)"
    )
    private String configPath;

    @CommandLine.Option(
        names = {"-p", "--profile"},
        description = "Configuration profile to use"
    )
    private String profile;

    @CommandLine.Option(
        names = {"-q", "--quiet"},
        description = "Suppress non-error output"
    )
    private boolean quiet;

    @CommandLine.Option(
        names = {"-v", "--verbose"},
        description = "Enable verbose logging to stderr"
    )
    private boolean verbose;

    public String getConfigPath() {
        return configPath;
    }

    public String getProfile() {
        return profile;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public Integer call() {
        CommandLine commandLine = new CommandLine(this);
        commandLine.usage(System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new TdlcCli()).execute(args);
        System.exit(exitCode);
    }
}
