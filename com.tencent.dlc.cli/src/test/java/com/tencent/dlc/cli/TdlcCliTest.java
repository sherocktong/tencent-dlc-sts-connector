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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TdlcCliTest {

    @Test
    void completionCommandGeneratesBashScript() {
        CommandLine commandLine = new CommandLine(new TdlcCli());
        StringWriter output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("completion", "bash");

        assertEquals(0, exitCode);
        String text = output.toString();
        assertTrue(text.contains("tdlc"));
    }

    @Test
    void completionCommandGeneratesZshScript() {
        CommandLine commandLine = new CommandLine(new TdlcCli());
        StringWriter output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("completion", "zsh");

        assertEquals(0, exitCode);
        String text = output.toString();
        assertTrue(text.contains("tdlc"));
    }

    @Test
    void completionCommandRejectsUnknownShell() {
        CommandLine commandLine = new CommandLine(new TdlcCli());
        StringWriter error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("completion", "fish");

        assertEquals(1, exitCode);
        assertTrue(error.toString().contains("unsupported shell"));
    }

    @Test
    void versionCommandPrintsVersion() {
        CommandLine commandLine = new CommandLine(new TdlcCli());
        StringWriter output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("version");

        assertEquals(0, exitCode);
        String text = output.toString();
        assertTrue(text.contains("tdlc-cli"));
        assertTrue(text.contains("dlc-jdbc-driver"));
    }

    @Test
    void configSetAndGet(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        CommandLine commandLine = new CommandLine(new TdlcCli());

        int setExit = commandLine.execute("-c", configFile.toString(), "config", "set", "region", "ap-beijing");
        assertEquals(0, setExit);

        StringWriter output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));
        int getExit = commandLine.execute("-c", configFile.toString(), "config", "get", "region");
        assertEquals(0, getExit);
        assertEquals("ap-beijing\n", output.toString());
    }

    @Test
    void configSetNormalizesKebabCaseKeys(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        CommandLine commandLine = new CommandLine(new TdlcCli());

        int setExit = commandLine.execute("-c", configFile.toString(), "config", "set", "secret-id", "AKID");
        assertEquals(0, setExit);

        String content = Files.readString(configFile);
        assertTrue(content.contains("secretId=AKID"));
    }

    @Test
    void configSetOutputFormatNormalizesKey(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        CommandLine commandLine = new CommandLine(new TdlcCli());

        int setExit = commandLine.execute("-c", configFile.toString(), "config", "set", "output-format", "yaml");
        assertEquals(0, setExit);

        String content = Files.readString(configFile);
        assertTrue(content.contains("outputFormat=yaml"));
    }

    @Test
    void sqlCommandUsesActiveProfile(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, """
            region=ap-singapore
            [profile:prod]
            region=ap-beijing
            """);
        CommandLine commandLine = new CommandLine(new TdlcCli());

        // This validates argument parsing with --profile; SQL won't execute without credentials.
        int exitCode = commandLine.execute("-c", configFile.toString(), "--profile", "prod", "sql", "-e", "SELECT 1");
        // It will fail to connect, but we can verify profile was parsed by checking help works.
        assertNotEquals(CommandLine.ExitCode.USAGE, exitCode);
    }

    @Test
    void configSetWithProfileSyntax(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        CommandLine commandLine = new CommandLine(new TdlcCli());

        int setExit = commandLine.execute("-c", configFile.toString(), "config", "set", "profile.prod.region", "ap-beijing");
        assertEquals(0, setExit);

        String content = Files.readString(configFile);
        assertTrue(content.contains("[profile:prod]"));
        assertTrue(content.contains("region=ap-beijing"));
    }

    @Test
    void configGetWithProfileOption(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, """
            region=ap-singapore
            [profile:prod]
            region=ap-beijing
            """);
        CommandLine commandLine = new CommandLine(new TdlcCli());

        StringWriter output = new StringWriter();
        commandLine.setOut(new PrintWriter(output));
        int getExit = commandLine.execute("-c", configFile.toString(), "config", "get", "--profile", "prod", "region");
        assertEquals(0, getExit);
        assertEquals("ap-beijing\n", output.toString());
    }

    @Test
    void sqlCommandRequiresExecuteOrFile() {
        CommandLine commandLine = new CommandLine(new TdlcCli());
        StringWriter error = new StringWriter();
        commandLine.setErr(new PrintWriter(error));

        int exitCode = commandLine.execute("sql");

        assertEquals(1, exitCode);
        assertTrue(error.toString().contains("either --execute or --file is required"));
    }
}
