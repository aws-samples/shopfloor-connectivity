# SQL Adapter Configuration

The SQL adapter for AWS IoT SiteWise Connector (SFC) enables data ingestion from SQL databases using JDBC connections. It allows you to execute custom SQL queries to retrieve data from various SQL databases like MySQL, PostgreSQL, Microsoft SQL Server, and Oracle. 

In order to use this adapter as in [in-process](../sfc-running-adapters.md#running-protocol-adapters-in-process) type adapter the type must be added to the [AdapterTypes](../core/sfc-configuration.md#adaptertypes)(../core/sfc-configuration.md#AdapterTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"AdapterTypes" :{
  "SQL" : {
    "JarFiles" : ["<location of deployment>/sql/lib"]
  },
  "FactoryClassName" : "com.amazonaws.sfc.sql.SqlAdapter"
}
```

**Configuration:**

- [SqlSourceConfiguration](#sqlsourceconfiguration)
- [SqlChannelConfiguration](#sqlchannelconfiguration)
- [SqlAdapterConfiguration](#sqladapterconfiguration)
- [DbServerConfiguration](#dbserverconfiguration)

---

## SqlSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) >  [Source](../core/source-configuration.md) 

SqlSourceConfiguration defines the mapping between SQL query results and IoT channels, specifying which database server to use (referenced from the configured database servers in the adapter) and how to read data from it. It contains the configuration parameters needed to execute queries.

 This type extends the [SourceConfiguration](../core/source-configuration.md) type.

- [Schema](#sqlsourceconfiguration-schema)
- [Examples](#sqlsourceconfiguration-examples)

**Properties:**
- [AdapterDbServer](#adapterdbserver)
- [Channels](#channels)
- [SingleRow](#singlerow)
- [SqlReadParameters](#sqlreadparameters)
- [SqlReadStatement](#sqlreadstatement)

---
### AdapterDbServer
The Database Server Identifier property specifies which database server configuration to use from the DbServers section defined in the SQL adapter configuration. This identifier must match exactly with one of the database server configurations defined in the adapter's [DbServers](#dbservers) section, creating a link between the source and its specific database connection parameters.

**Type**: String

---
### Channels
The Channels property defines the mapping between SQL query results and IoT channels. Each channel specifies how to retrieve data from the results of SQL statements. Channels can be selectively disabled by prefixing their identifier with a "#" character, enabling temporary removal of specific channels without deleting their configuration.

**Type**: Map[String,[SqlChannelConfiguration](#sqlchannelconfiguration)]

At least 1 channel must be configured.

---
### SingleRow
The SingleRow property, when set to true, ensures that only the first record from the SQL query result set is processed and returned. This simplifies channel value handling by returning single values instead of arrays - particularly useful when you know your query will (or should) only return one row. If false, the adapter will process all returned rows and the channel values will be arrays containing all retrieved values.

**Type**: Boolean

Default is false.

---
### SqlReadParameters
The SqlReadStatement parameters property accepts a list of values that will be substituted for the "?" placeholders in the SQL query statement. The number of parameters in this list must exactly match the number of placeholders in the query, and the values will be applied in order. 

**Type**: List[Any]

The number of items in the list must match the number of "?" placeholders in the SqlReadStatement.

---
### SqlReadStatement
The SqlReadStatement property defines the SQL query or stored procedure call that will be executed to retrieve data from the database. This can be either a SELECT statement or a stored procedure name. The statement is responsible for implementing the appropriate data retrieval strategy, such as marking processed records or implementing a mechanism to prevent duplicate reads. For example, the query might include logic to only select unprocessed records and update their status after reading, or delete records once they've been processed.

**Type**: String



### SqlSourceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for SQL source",
  "allOf": [
    {
      "$ref": "#/definitions/SourceConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AdapterDbServer": {
          "type": "string",
          "description": "Reference to the database server configuration in the adapter"
        },
        "Channels": {
          "type": "object",
          "description": "Map of SQL channel configurations",
          "additionalProperties": {
            "$ref": "#/definitions/SqlChannelConfiguration"
          },
          "minProperties": 1
        },
        "SingleRow": {
          "type": "boolean",
          "description": "Indicates if the query should return only a single row",
          "default": false
        },
        "SqlReadParameters": {
          "type": "object",
          "description": "Parameters to be used in the SQL read statement",
          "additionalProperties": {
            "type": "string"
          }
        },
        "SqlReadStatement": {
          "type": "string",
          "description": "SQL statement to read data from the database"
        }
      },
      "required": ["AdapterDbServer", "Channels", "SqlReadStatement"]
    }
  ]
}

```

### SqlSourceConfiguration Examples

