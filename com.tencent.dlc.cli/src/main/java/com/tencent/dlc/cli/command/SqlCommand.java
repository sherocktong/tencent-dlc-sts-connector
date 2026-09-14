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
import com.tencent.dlc.cli.jdbc.DlcConnectionFactory;
import com.tencent.dlc.cli.output.BeautifiedTableFormatter;
import com.tencent.dlc.cli.output.CsvFormatter;
import com.tencent.dlc.cli.output.JsonFormatter;
import com.tencent.dlc.cli.output.ResultFormatter;
import com.tencent.dlc.cli.output.TableFormatter;
import com.tencent.dlc.cli.output.YamlFormatter;
import com.tencent.dlc.cli.util.ProgressReporter;
import com.tencent.dlc.core.TencentDLCConstants;
import com.tencent.dlc.core.query.DlcQueryRewriter;
import com.tencent.dlc.core.query.DlcQueryRewriter.DescribeTarget;
import com.tencent.dlc.core.result.DlcDescribeFallback;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes SQL statements against Tencent DLC.
 */
@CommandLine.Command(
    name = "sql",
    description = "Execute SQL statements"
)
public class SqlCommand implements Callable<Integer> {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final int DEFAULT_MAX_ROWS = 10_000;

    @CommandLine.ParentCommand
    private TdlcCli parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(
        names = {"-e", "--execute"},
        description = "SQL statement to execute"
    )
    private String executeSql;

    @CommandLine.Option(
        names = {"-f", "--file"},
        description = "Path to a file containing SQL statements"
    )
    private String filePath;

    @CommandLine.Option(
        names = {"--secret-id"},
        description = "Tencent Cloud SecretId"
    )
    private String secretId;

    @CommandLine.Option(
        names = {"--secret-key"},
        description = "Tencent Cloud SecretKey"
    )
    private String secretKey;

    @CommandLine.Option(
        names = {"--token"},
        description = "STS temporary token"
    )
    private String token;

    @CommandLine.Option(
        names = {"--endpoint"},
        description = "DLC endpoint host (default: " + TencentDLCConstants.DEFAULT_HOST + ")"
    )
    private String endpoint;

    @CommandLine.Option(
        names = {"--region"},
        description = "Tencent Cloud region (default: " + TencentDLCConstants.DEFAULT_REGION + ")"
    )
    private String region;

    @CommandLine.Option(
        names = {"--engine"},
        description = "DLC data engine name"
    )
    private String engine;

    @CommandLine.Option(
        names = {"--datasource"},
        description = "Datasource connection name (default: " + TencentDLCConstants.DEFAULT_DATASOURCE_CONNECTION_NAME + ")"
    )
    private String datasource;

    @CommandLine.Option(
        names = {"--task-type"},
        description = "DLC task type (default: " + TencentDLCConstants.DEFAULT_TASK_TYPE + ")"
    )
    private String taskType;

    @CommandLine.Option(
        names = {"--result-type"},
        description = "Result storage type (default: " + TencentDLCConstants.DEFAULT_RESULT_TYPE + ")"
    )
    private String resultType;

    @CommandLine.Option(
        names = {"--read-type"},
        description = "Result read type (default: " + TencentDLCConstants.DEFAULT_READ_TYPE + ")"
    )
    private String readType;

    @CommandLine.Option(
        names = {"-p", "--parameter"},
        description = "SQL placeholder in key=value form (repeatable)"
    )
    private Map<String, String> parameters = new HashMap<>();

    @CommandLine.Option(
        names = {"-o", "--format", "--output-format"},
        description = "Output format: yaml, table, grid, csv, json (default: table)"
    )
    private String format;

    @CommandLine.Option(
        names = {"--no-result"},
        description = "Do not print result rows"
    )
    private boolean noResult;

    @CommandLine.Option(
        names = {"--max-rows"},
        description = "Maximum rows to print (default: " + DEFAULT_MAX_ROWS + ")"
    )
    private int maxRows = DEFAULT_MAX_ROWS;

    @Override
    public Integer call() {
        if ((executeSql == null || executeSql.isEmpty()) && (filePath == null || filePath.isEmpty())) {
            spec.commandLine().getErr().println("Error: either --execute or --file is required");
            return 1;
        }

        CliConfig config;
        try {
            config = buildConfig();
        } catch (IOException e) {
            spec.commandLine().getErr().println("Failed to load config: " + e.getMessage());
            return 1;
        }

        String sql;
        DescribeTarget describeTarget;
        try {
            sql = loadSql();
            sql = substituteParameters(sql);
            describeTarget = DlcQueryRewriter.parseDescribe(sql);
            String rewritten = DlcQueryRewriter.rewrite(sql, config.getDatasourceConnectionName());
            if (!rewritten.equals(sql) && isVerbose()) {
                spec.commandLine().getErr().println("Rewritten SQL: " + rewritten);
            }
            sql = rewritten;
        } catch (IOException e) {
            spec.commandLine().getErr().println("Failed to read SQL: " + e.getMessage());
            return 1;
        }

        ProgressReporter progress = isQuiet()
            ? ProgressReporter.NO_OP
            : ProgressReporter.console(System.err);

        try {
            return execute(config, sql, describeTarget, progress);
        } catch (SQLException e) {
            spec.commandLine().getErr().println("SQL execution failed: " + e.getMessage());
            if (isVerbose()) {
                e.printStackTrace(spec.commandLine().getErr());
            }
            return 1;
        }
    }

