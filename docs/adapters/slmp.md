# SLMP Protocol Configuration

The SFC SLMP protocol adapter enables communication with Mitsubishi/Melsec PLCs using the SLMP (Seamless Message Protocol) protocol. It allows reading data from Mitsubishi Q series, L series, and iQ-R series controllers over Ethernet.

**IMPORTANT** : SLMP controllers only supports a single concurrent session with the controller. When reading data by multiple schedules or adapters instances, or from another SLMP client, from the same controller, timeout and broken TCP pipe errors will occur.

In order to reduce the number of interactions between the adapter and the controller read action for single BIT, WORD and DOUBLEWORD elements are combined in batches of maximum 192 values using the SLMP Read Random request. For reading arrays of multiple values, STRING values and values of custom structured types a per configured channel SLMP Read request is used.

In order to use this adapter as in [in-process](../sfc-running-adapters.md#running-protocol-adapters-in-process) type adapter the type must be added to the [AdapterTypes](../core/sfc-configuration.md#adaptertypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"AdapterTypes" :{
  "SLMP" : {
    "JarFiles" : ["<location of deployment>/slmp/lib"]
  },
  "FactoryClassName" : "com.amazonaws.sfc.slmp.SlmpAdapter"
}
```

**Configuration:**

- [SlmpSourceConfiguration](#slmpsourceconfiguration)
- [SlmpChannelConfiguration](#slmpchannelconfiguration)
- [SlmpAdapterConfiguration](#slmpadapterconfiguration)
- [SlmpControllerConfiguration](#slmpcontrollerconfiguration)

---

## SlmpSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) >  [Source](../core/source-configuration.md) 

The SlmpSourceConfiguration class extends [SourceConfiguration](../core/source-configuration.md)  and defines the configuration for reading data from a Mitsubishi/Melsec PLC using SLMP protocol. It specifies the controller [AdapterController](#adaptercontroller) to use and the channels (values) to read from the PLC

- [Schema](#slmpsourceconfiguration-schema)
- [Examples](#slmpsourceconfiguration-examples)

**Properties:**

- [AdapterController](#adaptercontroller)
- [Channels](#channels)

---
### AdapterController
The AdapterController property specifies the identifier of the SLMP controller to read data from. This identifier must match a controller defined in the Controllers section of the SLMP adapter configuration that is referenced by the source's ProtocolAdapter attribute.

**Type**: String

---
### Channels
The Channels property is a map of channel configurations for an SLMP source, where each entry is keyed by a unique channel identifier. It defines how to read specific values from the SLMP controller. Individual channels can be disabled by prefixing their identifier with "#" in the configuration

**Type**: Map[String,[SlmpChannelConfiguration](#slmpchannelconfiguration)]

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

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#channels) > [Channel](../core/channel-configuration.md)

The SlmpChannelConfiguration class defines the configuration for reading a specific data point (channel) from a Mitsubishi/Melsec PLC using SLMP protocol. It specifies the device address to read from.

The SlmpChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the SLMP protocol adapter.

- [Schema](#slmpchannelconfiguration-schema)
- [Examples](#slmpchannelconfiguration-examples)

**Properties:**
- [AccessPoint](#accesspoint)
- [DataType](#datatype)
- [Size](#size)

---
### AccessPoint
The AccessPoint property defines the device address to read from in the Mitsubishi/Melsec PLC. It specifies the memory area and address using the standard SLMP addressing format (see below).

**Type**: String


Access points consists of a device code and a decimal device number, e.g. "D200" for Data register 200, "X0" for Input 0 and "Y0" for output 0.

Valid devices codes and their data types are:


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
The DataType property specifies the format of data to be read from the device address. If not specified, it reads a single value of the device's default type.




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

**Type**: String 

---
### Size
The Size property defines how many consecutive values to read starting from the specified access point.

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

[SFC Configuration](../core/sfc-configuration.md) > [ProtocolAdapters](../core/sfc-configuration.md#protocoladapters) > [Adapter](../core/protocol-adapter-configuration.md) 

The SlmpAdapterConfiguration class extends ProtocolAdapterConfiguration and defines the configuration for the SLMP protocol adapter. It includes settings for SLMP controllers (devices) to communicate with and optional custom data structure definitions that can be used when reading data from the controllers

SlmpAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the SLMP Protocol adapter.

- [Schema](#slmpadapterconfiguration-schema)
- [Examples](#slmpadapterconfiguration-examples)

  

  **Properties:**

- [Controllers](#controllers)
- [Structures](#structures)

---
### Controllers
The Controllers property is a map of SLMP controller configurations, where each entry is keyed by a controller identifier. Each source using this SLMP adapter must reference one of these configured controllers through its [AdapterController](#adaptercontroller) attribute. These controllers represent the Mitsubishi/Melsec PLCs that the adapter can communicate with.

**Type**: Map[String,[SlmpControllerConfiguration](#slmpcontrollerconfiguration)]



---
### Structures
The Structures property is a map of custom data structure definitions that can be used as data types for channel values in the SLMP adapter. Each structure can contain fields of basic data types (BIT, WORD, etc.) or other custom structures (which must be defined before they can be referenced). These structures allow reading complex data types from the PLC in a single operation

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

[SlmpAdapter](#slmpadapterconfiguration) > [Controllers](#controllers)

The SlmpControllerConfiguration class defines the configuration settings for connecting to and communicating with a specific Mitsubishi/Melsec PLC using the SLMP protocol. It includes network connection parameters, timing settings, and other communication-specific configurations needed to establish and maintain communication with the PLC.

- [Schema](#slmpcontrollerconfiguration-schema)
- [Examples](#slmpcontrollerconfiguration-examples)

**Properties:**
- [Address](#address)
- [CommandTimeout](#commandtimeout)
- [ConnectTimeout](#connecttimeout)
- [ModuleNumber](#modulenumber)
- [MonitoringTimer](#monitoringtimer)
- [MultiDropStationNumber](#multidropstationnumber)
- [NetworkNumber](#networknumber)
- [Port](#port)
- [ReadTimeout](#readtimeout)
- [StationNumber](#stationnumber)
- [WaitAfterConnectError](#waitafterconnecterror)
- [WaitAfterReadError](#waitafterreaderror)
- [WaitAfterWriteError](#waitafterwriteerror)

---
### Address
The Address property specifies the network location of the SLMP controller (PLC) that the adapter will communicate with. It can be set using either:

- An IPv4 address in dotted decimal format (e.g., "192.168.1.100") 
- A hostname (e.g., "plc-controller-1")

This address is used to establish the network connection with the PLC.

**Type**: String

---
### CommandTimeout
Timeout for executing commands in milliseconds

**Type**: Integer

Default is 10000 milliseconds

---
### ConnectTimeout
The ConnectTimeout property specifies how long the adapter will wait when attempting to establish a connection with the SLMP controller (PLC) before timing out. It is measured in milliseconds, with a default value of 10000 (10 seconds). If the connection cannot be established within this time period, the connection attempt will fail

**Type**: Integer

---
### ModuleNumber
The ModuleNumber property specifies the module number used in SLMP protocol communications. It is an integer value that defaults to 1023 (0x03FF).

In SLMP protocol, the module number is used to identify specific modules within a PLC system. The value 0x03FF (1023) is commonly used for built-in Ethernet ports on PLCs. This number helps route communications to the correct module when multiple modules are present in the PLC system.

The module number is part of the SLMP frame header and is used in conjunction with other addressing parameters to ensure messages are properly routed within the PLC system.

**Type**: Integer

Default is 1023 (0x03FF) 

---
### MonitoringTimer
The MonitoringTimer property sets a timeout value for waiting for a response from the SLMP device after it receives a request message. 

- The value is specified in units of 250ms (e.g., a value of 4 equals 1 second)
- Default value is 0, which means it will wait indefinitely for a response
- This timer starts after the device receives the request and begins processing
- It controls how long the adapter will wait for the PLC to complete processing and send back a response
- If the timer expires before receiving a response, the request will be considered failed

This is different from the ConnectTimeout as it specifically deals with the processing time of requests rather than the initial connection establishment.

**Type**: Integer

Default is 0 (unlimited wait)

---
### MultiDropStationNumber
The MultiDropStationNumber property specifies the station number in a multi-drop network configuration for SLMP communications.

Key points about MultiDropStationNumber:

- It's an integer value that defaults to 0 (0x00)
- Used in networks where multiple SLMP devices are connected in a multi-drop configuration
- Helps identify and address specific devices/stations in the network
- Each device in the multi-drop network must have a unique station number
- Station number 0 typically represents direct connection or the local station

This parameter is important when communicating with PLCs in a network where multiple devices are connected in a daisy-chain or multi-drop configuration, as it ensures messages are routed to the correct device

**Type**: Integer

Default is 0 (0x00)



---
### NetworkNumber
The NetworkNumber property specifies the destination network number in SLMP communications.

Key points about NetworkNumber:

- It's an integer value that defaults to 0 (0x00)
- Used to identify different networks in a multi-network SLMP system
- Part of the routing information in SLMP frames
- Network number 0 typically represents the local network
- Required for routing messages when communicating across different networks in a complex PLC system setup

This parameter is particularly important in larger PLC systems where multiple networks are interconnected, as it helps route messages to devices on the correct network segment. When communicating with a device on the same network, the default value of 0 is typically used.

**Type**: Integer

Default is 0 (0x00)

---
### Port
The PortNumber property specifies the TCP port number used for SLMP communications with the PLC.

- Default value is 48898, which is the standard port for SLMP protocol [[1\]](https://leki-hub.hashnode.dev/port-number)
- It's a 16-bit unsigned integer value (valid range 1-65535)
- Can be customized if the PLC is configured to use a different port
- Port 0 is reserved and cannot be used
- Ports below 1024 typically require administrative privileges on many operating systems

This setting must match the port number configured on the target PLC device for successful communication. If you modify this from the default, ensure that the PLC's network settings are configured to match.

**Type**: Integer

Default is 48898

---
### ReadTimeout
The ReadTimeout property defines how long the adapter will wait for a response packet from the SLMP controller (PLC) after sending a read request.

- Specified in milliseconds
- Default value is 50000 (50 seconds)
- Determines maximum time to wait for response data
- If no response is received within this time, the read operation will fail
- Different from ConnectTimeout (initial connection) and MonitoringTimer (processing time)

This timeout is important for preventing the adapter from hanging indefinitely when there are communication issues or when the PLC fails to respond. If you're experiencing timeout errors, you might need to increase this value, especially in networks with high latency or when reading large amounts of data

**Type**: Integer

Default is 50000

---
### StationNumber
The StationNumber property specifies the station number for SLMP communications.

- Default value is 255 (0xFF)
- Used to identify specific PLC stations in a network
- Part of the SLMP frame addressing information
- Value 255 (0xFF) typically represents the local station or host
- Different from MultiDropStationNumber which is used in multi-drop configurations

This parameter is used to:

- Address specific PLC stations in a network
- Route messages to the correct device
- Identify the target station for SLMP commands
- Enable communication with specific PLCs in a multi-station setup

The station number must match the configuration of the target PLC for successful communication. In simple point-to-point connections, the default value of 255 is commonly used

**Type**: Integer

Default is 255 (0xFF) 

---
### WaitAfterConnectError
The WaitAfterConnectError property specifies the delay time in milliseconds before attempting to reconnect after a connection failure.

If you're experiencing frequent connection issues, you might want to adjust this value based on your network conditions and operational requirements. A longer wait time might be appropriate in unstable network conditions, while a shorter time might be suitable in environments where quick recovery is critical.

**Type**: Integer

Default is 10000

---
### WaitAfterReadError
The WaitAfterReadError property specifies how long the adapter will pause before attempting another read operation after encountering a read error. The default value is 10000 milliseconds (10 seconds).

**Type**: Integer

Default is 10000

---
### WaitAfterWriteError
The WaitAfterWriteError property defines the pause duration after encountering an error while writing request packets to the PLC controller. The default value is 10000 milliseconds (10 seconds). 

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

