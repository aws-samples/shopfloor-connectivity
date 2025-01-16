# SQL Adapter Configuration

SQL Protocol adapter configuration

---
- [SqlSourceConfiguration](#SqlSourceConfiguration)
- [SqlChannelConfiguration](#SqlChannelConfiguration)
- [SqlAdapterConfiguration](#SqlAdapterConfiguration)
- [DbServerConfiguration](#DbServerConfiguration)

---

## SqlSourceConfiguration

[SFC Configuration](../core/sfc-configuration) > [Sources](../core/sfc-configuration#Sources) >  [Source](../core/source-configuration.md) 



Source configuration for the SQL protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type.

- [Schema](#SqlSourceConfiguration-Schema)
- [Examples](#SqlSourceConfiguration-Examples)

**Properties:**
- [AdapterDbServer](#AdapterDbServer)
- [Channels](#Channels)
- [SingleRow](#SingleRow)
- [SqlReadParameters](#SqlReadParameters)
- [SqlReadStatement](#SqlReadStatement)

---
### AdapterDbServer
Database Server Identifier for the Database server to read from. This referenced server must be present in the DbServers section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a database in the DbServers section of the SQL adapter used by the source.

---
### Channels
The channels configuration for an SQL source holds configuration data to read values through SQL statements. "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,[SqlChannelConfiguration](#SqlChannelConfiguration)]

At least 1 channel must be configured.



---
### SingleRow
Set to true to only return the first retrieved record by the SqlReadStatement. As the selects statement potentially will return multiple records, the values for each channel, will be of an array of values, even if only a single row is read. By setting this value to true it is guaranteed to only return the first row of the result set and the value of a channel is a single value e, not an array.

**Type**: Boolean

Default is false.

---
### SqlReadParameters
List of parameters that for SqlReadStatement.

**Type**: List[Any]

The number of items in the list must match the number of "?" placeholders in the SqlReadStatement.

---
### SqlReadStatement
This is the SQL statement that is executed to retrieve the values from the database. It can either be a SELECT statement selecting data from a table or a stored procedure

**Type**: String

The logic of the statement or is responsible that records are only read once or any other reading strategy. For example, the procedure can mark or delete the read records when returning the read records.

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

[SFC Configuration](../core/sfc-configuration) > [Sources](../core/sfc-configuration#Sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#Channels) > [Channel](../core/channel-configuration.md)



The SqlChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the SQL protocol adapter.

- [Schema](#SqlChannelConfiguration-Schema)
- [Examples](#SqlChannelConfiguration-Examples)


**Properties:**
- [ColumnNames](#ColumnNames)

---
### ColumnNames
List of column names to include in the value of the channel. A single value of  will retrieve all columns returned in the result set of the executed SqlReadStatement.

If the list contains a single column name, the value of the channel will be the native value retrieved from the column.

If multiple column names are specified, or "*" is used, then the value will be a map. The keys of the entries will be the name of the column and the value will be the native value retrieved for that column from the result set.

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

[SFC Configuration](../core/sfc-configuration) > [ProtocolAdapters](../core/sfc-configuration#ProtocolAdapters) > [Adapter](../core/protocol-adapter-configuration.md) 



SqlAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the SQL Protocol adapter.

- [Schema](#SqlAdapterConfiguration-Schema)
- [Examples](#SqlAdapterConfiguration-Examples)


**Properties:**
- [DbServers](#DbServers)

---
### DbServers
Database servers configured for this adapter. The sql source using the adapter must refer to one of these servers with the AdapterDbServer attribute.

**Type**: Map[String,[DbServerConfiguration](#DbServerConfiguration)]

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

[SqlAdapter](#SqlAdapterConfiguration) > [DbServers](#DbServers)



- [Schema](#DbServerConfiguration-Schema)
- [Examples](#DbServerConfiguration-Examples)

**Properties:**
- [ConnectTimeout](#ConnectTimeout)
- [DatabaseName](#DatabaseName)
- [DatabaseType](#DatabaseType)
- [Host](#Host)
- [InitScript](#InitScript)
- [InitSql](#InitSql)
- [Password](#Password)
- [Port](#Port)
- [UserName](#UserName)

---
### ConnectTimeout
Timeout in milliseconds to connect to the database server.

**Type**: Integer

Minimum is 1000, default is 10000

---
### DatabaseName
Name of the database, or SID for Oracle databases

**Type**: String

---
### DatabaseType
Type of the JDBC driver used to connect to the database. Possible values are:

postgresql
mariadb
sqlserver
mysql
oracle


**Type**: String

---
### Host
Database server host

**Type**: String

---
### InitScript
Pathname of a script file executed when a connection is made to the database.

**Type**: String

If both InitScript and InitSql are specified, Init SQL takes precedence.
The content of the InitScript file can not contain placeholders for secrets or environment placeholders

---
### InitSql
Text of a script executed when a connection is made to the database.

**Type**: String

If both InitScript and InitSql are specified, Init SQL takes precedence.
Placeholders for secrets and environment variables are supported for IniSql.



---
### Password
Database password

**Type**: String

**For this value it is strongly recommended to use a placeholder for a value stored in [AWS Secrets manager](../core/secrets-manager-configuration.md).**

---
### Port
Database server port number

**Type**: Integer

---
### UserName
Database username

**Type**: String

**For this value it is strongly recommended to use a placeholder for a value stored in [AWS Secrets manager](../core/secrets-manager-configuration.md).**

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

