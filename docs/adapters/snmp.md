
# SNMP Protocol Configuration

This section describes the configuration types for the SNMP protocol adapter and contains the extensions and specific
configuration types. The SNMP protocol adapter can be configured as an in-process, as well as an ipc server running as a
service.

- [SnmpSourceConfiguration](#snmpsourceconfiguration)
- [SnmpChannelConfiguration](#snmpchannelconfiguration)
- [SnmpAdapterConfiguration](#snmpadapterconfiguration)
- [SnmpDeviceConfiguration](#snmpdeviceconfiguration)


[Protocol Adapters](protocol-adapters.md)

## SnmpSourceConfiguration

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
<th colspan="7"><p><strong>Extends Source configuration</strong></p>
<p>The SnmpSourceConfiguration extends the common Source configuration with SNMP specific source device configuration data</p></th>
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
<td colspan="2"><p>The channels hold configuration data to read values from the SNMP source devices.</p>
<p>The element is a map indexed by the channel identifier.</p>
<p>Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.</p></td>
<td colspan="2">Map[String,<a href="#snmpchannelconfiguration">SnmpChannelConfiguration</a>]</td>
<td>At least 1 channel must be configured.</td>
</tr>
<tr class="odd">
<td>AdapterDevice</td>
<td colspan="2">Identifier for the device to read from. This referenced device must be present in the Devices section of the adapter referred to by the ProtocolAdapter attribute of the source.</td>
<td colspan="2">String</td>
<td colspan="2"><p>Must be an identifier of a device in the Devices section of the SNMP adapter used by the source.</p>
<p>Note this is not the modbus device is, this id is set in the DeviceId attribute of the referenced device.</p></td>
</tr>
</tbody>
</table>

[^top](#snmp-protocol-configuration)

## SnmpChannelConfiguration

<table>
<colgroup>
<col style="width: 17%" />
<col style="width: 29%" />
<col style="width: 26%" />
<col style="width: 26%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p><strong>Extends ChannelConfiguration</strong></p>
<p>The SnmpChannelConfiguration extends the common Channel configuration with Modbus specific channel configuration data</p></th>
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
<td>ObjectId</td>
<td>ID of the object to read</td>
<td>string</td>
<td>Must be in valid dot format notation</td>
</tr>
</tbody>
</table>

[^top](#snmp-protocol-configuration)

## SnmpAdapterConfiguration

##  

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 19%" />
<col style="width: 25%" />
<col style="width: 40%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p><strong>Extends ProtocolAdapterConfiguration</strong></p>
<p>The SnmpAdapterConfiguration extends the common adapter configuration with SNMP specific adapter configuration settings. The AdapterType to use for this adapter is "SNMP".</p></th>
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
<td>Devices</td>
<td>Snmp devices configured for this adapter. The modbus tcp source using the adapter must refer to one of these servers with the AdapterDevice attribute.</td>
<td>Map[String,<a href="#snmpdeviceconfiguration">SnmpDeviceConfiguration</a>]</td>
<td></td>
</tr>
</tbody>
</table>

## SnmpDeviceConfiguration

| Configuration data for connecting to and reading from a SNMP device. |                                                             |          |                                              |
|----------------------------------------------------------------------|-------------------------------------------------------------|----------|----------------------------------------------|
| **Name**                                                             | **Description**                                             | **Type** | Comments                                     |
| Address                                                              | IP Address of the device                                    | String   |                                              |
| Port                                                                 | The port on the device                                      | Integer  | Default is 161                               |
| NetworkProtocol                                                      | UDP or TCP protocol                                         | String   | Default is UDP                               |
| Timeout                                                              | The timeout period in milliseconds to read from the device. | Integer  | Default is 10000                             |
| Retries                                                              | Number of retries to read from the device                   | Integer  | Default is 2                                 |
| ReadBatchSize                                                        | Number of values to read in a batch                         | Integer  | Default is 100                               |
| SnmpVersion                                                          | Used SNMP version                                           | Integer  | Supported versions are 1 and 2, default is 2 |
| Community                                                            | SNMP community string for SNMP V1 and V2                    | String   | Default is "public"                          |

[^top](#snmp-protocol-configuration)