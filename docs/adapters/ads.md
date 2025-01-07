
# ADS Protocol Configuration


---
- [AdsSourceConfiguration](#AdsSourceConfiguration)
- [AdsChannelConfiguration](#AdsChannelConfiguration)
- [AdsAdapterConfiguration](#AdsAdapterConfiguration)
- [AdsDeviceConfiguration](#AdsDeviceConfiguration)

---

## AdsSourceConfiguration


**Properties:**
- [AdapterDevice](#AdapterDevice)
- [Channels](#Channels)
- [SourceAmsId](#SourceAmsId)
- [SourceAmsPort](#SourceAmsPort)
- [TargetAmsId](#TargetAmsId)
- [TargetAmsPort](#TargetAmsPort)

---
### AdapterDevice
Device Identifier for the controller to read from. This referenced server must be present in the Devices section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a server in the [Controllers](#Controllers) section of the [ADS adapter](#AdsAdapterConfiguration) used by the source.

---
### Channels
The channels configuration for an ADS source holds configuration data to read values from fields on the source controller.
The element is a map indexed by the channel identifier.
Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,[AdsChannelConfiguration](#AdsChannelConfiguration)]

At least 1 channel must be configured.

---
### SourceAmsId
The Ams netID of the device.


**Type**: String

The AMS Net ID consists of 6 bytes and is represented in a dot notation.

---
### SourceAmsPort
The ADS port number. ADS devices in the TwinCAT network are identified by an AMS network address and a port number.

**Type**: Integer

**Default,Constraints,Examples**: The following decimal port numbers are invariant defined on each TwinCAT single system.

- Runtime system 1: 851 (in TwinCAT 2: 801)
- Runtime system 2: 852 (in TwinCAT 2: 811)
- Runtime system 3: 853 (in TwinCAT 2: 821)
- Runtime system 4: 854 (in TwinCAT 2: 831)
- Runtime system 5: 855
- Runtime system n: 850 + n, etc.

---
### TargetAmsId
The AMS Net ID of the client.


**Type**: String

The AMSNetID consists of 6 bytes and is represented in a dot notation. For clients this is typically the network address + .1.1, e.g. 192.168.1.65.1.1
To authorize the client this AMS Net ID must be added as an AMS route in the SYSTEM/Routes of the Twincat target.

---
### TargetAmsPort
Contains the ADS port number of the client.

**Type**: Integer

This can be any value.

[^top](#ads-protocol-configuration)




## AdsChannelConfiguration

**Properties:**

- [SymbolName](#SymbolName)

---
### SymbolName
A string containing the name of the symbol to read from the device.

**Type**: String

[^top](#ads-protocol-configuration)




## AdsAdapterConfiguration


**Properties:**
- [Controllers](#Controllers)

---
### Controllers
Devices configured for this adapter. The ADS source using the adapter must have a reference to one of these in its AdapterDevice attribute.

**Type**: Map[String,[AdsDeviceConfiguration](#AdsDeviceConfiguration)]

[^top](#ads-protocol-configuration)




## AdsDeviceConfiguration


**Properties:**
- [Address](#Address)
- [CommandTimeout](#CommandTimeout)
- [ConnectTimeout](#ConnectTimeout)
- [Port](#Port)
- [ReadTimeout](#ReadTimeout)
- [WaitAfterConnectError](#WaitAfterConnectError)
- [WaitAfterReadError](#WaitAfterReadError)
- [WaitAfterWriteError](#WaitAfterWriteError)

---
### Address
IP Address of the device

**Type**: String

IP address in format aaa.bbb.ccc.ddd

---
### CommandTimeout
Timeout for executing commands in millisecond fs

**Type**: Integer

Default is 10000 milliseconds

---
### ConnectTimeout
Timeout for connecting to the device in milliseconds

**Type**: Integer

Default is 10000

---
### Port
Port number

**Type**: Integer

Default is 48898

---
### ReadTimeout
Timeout for reading response packets from the controller in milliseconds

**Type**: Integer

Default is 10000

---
### WaitAfterConnectError
Time to wait before (re)connecting after a connection error in milliseconds

**Type**: Integer

Default is 10000

---
### WaitAfterReadError
Time to wait before reading values from the controller after a read error in milliseconds

**Type**: Integer

Default is 10000

---
### WaitAfterWriteError
Time to wait after an error writing request packets to the controller in milliseconds

**Type**: Integer

Default is 10000

[^top](#ads-protocol-configuration)