    private CliConfig buildConfig() throws IOException {
        ConfigManager manager = createConfigManager();
        CliConfig fileConfig = manager.load(getProfile());

        CliConfig overrides = new CliConfig();
        overrides.setSecretId(secretId);
        overrides.setSecretKey(secretKey);
        overrides.setToken(token);
        overrides.setHost(endpoint);
        overrides.setRegion(region);
        overrides.setTaskType(taskType);
        overrides.setDataEngineName(engine);
        overrides.setDatasourceConnectionName(datasource);
        overrides.setResultType(resultType);
        overrides.setReadType(readType);
        overrides.setOutputFormat(format);
        overrides.getParameters().putAll(parameters);

        CliConfig merged = manager.merge(fileConfig, overrides);
        applyDefaults(merged);
        return merged;
    }

    private static void applyDefaults(CliConfig config) {
        if (isEmpty(config.getHost())) {
            config.setHost(TencentDLCConstants.DEFAULT_HOST);
        }
        if (isEmpty(config.getRegion())) {
            config.setRegion(TencentDLCConstants.DEFAULT_REGION);
        }
        if (isEmpty(config.getTaskType())) {
            config.setTaskType(TencentDLCConstants.DEFAULT_TASK_TYPE);
        }
        if (isEmpty(config.getDatasourceConnectionName())) {
            config.setDatasourceConnectionName(TencentDLCConstants.DEFAULT_DATASOURCE_CONNECTION_NAME);
        }
        if (isEmpty(config.getResultType())) {
            config.setResultType(TencentDLCConstants.DEFAULT_RESULT_TYPE);
        }
        if (isEmpty(config.getReadType())) {
            config.setReadType(TencentDLCConstants.DEFAULT_READ_TYPE);
        }
        if (isEmpty(config.getOutputFormat())) {
            config.setOutputFormat("table");
        }
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private ConfigManager createConfigManager() {
        if (parent != null && parent.getConfigPath() != null && !parent.getConfigPath().isEmpty()) {
            return new ConfigManager(Paths.get(parent.getConfigPath()));
        }
        return new ConfigManager();
    }

    private String getProfile() {
        return parent != null ? parent.getProfile() : null;
    }

    private String loadSql() throws IOException {
        if (executeSql != null && !executeSql.isEmpty()) {
            return executeSql;
        }
        Path path = Paths.get(filePath);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private String substituteParameters(String sql) {
        if (parameters.isEmpty()) {
            return sql;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(sql);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = parameters.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private int execute(CliConfig config, String sql, DescribeTarget describeTarget, ProgressReporter progress)
        throws SQLException {
        progress.report("Connecting to " + DlcConnectionFactory.buildUrl(config) + " ...");

        DlcConnectionFactory connectionFactory = new DlcConnectionFactory();
        try (Connection connection = connectionFactory.open(config);
             Statement statement = connection.createStatement()) {

            progress.report("Executing SQL ...");
            boolean hasResultSet = statement.execute(sql);
            PrintWriter out = spec.commandLine().getOut();

            do {
                if (hasResultSet) {
                    try (ResultSet rawResultSet = statement.getResultSet()) {
                        if (!noResult) {
                            // information_schema misses views and non-default catalogs;
                            // fall back to a zero-row select that resolves the schema.
                            ResultSet resultSet = describeTarget != null
                                ? DlcDescribeFallback.materialize(describeTarget, connection, rawResultSet)
                                : rawResultSet;
                            ResultFormatter formatter = createFormatter(config);
                            formatter.format(resultSet, out, maxRows);
                        }
                    }
                } else {
                    int updateCount = statement.getUpdateCount();
                    if (updateCount >= 0 && !isQuiet()) {
                        out.println(updateCount + " row(s) affected");
                    }
                }
                hasResultSet = statement.getMoreResults();
            } while (hasResultSet || statement.getUpdateCount() != -1);

            progress.done();
            return 0;
        }
    }

    private ResultFormatter createFormatter(CliConfig config) {
        String outputFormat = config.getOutputFormat();
        return switch (outputFormat.toLowerCase()) {
            case "yaml" -> new YamlFormatter();
            case "grid" -> new TableFormatter();
            case "csv" -> new CsvFormatter();
            case "json" -> new JsonFormatter();
            default -> new BeautifiedTableFormatter();
        };
    }

    private boolean isQuiet() {
        return parent != null && parent.isQuiet();
    }

    private boolean isVerbose() {
        return parent != null && parent.isVerbose();
    }
}
