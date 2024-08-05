# PCCC Protocol Configuration

This section describes the configuration types for the PCCC protocol adapter and contains the extensions and specific
configuration types
- [PCCC Addressing](#pccc-addressing)
- [PcccSourceConfiguration](#pcccsourceconfiguration)
- [PcccChannelConfiguration](#pcccchannelconfiguration)
- [PcccAdapterConfiguration](#pcccadapterconfiguration)
- [PcccControllerConfiguration](#pccccontrollerconfiguration)
- [PcccConnectPathConfiguration](#pcccconnectpathconfiguration)

[Protocol Adapters](./README.md)

## PCCC Addressing

The following datatype with their addresses can be used as the value of “Address” in a PcccChannel.

Datatype OUPUT, Prefix O

Default file number 0

Syntax: 0\<filenumber\>:\<element index\>\[/bit offset\]\[,arraylen\]

**O0:0** First 16 output bits as Boolean values in logical order, the bit at offset 0 becomes the first item in the
array.

<img src="./img/pccc/image1.png" style="width:4.42951in;height:0.58019in" />

**O0:0.1** Second set of 16 output bites as 16 Boolean values in logical order

<img src="./img/pccc/image2.png" style="width:4.28618in;height:0.56142in" />

**O0:0,2** First 32 output bits as 2 sets of 16 Boolean values in logical order

<img src="./img/pccc/image3.png" style="width:4.26523in;height:0.55867in" />

**O0:0/0** First output at offset 0 bit as Boolean value

<img src="./img/pccc/image4.png" style="width:4.22175in;height:0.55298in" />

**O0:0/15** Fifteenth output bit at offset 15 as Boolean value

<img src="./img/pccc/image5.png" style="width:4.15827in;height:0.54466in" />

**Datatype INPUT, Prefix I**

Default file number 1

Syntax: 0\<file number\>:\<element index\>\[/bit offset\]\[,array len\]

**I1:0** First 16 input bits as Boolean values in logical order, the bit at offset 0 becomes the first item in the
array.

<img src="./img/pccc/image6.png" style="width:4.20977in;height:0.55456in" />

**I1:0.1.** Second set of 16 input bites as 16 Boolean values in logical

<img src="./img/pccc/image7.png" style="width:4.18124in;height:0.5508in" />

**I1:0,2** First 32 input bits as 2 sets of 16 Boolean values in logical order

<img src="./img/pccc/image8.png" style="width:4.12484in;height:0.54337in" />

**I1:0/0**. First input bit at offset 0 as Boolean value

<img src="./img/pccc/image9.png" style="width:4.11139in;height:0.5416in" />

**O0:0/15** Fifteenth output at offset 15 bit as Boolean value

<img src="./img/pccc/image10.png" style="width:4.09619in;height:0.5396in" />

**Datatype BINARY, Prefix B**

Default file number 3

Syntax: B\<file number\>:\<element index\>\[/bit offset\]

**B3:0** First 16 binary bits as Boolean values in logical order, the bit shown below at offset 0 becomes the first item
in the array.

<img src="./img/pccc/image11.png" style="width:4.16323in;height:0.55777in" />

**B3:0.1** Second set of 16 binary bites as 16 Boolean values in logical

<img src="./img/pccc/image12.png" style="width:4.20022in;height:0.56272in" />

**B3:0/0.** First binary bit at offset as Boolean value

<img src="./img/pccc/image13.png" style="width:4.10473in;height:0.54993in" />

**B3:0/15** Fifteenth binary bit at offset 15 as Boolean value

<img src="./img/pccc/image14.png" style="width:4.10599in;height:0.5501in" />

**Datatype TIMER, Prefix T**

Default file number 4

Syntax: T\<file number\>:\<element index\>\[/bit offset\] for bit values

T\<file number\>:\<element index\>\[.value by name\] for named numeric values

**T4:0** Timer as a structure containing all elements

<img src="./img/pccc/image15.png" style="width:3.05094in;height:0.57004in" />

**T4:0.ACC** Timer numeric ACC value.

<img src="./img/pccc/image16.png" style="width:3.05626in;height:0.57104in" />

**T4:0.EN** Timer Boolean EN bit value

<img src="./img/pccc/image17.png" style="width:3.04003in;height:0.56801in" />

**Datatype COUNTER, Prefix C**

Default file number 5

Syntax: C\<file number\>:\<element index\>\[/bit offset\] for bit values

C\<file number\>:\<element index\>\[.value by name\] for named numeric values

**C5:0** Counter as a structure containing all elements

<img src="./img/pccc/image18.png" style="width:2.96261in;height:0.55884in" />

**C5:0.ACC Counter ACC numeric value**

<img src="./img/pccc/image19.png" style="width:2.99306in;height:0.56458in" />

**C5:0.ACC Counter CU bit value**

<img src="./img/pccc/image20.png" style="width:3.06239in;height:0.57766in" />

**Datatype CONTROL, Prefix R**

Default file number 6

Syntax: R\<file number\>:\<element index\>\[/bit offset\] for bit values

R\<file number\>:\<element index\>\[.value by name\] for named numeric values

**R6:0** Control as a structure containing all elements

<img src="./img/pccc/image21.png" style="width:3.18222in;height:0.51584in" />

**R6:0.POS Counter POS numeric value**

<img src="./img/pccc/image22.png" style="width:3.14338in;height:0.50954in" />

**R6:0.ACC Control EN bit value**

<img src="./img/pccc/image23.png" style="width:3.08741in;height:0.50047in" />

**Datatype INTEGER, Prefix N (16 bit)**

Default file number 7

Syntax: N\<file number\>:\<element index\>\[\<array len\>\]

**N7:0 First 16 bits integer value**

<img src="./img/pccc/image24.png" style="width:5.79437in;height:0.57263in" />

**N7:1 Second 16 bits integer value**

<img src="./img/pccc/image25.png" style="width:5.4056in;height:0.53421in" />

**N7:0,3 First 3 16 bits integer values**

<img src="./img/pccc/image26.png" style="width:5.3826in;height:0.53193in" />

**Datatype FLOAT, Prefix F**

Syntax: F\<file number\>:\<element index\>\[\<array len\>\]

**F8:0 First float value**

<img src="./img/pccc/image27.png" style="width:5.37544in;height:0.53238in" />

**F8:1 Second float value**

<img src="./img/pccc/image28.png" style="width:5.52594in;height:0.54728in" />

**F8:0,2 First 2 float value**

<img src="./img/pccc/image29.png" style="width:5.39762in;height:0.53457in" />

**Datatype STRING, Prefix ST**

Syntax: ST\<file number\>:\<element index\>

**ST9:0 First string value**

<img src="./img/pccc/image30.png" style="width:1.74001in;height:0.52925in" />

**ST9:1 Second string value**

<img src="./img/pccc/image31.png" style="width:1.74041in;height:0.52938in" />

**Datatype LONG, Prefix L (32 bit)**

Syntax: L\<file number\>:\<element index\>\[\<array len\>\]

**L10:0 First 32 bits integer value**

<img src="./img/pccc/image32.png" style="width:5.38738in;height:0.53298in" />

**L10:1 Second 32 bits integer value**

<img src="./img/pccc/image33.png" style="width:5.3943in;height:0.53367in" />

**L10:0,3 First 3 32 bits integer values**

<img src="./img/pccc/image34.png" style="width:5.41913in;height:0.53612in" />

**Datatype ASCII, Prefix A**

Default file number 11

Syntax: A\<file number\>:\<element index\>\[/character offset\]

**A11:0 First character pair**

<img src="./img/pccc/image35.png" style="width:5.99478in;height:0.56762in" />

**A11:0 Second character pair**

<img src="./img/pccc/image36.png" style="width:6.42308in;height:0.60417in" />

**A11:0/0 First character of in first character pair**

<img src="./img/pccc/image37.png" style="width:6.42292in;height:0.60417in" />

**A11:0/0 Second character of in seconds character pair**

<img src="./img/pccc/image38.png" style="width:6.42319in;height:0.60417in" />

[^top](#pccc-protocol-configuration)


## PcccSourceConfiguration

The PCCCSourceConfiguration extends the common Source configuration with PCCC specific source configuration data

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
<td><p>The channels configuration for a PCCC source holds configuration data to read values from fields on the source controller.</p>
<p>The element is a map indexed by the channel identifier.</p>
<p>Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.</p></td>
<td>Map[String,<a href="#pcccchannelconfiguration">PcccChannelConfiguration</a>]</td>
<td>At least 1 channel must be configured.</td>
</tr>

<tr class="odd">
<td>AdapterController</td>
<td>Server Identifier for the controller to read from. This referenced server must be present in the Controllers section of the adapter referred to by the ProtocolAdapter attribute of the source.</td>
<td>String</td>
<td>Must be an identifier of a server in the Controllers section of the PCCC adapter used by the source.</td>
</tr>

</tbody>
</table>

[^top](#pccc-protocol-configuration)

## PcccChannelConfiguration


The PcccChannelConfiguration extends the common Channel configuration with PCCC specific channel configuration data


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
<td>Address</td>
<td>A string containing the address of the field to read from the controller.</td>
<td>String</td>
<td>For supported datatype and address syntax see <a href="#pccc-addressing">PCCC Addressing</a>.</td>
</tr>

</tbody>
</table>

[^top](#pccc-protocol-configuration)


## PcccAdapterConfiguration

The PcccAdapterConfiguration extends the common adapter configuration with PCCC specific adapter configuration settings. The AdapterType to use for this adapter is <strong>"PCCC"</strong>

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
<td>PLCs servers configured for this adapter. The PCCC source using the adapter must have a reference to one of these in its AdapterController attribute.</td>
<td>Map[String,<a href="#pccccontrollerconfiguration">PcccControllerConfiguration</a>]</td>
<td></td>
</tr>

</tbody>
</table>

[^top](#pccc-protocol-configuration)

## PcccControllerConfiguration

Configuration data for connecting to and reading from sources for controllers using PCCC

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
<td>Comments</td>
</tr>

<tr class="even">
<td>Address</td>
<td>IP Address of the controller</td>
<td>String</td>
<td>IP address in format aaa.bbb.ccc.ddd</td>
</tr>

<tr class="odd">
<td>Port</td>
<td>Port number</td>
<td>Integer</td>
<td>Default is 44818</td>
</tr>

<tr class="even">
<td>ConnectPath</td>
<td>Connect path for controller</td>
<td><a href="#pcccconnectpathconfiguration">PcccConnectPathConfiguration</a></td>
<td>Optional</td>
</tr>

<tr class="odd">
<td>ConnectTimeout</td>
<td>Timeout for connecting to the controller in milliseconds</td>
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

<tr class="even">
<td>OptimizeReads</td>
<td>Optimized the reading of data from the controller by combining the reads for (near) adjacent fields in a single read request.</td>
<td>Boolean</td>
<td><p>Default is true</p>
<p>Optimization reduces the calls made to the controller to read data. When troubleshooting optimization it can be disabled to find specific fields that make the (combined) reads to fail.</p></td>
</tr>

<tr class="odd">
<td>MaxReadGap</td>
<td>When optimization is used this specified the max number of bytes between near adjacent fields that may be combined in a single read.</td>
<td>Integer</td>
<td>Default is 32</td>
</tr>

</tbody>
</table>

[^top](#pccc-protocol-configuration)

## PcccConnectPathConfiguration

Configuration connect path of a controller used for routing

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
<td>Comments</td>
</tr>

<tr class="even">
<td>Backplane</td>
<td>Backplane number</td>
<td>Integer</td>
<td>Default is 1</td>
</tr>

<tr class="odd">
<td>Slot</td>
<td>Slot number</td>
<td>Integer</td>
<td>Default is 0</td>
</tr>

</tbody>
</table>

[^top](#pccc-protocol-configuration)