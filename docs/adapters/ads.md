# ADS Protocol Configuration

- [AdsSourceConfiguration](#adssourceconfiguration)
- [AdsChannelConfiguration](#adschannelconfiguration)
- [AdsAdapterConfiguration](#adsadapterconfiguration)
- [AdsDeviceConfiguration](#adsdeviceconfiguration)

---

## AdsSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) >  [Source](../core/source-configuration.md) 



Source configuration for the ADS protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type. 

- [Schema](#adssourceconfiguration-schema)
- [Examples](#adssourceconfiguration-example) 

**Properties:**

- [AdapterDevice](#adapterdevice)
- [Channels](#channels)
- [SourceAmsId](#sourceamsid)
- [SourceAmsPort](#sourceamsport)
- [TargetAmsId](#targetamsid)
- [TargetAmsPort](#targetamsport)

---
### AdapterDevice
Device Identifier for the device to read from. This referenced device must be present in the Devices section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a server in the [Devices](#devices) section of the [ADS adapter](#adsadapterconfiguration) used by the source.

---
### Channels
The channels configuration for an ADS source holds configuration data to read values from fields on the source device.
The element is a map indexed by the channel identifier.
Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,[AdsChannelConfiguration](#adschannelconfiguration)]

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

The following decimal port numbers are invariant defined on each TwinCAT single system.

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



### AdsSourceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "allOf": [
    {
      "$ref": "#/definitions/BaseSourceConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AdapterDevice": {
          "type": "string"
          "description": "ADS adapter device identifier"
        },
        "Channels": {
          "type": "object",
          "description": "Map of channel configurations indexed by string",
          "patternProperties": {
            "^.*$": {
              "$ref": "external-schema.json#/definitions/AdsChannelConfiguration"
            }
          },
          "minProperties" : 1,
        },
        "SourceAmsId": {
          "type": "string"
          "description": "Source AMS ID for the ADS connection"
        },
        "SourceAmsPort": {
          "type":"integer", 
          "description": "Source AMS port number",
          "minimum": 0,
          "maximum": 65535
        },
        "TargetAmsId": {
          "type": "string"
          "description": "Target AMS ID for the ADS connection"
        },
        "TargetAmsPort": {
          "type": "integer"
          "description": "Target AMS port number"
        }
      },
      "required": ["Channels"],
      "additionalProperties": false
    }
  ]
}

```

### AdsSourceConfiguration Example

```json
{
  "Description": "Assembly Line PLC",
  "Name": "AssemblyPLC1",
  "ProtocolAdapter": "ads-adapter",
  "AdapterDevice": "PLC1",
  "Channels": {
    "temperature": {
      "Name": "Temperature_Sensor",
      "SymbolName": "MAIN.Temperature"
    },
    "pressure": {
      "Name": "Pressure_Sensor",
      "SymbolName": "MAIN.PressureValue"
    },
    "speed": {
      "Name": "Conveyor_Speed",
      "SymbolName": "MAIN.ConveyorSpeed"
    }
  },
  "SourceAmsId": "192.168.1.10.1.1",
  "SourceAmsPort": 851,
  "TargetAmsId": "192.168.1.20.1.1",
  "TargetAmsPort": 852
}
```





## AdsChannelConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#channels) > [Channel](../core/channel-configuration.md)



The AdsChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the ADS protocol adapter.



- [Schema](#adschannelconfiguration-schema)
- [Example](#adschannelconfiguration-example)

**Properties:**

- [SymbolName](#symbolname)

---
### SymbolName
A string containing the name of the symbol to read from the device.

**Type**: String

[^top](#ads-protocol-configuration)



### AdsChannelConfiguration Schema

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
        "SymbolName": {
          "type": "string",
          "description": "ADS symbol name for the channel"
        }
      },
      "additionalProperties": false
    }
  ]
}

```



### AdsChannelConfiguration Example

```json
{
      "Name": "Temperature_Sensor",
      "SymbolName": "MAIN.Temperature"
    }
```



## AdsAdapterConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [ProtocolAdapters](../core/sfc-configuration.md#protocoladapters) > [Adapter](../core/protocol-adapter-configuration.md) 



AdsAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the ADS Protocol adapter.

- [Schema](#adsadapterconfiguration-schema)
- [Example](#adsadapterconfiguration-example)

**Properties:**

- [Devices](#devices)

---
### Devices
Devices configured for this adapter. The ADS source using the adapter must have a reference to one of these in its AdapterDevice attribute.

**Type**: Map[String,[AdsDeviceConfiguration](#adsdeviceconfiguration)]

[^top](#ads-protocol-configuration)



### AdsAdapterConfiguration Schema

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
        "Controllers": {
          "type": "object",
          "description": "Map of ADS devices indexed by string",
          "patternProperties": {
            "^.*$": {
              "$ref": "external-schema.json#/definitions/AdsDevice"
            }
          },
          "minProperties": 1
        }
      },
      "required": ["Controllers"]
    }
  ]
}

```



### AdsAdapterConfiguration Example

```json
{
  "Name": "ProductionAdsAdapter",
  "Description": "Production line ADS adapter",
  "AdapterType" : "ADS",
  "Controllers": {
    "assembly_plc": {
      "Name": "AssemblyPLC",
      "IpAddress": "192.168.1.101"
    },
    "packaging_plc": {
      "Name": "PackagingPLC",
      "IpAddress": "192.168.1.102"
    },
    "quality_plc": {
      "Name": "QualityControlPLC",
      "IpAddress": "192.168.1.103"
    }
  }
}

```





## AdsDeviceConfiguration

[AdsAdapterConfiguration](#adsadapterconfiguration) > [Devices](#devices)



Configuration for an ADS Device.

- [Schema](#adsdeviceconfiguration-schema)
-[Example](#adsdeviceconfiguration-example)


**Properties:**
- [Address](#address)
- [CommandTimeout](#commandtimeout)
- [ConnectTimeout](#connecttimeout)
- [Port](#port)
- [ReadTimeout](#readtimeout)
- [WaitAfterConnectError](#waitafterconnecterror)
- [WaitAfterReadError](#waitafterreaderror)
- [WaitAfterWriteError](#waitafterwriteerror)

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
Timeout for reading response packets from the device in milliseconds

**Type**: Integer

Default is 10000

---
### WaitAfterConnectError
Time to wait before (re)connecting after a connection error in milliseconds

**Type**: Integer

Default is 10000

---
### WaitAfterReadError
Time to wait before reading values from the device after a read error in milliseconds

**Type**: Integer

Default is 10000

---
### WaitAfterWriteError
Time to wait after an error writing request packets to the device in milliseconds

**Type**: Integer

Default is 10000

[^top](#ads-protocol-configuration)



### AdsDeviceConfiguration Schema

```json
[
  {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": ":"
  },
  {
    "type": "object",
    "properties": {
      "Address": {
        "type": "string",
        "description": "IP address or hostname of the ADS device"
      },
      "CommandTimeout": {
        "type": [
          "integer",
          "null"
        ],
        "description": "Timeout for command execution in milliseconds",
        "default": 10000
      },
      "ConnectTimeout": {
        "type": "integer",
        "description": "Timeout for connection establishment in milliseconds",
        "default": 10000
      },
      "Port": {
        "type": "integer",
        "description": "TCP port number for the ADS connection",
        "default": 48898
      },
      "ReadTimeout": {
        "type": "integer",
        "description": "Timeout for read operations in milliseconds",
        "default": 10000
      },
      "WaitAfterConnectError": {
        "type": "integer",
        "description": "Wait time after connection error in milliseconds"
      },
      "WaitAfterReadError": {
        "type": [
          "integer",
          "null"
        ],
        "description": "Wait time after read error in milliseconds",
        "default": 10000
      },
      "WaitAfterWriteError": {
        "type": [
          "integer",
          "null"
        ],
        "description": "Wait time after write error in milliseconds",
        "default": 10000
      }
    }
  }
]
```



### AdsDeviceConfiguration Example

Basic configuration:

```json
{
  "Address": "192.168.1.100",
  "Port": 48898
}
```

Full configuration:

```json
{
  "Address": "192.168.1.100",
  "Port": 48898,
  "CommandTimeout": 5000,
  "ConnectTimeout": 10000,
  "ReadTimeout": 1000,
  "WaitAfterConnectError": 5000,
  "WaitAfterReadError": 1000,
  "WaitAfterWriteError": 1000
}
```

```json
```

