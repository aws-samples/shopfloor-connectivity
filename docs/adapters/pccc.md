# PCCC Protocol Configuration

Configuration for PCCC protocol adapter.

- [PCCC Addressing](#pccc-addressing)

**Configuration**

- [PcccSourceConfiguration](#PcccSourceConfiguration)
- [PcccChannelConfiguration](#PcccChannelConfiguration)
- [PcccAdapterConfiguration](#PcccAdapterConfiguration)
- [PcccControllerConfiguration](#PcccControllerConfiguration)
- [PcccConnectPathConfiguration](#PcccConnectPathConfiguration)



## PCCC Addressing

The following datatype with their addresses can be used as the value of “Address” in a PcccChannel.

Datatype OUTPUT, Prefix O

Default file number 0

Syntax: `0<filenumber>:<element index>[/bit offset][,arraylen]`

**O0:0** First 16 output bits as Boolean values in logical order, the bit at offset 0 becomes the first item in the array.

![img](./img/pccc/image1.png)



**O0:0.1** Second set of 16 output bites as 16 Boolean values in logical order

![img](./img/pccc/image2.png)



**O0:0,2** First 32 output bits as 2 sets of 16 Boolean values in logical order

![img](./img/pccc/image3.png)



**O0:0/0** First output at offset 0 bit as Boolean value

![img](./img/pccc/image4.png)



**O0:0/15** Fifteenth output bit at offset 15 as Boolean value

![img](./img/pccc/image5.png)



---



**Datatype INPUT, Prefix I**

Default file number 1

Syntax: `0<file number>:<element index>[/bit offset][,array len]`



**I1:0** First 16 input bits as Boolean values in logical order, the bit at offset 0 becomes the first item in the array.

![img](./img/pccc/image6.png)



**I1:0.1.** Second set of 16 input bites as 16 Boolean values in logical

![img](./img/pccc/image7.png)



**I1:0,2** First 32 input bits as 2 sets of 16 Boolean values in logical order

![img](./img/pccc/image8.png)



**I1:0/0**. First input bit at offset 0 as Boolean value

![img](./img/pccc/image9.png)



**O0:0/15** Fifteenth output at offset 15 bit as Boolean value

![img](./img/pccc/image10.png)



---

**Datatype BINARY, Prefix B**

Default file number 3

Syntax: `B<file number>:<element index>[/bit offset]`

**B3:0** First 16 binary bits as Boolean values in logical order, the bit shown below at offset 0 becomes the first item in the array.

![img](./img/pccc/image11.png)



**B3:0.1** Second set of 16 binary bites as 16 Boolean values in logical

![img](./img/pccc/image12.png)



**B3:0/0.** First binary bit at offset as Boolean value

![img](./img/pccc/image13.png)



**B3:0/15** Fifteenth binary bit at offset 15 as Boolean value

![img](./img/pccc/image14.png)



---

**Datatype TIMER, Prefix T**

Default file number 4

Syntax: 

`T<file number>:<element index>[/bit offset]` for bit values

`T<file number>:<element index>[.value by name]` for named numeric values

**T4:0** Timer as a structure containing all elements

![img](./img/pccc/image15.png)



**T4:0.ACC** Timer numeric ACC value.

![img](./img/pccc/image16.png)



**T4:0.EN** Timer Boolean EN bit value

![img](./img/pccc/image17.png)



---

**Datatype COUNTER, Prefix C**

Default file number 5

Syntax: 

`C<file number>:<element index>[/bit offset]` for bit values

`C<file number>:<element index>[.value by name]` for named numeric values

**C5:0** Counter as a structure containing all elements

![img](./img/pccc/image18.png)



**C5:0.ACC Counter ACC numeric value**

![img](./img/pccc/image19.png)



**C5:0.ACC Counter CU bit value**

![img](./img/pccc/image20.png)



---

**Datatype CONTROL, Prefix R**

Default file number 6

Syntax: 

`R<file number>:<element index>[/bit offset]` for bit values

`R<file number>:<element index>[.value by name]` for named numeric values

**R6:0** Control as a structure containing all elements

![img](./img/pccc/image21.png)



**R6:0.POS Counter POS numeric value**

![img](./img/pccc/image22.png)



**R6:0.ACC Control EN bit value**

![img](./img/pccc/image23.png)



---

**Datatype OUTPUT, Prefix O**

Default file number 7

Syntax: `N<file number>:<element index>[<array len>]`

**N7:0 First 16 bits integer value**

![img](./img/pccc/image24.png)



**N7:1 Second 16 bits integer value**

![img](./img/pccc/image25.png)



**N7:0,3 First 3 16 bits integer values**

![img](./img/pccc/image26.png)



---

**Datatype FLOAT, Prefix F**

Syntax: `F<file number>:<element index>[<array len>]`

**F8:0 First float value**

![img](./img/pccc/image27.png)



**F8:1 Second float value**

![img](./img/pccc/image28.png)



**F8:0,2 First 2 float value**

![img](./img/pccc/image29.png)



---

**Datatype STRING, Prefix ST**

Syntax: `ST<file number>:<element index>`

**ST9:0 First string value**

![img](./img/pccc/image30.png)



**ST9:1 Second string value**

![img](./img/pccc/image31.png)



---

**Datatype LONG, Prefix L (32 bit)**

Syntax: `L<file number>:<element index>[<array len>]`

**L10:0 First 32 bits integer value**

![img](./img/pccc/image32.png)



**L10:1 Second 32 bits integer value**

![img](./img/pccc/image33.png)



**L10:0,3 First 3 32 bits integer values**

![img](./img/pccc/image34.png)



---

**Datatype ASCII, Prefix A**

Default file number 11

Syntax: `A<file number>:<element index>[/character offset]`

**A11:0 First character pair**

![img](./img/pccc/image35.png)



**A11:0 Second character pair**

![img](./img/pccc/image36.png)



**A11:0/0 First character of in first character pair**

![img](./img/pccc/image37.png)



**A11:0/0 Second character of in seconds character pair**

![img](./img/pccc/image38.png)

---







## PcccSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration#Sources) >  [Source](../core/source-configuration.md) 

Source configuration for the PCCC protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type.

- [Schema](#PcccSourceConfiguration-Schema)
- [Examples](#PcccSourceConfiguration-Examples)

**Properties:**

- [AdapterController](#AdapterController)
- [Channels](#Channels)

---

### AdapterController

Server Identifier for the controller to read from. This referenced server must be present in the Controllers section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a server in the Controllers section of the PCCC adapter used by the source.

---

### Channels

The channels configuration for a PCCC source holds configuration data to read values from fields on the source controller.
The element is a map indexed by the channel identifier.
Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,[PcccChannelConfiguration](#PcccChannelConfiguration)]

At least 1 channel must be configured.

### PcccSourceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for PCCC source",
  "allOf": [
    {
      "$ref": "#/definitions/SourceConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AdapterController": {
          "type": "string",
          "description": "Reference to the PCCC controller configuration in the adapter"
        },
        "Channels": {
          "type": "object",
          "description": "Map of PCCC channel configurations",
          "additionalProperties": {
            "$ref": "#/definitions/PcccChannelConfiguration"
          },
          "minProperties": 1
        }
      },
      "required": ["AdapterController", "Channels"]
    }
  ]
}

```

### PcccSourceConfiguration Examples

```json
{
  "Name": "ProductionLine1",
  "ProtocolAdapter" : "PCCCAdapter",
  "AdapterController": "MainPLC",
  "Channels": {
    "ProductCount": {
      "Name": "count",
      "Address": "N7:0",
      "Description": "Daily production counter"
    },
    "MachineStatus": {
      "Name": "status",
      "Address": "B3:0",
      "Description": "Machine running status"
    }
  }
}

```

[^top](#pccc-protocol-configuration)



## PcccChannelConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration#Sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#Channels) > [Channel](../core/channel-configuration.md)

The PcccChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the PCCC protocol adapter.

- [Schema](#PcccChannelConfiguration-Schema)
- [Examples](#PcccChannelConfiguration-Examples)

**Properties:**

- [Address](#Address)

---

### Address

A string containing the address of the field to read from the controller.

**Type**: String

For supported datatype and address syntax see [PCCC Addressing](#pccc-addressing).

### PcccChannelConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for PCCC channel",
  "allOf": [
    {
      "$ref": "#/definitions/ChannelConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Address": {
          "type": "string",
          "description": "PCCC address for the channel",
          "pattern": "^[A-Z]:\\d+(\\.\\d+)?$"
        }
      },
      "required": ["Address"]
    }
  ]
}

```

### PcccChannelConfiguration Examples

Basic integer address:

```json
{
  "Name": "Counter1",
  "Address": "N7:0",
  "Description": "Production counter"
}
```

.

Bit address:

```json
{
  "Name": "RunningStatus",
  "Address": "B3:0",
  "Description": "Machine running status bit"
}
```

.

 Float address:

```json
{
  "Name": "Temperature",
  "Address": "F8:0",
  "Description": "Process temperature"
}
```

.

Timer address:

```json
{
  "Name": "CycleTimer",
  "Address": "T4:0",
  "Description": "Process cycle timer"
}
```

.

Counter with bit:

```json
{
  "Name": "PartCounter",
  "Address": "C5:0.0",
  "Description": "Parts counter done bit"
}
```



[^top](#pccc-protocol-configuration)



## PcccAdapterConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [ProtocolAdapters](../core/sfc-configuration#ProtocolAdapters) > [Adapter](../core/protocol-adapter-configuration.md) 



PcccAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the PCCC Protocol adapter.

- [Schema](#PcccAdapterConfiguration-Schema)
- [Examples](#PcccAdapterConfiguration-Examples)

**Properties:**

- [Controllers](#Controllers)

---

### Controllers

PLCs servers configured for this adapter. The PCCC source using the adapter must have a reference to one of these in its AdapterController attribute.

**Type**: Map[String,[PcccControllerConfiguration](#PcccControllerConfiguration)]

### PcccAdapterConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for PCCC adapter",
  "allOf": [
    {
      "$ref": "#/definitions/AdapterConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Controllers": {
          "type": "object",
          "description": "Map of PCCC controller configurations",
          "additionalProperties": {
            "$ref": "#/definitions/PcccControllerConfiguration"
          },
          "minProperties": 1
        }
      },
      "required": ["Controllers"]
    }
  ]
}
```

### PcccAdapterConfiguration Examples

```json
{
  "AdapterType": "PcccAdapterType",
  "Controllers": {
    "MainPLC": {
      "Address": "192.168.1.100",
      "ConnectPath": {
        "Backplane": 1,
        "Slot": 0
      },
      "ConnectTimeout": 10000,
      "ReadTimeout": 5000
    }
  }
}

```

[^top](#pccc-protocol-configuration)



## PcccControllerConfiguration

[PccAdapter](#PcccAdapterConfiguration) > [Controllers](#controllers)



- [Schema](#PcccControllerConfiguration-Schema)
- [Examples](#PcccControllerConfiguration-Examples)

**Properties:**

- [Address](#Address)
- [ConnectPath](#ConnectPath)
- [ConnectTimeout](#ConnectTimeout)
- [MaxReadGap](#MaxReadGap)
- [OptimizeReads](#OptimizeReads)
- [Port](#Port)
- [ReadTimeout](#ReadTimeout)
- [WaitAfterConnectError](#WaitAfterConnectError)
- [WaitAfterReadError](#WaitAfterReadError)
- [WaitAfterWriteError](#WaitAfterWriteError)

---

### Address

IP Address of the controller

**Type**: String

IP address in format aaa.bbb.ccc.ddd

---

### ConnectPath

Connect path for controller

**Type**: PcccConnectPathConfiguration

Optional

---

### ConnectTimeout

Timeout for connecting to the controller in milliseconds

**Type**: Integer

Default is 10000

---

### MaxReadGap

When optimization is used this specified the max number of bytes between near adjacent fields that may be combined in a single read.

**Type**: Integer

Default is 32

---

### Name

Description

**Type**: Type

Comments

---

### OptimizeReads

Optimized the reading of data from the controller by combining the reads for (near) adjacent fields in a single read request.

**Type**: Boolean

Default is true

Optimization reduces the calls made to the controller to read data. When troubleshooting optimization it can be disabled to find specific fields that make the (combined) reads to fail.

---

### Port

Port number

**Type**: Integer

Default is 44818

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

### PcccControllerConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for PCCC controller",
  "properties": {
    "Address": {
      "type": "string",
      "description": "IP address or hostname of the PCCC controller"
    },
    "ConnectPath": {
      "$ref": "#/definitions/PcccConnectPathConfiguration",
      "description": "Connection path configuration"
    },
    "ConnectTimeout": {
      "type": "integer",
      "description": "Timeout for connection attempts in milliseconds",
      "default": 10000
    },
    "MaxReadGap": {
      "type": "integer",
      "description": "Maximum gap between addresses to combine reads",
      "default": 32
    },
    "OptimizeReads": {
      "type": "boolean",
      "description": "Enable read optimization by combining adjacent addresses",
      "default": true
    },
    "Port": {
      "type": "integer",
      "description": "Port number for the PCCC connection",
      "default": 44818
    },
    "ReadTimeout": {
      "type": "integer",
      "description": "Timeout for read operations in milliseconds",
      "default": 10000
    },
    "WaitAfterConnectError": {
      "type": "integer",
      "description": "Wait time after connection error in milliseconds",
      "minimum": 0,
      "default": 5000
    },
    "WaitAfterReadError": {
      "type": "integer",
      "description": "Wait time after read error in milliseconds",
      "minimum": 0,
      "default": 10000
    },
    "WaitAfterWriteError": {
      "type": "integer",
      "description": "Wait time after write error in milliseconds",
      "minimum": 0,
      "default": 10000
    }
  },
  "required": ["Address", "ConnectPath"]
}

```

### PcccControllerConfiguration Examples

```json
{
  "Address": "192.168.1.100",
  "ConnectPath": {
    "Backplane": 1,
    "Slot": 0
  }
}
```

[^top](#pccc-protocol-configuration)



## PcccConnectPathConfiguration

[PccAdapter](#PcccAdapterConfiguration) > [Controllers](#controllers) > [PcccController](#PcccControllerConfiguration) > [ConnectPath](#ConnectPath)



- [Schema](#PcccConnectPathConfiguration-Schema)
- [Examples](#PcccConnectPathConfiguration-Examples)

**Properties:**

- [Backplane](#Backplane)
- [Slot](#Slot)

---

### Backplane

Backplane number

**Type**: Integer

Default is 1

---

### Name

Description

**Type**: Type

Comments

---

### Slot

Slot number

**Type**: Integer

Default is 0

### PcccConnectPathConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for PCCC connection path",
  "properties": {
    "Backplane": {
      "type": "integer",
      "description": "Backplane number",
      "default" : 1
    },
    "Slot": {
      "type": "integer",
      "description": "Slot number",
      "default": 0
    }
  }
}

```

### PcccConnectPathConfiguration Examples

```json
{
  "Backplane": 1,
  "Slot": 0
}

```

[^top](#pccc-protocol-configuration)

