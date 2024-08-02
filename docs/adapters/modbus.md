# Modbus TCP Protocol Configuration

This section describes the configuration types for the Modbus TCP protocol adapter and contains the extensions and
specific configuration types.


- [ModbusSourceConfiguration](#modbussourceconfiguration)
- [ModbusOptimization](#modbusoptimization)
- [ModbusChannelConfiguration](#modbuschannelconfiguration)
- [ModbusTcpAdapterConfiguration](#modbustcpadapterconfiguration)
- [ModbusTcpDeviceConfiguration](#modbustcpdeviceconfiguration)

[Protocol Adapters](protocol-adapters.md)

## ModbusSourceConfiguration

The ModbusSourceConfiguration extends the common Source configuration with Modbus specific source device configuration data</p></th>


<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 25%" />
<col style="width: 32%" />
<col style="width: 22%" />
</colgroup>
<thead>
<tr class="header">

<p></tr>

</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Channels</td>
<td>The channels hold configuration data to read values from the Modbus source device.</p>
<p>The element is a map indexed by the channel identifier.</p>
<p>Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.</p></td>
<td>Map[String,<a href="#modbuschannelconfiguration">ModbusChannelConfiguration</a>]</td>
<td>At least 1 channel must be configured.</td>
</tr>

<tr class="odd">
<td>AdapterDevice</td>
<td Identifier for the device to read from. This referenced device must be present in the Devices section of the adapter referred to by the ProtocolAdapter attribute of the source.</td>
<td>String</td>
<td><p>Must be an identifier of a device in the Devices section of the MODBUS-TCP adapter used by the source.</p>
<p>Note this is not the modbus device is, this id is set in the DeviceId attribute of the referenced device.</p></td>
</tr>

<tr class="even">
<td>Optimization</td>
<td>Optimization for combining reading values from adjacent or near adjacent in a single read request.</td>
<td><a href="#modbusoptimization">ModbusOptimization</a></td>
<td>Default optimization is enabled with a RegisterMaxGapSize of 8 and a CoilMaxGapSize of 16.</td>
</tr>

<tr class="odd">
<td >ReadTimeout</td>
<td >Timeout for reading from Modbus device in milliseconds.</td>
<td >Integer</td>
<td>Default is 10000.</td>
</tr>

</tbody>
</table>

[^top](#modbus-tcp-protocol-configuration)

## ModbusOptimization

The ModbusOptimization contains the optimization parameters for combining reading values from adjacent or near adjacent addresses in a single read request.

<table>
<col style="width: 19%" />
<col style="width: 25%" />
<col style="width: 32%" />
<col style="width: 22%" />

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Active</td>
<td>State of optimization.</td>
<td>Boolean</td>
<td>true</td>
</tr>

<tr class="odd">
<td>RegisterMaxGapSize</td>
<td>The maximum gap between register addresses to combine read actions in a single request.</td>
<td>Integer</td>
<td>Default is 8</td>
</tr>

<tr class="even">
<td>RegisterMaxGapSize</td>
<td>The maximum gap between the coil and distinct input addresses to combine read actions in a single request.</td>
<td>Integer</td>
<td>Default is 16</td>
</tr>

</tbody>
</table>

[^top](#modbus-tcp-protocol-configuration)

## ModbusChannelConfiguration

The ModbusChannelConfiguration extends the common Channel configuration with Modbus specific channel configuration data

<table>
<col style="width: 19%" />
<col style="width: 25%" />
<col style="width: 32%" />
<col style="width: 22%" />

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Type</td>
<td>Modbus channel type to read from</td>
<td>String, any of “Coil”, “DiscreteInput”, “HoldingRegister” or “InputRegister”</td>
<td></td>
</tr>

<tr class="odd">
<td>Address</td>
<td>Modbus address of the channel.</td>
<td>Integer</td>
<td></td>
</tr>

<tr class="even">
<td>Size</td>
<td>The number of values to read.</td>
<td>Integer</td>
<td><p>Default is 1.</p>
<p>The maximum for reading coils and discrete inputs is 2000.</p>
<p>The maximum for reading registers is 125.</p></td>
</tr>

</tbody>
</table>

[^top](#modbus-tcp-protocol-configuration)

## ModbusTcpAdapterConfiguration

he ModbusTcpAdapterConfiguration extends the common adapter configuration with Modbus TCP specific adapter configuration settings. The AdapterType to use for this adapter is "MODBUS-TCP"
<br>
<table>
<col style="width: 19%" />
<col style="width: 25%" />
<col style="width: 32%" />
<col style="width: 22%" />

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Devices</td>
<td>Modbus devices configured for this adapter. The modbus tcp source using the adapter must refer to one of these servers with the AdapterDevice attribute.</td>
<td>Map[String,<a href="#modbustcpdeviceconfiguration">ModbusTcpDeviceConfiguration</a>]</td>
<td></td>
</tr>
</tbody>
</table>

[^top](#modbus-tcp-protocol-configuration)

## ModbusTcpDeviceConfiguration

Configuration data for connecting to and reading from a Modbus TCP device.

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
<td>Address of the device</td>
<td>String</td>
<td></td>
</tr>

<tr class="odd">
<td>Port</td>
<td>Port on the device</td>
<td>Integer</td>
<td>Default is 502</td>
</tr>

<tr class="even">
<td>DeviceId</td>
<td>Modbus device identifier</td>
<td>Integer</td>
<td>Default is 1</td>
</tr>

<tr class="odd">
<td>ConnectTimeout</td>
<td>	The timeout period in milliseconds to connect to the device.</td>
<td>Integer</td>
<td>Default is 1000, the minimum value is 1000</td>
</tr>

<tr class="even">
<td>WaitAfterConnectError</td>
<td>The period in milliseconds to wait after a connection failure.</td>
<td>Integer</td>
<td>Default is 10000, the minimum value is 1000</td>
</tr>

<tr class="odd">
<td>WaitAfterReadError</td>
<td>The period in milliseconds to wait after a read failure.</td>
<td>Integer</td>
<td>Default is 10000, the minimum value is 1000</td>
</tr>

</tbody>
</table>

[^top](#modbus-tcp-protocol-configuration)