# Tencent DLC DBeaver Plugin

DBeaver plugin for [Tencent Cloud Data Lake Compute (DLC)](https://cloud.tencent.com/product/dlc) — a Spark SQL engine. Provides native driver integration with custom connection UI and STS token authentication.

## Features

- **Native DBeaver integration** — Tencent DLC appears as a first-class data source in the connection wizard.
- **Custom connection page** — dedicated UI fields for host, region, task type, data engine, datasource connection, result type, and read type.
- **STS token authentication** — supports SecretId, SecretKey, and temporary security token (STS) login.
- **p2 update site** — install via DBeaver's **Help → Install New Software**.

## Project Structure

```
├── com.tencent.dbeaver.ext.dlc          # Plugin bundle (model + UI)
├── com.tencent.dbeaver.ext.dlc.feature  # Eclipse feature
├── com.tencent.dbeaver.ext.dlc.site     # p2 update site
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

## Configuration Reference

The JDBC URL is constructed automatically from the connection page:

```
jdbc:dlc:{host}?task_type={task_type}&region={region}&data_engine_name={data_engine_name}&datasource_connection_name={datasource_connection_name}&result_type={result_type}&read_type={read_type}
```

All parameters except `host` are optional and omitted when empty.

## Driver JAR

The plugin references the DLC JDBC driver at:

```
/Users/kangtong/.local/share/jdbc/drivers/dlc-jdbc-2.5.9-jar-with-dependencies.jar
```

Update the `<file>` path in `com.tencent.dbeaver.ext.dlc/plugin.xml` if the driver is located elsewhere, or bundle it inside `com.tencent.dbeaver.ext.dlc/drivers/tencent_dlc/` for redistribution.

## License

MIT License — see [LICENSE](LICENSE) for details.
