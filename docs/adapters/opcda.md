# OPCDA Protocol Configuration

This section describes the configuration types for the OPCDA protocol adapter and contains the extensions and specific
configuration types. Note that the OPCDA connector is a .NET Core based connector, which required Microsoft DCOM and can
only be configures as an external IPC Protocol Server.

  - [OpcdaSourceConfiguration](#opcdasourceconfiguration)
  - [OpcdaChannelConfiguration](#opcdachannelconfiguration)
  - [OpcdaAdapterConfiguration](#opcdaadapterconfiguration)
  - [OpcdaServerConfiguration](#opcdaserverconfiguration)
  - [OpcdaServerConfiguration](#opcdaserverconfiguration)

## OpcdaSourceConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 0%" />
<col style="width: 27%" />
<col style="width: 0%" />
<col style="width: 28%" />
<col style="width: 0%" />
<col style="width: 24%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="7"><p>Extends SourceConfiguration</p>
<p>The OpcuaSourceConfiguration extends the common Source configuration with OPCUA specific source configuration data</p></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td colspan="2"><strong>Name</strong></td>
<td colspan="2"><strong>Description</strong></td>
<td colspan="2"><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>
<tr class="even">
<td colspan="2">Channels</td>
<td colspan="2"><p>The channels configuration for an OPCDA source holds configuration data to read values from items on the source OPCDA server.</p>
<p>The element is a map indexed by the channel identifier.</p>
<p>Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.</p></td>
<td colspan="2">Map[String,<a href="#opcdachannelconfiguration">OpcdaChannelConfiguration</a>]</td>
<td>At least 1 channel must be configured.</td>
</tr>
<tr class="odd">
<td>AdapterOpcdaServer</td>
<td colspan="2">Server Identifier for the OPCDA server to read from. This referenced server must be present in the dServers section of the adapter referred to by the ProtocolAdapter attribute of the source.</td>
<td colspan="2">String</td>
<td colspan="2">Must be an identifier of a server in the OpcdaServers section of the OPCDA adapter used by the source.</td>
</tr>
<tr class="even">
<td colspan="2">SourceReadingMode</td>
<td colspan="2"><p>Mode for reading values from OPCDA server.</p>
<ul>
<li><p>"Subscription": connector will create a subscription and will monitor the items configured in the channels for the source. When reading from the adapter in this mode, only items that have been changed in the schedule interval period will be returned, except for the initial read that will return all monitored items.</p></li>
<li><p>"Polling", the connector will batch-read all items configured in the channels for the source with the interval defined in the schedule.</p></li>
</ul></td>
<td colspan="2">A string that can have the value “Subscription" or "Polling".</td>
<td>Default is "Subscription".</td>
</tr>
</tbody>
</table>

[OpcdaProtocolConfiguration](#opcda-protocol-configuration)

## OpcdaChannelConfiguration

<table>
<colgroup>
<col style="width: 15%" />
<col style="width: 19%" />
<col style="width: 22%" />
<col style="width: 43%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p><strong>Extends ChannelConfiguration</strong></p>
<p>The OpcdaChannelConfiguration extends the common Channel configuration with OPCDA specific channel configuration data</p></th>
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
<td>Item</td>
<td>A string containing the name of the item to read the value from or to monitor.</td>
<td>String</td>
<td></td>
</tr>
</tbody>
</table>

[OpcdaProtocolConfiguration](#opcda-protocol-configuration)

## OpcdaAdapterConfiguration

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 19%" />
<col style="width: 26%" />
<col style="width: 39%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p><strong>Extends ProtocolAdapterConfiguration</strong></p>
<p>The OpcdaAdapterConfiguration extends the common adapter configuration with OPCDA specific adapter configuration settings. The AdapterType to use for this adapter is "OPCDA".</p></th>
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
<td>OpcdaServers</td>
<td>Opcda servers configured for this adapter. The Opcda source using the adapter must refer to one of these servers with the AdapterOpcdaServer attribute.</td>
<td>Map[String,<a href="#opcdaserverconfiguration">OpcdaServerConfiguration</a>]</td>
<td></td>
</tr>
</tbody>
</table>

## OpcdaServerConfiguration

| Configuration data for connecting to and reading from source OPCDA servers |                                                                    |          |                                                                                                                         |
|----------------------------------------------------------------------------|--------------------------------------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------|
| **Name**                                                                   | **Description**                                                    | **Type** | Comments                                                                                                                |
| Url                                                                        | Address of the OPCDA server                                        | String   | e.g., "opcda://192.168.1.145/Simulation"                                                                                |
| ConnectTimeout                                                             | Timeout in milliseconds connecting to the server                   | Integer  | Default is 10000, the minimum value is 1000                                                                             |
| ReadTimeout                                                                | Timeout in milliseconds reading from the server                    | Integer  | Default is 10000                                                                                                        |
| WaitAfterConnectError                                                      | Time in milliseconds to wait to reconnect after a connection error | Integer  | Default is 10000, the minimum value is 1000                                                                             |
| SamplingRate                                                               | Time in milliseconds for sampling items in subscription mode.      | Integer  | If not specified then the shorted interval will be used from all active schedules that have a source using this server. |
| ReadBatchSize                                                              | Max number of items to read in a single batch read from the server | Integer  | If not specified all configured items are read in a single read,                                                        |

[OpcdaProtocolConfiguration](#opcda-protocol-configuration)

[^top](../README.md#toc)
