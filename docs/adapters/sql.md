


# SQL Adapter Configuration

This section describes the configuration types for the SQL adapter and contains the extensions and specific
configuration types

- [SqlSourceConfiguration](#sqlsourceconfiguration)
- [SqlChannelConfiguration](#sqlchannelconfiguration)
- [SqlAdapterConfiguration](#sqladapterconfiguration)
- [DbServerConfiguration](#dbserverconfiguration)

[Protocol Adapters](protocol-adapters.md)

## SqlSourceConfiguration

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 0%" />
<col style="width: 28%" />
<col style="width: 0%" />
<col style="width: 24%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="6"><p><strong>Extends Source configuration</strong></p>
<p>The SqlSourceConfiguration extends the common Source configuration with SQL specific source configuration data</p></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td colspan="2"><strong>Description</strong></td>
<td colspan="2"><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>
<tr class="even">
<td>Channels</td>
<td colspan="2">The channels configuration for an SQL source holds configuration data to read values through SQL statements. "commented" out by adding a "#" at the beginning of the identifier of that channel.</td>
<td colspan="2">Map[String,<a href="#sqlchannelconfiguration">SqlChannelConfiguration</a>]</td>
<td>At least 1 channel must be configured.</td>
</tr>
<tr class="odd">
<td>AdapterDbServer</td>
<td>Database Server Identifier for the Database server to read from. This referenced server must be present in the DbServers section of the adapter referred to by the ProtocolAdapter attribute of the source.</td>
<td colspan="2">String</td>
<td colspan="2">Must be an identifier of a database in the DbServers section of the SQL adapter used by the source.</td>
</tr>
<tr class="even">
<td>SqlReadStatement</td>
<td>This is the SQL statement that is executed to retrieve the values from the database. It can either be a SELECT statement selecting data from a table or a stored procedure</td>
<td colspan="2">String</td>
<td colspan="2">The logic of the statement or is responsible that records are only read once or any other reading strategy. For example, the procedure can mark or delete the read records when returning the read records.</td>
</tr>
<tr class="odd">
<td>SqlReadParameters</td>
<td>List of parameters that for SqlReadStatement.</td>
<td colspan="2">List[Any]</td>
<td colspan="2">The number of items in the list must match the number of "?" placeholders in the SqlReadStatement.</td>
</tr>
<tr class="even">
<td>SingleRow</td>
<td>Set to true to only return the first retrieved record by the SqlReadStatement. As the selects statement potentially will return multiple records, the values for each channel, will be of an array of values, even if only a single row is read. By setting this value to true it is guaranteed to only return the first row of the result set and the value of a channel is a single value e, not an array.</td>
<td colspan="2">Boolean</td>
<td colspan="2">Default is false.</td>
</tr>
</tbody>
</table>

[^top](#sql-adapter-configuration)

## SqlChannelConfiguration

<table>
<colgroup>
<col style="width: 17%" />
<col style="width: 29%" />
<col style="width: 27%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p><strong>Extends Channel configuration</strong></p>
<p>The SqlChannelConfiguration extends the common Channel configuration with SQL specific channel configuration data</p></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>
<tr class="even">
<td>ColumnNames</td>
<td><p>List of column names to include in the value of the channel. A single value of "*" will retrieve all columns returned in the result set of the executed SqlReadStatement.</p>
<p>If the list contains a single column name, the value of the channel will be the native value retrieved from the column.</p>
<p>If multiple column names are specified, or "*" is used, then the value will be a map. The keys of the entries will be the name of the column and the value will be the native value retrieved for that column from the result set.</p></td>
<td>String[]</td>
<td>Default value is ["*"]</td>
</tr>
</tbody>
</table>

[^top](#sql-adapter-configuration)

## SqlAdapterConfiguration

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 19%" />
<col style="width: 24%" />
<col style="width: 41%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p><strong>Extends ProtocolAdapterConfiguration</strong></p>
<p>The SqlAdapterConfiguration extends the common adapter configuration with SQL specific adapter configuration settings. The AdapterType to use for this adapter is "SQL".</p></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>
<tr class="even">
<td>DbServers</td>
<td>Database servers configured for this adapter. The sql source using the adapter must refer to one of these servers with the AdapterDbServer attribute.</td>
<td>Map[String,<a href="#dbserverconfiguration">DbServerConfiguration</a>]</td>
<td></td>
</tr>
</tbody>
</table>

[^top](#sql-adapter-configuration)

## DbServerConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4">Configuration data for connecting to and reading from source SQL servers</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>
<tr class="even">
<td>Host</td>
<td>Database server host</td>
<td>String</td>
<td></td>
</tr>
<tr class="odd">
<td>Port</td>
<td>Database server port number</td>
<td>Integer</td>
<td></td>
</tr>
<tr class="even">
<td>DatabaseType</td>
<td><p>Type of the JDBC driver used to connect to the database. Possible values are:</p>
<ul>
<li><p>postgresql</p></li>
<li><p>mariadb</p></li>
<li><p>sqlserver</p></li>
<li><p>mysql</p></li>
<li><p>oracle</p></li>
</ul></td>
<td>String</td>
<td></td>
</tr>
<tr class="odd">
<td>DatabaseName</td>
<td>Name of the database, or SID for Oracle databases</td>
<td>String</td>
<td></td>
</tr>
<tr class="even">
<td>UserName</td>
<td>Database username</td>
<td>String</td>
<td><strong>For this value it is strongly recommended to use a placeholder for a value stored in AWS Secrets manager.</strong></td>
</tr>
<tr class="odd">
<td>Password</td>
<td>Database password</td>
<td>String</td>
<td><strong>For this value it is strongly recommended to use a placeholder for a value stored in AWS Secrets manager.</strong></td>
</tr>
<tr class="even">
<td>InitScript</td>
<td>Pathname of a script file executed when a connection is made to the database.</td>
<td>String</td>
<td><p>If both InitScript and InitSql are specified, Init SQL takes precedence.</p>
<p>The content of the InitScript file can <strong>not</strong> contain placeholders for secrets or environment placeholders</p></td>
</tr>
<tr class="odd">
<td>InitSql</td>
<td>Text of a script executed when a connection is made to the database.</td>
<td>String</td>
<td><p>If both InitScript and InitSql are specified, Init SQL takes precedence.</p>
<p>Placeholders for secrets and environment variables are supported for IniSql.</p></td>
</tr>
<tr class="even">
<td>ConnectTimeout</td>
<td>Timeout in milliseconds to connect to the database server.</td>
<td>Integer</td>
<td>Minimum is 1000, default is 10000</td>
</tr>
</tbody>
</table>


[^top](#sql-adapter-configuration)