```json
{
  "ProtocolAdapter": "SqlAdapter",
  "Description": "Process monitoring metrics",
  "AdapterDbServer": "MainDB",
  "SqlReadStatement": "SELECT timestamp, temperature, pressure, flow_rate FROM process_metrics WHERE machine_id = :machineId",
  "SqlReadParameters": {
    "machineId": "MACHINE001"
  },
  "SingleRow": true,
  "Channels": {
    "Temperature": {
      "Name": "Temperature",
      "Description": "Process temperature",
      "ColumnNames": ["temperature"]
    },
    "Pressure": {
      "Name": "Pressure",
      "Description": "Process pressure",
      "ColumnNames": ["pressure"]
    },
    "FlowRate": {
      "Name": "FlowRate",
      "Description": "Process flow rate",
      "ColumnNames": ["flow_rate"]
    }
  }
}
```

[^top](#sql-adapter-configuration)



## SqlChannelConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#channels) > [Channel](../core/channel-configuration.md)

SqlChannelConfiguration extends ChannelConfiguration to provide SQL-specific channel mapping functionality, inheriting base channel properties like data type handling, validation, and transformation settings. This class adds SQL-specific configurations to define how values should be extracted from database query results  and how to process these values.

The SqlChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the SQL protocol adapter.

- [Schema](#sqlchannelconfiguration-schema)
- [Examples](#sqlchannelconfiguration-examples)


**Properties:**
- [ColumnNames](#columnnames)

---
### ColumnNames

The ColumnNames property specifies which columns from the SQL query result set should be included in the channel value. It accepts either a list of specific column names or  *  to include all columns.

 When a single column is specified, the channel value will be the direct value from that column. When multiple columns or  * is specified, the channel value becomes a map where keys are column names and values are the corresponding data from those columns. 

The default value  *  includes all columns from the result set



**Type**: String[]

Default value is ["*"]

### SqlChannelConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for SQL channel",
  "allOf": [
    {
      "$ref": "#/definitions/ChannelConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "ColumnNames": {
          "type": "array",
          "description": "List of column names for the SQL query results",
          "items": {
            "type": "string"
          },
          "minItems": 1
        }
      },
      "required": ["ColumnNames"]
    }
  ]
}

```

### SqlChannelConfiguration Examples

```json
{
  "Name": "ProductionStatus",
  "Description": "Production line monitoring columns",
  "ColumnNames": [
    "line_id",
    "product_code",
    "quantity",
    "cycle_time",
    "defect_count",
    "operator_id"
  ]
}

```

[^top](#sql-adapter-configuration)

## SqlAdapterConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [ProtocolAdapters](../core/sfc-configuration.md#protocoladapters) > [Adapter](../core/protocol-adapter-configuration.md) 

SqlAdapterConfiguration defines the configuration for the SQL adapter, including database server configurations (DbServers) and protocol-specific settings for connecting to and reading from SQL databases. It extends the class  [ProtocolAdapterConfiguration](../core/protocol-adapter-configuration.md)  class to include SQL-specific functionality.

- [Schema](#sqladapterconfiguration-schema)
- [Examples](#sqladapterconfiguration-examples)


**Properties:**
- [DbServers](#dbservers)

---
### DbServers
The DbServers property defines a collection of database server configurations that can be used by SQL sources in the adapter. Each source must reference one of these predefined server configurations using its [AdapterDbServer](#adapterdbserver) attribute, allowing for centralized configuration of database connection settings and reuse across multiple sources.

**Type**: Map[String,[DbServerConfiguration](#dbserverconfiguration)]

### SqlAdapterConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for SNMP adapter with database servers",
  "allOf": [
    {
      "$ref": "#/definitions/AdapterConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "DbServers": {
          "type": "object",
          "description": "Map of database server configurations",
          "additionalProperties": {
            "$ref": "#/definitions/DbServerConfiguration"
          },
          "minProperties": 1
        }
      },
      "required": ["DbServers"]
    }
  ]
}

```

### SqlAdapterConfiguration Examples

Basic configuration with single database:

```json
{
  "Name": "SnmpLoggingAdapter",
  "Description": "SNMP adapter with database logging",
  "DbServers": {
    "MainDB": {
      "DatabaseType": "PostgreSQL",
      "Host": "localhost",
      "Port": 5432,
      "DatabaseName": "snmp_logs",
      "UserName": "snmp_user",
      "Password": "secure_password",
      "ConnectTimeout": 30
    }
  }
}
```



Example 2 - Multi-database configuration:

```json
{
  "AdapterType": "SqlAdapterType",
  "DbServers": {
    "PrimaryDB": {
      "DatabaseType": "mysql",
      "Host": "primary.db.local",
      "Port": 3306,
      "DatabaseName": "primary-db",
      "UserName": "${user}",
      "Password": "${password}",
      "ConnectTimeout": 30,
      "InitSql": "init.sql"
    },
    "BackupDB": {
      "DatabaseType": "mysql",
      "Host": "backup.db.local",
      "Port": 3306,
      "DatabaseName": "backup-db",
      "UserName": "${user}",
      "Password": "${password}",
      "ConnectTimeout": 30,
      "InitSql": "init.sql"
    }
  }
}
```




[^top](#sql-adapter-configuration)



## DbServerConfiguration

[SqlAdapter](#sqladapterconfiguration) > [DbServers](#dbservers)

DbServerConfiguration defines the connection settings and authentication details needed to connect to a specific database server, including the server address, port, credentials, and database type. It supports various database types and connection parameters required for establishing database connections.

- [Schema](#dbserverconfiguration-schema)
- [Examples](#dbserverconfiguration-examples)

**Properties:**
- [ConnectTimeout](#connecttimeout)
- [DatabaseName](#databasename)
- [DatabaseType](#databasetype)
- [Host](#host)
- [InitScript](#initscript)
- [InitSql](#initsql)
- [Password](#password)
- [Port](#port)
- [UserName](#username)

---
### ConnectTimeout
The ConnectTimeout property specifies how long (in milliseconds) the adapter will wait while attempting to establish a connection to the database server before timing out. It must be at least 1000 milliseconds (1 second), with a default value of 10000 milliseconds (10 seconds).

**Type**: Integer

---
### DatabaseName
The DatabaseName property specifies the name of the database to connect to, or in the case of Oracle databases, it represents the System Identifier (SID). For Oracle, the SID uniquely identifies the database instance and its memory and processes, while for other database types like MySQL, PostgreSQL, or SQL Server, it's simply the name of the database to be accessed.

**Type**: String

---
### DatabaseType
The DatabaseType property specifies which JDBC driver should be used to connect to the database, with supported options being: 

- "postgresql"
- "mariadb"
- "sqlserver"
- "mysql"
- "oracle"

 This setting determines which database-specific driver and connection protocol will be used for establishing the database connection. 


**Type**: String

---
### Host
The Host property specifies the hostname or IP address of the database server to connect to. 

**Type**: String

---
### InitScript
The InitScript property specifies the path to a SQL script file that will be executed automatically when a new database connection is established. This script runs before any other database operations, but it cannot contain placeholders for secrets or environment variables. Note that if both InitScript and [InitSql](#initsql) properties are defined, the InitSql property will take precedence.

**Type**: String

---
### InitSql
The InitSql property allows you to specify SQL commands that will be executed immediately after establishing a database connection. Unlike InitScript, InitSql accepts the SQL commands directly as text rather than from a file, and it supports placeholders for secrets and environment variables. If both InitSql and [InitScript](#initscript) are configured, the InitSql commands will be executed instead of the InitScript.

**Type**: String

---
### Password
The Password property specifies the authentication password used to connect to the database server. For security best practices, it is strongly recommended to not store this password directly in the configuration, but instead use a placeholder that references a password stored in [AWS Secrets manager](../core/secrets-manager-configuration.md), which provides secure, encrypted storage and management of database credentials.

**Type**: String

---
### Port
The Port property specifies the TCP port number where the database server is listening for connections. 

Port number for supported database servers are 3306 for MySQL/MariaDB, 1433 for SQL Server, 5432 for PostgreSQL, or 1521 for Oracle.

**Type**: Integer

---
### UserName
The UserName property specifies the database user account used to authenticate with the database server. For security best practices, it is strongly recommended to not store this username directly in the configuration, but instead use a placeholder that references a value stored in  [AWS Secrets manager](../core/secrets-manager-configuration.md), which provides secure, encrypted storage and management of database credentials.

**Type**: String



### DbServerConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for database server connection",
  "properties": {
    "ConnectTimeout": {
      "type": "integer",
      "description": "Connection timeout in seconds",
      "minimum": 1
    },
    "DatabaseName": {
      "type": "string",
      "description": "Name of the database to connect to"
    },
    "DatabaseType": {
      "type": "string",
      "description": "Type of database server",
      "enum": ["mysql", "postgresql", "sqlerver", "oracle", "mariadb"]
    },
    "Host": {
      "type": "string",
      "description": "Hostname or IP address of the database server"
    },
    "InitScript": {
      "type": "string",
      "description": "Path to initialization script file"
    },
    "InitSql": {
      "type": "string",
      "description": "SQL statements to execute upon connection"
    },
    "Password": {
      "type": "string",
      "description": "Database user password"
    },
    "Port": {
      "type": "integer",
      "description": "Database server port number"
    },
    "UserName": {
      "type": "string",
      "description": "Database username"
    }
  },
  "required": ["DatabaseName", "DatabaseType", "Host", "UserName", "Password"]
}

```

### DbServerConfiguration Examples

```json
{
  "DatabaseType": "mysql",
  "Host": "localhost",
  "Port": 3306,
  "DatabaseName": "myapp_db",
  "UserName": "${user}",
  "Password": "${password}",
  "ConnectTimeout": 30
}

```

[^top](#sql-adapter-configuration)

