# Modbus TCP Protocol Configuration


---
- [ModbusSourceConfiguration](#ModbusSourceConfiguration)
- [ModbusOptimization](#ModbusOptimization)
- [ModbusChannelConfiguration](#ModbusChannelConfiguration)
- [ModbusTcpAdapterConfiguration](#ModbusTcpAdapterConfiguration)
- [ModbusTcpDeviceConfiguration](#ModbusTcpDeviceConfiguration)

---

## ModbusSourceConfiguration


**Properties:**
- [AdapterDevice](#AdapterDevice)
- [Channels](#Channels)
- [Optimization](#Optimization)
- [ReadTimeout](#ReadTimeout)

---
### AdapterDevice
Identifier for the device to read from. This referenced device must be present in the Devices section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a device in the Devices section of the MODBUS-TCP adapter used by the source.
Note this is not the modbus device is, this id is set in the DeviceId attribute of the referenced device.

---
### Channels
The channels hold configuration data to read values from the Modbus source device.
The element is a map indexed by the channel identifier.
Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,ModbusChannelConfiguration]

At least 1 channel must be configured.

---
### Optimization
Optimization for combining reading values from adjacent or near adjacent in a single read request.

**Type**: ModbusOptimization

Default optimization is enabled with a [RegisterMaxGapSize](#RegisterMaxGapSize) of 8 and a [CoilMaxGapSize](#CoilMaxGapSize) of 16.

---
### ReadTimeout
Timeout for reading from Modbus device in milliseconds.

**Type**: Integer

Default is 10000.

[^top](#modbus-tcp-protocol-configuration)




## ModbusOptimization


**Properties:**
- [Active](#Active)
- [RegisterMaxGapSize](#RegisterMaxGapSize)
- [RegisterMaxGapSize](#RegisterMaxGapSize)

---
### Active
State of optimization.

**Type**: Boolean

Default is true

---
### RegisterMaxGapSize
The maximum gap between register addresses to combine read actions in a single request.

**Type**: Integer

Default is 8

---
### CoilMaxGapSize
The maximum gap between the coil and distinct input addresses to combine read actions in a single request.

**Type**: Integer

Default is 16

[^top](#modbus-tcp-protocol-configuration)




## ModbusChannelConfiguration


**Properties:**
- [Address](#Address)
- [Size](#Size)
- [Type](#Type)

---
### Address
Modbus address of the channel.

**Type**: Integer

---
### Size
The number of values to read.

**Type**: Integer

Default is 1.
The maximum for reading coils and discrete inputs is 2000.
The maximum for reading registers is 125.

---
### Type
Modbus channel type to read from

**Type**: String, any of 

- “Coil”
- “DiscreteInput”,
- HoldingRegister”
- “InputRegister”

[^top](#modbus-tcp-protocol-configuration)




## ModbusTcpAdapterConfiguration


**Properties:**
- [Devices](#Devices)

---
### Devices
Modbus devices configured for this adapter. The modbus tcp source using the adapter must refer to one of these servers with the AdapterDevice attribute.

**Type**: Map[String,[ModbusTcpDeviceConfiguration](#ModbusTcpDeviceConfiguration)]



[^top](#modbus-tcp-protocol-configuration)




## ModbusTcpDeviceConfiguration


**Properties:**
- [Address](#Address)
- [ConnectTimeout](#ConnectTimeout)
- [DeviceId](#DeviceId)
- [Port](#Port)
- [WaitAfterConnectError](#WaitAfterConnectError)
- [WaitAfterReadError](#WaitAfterReadError)

---
### Address
Address of the device

**Type**: String

---
### ConnectTimeout
The timeout period in milliseconds to connect to the device.

**Type**: Integer

Default is 1000, the minimum value is 1000

---
### DeviceId
Modbus device identifier

**Type**: Integer

Default is 1

---
### Port
Port on the device

**Type**: Integer

Default is 502

---
### WaitAfterConnectError
The period in milliseconds to wait after a connection failure.

**Type**: Integer

Default is 10000, the minimum value is 1000

---
### WaitAfterReadError
The period in milliseconds to wait after a read failure.

**Type**: Integer

Default is 10000, the minimum value is 1000

[^top](#modbus-tcp-protocol-configuration)

