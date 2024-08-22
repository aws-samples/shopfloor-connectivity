
# SNMP Protocol Configuration

This section describes the configuration types for the SNMP protocol adapter and contains the extensions and specific
configuration types. The SNMP protocol adapter can be configured as an in-process, as well as an ipc server running as a
service.

- [SnmpSourceConfiguration](#snmpsourceconfiguration)
- [SnmpChannelConfiguration](#snmpchannelconfiguration)
- [SnmpAdapterConfiguration](#snmpadapterconfiguration)
- [SnmpDeviceConfiguration](#snmpdeviceconfiguration)


[Protocol Adapters](./README.md)

## SnmpSourceConfiguration

The SnmpSourceConfiguration extends the common <a href="../core/source-configuration.md">Source configuration</a> with SNMP specific source device configuration data

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Channels</td>
<td><p>The channels hold configuration data to read values from the SNMP source devices.</p>
<p>The element is a map indexed by the channel identifier.</p>
<p>Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.</p></td>
<td>Map[String,<a href="#snmpchannelconfiguration">SnmpChannelConfiguration</a>]</td>
<td>At least 1 channel must be configured.</td>
</tr>

<tr class="odd">
<td>AdapterDevice</td>
<td>Identifier for the device to read from. This referenced device must be present in the Devices section of the adapter referred to by the ProtocolAdapter attribute of the source.</td>
<td>String</td>
<td><p>Must be an identifier of a device in the Devices section of the SNMP adapter used by the source.</p>
<p>Note this is not the modbus device is, this id is set in the DeviceId attribute of the referenced device.</p></td>
</tr>

</tbody>
</table>

[^top](#snmp-protocol-configuration)

## SnmpChannelConfiguration

The SnmpChannelConfiguration extends the common Channel configuration with Modbus specific channel configuration data</p></th>


<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

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

The SnmpAdapterConfiguration extends the common adapter configuration with SNMP specific adapter configuration settings. The AdapterType to use for this adapter is <strong>"SNMP"</strong>.

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

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

Configuration data for connecting to and reading from a SNMP device.

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Address</td>
<td>IP Address of the device</td>
<td>String</td>
<td></td>
</tr>

<tr class="odd">
<td>Port</td>
<td>The port on the device</td>
<td>Integer</td>
<td>Default is 161</td>
</tr>

<tr class="even">
<td>NetworkProtocol</td>
<td>"UDP" or "TCP" protocol</td>
<td>String</td>
<td>Default is UDP</td>
</tr>

<tr class="odd">
<td>Timeout</td>
<td>The timeout period in milliseconds to read from the device.</td>
<td>Integer</td>
<td>Default is 10000</td>
</tr>

<tr class="even">
<td>Retries</td>
<td>Number of retries to read from the device</td>
<td>Integer</td>
<td>Default is 2</td>
</tr>

<tr class="odd">
<td>ReadBatchSize</td>
<td>Number of values to read in a batch</td>
<td>Integer</td>
<td>Default is 100</td>
</tr>

<tr class="even">
<td>SnmpVersion</td>
<td>Used SNMP version</td>
<td>Integer</td>
<td>Supported versions are 1 and 2, default is 2</td>
</tr>

<tr class="odd">
<td>Community</td>
<td>SNMP community string for SNMP V1 and V2</td>
<td>String</td>
<td>Default is "public"</td>
</tr>

</tbody>
</table>

[^top](#snmp-protocol-configuration)