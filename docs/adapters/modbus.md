# Modbus TCP Protocol Configuration

The Modbus TCP protocol adapter enables communication with devices supporting the Modbus TCP protocol over TCP/IP networks. It supports standard Modbus functions for reading and writing coils, discrete inputs, holding registers, and input registers. Configure device connections using IP address and port, specify unit IDs, and define register addresses and data types for your channels. The adapter handles all protocol-specific details, making it easy to integrate Modbus device data into your SFC infrastructure.

In order to use this adapter as an [in-process](file:///Applications/Typora.app/Contents/Resources/sfc-running-adapters.md#running-protocol-adapters-in-process) type adapter, the type must be added to the [AdapterTypes](../core/sfc-configuration.md#adaptertypes) section in the [SFC configuration file](file:///Applications/Typora.app/Contents/Resources/core/sfc-configuration.md).

```json
"AdapterTypes" :{
  "MODBUS-TCP" : {
    "JarFiles" : ["<location of deployment>/modbus-tcp/lib"]
  },
  "FactoryClassName" : "com.amazonaws.sfc.modbus.tcp.ModbusTcpAdapter"
}
```



**Configuration**:

- [ModbusSourceConfiguration](#modbussourceconfiguration)
- [ModbusOptimization](#modbusoptimization)
- [ModbusChannelConfiguration](#modbuschannelconfiguration)
- [ModbusTcpAdapterConfiguration](#modbustcpadapterconfiguration)
- [ModbusTcpDeviceConfiguration](#modbustcpdeviceconfiguration)

## ModbusSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) >  [Source](../core/source-configuration.md) 

Source configuration for the Modbus TCP protocol adapter. This type extends the SourceConfiguration type and defines the necessary parameters to establish communication with Modbus TCP devices. It specifies the device connection details and channel configurations for data collection.Source configuration for the Modbus protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type. 

- [Schema](#modbussourceconfiguration-schema)
- [Example](#modbussourceconfiguration-example)

**Properties:**
- [AdapterDevice](#adapterdevice)
- [Channels](#channels)
- [Optimization](#optimization)
- [ReadTimeout](#readtimeout)

---

### AdapterDevice
The identifier that references a specific Modbus TCP device defined in the Devices section of the adapter configuration. This identifier must match a DeviceId in the adapter's Devices section and is used to establish which physical Modbus device to communicate with. Note that this is not the Modbus Unit ID (slave address), but rather the logical device identifier used within the SFC configuration.

**When multiple sources read from the same device, by using the same IP address, then a device for each source must be configured for each source to use.**

**Type**: String

---
### Channels
A collection of channel configurations that define what data to read from the Modbus TCP device. Each channel is identified by a unique key in the map and contains the configuration for reading specific registers or coils. Channels can be temporarily disabled by prefixing the channel identifier with '#'. The channel identifier serves as both the map key and the name used to reference the data point in the SFC system.

**Type**: Map[String,[ModbusChannelConfiguration](#modbuschannelconfiguration)

At least 1 channel must be configured.

---
### Optimization
Settings that control how the adapter optimizes read operations by combining multiple register or coil reads into single Modbus requests. When enabled, the adapter will analyze channel configurations to group adjacent or nearby addresses into consolidated read operations, reducing network traffic and improving performance.

**Type**: ModbusOptimization

Default optimization is enabled with a [RegisterMaxGapSize](#registermaxgapsize) of 8 and a [CoilMaxGapSize](#coilmaxgapsize) of 16.

---
### ReadTimeout
The maximum time, in milliseconds, to wait for a response from the Modbus device when executing a read request. If the device does not respond within this time period, the read operation will be considered failed and an error will be raised. This setting helps prevent the adapter from hanging when communication issues occur.

**Type**: Integer

Default is 10000.

[^top](#modbus-tcp-protocol-configuration)\

### ModbusSourceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ModbusSourceConfiguration",
  "type": "object",
  "allOf": [
    {
      "$ref": "external-schema.json#/definitions/SourceConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AdapterDevice": {
          "type": "string",
          "description": "Identifier of the Modbus adapter device"
        },
        "Channels": {
          "type": "object",
          "description": "Map of Modbus channel configurations indexed by string",
          "patternProperties": {
            "^.*$": {
              "$ref": "external-schema.json#/definitions/ModbusChannelConfiguration"
            }
          },
          "minProperties": 1
        },
        "Optimization": {
          "type": "boolean",
          "description": "Enable/disable Modbus optimization"
        },
        "ReadTimeout": {
          "type": "integer",
          "description": "Timeout for read operations in milliseconds",
          "minimum": 0
        }
      },
      "required": [
        "AdapterDevice",
        "Channels"
      ],
      "additionalProperties": false
    }
  ]
}

```



### ModbusSourceConfiguration Example

Minimal configuration:

```json
{
  "AdapterDevice": "ModbusDevice1",
  "ProtocolAdapter" : "ModbusAdapter",
  "Channels": {
    "temperature": {
      "Name": "Temperature",
      "Address": 40001,
      "Type": "DiscreteInput"
    }
  }
}
```



Full configuration:

```json
{
  "Name": "ModbusSource1",
  "Description": "Production line Modbus source",
  "AdapterDevice": "PLC1",
  "Channels": {
    "temp1": {
      "Name": "Temperature1",
      "Address": 40001,
      "Type": "HoldingRegister"
    },
    "pressure1": {
      "Name": "Pressure1",
      "Address": 40002,
      "Type": "HoldingRegister"
    },
    "status": {
      "Name": "Status",
      "Address": 10001,
      "Type": "DiscreteInput"
    }
  },
  "Optimization": true,
  "ReadTimeout": 5000
}
```





## ModbusOptimization

[ModbusSource](#modbussourceconfiguration) > [Optimization](#modbusoptimization)

Configuration settings that control how the Modbus TCP adapter optimizes read operations by combining multiple register or coil reads into single requests. This optimization reduces network traffic and improves overall performance by minimizing the number of individual Modbus transactions required to collect data from adjacent or nearby addresses.

- [Schema](#modbusoptimization-schema)
- [Example](#modbusoptimization-example)

**Properties:**

- [Active](#active)
- [RegisterMaxGapSize](#registermaxgapsize)
- [CoilMaxGapSize](#coilmaxgapsize)

---
### Active
Controls whether the Modbus read optimization feature is enabled or disabled. When enabled, the adapter will attempt to combine multiple register or coil reads into single requests based on the configured gap settings. When disabled, each channel will generate its own individual read request.

**Type**: Boolean

Default is true

---
### RegisterMaxGapSize
The maximum number of registers that can be skipped between two read operations while still combining them into a single Modbus request. For example, if set to 10, two register reads separated by up to 10 addresses will be combined into one request. This helps optimize network traffic while balancing memory usage and response time.

**Type**: Integer

Default is 8

---
### CoilMaxGapSize
The maximum number of coils or discrete inputs that can be skipped between two read operations while still combining them into a single Modbus request. For example, if set to 10, two coil reads separated by up to 10 addresses will be combined into one request. This helps optimize network traffic while balancing memory usage and response time.

**Type**: Integer

Default is 16

[^top](#modbus-tcp-protocol-configuration)



### ModbusOptimization Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ModbusOptimization",
  "type": "object",
  "properties": {
    "Active": {
      "type": "boolean",
      "description": "Enable/disable Modbus optimization",
      "default" : true
    },
    "RegisterMaxGapSize": {
      "type": "integer",
      "description": "Maximum gap size between registers for optimization",
      "default": 8
    },
    "CoilMaxGapSize": {
      "type": "integer",
      "description": "Maximum gap size between coils for optimization",
      "default": 16
    }
  }
}

```



### ModbusOptimization Example

```json
{
  "Active": true,
  "RegisterMaxGapSize": 8,
  "CoilMaxGapSize": 16
}
```



## ModbusChannelConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#channels) > [Channel](../core/channel-configuration.md)

A configuration class that defines how to read a specific data point from a Modbus device. It specifies the register type (coil, discrete input, input register, or holding register), address location, data format, and optional scaling parameters. Each channel configuration maps to a single data point that will be collected from the Modbus device and made available through the SFC system.

The ModbusChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the Modbus TCP  protocol adapter.

- [Schema](#modbuschannelconfiguration-schema)
- [Examples](#modbuschannelconfiguration-examples)


**Properties:**
- [Address](#address)
- [Size](#size)
- [Type](#type)

---
### Address
The Modbus address location from which to read the data value. This address specifies the exact register or coil location in the Modbus device's memory map. The interpretation of this address depends on the selected RegisterType (coil, discrete input, input register, or holding register) and follows the standard Modbus addressing scheme

**Type**: Integer

---
### Size
The number of consecutive registers or coils to read starting from the specified address. This is particularly useful when reading multi-register values like floating point numbers, strings, or arrays of values. 

**Type**: Integer

Default is 1.
The maximum for reading coils and discrete inputs is 2000.
The maximum for reading registers is 125.

---
### Type
Specifies which type of Modbus data object to read from the device. Each type serves a different purpose in Modbus communications: [[1\]](https://stackoverflow.com/questions/65286426)

- Coil: Single-bit read/write values typically used for digital outputs or control flags
- DiscreteInput: Single-bit read-only values usually representing digital inputs or status flags
- HoldingRegister: 16-bit read/write registers used for configuration values or analog outputs
- InputRegister: 16-bit read-only registers commonly used for measured values or analog inputs

The selection determines how the adapter interprets the address and interacts with the device.

**Type**: String, any of 

- “Coil”
- “DiscreteInput”,
- HoldingRegister”
- “InputRegister”

[^top](#modbus-tcp-protocol-configuration)



### ModbusChannelConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "allOf": [
    {
      "$ref": "external-schema.json#/definitions/ChannelConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Address": {
          "type": "integer",
          "description": "Modbus address for the channel",
          "minimum": 0
        },
        "Size": {
          "type": "integer",
          "description": "Size of the channel in registers/coils",
          "default": 1
        },
        "Type": {
          "type": "string",
          "description": "Type of Modbus data point",
          "enum": [
            "Coil",
            "DiscreteInput",
            "HoldingRegister",
            "InputRegister"
          ]
        }
      },
      "required": [
        "Address",
        "Type"
      ]
    }
  ]
}

```



### ModbusChannelConfiguration Examples

```json
{    
   "Address": 10001,
   "Type": "DiscreteInput"
}
```



```json
{
    "Name": "Speed",
    "Address": 40001,
    "Size": 2,
    "Type": "HoldingRegister"
  }
```



```json
{
    "Name": "Pump",
    "Address": 1,
    "Type": "Coil"
  }
```



## ModbusTcpAdapterConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [ProtocolAdapters](../core/sfc-configuration.md#protocoladapters) > [Adapter](../core/protocol-adapter-configuration.md) 

A configuration class that defines the connection and behavior settings for communicating with Modbus TCP devices.

ModbusTcpAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the Modbus TCP Protocol adapter.

AdsAdapterConfiguration 

- [Schema](#modbustcpadapterconfiguration-schema)
- [Example](#modbustcpadapterconfiguration-example)

**Properties:**

- [Devices](#devices)

---
### Devices
A collection of Modbus TCP device configurations that defines all the available Modbus servers this adapter can communicate with. Each device configuration specifies connection details like IP address and port number. When setting up a Modbus TCP source in the SFC system, the AdapterDevice property must reference one of these configured devices by name to establish which specific Modbus server to connect to for data collection.

NOTE: When multiple sources read from the same device by using the same IP address, then for each source, a device must be configured in the adapter.

**Type**: Map[String,[ModbusTcpDeviceConfiguration](#modbustcpdeviceconfiguration)]



### ModbusTcpAdapterConfiguration schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "allOf": [
    {
      "$ref": "external-schema.json#/definitions/AdapterConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Devices": {
          "type": "object",
          "description": "Map of Modbus TCP device configurations indexed by string",
          "patternProperties": {
            "^.*$": {
              "$ref": "external-schema.json#/definitions/ModbusTcpDeviceConfiguration"
            }
          },
          "minProperties": 1
        }
      },
      "required": ["Devices"],
    }
  ]
}

```

### ModbusTcpAdapterConfiguration Example

```json
{
  "AdapterType" : "MODBUS-TCP",
  "Devices": {
    "plc1": {
      "Name": "PLC1",
      "Address": "192.168.1.100",
      "Port": 502,
      "ConnectTimeout": 5000,
      "WaitAfterConnectError": 5000,
      "WaitAfterWriteError": 1000,
      "WaitAfterReadError": 1000
    }
  }
}

```



[^top](#modbus-tcp-protocol-configuration)



## ModbusTcpDeviceConfiguration

[ModbusTcpAdapter](#modbustcpadapterconfiguration) > [Devices](#devices)

A configuration class that defines the connection parameters for a specific Modbus TCP server device. It contains the network addressing information and device-specific settings needed to establish communication with a Modbus TCP device. This configuration is referenced by Modbus TCP sources to identify which device to connect to and how to communicate with it, ensuring proper routing of Modbus requests to the correct network endpoint

- [Schema](#modbustcpdeviceconfiguration-schema)
- [Example](#modbustcpdeviceconfiguration-example)

**Properties:**

- [Address](#address)
- [ConnectTimeout](#connecttimeout)
- [DeviceId](#deviceid)
- [Port](#port)
- [WaitAfterConnectError](#waitafterconnecterror)
- [WaitAfterReadError](#waitafterreaderror)

---
### Address
The IP address or hostname of the Modbus TCP server device. This address specifies the network location where the device can be reached for Modbus communications. For example, "192.168.1.100" or "plc.local".

**Type**: String

---
### ConnectTimeout
The maximum time in milliseconds that the adapter will wait when attempting to establish a TCP connection with the Modbus device. If a connection cannot be established within this time period, the connection attempt will be aborted and considered failed. This setting helps prevent the system from hanging when network issues or device unavailability occur.

**Type**: Integer

Default is 1000, the minimum value is 1000

---
### DeviceId
The Modbus device identifier (also known as Unit ID or Slave ID) used to communicate with this specific device. In Modbus TCP networks, this ID helps identify the target device when multiple Modbus devices are connected through the same TCP/IP connection, such as when communicating through a gateway or when a single TCP/IP endpoint serves multiple logical Modbus devices. In a Modbus network, this ID allows the master device to address specific slave devices, as each slave must have a unique identifier to ensure proper request routing and response handling.

**Type**: Integer

Default is 1

---
### Port
The TCP port number that the Modbus TCP server is listening on. The default Modbus TCP port is 502, but some devices or configurations may use different port numbers. This port must match the listening port configured on the target Modbus device for successful communication.

**Type**: Integer

Default is 502

---
### WaitAfterConnectError
The time in milliseconds to wait before attempting to reconnect after a connection error occurs. This delay helps prevent excessive reconnection attempts when a device is unavailable, reducing network traffic and system resource usage. The waiting period provides time for potential temporary network issues to resolve or for the target device to recover before initiating a new connection attempt

**Type**: Integer

Default is 10000, the minimum value is 1000

---
### WaitAfterReadError
The time in milliseconds to wait before retrying a read operation after encountering a read error. This delay helps manage error recovery when data reading fails, preventing rapid-fire retry attempts that could overwhelm the device or network. The waiting period allows time for temporary communication issues to clear or for the device to recover from busy states before attempting another read operation.

**Type**: Integer

Default is 10000, the minimum value is 1000



### ModbusTcpDeviceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "Address": {
      "type": "string",
      "description": "IP address or hostname of the Modbus TCP device"
    },
    "ConnectTimeout": {
      "type": "integer",
      "description": "Connection timeout in milliseconds",
      "minimum": 1000,
      "default" : 1000
    },
    "DeviceId": {
      "type": "integer",
      "description": "Modbus device identifier",
      "default" : 1
    },
    "Port": {
      "type": "integer",
      "description": "TCP port number",
      "default": 502
    },
    "WaitAfterConnectError": {
      "type": "integer",
      "description": "Wait time after connection error in milliseconds",
      "minimum": 1000,
      "default" : 10000
    },
    "WaitAfterReadError": {
      "type": "integer",
      "description": "Wait time after read error in milliseconds",
      "minimum": 1000,
      "default" : 10000
    }
  },
  "required": [
    "Address"
  ]
}
```



### ModbusTcpDeviceConfiguration Example

```json
{
  "Address": "192.168.1.100",
  "ConnectTimeout": 10000,
  "DeviceId": 1,
  "Port": 502,
  "WaitAfterConnectError": 5000,
  "WaitAfterReadError": 1000
}
```



[^top](#modbus-tcp-protocol-configuration)

