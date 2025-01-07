# SQL Adapter Configuration


---
- [SqlSourceConfiguration](#SqlSourceConfiguration)
- [SqlChannelConfiguration](#SqlChannelConfiguration)
- [SqlAdapterConfiguration](#SqlAdapterConfiguration)
- [DbServerConfiguration](#DbServerConfiguration)

---

## SqlSourceConfiguration


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

[^top](#sql-adapter-configuration)




## SqlChannelConfiguration


**Properties:**
- [ColumnNames](#ColumnNames)

---
### ColumnNames
List of column names to include in the value of the channel. A single value of  will retrieve all columns returned in the result set of the executed SqlReadStatement.

If the list contains a single column name, the value of the channel will be the native value retrieved from the column.

If multiple column names are specified, or "*" is used, then the value will be a map. The keys of the entries will be the name of the column and the value will be the native value retrieved for that column from the result set.

**Type**: String[]

Default value is ["*"]

[^top](#sql-adapter-configuration)




## SqlAdapterConfiguration


**Properties:**
- [DbServers](#DbServers)

---
### DbServers
Database servers configured for this adapter. The sql source using the adapter must refer to one of these servers with the AdapterDbServer attribute.

**Type**: Map[String,[DbServerConfiguration](#DbServerConfiguration)]



[^top](#sql-adapter-configuration)




## DbServerConfiguration


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

[^top](#sql-adapter-configuration)

