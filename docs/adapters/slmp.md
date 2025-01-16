# SLMP Protocol Configuration

SLMP Protocol adapter configuration.

This section describes the configuration types for the SLMP protocol adapter and contains the extensions and specific configuration types.

IMPORTANT : SLMP controllers only supports a single concurrent session with the controller. When reading data by multiple schedules or adapters instances, or from another SLMP client, from the same controller, timeout and broken TCP pipe errors will occur.

### SLMP channel reading optimization

In order to reduce the number of interactions between the adapter and the controller read action for single BIT, WORD and DOUBLEWORD elements are combined in batches of maximum 192 values using the SLMP Read Random request. For reading arrays of multiple values, STRING values and values of custom structured types a per channel SLMP Read request is used.


---

## Configuration

- [SlmpSourceConfiguration](#SlmpSourceConfiguration)
- [SlmpChannelConfiguration](#SlmpChannelConfiguration)
- [SlmpAdapterConfiguration](#SlmpAdapterConfiguration)
- [SlmpControllerConfiguration](#SlmpControllerConfiguration)

---

## SlmpSourceConfiguration

[SFC Configuration](../core/sfc-top-level-config.md) > [Sources](../core/sfc-top-level-config.md#Sources) >  [Source](../core/source-configuration.md) 



Source configuration for the SLMP protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type.

- [Schema](#SlmpSourceConfiguration-Schema)
- [Examples](#SlmpSourceConfiguration-Examples)

**Properties:**
- [AdapterController](#AdapterController)
- [Channels](#Channels)

---
### AdapterController
Controller Identifier for the controller to read from. This referenced server must be present in the Devices section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a server in the Controllers section of the SLMP adapter used by the source.

---
### Channels
The channels configuration for an SLMP source holds configuration data to read values from fields on the source controller.
The element is a map indexed by the channel identifier.
Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,[SlmpChannelConfiguration](#SlmpChannelConfiguration)]

At least 1 channel must be configured.

### SlmpSourceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for S7 source",
  "allOf": [
    {
      "$ref": "#/definitions/SourceConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AdapterController": {
          "type": "string",
          "description": "Reference to the S7 controller configuration in the adapter"
        },
        "Channels": {
          "type": "object",
          "description": "Map of SLMP channel configurations",
          "additionalProperties": {
            "$ref": "#/definitions/SLMPChannelConfiguration"
          },
          "minProperties": 1
        }
      },
      "required": ["AdapterController", "Channels"]
    }
  ]
}

```

### SlmpSourceConfiguration Examples

```json
{
  "Name": "ProcessMonitor",
  "ProtocolAdapter" : "SlmpAdapter",
  "AdapterController": "MainPLC",
  "Channels": {
    "Temperature": {
      "Name": "Temperature",
      "Description": "Process temperature",
      "AccessPoint": "D100",
      "DataType": "Float"
    },
    "RunStatus": {
      "Name": "RunStatus",
      "Description": "Process running status",
      "AccessPoint": "M0",
      "DataType": "Bit"
    }
  }
}

```

[^top](#slmp-protocol-configuration)



## SlmpChannelConfiguration

[SFC Configuration](../core/sfc-top-level-config.md) > [Sources](../core/sfc-top-level-config.md#Sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#Channels) > [Channel](../core/channel-configuration.md)



The SlmpChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the SLMP protocol adapter.

- [Schema](#SlmpChannelConfiguration-Schema)
- [Examples](#SlmpChannelConfiguration-Examples)

**Properties:**
- [AccessPoint](#AccessPoint)
- [DataType](#DataType)
- [Size](#Size)

---
### AccessPoint
A string containing the access point for the value to read from the device.

**Type**: String


Access points consists of a device code and a decimal device number, e.g. "D200" for Data register 200, "X0" for Input 0 and "Y0" for output 0.

Valid devices codes and their data types are listed below.


- "B"                Link relay (BIT)
- "CC"	     Counter coil (BIT)
- "CN"	     Counter timer current value (WORD)
- "CS"	      Counter contact (BIT)
- "D"		Data register (WORD)
- "DX"	      Direct access input (BIT)
- "DY"	      Direct access output (BIT)
- "F" 	        Alarm (BIT)
- "L" 	        Latching relay (BIT)
- "LCC"	    Counter coil (BIT)
- "LCN"	    Counter timer current value (DOUBLEWORD)
- "LCS"	     Counter contact (BIT)
- "LSTC"	   Long retentive timer coil (BIT)
- "LSTN"	  Long retentive timer current value (DOUBLEWORD)
- "LSTS"	   Long retentive timer contact (BIT)
- "LTC"	     Long timer coil (BIT)
- "LTN"	    Long timer current value (DOUBLEWORD)
- "LTS"	     Long timer contact (BIT)
- "LZ"	       Long index register (DOUBLEWORD)
- "M" 	       Internal relay (BIT)
- "R"		 File register (WORD)
- "S"		 Step relay (BIT)
- "SB"	       Link special relay (BIT)
- "SD"  	     Special register (WORD)
- "SM"     	 Special relay (Bit)
- "SW"	      Link special register (WORD)
- "TC"	       Timer coil (BIT)
- "TN"	      Timer current value (WORD)
- "TS"	       Timer contact (BIT)
- "V" 		Edge relay (BIT)
- "W"		Link register (WORD)
- "X"   	      Input (BIT)
- "Y" 		output (BIT)
- "Z"		 Index register (WORD)
- "ZR"	       File register ZR (WORD)


---
### DataType
The type of data to read from the device. If no type is specified then a single value of the default type of the device is read.

**Type**: String


Valid data types are:


- "BIT" (read as a boolean value)
- "WORD" (read as a 16-bit integer value)
- "DOUBLEWORD" (read as 32-bit integer)
- "STRING(x)" (read as words and decoded to as a string of length x or shorter if the string is zero terminated)

It is possible to define custom structures and use these as a data type as well. These structures are defined in the "Structures" section of the SLMP adapter configuration. All fields which can be any the types mentioned above, or another custom structure type, are mapped from the read word data to the fields of the structure in the order in which they are declared in the type.


In order to read multiple values, returned as an array, starting at the specified access point the number of items can be appended to the data type.

E.g. 

- "BIT[4]" reads 4 BIT values and returns an array of 4 boolean values.
- "WORD[8]" reads 16 word values and returns an array of 8 16-bit integers.
- "STRING(16)[2]" reads and array of 16 characters

 

---
### Size
The number of values to read starting from the access point.

**Type**: Integer


The number of items to read can be specified as well in the DataType of the channel, e.g. WORD[size]. The Size setting can be used if the DataType field is omitted to read the default data type for the device. If the length is both specified in the DataType in both the Size setting a configuration error is raised.

### SlmpChannelConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for SLMP channel",
  "allOf": [
    {
      "$ref": "#/definitions/ChannelConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AccessPoint": {
          "type": "string",
          "description": "Access point address for the SLMP channel"
        },
        "DataType": {
          "type": "string",
          "description": "Data type of the channel"
        },
        "Size": {
          "type": "integer",
          "description": "Number of items to read",
          "minimum": 1
        }
      },
      "required": ["AccessPoint", "DataType"]
    }
  ]
}

```

### SlmpChannelConfiguration Examples

```json
{
  "Name": "MotorStatus",
  "Description": "Motor running status",
  "AccessPoint": "M100",
  "DataType": "Bit"
}

```

[^top](#slmp-protocol-configuration)



## SlmpAdapterConfiguration

[SFC Configuration](../core/sfc-top-level-config.md) > [ProtocolAdapters](../core/sfc-top-level-config.md#ProtocolAdapters) > [Adapter](../core/protocol-adapter-configuration.md) 



SlmpAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the SLMP Protocol adapter.

- [Schema](#SlmpAdapterConfiguration-Schema)

- [Examples](#SlmpAdapterConfiguration-Examples)

  

  **Properties:**

- [Controllers](#Controllers)

- [Structures](#Structures)

---
### Controllers
Controllers configured for this adapter. The SLMP source using the adapter must have a reference to one of these in its AdapterController attribute.

**Type**: Map[String,[SlmpControllerConfiguration](#SlmpControllerConfiguration)]



---
### Structures
Custom data structures configured for this adapter. Structured defined in this section can be uses as custom structured data types for channel values. If a structure has a field which is of a custom structure type, then this type must be defined first.

**Type**: Map[String,Map{String,String]]


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

### SlmpAdapterConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for SLMP adapter",
  "allOf": [
    {
      "$ref": "#/definitions/AdapterConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Controllers": {
          "type": "object",
          "description": "Map of SLMP controller configurations",
          "additionalProperties": {
            "$ref": "#/definitions/SlmpControllerConfiguration"
          },
          "minProperties": 1
        },
        "Structures": {
          "type": "object",
          "description": "Map of structure definitions",
          "additionalProperties": {
            "type": "object",
            "additionalProperties": {
              "type": "array",
              "items": {
                "type": "string"
              },
              "minItems": 1
            }
          }
        }
      },
      "required": ["Controllers"]
    }
  ]
}

```

### SlmpAdapterConfiguration Examples

```json
{
  "AdapterType": "SlmpAdapter",
  "Controllers": {
    "MainController": {
      "Address": "192.168.1.100",
      "Port": 1025,
      "NetworkNumber": 1,
      "StationNumber": 1
    }
  }
}
```



```json
{
  "Name": "BasicSlmpAdapter",
  "Controllers": {
    "MainController": {
      "Address": "192.168.1.100",
      "Port": 1025,
      "NetworkNumber": 1,
      "StationNumber": 1
    }
  },
  "Structures": {
    "ProductData": {
      "Fields": ["ItemCode", "Quantity", "Status"]
    }
  }
}
```

[^top](#slmp-protocol-configuration)



## SlmpControllerConfiguration

[SlmpAdapter](#SlmpAdapterConfiguration) > [Controllers](#Controllers)



- [Schema](#SlmpControllerConfiguration-Schema)
- [Examples](#SlmpControllerConfiguration-Examples)

**Properties:**
- [Address](#Address)
- [CommandTimeout](#CommandTimeout)
- [ConnectTimeout](#ConnectTimeout)
- [ModuleNumber](#ModuleNumber)
- [MonitoringTimer](#MonitoringTimer)
- [MultiDropStationNumber](#MultiDropStationNumber)
- [NetworkNumber](#NetworkNumber)
- [Port](#Port)
- [ReadTimeout](#ReadTimeout)
- [StationNumber](#StationNumber)
- [WaitAfterConnectError](#WaitAfterConnectError)
- [WaitAfterReadError](#WaitAfterReadError)
- [WaitAfterWriteError](#WaitAfterWriteError)

---
### Address
IP Address of the device

**Type**: String

IP address in format aaa.bbb.ccc.ddd or a hostname

---
### CommandTimeout
Timeout for executing commands in milliseconds

**Type**: Integer

Default is 10000 milliseconds

---
### ConnectTimeout
Timeout for connecting to the device in milliseconds

**Type**: Integer

Default is 10000

---
### ModuleNumber
Request module number

**Type**: Integer

Default is 1023 (0x03FF) 

---
### MonitoringTimer
Timer to set the waiting time until the access destination send back a response after the SLMP compatible device
which received a request message from the external device requests a processing to the destination in units of 250ms

**Type**: Integer

Default is 0 (unlimited wait)

---
### MultiDropStationNumber
Request multidrop station number

**Type**: Integer

Default is 0 (0x00)



---
### NetworkNumber
Request destination network number

**Type**: Integer

Default is 0 (0x00)

---
### Port
Port number

**Type**: Integer

Default is 48898

---
### ReadTimeout
Timeout for reading response packets from the controller in milliseconds

**Type**: Integer

Default is 50000

---
### StationNumber
Request station number

**Type**: Integer

Default is 255 (0xFF) 

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

### SlmpControllerConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for SLMP controller",
  "properties": {
    "Address": {
      "type": "string",
      "description": "IP address of the SLMP controller"
    },
    "CommandTimeout": {
      "type": "integer",
      "description": "Timeout for command execution in milliseconds"
    },
    "ConnectTimeout": {
      "type": "integer",
      "description": "Timeout for connection establishment in milliseconds"
    },
    "ModuleNumber": {
      "type": "integer",
      "description": "Module number in the SLMP network"
    },
    "MonitoringTimer": {
      "type": "integer",
      "description": "Monitoring timer value in milliseconds"
    },
    "MultiDropStationNumber": {
      "type": "integer",
      "description": "Station number for multi-drop configuration"
    },
    "NetworkNumber": {
      "type": "integer",
      "description": "Network number in the SLMP system"
    },
    "Port": {
      "type": "integer",
      "description": "TCP port number for SLMP communication"
    },
    "ReadTimeout": {
      "type": "integer",
      "description": "Timeout for read operations in milliseconds"
    },
    "StationNumber": {
      "type": "integer",
      "description": "Station number in the SLMP network"
    },
    "WaitAfterConnectError": {
      "type": "integer",
      "description": "Wait time after connection error in milliseconds"
    },
    "WaitAfterReadError": {
      "type": "integer",
      "description": "Wait time after read error in milliseconds"
    },
    "WaitAfterWriteError": {
      "type": "integer",
      "description": "Wait time after write error in milliseconds"
    }
  },
  "required": ["Address"]
}

```

### SlmpControllerConfiguration Examples

```json
{
  "Address": "192.168.1.100",
  "Port": 1025,
  "NetworkNumber": 1,
  "StationNumber": 1,
  "ReadTimeout": 5000,
  "ConnectTimeout": 10000
}
```


[^top](#slmp-protocol-configuration)

