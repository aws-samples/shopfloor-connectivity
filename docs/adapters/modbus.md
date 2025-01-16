# Modbus TCP Protocol Configuration

Configuration for Modbus TCP protocol adapter.

- [ModbusSourceConfiguration](#ModbusSourceConfiguration)
- [ModbusOptimization](#ModbusOptimization)
- [ModbusChannelConfiguration](#ModbusChannelConfiguration)
- [ModbusTcpAdapterConfiguration](#ModbusTcpAdapterConfiguration)
- [ModbusTcpDeviceConfiguration](#ModbusTcpDeviceConfiguration)

---

## ModbusSourceConfiguration

[SFC Configuration](../core/sfc-configuration) > [Sources](../core/sfc-configuration#Sources) >  [Source](../core/source-configuration.md) 



Source configuration for the Modbus protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type. 

- [Schema](#ModbusSourceConfiguration-schema)
- [Example](#ModbusSourceConfiguration-example)


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

**Type**: Map[String,[ModbusChannelConfiguration](#ModbusChannelConfiguration)

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

[ModbusSource](#ModbusSourceConfiguration) > [Optimization](#modbusoptimization)



- [Schema](#ModbusOptimization-schema)

- [Example](#ModbusOptimization-example)

**Properties:**

- [Active](#Active)
- [RegisterMaxGapSize](#RegisterMaxGapSize)
- [CoilMaxGapSize](#CoilMaxGapSize)

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

[SFC Configuration](../core/sfc-configuration) > [Sources](../core/sfc-configuration#Sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#Channels) > [Channel](../core/channel-configuration.md)



The ModbusChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the Modbus TCP  protocol adapter.

- [Schema](#ModbusChannelConfiguration-schema)

- [Examples](#ModbusChannelConfiguration-examples)


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

[SFC Configuration](../core/sfc-configuration) > [ProtocolAdapters](../core/sfc-configuration#ProtocolAdapters) > [Adapter](../core/protocol-adapter-configuration.md) 



ModbusTcpAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the Modbus TCP Protocol adapter.

AdsAdapterConfiguration 

- [Schema](#ModbusTcpAdapterConfiguration-schema)

- [Example](#ModbusTcpAdapterConfiguration-example)

**Properties:**

- [Devices](#Devices)

---
### Devices
Modbus devices configured for this adapter. The modbus tcp source using the adapter must refer to one of these servers with the AdapterDevice attribute.

**Type**: Map[String,[ModbusTcpDeviceConfiguration](#ModbusTcpDeviceConfiguration)]



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

[ModbusTcpAdapter](#ModbusTcpAdapterConfiguration) > [Devices](#Devices)



- [Schema](#ModbusTcpDeviceConfiguration-schema)

- [Example](#ModbusTcpDeviceConfiguration-example)

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
  "Address": ""192.168.1.100",
  "ConnectTimeout": 10000,
  "DeviceId": 1,
  "Port": 502,
  "WaitAfterConnectError": 5000,
  "WaitAfterReadError": 1000
}
```



[^top](#modbus-tcp-protocol-configuration)

