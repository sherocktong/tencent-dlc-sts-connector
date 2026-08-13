# Tencent DLC DBeaver Plugin

DBeaver plugin for [Tencent Cloud Data Lake Compute (DLC)](https://cloud.tencent.com/product/dlc) — a Spark SQL engine. Provides native driver integration with custom connection UI and STS token authentication.

## Features

- **Native DBeaver integration** — Tencent DLC appears as a first-class data source in the connection wizard.
- **Custom connection page** — dedicated UI fields for host, region, task type, data engine, datasource connection, result type, and read type.
- **STS token authentication** — supports SecretId, SecretKey, and temporary security token (STS) login.
- **p2 update site** — install via DBeaver's **Help → Install New Software**.

## Project Structure

```
├── com.tencent.dlc.core                 # Shared constants, JDBC URL builder, and driver/result wrappers
├── com.tencent.dbeaver.ext.dlc          # Plugin bundle (model + UI)
├── com.tencent.dbeaver.ext.dlc.feature  # Eclipse feature
├── com.tencent.dbeaver.ext.dlc.site     # p2 update site
├── com.tencent.dlc.cli                  # Standalone tdlc CLI
└── pom.xml                              # Parent Maven POM (Tycho)
```

## Requirements

- **JDK 21** — DBeaver's latest update site targets `JavaSE-21`, so the build requires JDK 21.
- **Maven 3.9.0+** — required by Tycho 4.x.

> If you only have Maven 3.6.x installed, the build script downloads Maven 3.9.6 automatically to `/tmp/apache-maven-3.9.6/`.

## Build

```bash
# Using system Maven (must be 3.9+)
mvn clean verify

# Or using the downloaded Maven 3.9.6
/tmp/apache-maven-3.9.6/bin/mvn clean verify
```

Artifacts are produced in:

| Artifact | Path |
|----------|------|
| Plugin JAR | `com.tencent.dbeaver.ext.dlc/target/*.jar` |
| Feature JAR | `com.tencent.dbeaver.ext.dlc.feature/target/*.jar` |
| Update Site ZIP | `com.tencent.dbeaver.ext.dlc.site/target/*.zip` |
| p2 Repository | `com.tencent.dbeaver.ext.dlc.site/target/repository/` |

## Installation

### Option A — Install from local update site

1. Build the project (`mvn clean verify`).
2. In DBeaver, go to **Help → Install New Software…**.
3. Click **Add…** → **Local…** and select `com.tencent.dbeaver.ext.dlc.site/target/repository/`.
4. Select **Tencent DLC DBeaver Plugin** and finish the wizard.
5. Restart DBeaver when prompted.

### Option B — Drop plugin JAR (not recommended)

Copy `com.tencent.dbeaver.ext.dlc/target/com.tencent.dbeaver.ext.dlc-*.jar` into DBeaver's `plugins/` folder and restart. This bypasses dependency resolution; use only for quick testing.

## Usage

1. Open **Database → New Database Connection**.
2. Search for or scroll to **Tencent DLC Spark SQL** in the driver list.
3. Click **Next** — this opens the connection settings page.
4. Fill in the connection fields:
   - **Host**: `dlc.tencentcloudapi.com` (default)
   - **Region**: e.g. `ap-singapore`
   - **Task Type**: e.g. `SparkSQLTask`
   - **Data Engine Name**: your engine name
   - **Datasource Connection Name**: e.g. `DataLakeCatalog`
   - **Result Type**: `COS`
   - **Read Type**: `Stream`
5. In the **Authentication** section, choose **Tencent DLC SecretId / SecretKey / Token** and enter:
   - **User name**: your Tencent Cloud `SecretId`
   - **Password**: your Tencent Cloud `SecretKey`
   - **Token**: your STS temporary token (optional)
6. Click **Test Connection** to verify, then click **Finish**.

> **Note**: There is no "Next" button on the connection settings page — that is by design in DBeaver's connection wizard. Click **Finish** directly from the settings page to save the connection.

## tdlc CLI

A standalone command-line interface for executing SQL against Tencent DLC is included in `com.tencent.dlc.cli`.

### Build the CLI

```bash
mvn -f com.tencent.dlc.cli/pom.xml clean package
```

The executable uber-JAR is produced at:

```
com.tencent.dlc.cli/target/tdlc-cli-1.0.0-SNAPSHOT-shaded.jar
```

### Usage

```bash
alias tdlc='java -jar com.tencent.dlc.cli/target/tdlc-cli-1.0.0-SNAPSHOT-shaded.jar'
```

Global options:

```
-c, --config=<configPath>   Path to config file (default: ~/.tdlc/config.properties)
-p, --profile=<profile>     Configuration profile to use
-q, --quiet                 Suppress non-error output
-v, --verbose               Verbose logging to stderr
-h, --help                  Show help
-V, --version               Print version
```

### Commands

#### `config` — manage defaults

```bash
tdlc config set secret-id AKIDxxxxxxxx
tdlc config set secret-key xxxxxxxxxx
tdlc config set region ap-singapore
tdlc config set endpoint dlc.tencentcloudapi.com
tdlc config set engine my-engine
tdlc config set output-format yaml
tdlc config get region
tdlc config list
tdlc config unset secret-key
```

Keys may use kebab-case (`secret-id`) or camelCase (`secretId`).

##### Profiles

Configuration profiles let you switch between environments (e.g. `dev` and `prod`).

```bash
# Default settings apply when no profile is active
tdlc config set region ap-singapore

# Per-profile settings
tdlc config set --profile prod region ap-beijing
tdlc config set --profile prod engine prod-engine

# Or use the dotted profile.key syntax
tdlc config set profile.prod.secret-id AKIDprod
tdlc config get profile.prod.region

# List values for a specific profile
tdlc config list --profile prod

# Activate a profile for a single command
tdlc --profile prod sql -e "SHOW DATABASES"
# or with the short alias:
tdlc -p prod sql -e "SHOW DATABASES"
```

Profile files use INI-style sections:

```properties
region=ap-singapore
engine=default-engine

[profile:prod]
region=ap-beijing
engine=prod-engine
```

Precedence (highest first): command-line flags > active profile > default section.

#### `sql` — execute SQL

```bash
# Inline SQL
tdlc sql -e "SHOW DATABASES"

# SQL from file
tdlc sql -f ./analytics.sql

# Override config inline
tdlc sql -e "SELECT * FROM orders" --region ap-beijing --engine my-engine

# Parameter substitution
tdlc sql -e "SELECT * FROM orders WHERE dt = '${date}'" -p date=2024-01-01

# Output formats
tdlc sql -e "SELECT 1" -o csv
tdlc sql -e "SELECT 1" --format json
tdlc sql -e "SELECT 1" --output-format yaml
```

`DESCRIBE TABLE db.table` is rewritten automatically to a query against
`information_schema.columns` because the DLC JDBC driver returns empty results
for native `DESCRIBE` statements. Use `-v` to see the rewritten SQL.

SQL options:

| Option | Description |
|--------|-------------|
| `-e, --execute <sql>` | Inline SQL statement |
| `-f, --file <path>` | Read SQL from file |
| `--secret-id`, `--secret-key`, `--token` | Tencent Cloud credentials |
| `--endpoint` | DLC endpoint host |
| `--region` | Tencent Cloud region |
| `--engine` | DLC data engine name |
| `--datasource` | Datasource connection name |
| `--task-type`, `--result-type`, `--read-type` | DLC task options |
| `-p, --parameter <k=v>` | Substitute `${key}` placeholders |
| `-o, --format, --output-format <fmt>` | Output format: `yaml`, `table`, `grid`, `csv`, `json` (default: `table`) |
| `--no-result` | Do not print result rows |
| `--max-rows <n>` | Maximum rows to print (default: 10000) |

The `table` format is a compact, single-line-per-row table without embedded return lines. Use `grid` for the previous boxed table style.

#### `version` — show versions

```bash
tdlc version
```

#### `completion` — generate shell completions

```bash
# Bash
tdlc completion bash > ~/.local/share/bash-completion/completions/tdlc

# Zsh
tdlc completion zsh > ~/.local/share/zsh/completions/_tdlc
```

The `deploy-cli.sh` script installs both completion scripts automatically.
For zsh, reload completions with:

```bash
rm -f ~/.zcompdump && autoload -Uz compinit && compinit
```

## Configuration Reference

The JDBC URL is constructed automatically from the connection page:

```
jdbc:dlc:{host}?task_type={task_type}&region={region}&data_engine_name={data_engine_name}&datasource_connection_name={datasource_connection_name}&result_type={result_type}&read_type={read_type}&timezone={timezone}
```

All parameters except `host` are optional and omitted when empty.

## Driver JAR

The plugin references the DLC JDBC driver at:

```
/Users/kangtong/.local/share/jdbc/drivers/dlc-jdbc-2.5.9-jar-with-dependencies.jar
```

Update the `<file>` path in `com.tencent.dbeaver.ext.dlc/plugin.xml` if the driver is located elsewhere, or bundle it inside `com.tencent.dbeaver.ext.dlc/drivers/tencent_dlc/` for redistribution.

## Known Driver Quirks

Some DDL statements, such as `DESCRIBE TABLE`, return a `ResultSet` whose `getMetaData()` is `null` and whose row count is zero. Both the DBeaver plugin and the `tdlc` CLI share a common workaround in `com.tencent.dlc.core`:

- The plugin loads the DLC driver through `com.tencent.dlc.core.jdbc.DlcDriverWrapper`. The wrapper provides a fallback metadata object when the driver returns none and transparently rewrites simple `DESCRIBE TABLE schema.table` statements into a query against `information_schema.columns`.
- The CLI wraps each result set with `com.tencent.dlc.core.result.DlcResultSetHandler.safeMetaData(...)` and applies the same `DESCRIBE` rewrite before execution.

## License

MIT License — see [LICENSE](LICENSE) for details.
