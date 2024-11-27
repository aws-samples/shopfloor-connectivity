
# SLMP Protocol Configuration

This section describes the configuration types for the SLMP protocol adapter and contains the extensions and specific
configuration types.


**IMPORTANT : SLMP controllers only supports a single concurrent session with the controller. When reading data by multiple schedules or adapters instances, or from another SLMP client, from the same controller, timeout and 
broken TCP pipe errors will occur.**

- [SLMP channel reading optimization](#slmp-channel-reading-optimization)
- [SlmpSourceConfiguration](#slmpsourceconfiguration)
- [SlmpChannelConfiguration](#slmpchannelconfiguration)
- [SlmpAdapterConfiguration](#slmpadapterconfiguration)
- [SlmpControllerConfiguration](#slmpcontrollerconfiguration)

[Protocol Adapters](./README.md)

### SLMP channel reading optimization
In order to reduce the number of interactions between the adapter and the controller  read action for  single BIT, WORD and DOUBLEWORD elements are combined in batches of maximum 192 values using the SLMP Read Random request.   For reading arrays of multiple values, STRING values and values of custom structured types a per channel SLMP Read request is used.

[^top](#slmp-protocol-configuration)


## SlmpSourceConfiguration

The SLMPSourceConfiguration extends the common <a href="../core/source-configuration.md">Source configuration</a> with SLMP specific source configuration data.

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
<td><p>The channels configuration for an SLMP source holds configuration data to read values from fields on the source controller.</p>
<p>The element is a map indexed by the channel identifier.</p>
<p>Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.</p></td>
<td>Map[String,<a href="#slmpchannelconfiguration">SLMPChannelConfiguration</a>]</td>
<td>At least 1 channel must be configured.</td>
</tr>

<tr class="odd">
<td>AdapterController</td>
<td>Controller Identifier for the controller to read from. This referenced server must be present in the Devices section of the adapter referred to by the ProtocolAdapter attribute of the source.</td>
<td>String</td>
<td>Must be an identifier of a server in the Controllers section of the SLMP adapter used by the source.</td>
</tr>



</tbody>
</table>

[^top](#slmp-protocol-configuration)

## SlmpChannelConfiguration

The SlmpChannelConfiguration extends the common Channel configuration with SLMP specific channel configuration data

<table>
<colgroup>
<col style="width: 15%" />
<col style="width: 19%" />
<col style="width: 22%" />
<col style="width: 43%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>AccessPoint</td>
<td>A string containing the access point for the value to read from the device.</td>
<td>String</td>
<td>
An access points consists of a device code and a decimal device number, e.g. "D200" for Data register 200, "X0" for Input 0 and "Y0" for output 0.
<p>
<p>Valid devices codes and their data types are listed below.
<p>

- "B" 		Link relay (BIT)
- "CC"		Counter coil (BIT)
- "CN"		Counter timer current value (WORD)
- "CS"		Counter contact (BIT)
- "D"		Data register (WORD)
- "DX"		Direct access input (BIT)
- "DY"		Direct access output (BIT)
- "F" 		Alarm (BIT)
- "L" 		Latching relay (BIT)
- "LCC"	    Counter coil (BIT)
- "LCN"	    Counter timer current value (DOUBLEWORD)
- "LCS"	    Counter contact (BIT)
- "LSTC"	Long retentive timer coil (BIT)
- "LSTN"	Long retentive timer current value (DOUBLEWORD)
- "LSTS"	Long retentive timer contact (BIT)
- "LTC"	    Long timer coil (BIT)
- "LTN"	    Long timer current value (DOUBLEWORD)
- "LTS"	    Long timer contact (BIT)
- "LZ"		Long index register (DOUBLEWORD)
- "M" 		Internal relay (BIT)
- "R"		File register (WORD)
- "S"		Step relay (BIT)
- "SB"		Link special relay (BIT)
- "SD"  	Special register (WORD)
- "SM"     	Special relay (Bit)
- "SW"		Link special register (WORD)
- "TC"		Timer coil (BIT)
- "TN"		Timer current value (WORD)
- "TS"		Timer contact (BIT)
- "V" 		Edge relay (BIT)
- "W"		Link register (WORD)
- "X"   	Input (BIT)
- "Y" 		Output (BIT)
- "Z"		Index register (WORD)
- "ZR"		File register ZR (WORD)
 </td>
</tr>




<tr class="odd">
<td>DataType</td>
<td>The type of data to read from the device. If no type is specified then a single value of the default type of the device is read.</td>
<td>String</td>
<td>
Valid data types are:
<p>

- "BIT" (read as a boolean value)
- "WORD" (read as a 16-bit integer value)
- "DOUBLEWORD" (read as 32-bit integer)
- "STRING(x)" (read as words and decoded to as a string of length x or shorter if the string is zero terminated)

It is possible to define custom structures and use these as a data type as well. These structures are defined in the "Structures" section of the SLMP adapter configuration. All fiels which can be any the types mentioned above, or another custom structure type, are mapped from the read word data to the fields of the structure in the order in which they are declared in the type.

<p>
In order to read multiple values, returned as an array, starting at the specified access point the number of items can be appended to the data type.

E.g. <p>
"BIT[4]" reads 4 BIT values and returns an array of 4 boolean values.
"WORD[8]" reads 16 word values and returns an array of 8 16-bit integers.
"STRING(16)[2]" reads and array of 16 characters

 </td>
</tr>


<tr class="even">
<td>Size</td>
<td>The number of values to read starting from the access point.</td>
<td>Integer</td>
<td>
The number of items to read can be specified as well in the DataType of the channel, e.g. WORD[size]. The Size setting can be used if the DataType field is omitted to read the default data type for the device. If the length is both specified in the DataType in both the Size setting a configuration error is raised.
 </td>
</tr>


</tbody>
</table>

[^top](#slmp-protocol-configuration)

## SlmpAdapterConfiguration
The SlmpAdapterConfiguration extends the common adapter configuration with SLMP specific adapter configuration settings. The AdapterType to use for this adapter is <strong>"SLMP"</strong>.


<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 39%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Controllers</td>
<td>Controllers configured for this adapter. The SLMP source using the adapter must have a reference to one of these in its AdapterController attribute.</td>
<td>Map[String,<a href="#slmpdeviceconfiguration">SlmpControllerConfiguration</a>]</td>
<td></td>
</tr>

<tr class="odd">
<td>Structures</td>
<td>Custom data structures configured for this adapter. Structured defined in this section can be uses as custom structured data types for channel values. If a structure has a field which is of a custom structure type,t hen this type must be defined first.</td>
<td>Map[String,Map{String,String]]</td>
<td>
Below is an example defining a custom structure "STRUCT1" containing two fields "A1" and "B1" of type word. This type used in a second type "STRUCT2" having a field "A2" containing an array of size 2 containing values of "STRUCT1", as well as a field "B2", containing 16 words and a field "C2" containing a 32 character string.



```json
 "Structures": {
   "STRUCT1": {
      "A1": "WORD",
      "B1" : "WORD"
    },
    "STRUCT2": {
      "A2": "STRUCT1[2]",
      "B2": "WORD[16]",
      "C2": "STRING(32)"
    }
}
```

A SLMP channel can now use both type "STRUCT1" as "STRUCT2" as a DataType. The data is returned as a map of values indexed by the names of the fields.

</td>
</tr>


</tbody>
</table>

[^top](#slmp-protocol-configuration)

## SlmpControllerConfiguration

Configuration data for connecting to and reading from sources from devices using SLMP protocol

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 25%" />
</colgroup>

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
<td>IP address in format aaa.bbb.ccc.ddd or a hostname</td>
</tr>

<tr class="odd">
<td>Port</td>
<td>Port number</td>
<td>Integer</td>
<td>Default is 48898</td>
</tr>

<tr class="even">
<td>CommandTimeout</td>
<td>Timeout for executing commands in milliseconds</td>
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
<td>Default is 50000</td>
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

<tr class="even">
<td>NetworkNumber</td>
<td>Request destination network number</td>
<td>Integer</td>
<td>Default is 0 (0x00)</td>
</tr>

<tr class="odd">
<td>StationNumber</td>
<td>Request station number</td>
<td>Integer</td>
<td>Default is 255 (0xFF) </td>
</tr>

<tr class="even">
<td>ModuleNumber</td>
<td>Request module number</td>
<td>Integer</td>
<td>Default is 1023 (0x03FF) </td>
</tr>

<tr class="odd">
<td>MultiDropStationNumber</td>
<td>Request multidrop station number</td>
<td>Integer</td>
<td>Default is 0 (0x00)</td>
</tr>

<tr class="even">
<td>MonitoringTimer</td>
<td>Timer to set the waiting time until the access destination send back a response after the SLMP compatible device
which received a request message from the external device requests a processing to the destination in units of 250ms</td>
<td>Integer</td>
<td>Default is 0 (unlimited wait)</td>
</tr>


</tbody>
</table>


[^top](#slmp-protocol-configuration)

---

