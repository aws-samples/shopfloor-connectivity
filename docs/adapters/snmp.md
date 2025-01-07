# SNMP Protocol Configuration


---
- [SnmpSourceConfiguration](#SnmpSourceConfiguration)
- [SnmpChannelConfiguration](#SnmpChannelConfiguration)
- [SnmpAdapterConfiguration](#SnmpAdapterConfiguration)
- [SnmpDeviceConfiguration](#SnmpDeviceConfiguration)

---

## SnmpSourceConfiguration


**Properties:**
- [AdapterDevice](#AdapterDevice)
- [Channels](#Channels)

---
### AdapterDevice
Identifier for the device to read from. This referenced device must be present in the Devices section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a device in the Devices section of the SNMP adapter used by the source.
Note this is not the modbus device is, this id is set in the DeviceId attribute of the referenced device.

---
### Channels
The channels hold configuration data to read values from the SNMP source devices.
The element is a map indexed by the channel identifier.
Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,[SnmpChannelConfiguration](#SnmpChannelConfiguration)]

At least 1 channel must be configured.

[^top](#snmp-protocol-configuration)




## SnmpChannelConfiguration

**Properties:**

- [ObjectId](#ObjectId)



---
### ObjectId
ID of the object to read

**Type**: string

Must be in valid dot format notation

[^top](#snmp-protocol-configuration)




## SnmpAdapterConfiguration


**Properties:**
- [Devices](#Devices)
- 

---
### Devices
Snmp devices configured for this adapter. The modbus tcp source using the adapter must refer to one of these servers with the AdapterDevice attribute.

**Type**: Map[String,[SnmpDeviceConfiguration](#SnmpDeviceConfiguration)]

[^top](#snmp-protocol-configuration)




## SnmpDeviceConfiguration


**Properties:**
- [Address](#Address)
- [Community](#Community)
- [NetworkProtocol](#NetworkProtocol)
- [Port](#Port)
- [ReadBatchSize](#ReadBatchSize)
- [Retries](#Retries)
- [SnmpVersion](#SnmpVersion)
- [Timeout](#Timeout)

---
### Address
IP Address of the device

**Type**: String

---
### Community
SNMP community string for SNMP V1 and V2

**Type**: String

Default is "public"

---
### NetworkProtocol
"UDP" or "TCP" protocol

**Type**: String

Default is UDP

---
### Port
The port on the device

**Type**: Integer

Default is 161

---
### ReadBatchSize
Number of values to read in a batch

**Type**: Integer

Default is 100

---
### Retries
Number of retries to read from the device

**Type**: Integer

Default is 2

---
### SnmpVersion
Used SNMP version

**Type**: Integer

Supported versions are 1 and 2, default is 2

---
### Timeout
The timeout period in milliseconds to read from the device.

**Type**: Integer

Default is 10000

[^top](#snmp-protocol-configuration)

