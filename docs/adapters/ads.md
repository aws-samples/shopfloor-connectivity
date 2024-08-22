# ADS Protocol Configuration

This section describes the configuration types for the ADS protocol adapter and contains the extensions and specific
configuration types.

<p>The AdsSourceConfiguration extends the common <a href="../core/source-configuration.md">Source configuration</a> with ADS specific source configuration data.</p>

- [AdsSourceConfiguration](#adssourceconfiguration)
- [AdsChannelConfiguratio](#adschannelconfiguration)
- [AdsAdapterConfiguration](#adsadapterconfiguration)
- [AdsDeviceConfiguration](#adsdeviceconfiguration)

[Protocol Adapters](./README.md)

## AdsSourceConfiguration

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 25%" />
<col style="width: 32%" />
<col style="width: 22%" />
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
<td><p>The channels configuration for an ADS source holds configuration data to read values from fields on the source controller.</p>
<p>The element is a map indexed by the channel identifier.</p>
<p>Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.</p></td>
<td>Map[String,<a href="#adschannelconfiguration">AdsChannelConfiguration</a>]</td>
<td>At least 1 channel must be configured.</td>
</tr>

<tr class="odd">
<td>AdapterDevice</td>
<td>Device Identifier for the controller to read from. This referenced server must be present in the Devices section of the adapter referred to by the ProtocolAdapter attribute of the source.</td>
<td>String</td>
<td>Must be an identifier of a server in the Controllers section of the ADS adapter used by the source.</td>
</tr>

<tr class="even">
<td>SourceAmsId</td>
<td><p>The Ams netID of the device.</p>
</td>
<td>String</td>
<td>The AMS Net ID consists of 6 bytes and is represented in a dot notation.</td>
</tr>

<tr class="odd">
<td>SourceAmsPort</td>
<td>The ADS port number. ADS devices in the TwinCAT network are identified by an AMS network address and a port number.</td>
<td>Integer</td>
<td>The following decimal port numbers are invariantly defined on each TwinCAT single system.

- Runtime system 1: 851 (in TwinCAT 2: 801)
- Runtime system 2: 852 (in TwinCAT 2: 811)
- Runtime system 3: 853 (in TwinCAT 2: 821)
- Runtime system 4: 854 (in TwinCAT 2: 831)
- Runtime system 5: 855
- Runtime system n: 850 + n, etc.</td>
</tr>

<tr class="even">
<td>TargetAmsId</td>
<td><p>The AMS Net ID of the client.</p>
</td>
<td>String</td>
<td>The AMSNetID consists of 6 bytes and is represented in a dot notation. For clients this is typically the network address + .1.1, e.g. 192.168.1.65.1.1<p>
To authorize the client this AMS Net ID must be added as an AMS route in the SYSTEM/Routes of the Twincat target.</p></td>
</tr>

<tr class="odd">
<td>TargetAmsPort</td>
<td>Contains the ADS port number of the client.</td>
<td>Integer</td>
<td> This can be any value.</td>
</tr>

</tbody>
</table>

[^top](#ads-protocol-configuration)

## AdsChannelConfiguration

<table>
<colgroup>
<col style="width: 15%" />
<col style="width: 19%" />
<col style="width: 22%" />
<col style="width: 43%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p><strong>AdsChannelConfiguration</strong></p>
<p><strong>Extends ChannelConfiguration</strong></p>
<p>The AdsChannelConfiguration extends the common Channel configuration with ADS specific channel configuration data</p></th>

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
<td>SymbolName</td>
<td>A string containing the name of the symbol to read from the device.</td>
<td>String</td>
<td></td>

</tr>
</tbody>
</table>

[^top](#ads-protocol-configuration)

## AdsAdapterConfiguration

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 39%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p><strong>AdsAdapterConfiguration</strong></p>
<p><strong>Extends ProtocolAdapterConfiguration</strong></p>
<p>The AdsAdapterConfiguration extends the common adapter configuration with ADS specific adapter configuration settings. The AdapterType to use for this adapter is "ADS".</p></th>
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
<td>Controllers</td>
<td>Devices configured for this adapter. The ADS source using the adapter must have a reference to one of these in its AdapterDevice attribute.</td>
<td>Map[String,<a href="#adsdeviceconfiguration">AdsDeviceConfiguration</a>]</td>
<td></td>
</tr>
</tbody>
</table>

[^top](#ads-protocol-configuration)

## AdsDeviceConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p><strong>AdsDeviceConfiguration</strong></p>
<p>Configuration data for connecting to and reading from sources from devices using ADS protocol</p></th>
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
<td>Address</td>
<td>IP Address of the device</td>
<td>String</td>
<td>IP address in format aaa.bbb.ccc.ddd</td>
</tr>
<tr class="odd">
<td>Port</td>
<td>Port number</td>
<td>Integer</td>
<td>Default is 48898</td>
</tr>
<tr class="even">
<td>CommandTimeout</td>
<td>Timeout for executing commands in millisecond fs</td>
<td>Integer</td>
<td>Default is 10000 milliseconds</td>
</tr>
<tr class="odd">
<td>ConnectTimeout</td>
<td>Timeout for connecting to the device in milliseconds</td>
<td>Integer</td>
<td>Default is 10000</td>
</tr>
<tr class="even">
<td>ReadTimeout</td>
<td>Timeout for reading response packets from the controller in milliseconds</td>
<td>Integer</td>
<td>Default is 10000</td>
</tr>
<tr class="odd">
<td>WaitAfterConnectError</td>
<td>Time to wait before (re)connecting after a connection error in milliseconds</td>
<td>Integer</td>
<td>Default is 10000</td>
</tr>
<tr class="even">
<td>WaitAfterReadError</td>
<td>Time to wait before reading values from the controller after a read error in milliseconds</td>
<td>Integer</td>
<td>Default is 10000</td>
</tr>
<tr class="odd">
<td>WaitAfterWriteError</td>
<td>Time to wait after an error writing request packets to the controller in milliseconds</td>
<td>Integer</td>
<td>Default is 10000</td>
</tr>
</tbody>
</table>


[^top](#ads-protocol-configuration)

