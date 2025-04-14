# SNMP Protocol Configuration

The SFC SNMP Protocol Adapter enables communication with devices using Simple Network Management Protocol (SNMP) versions 1 and 2. The adapter collects data from SNMP-enabled network devices, sensors, and equipment by polling OIDs.

In order to use this adapter as in [in-process](../sfc-running-adapters.md#running-protocol-adapters-in-process) type adapter the type must be added to the [AdapterTypes](../core/sfc-configuration.md#adaptertypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"AdapterTypes" :{
  "SNMP" : {
    "JarFiles" : ["<location of deployment>/snmp/lib"]
  },
  "FactoryClassName" : "com.amazonaws.sfc.snmp.SnmpAdapter"
}
```

**Configuration:**

- [SnmpSourceConfiguration](#snmpsourceconfiguration)

- [SnmpChannelConfiguration](#snmpchannelconfiguration)

- [SnmpAdapterConfiguration](#snmpadapterconfiguration)

- [SnmpDeviceConfiguration](#snmpdeviceconfiguration)

  

---

## SnmpSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) >  [Source](../core/source-configuration.md) 

The SnmpSourceConfiguration class defines which data (OIDs) to read and maps these to channel names, while referencing a pre-configured adapter device that contains the actual connection parameters. This configuration determines what data to collect from the specified SNMP device.

Source configuration for the SNMP protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type.

- [Schema](#snmpsourceconfiguration-schema)
- [Examples](#snmpsourceconfiguration-examples)


**Properties:**
- [AdapterDevice](#adapterdevice)
- [Channels](#channels)

---
### AdapterDevice
The AdapterDevice property specifies the identifier that references a device defined in the [Devices](#devices) section of the SNMP adapter configuration.

**Type**: String

---
### Channels
The Channels property is a map of channel configurations, where each key is a unique channel identifier and its corresponding value contains the configuration for reading data from the SNMP device. Individual channels can be disabled by prefixing their identifier with a "#" character.

**Type**: Map[String,[SnmpChannelConfiguration](#snmpchannelconfiguration)]

At least 1 channel must be configured.

### SnmpSourceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for SNMP source",
  "allOf": [
    {
      "$ref": "#/definitions/SourceConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AdapterDevice": {
          "type": "string",
          "description": "Reference to the SNMP device configuration in the adapter"
        },
        "Channels": {
          "type": "object",
          "description": "Map of SNMP channel configurations",
          "additionalProperties": {
            "$ref": "#/definitions/SnmpChannelConfiguration"
          },
          "minProperties": 1
        }
      },
      "required": ["AdapterDevice", "Channels"]
    }
  ]
}

```

### SnmpSourceConfiguration Examples

```json
{
  "ProtocolAdapter": "SnmpAdapter",
  "Description": "Core switch monitoring",
  "AdapterDevice": "CoreSwitch",
  "Channels": {
    "Uptime": {
      "Name": "SystemUptime",
      "Description": "System uptime",
      "ObjectId": "1.3.6.1.2.1.1.3.0"
    },
    "InOctets": {
      "Name": "IncomingTraffic",
      "Description": "Incoming traffic on port 1",
      "ObjectId": "1.3.6.1.2.1.2.2.1.10.1"
    },
    "OutOctets": {
      "Name": "OutgoingTraffic",
      "Description": "Outgoing traffic on port 1",
      "ObjectId": "1.3.6.1.2.1.2.2.1.16.1"
    }
  }
}

```

[^top](#snmp-protocol-configuration)



## SnmpChannelConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#channels) > [Channel](../core/channel-configuration.md)

The SnmpChannelConfiguration class extends [ChannelConfiguration](../core/channel-configuration.md)  and specifies which SNMP ObjectID (OID) to read for obtaining the channel's value. It inherits common channel properties from its parent class while adding SNMP-specific configuration for data collection. 

- [Schema](#snmpchannelconfiguration-schema)
- [Examples](#snmpchannelconfiguration-examples)

**Properties:**

- [ObjectId](#objectid)



---
### ObjectId
The ObjectId property specifies the SNMP Object Identifier (OID) that identifies which data point to read from the SNMP device. The OID can be written in dot notation format (e.g., "1.3.6.1.2.1.1.3.0") where each number represents a node in the SNMP Management Information Base (MIB) tree hierarchy.

**Type**: string

### SnmpChannelConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for SNMP channel",
  "allOf": [
    {
      "$ref": "#/definitions/ChannelConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "ObjectId": {
          "type": "string",
          "description": "SNMP Object Identifier (OID)",
          "pattern": "^([0-9]+\\.)*[0-9]+$"
        }
      },
      "required": ["ObjectId"]
    }
  ]
}
```

### SnmpChannelConfiguration Examples

```json
{
  "Name": "SystemUptime",
  "Description": "System uptime in timeticks",
  "ObjectId": "1.3.6.1.2.1.1.3.0"
}
```

[^top](#snmp-protocol-configuration)



## SnmpAdapterConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [ProtocolAdapters](../core/sfc-configuration.md#protocoladapters) > [Adapter](../core/protocol-adapter-configuration.md) 

The SnmpAdapterConfiguration class defines the configuration for the SNMP adapter, including a collection of SNMP devices to communicate with and their associated settings. It extends [AdapterConfiguration](../core/protocol-adapter-configuration.md) which provides common adapter configuration properties.

- [Schema](#snmpadapterconfiguration-schema)
- [Examples](#snmpadapterconfiguration-examples)

**Properties:**
- [Devices](#devices)


---
### Devices
The Devices property is a map of SNMP device configurations, where each key is a unique device identifier and its value contains the device's connection settings. These configured devices can be referenced by SNMP sources using their [AdapterDevice](#adapterdevice) attribute to specify which device to read from.

**Type**: Map[String,[SnmpDeviceConfiguration](#snmpdeviceconfiguration)]

### SnmpAdapterConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for SNMP adapter",
  "allOf": [
    {
      "$ref": "#/definitions/AdapterConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Devices": {
          "type": "object",
          "description": "Map of SNMP device configurations",
          "additionalProperties": {
            "$ref": "#/definitions/SnmpDeviceConfiguration"
          },
          "minProperties": 1
        }
      },
      "required": ["Devices"]
    }
  ]
}

```

### SnmpAdapterConfiguration Examples

```json
{
  "AdapterType": "SnmpAdapter",
  "Description": "Basic network device monitoring",
  "Devices": {
    "MainSwitch": {
      "Address": "192.168.1.1",
      "Community": "public",
      "SnmpVersion": 2,
      "Port": 161,
      "Timeout": 5000,
      "Retries": 3
    },
    "BackupSwitch": {
      "Address": "192.168.1.2",
      "Community": "public",
      "SnmpVersion": 2,
      "Port": 161,
      "Timeout": 5000,
      "Retries": 3
    }
  }
}

```

[^top](#snmp-protocol-configuration)



## SnmpDeviceConfiguration

[SnmpAdapter](#snmpadapterconfiguration) > [Devices](#devices)

The SnmpDeviceConfiguration class defines the connection parameters needed to communicate with a specific SNMP-enabled device. 


- [Schema](#snmpdeviceconfiguration-schema)
- [Examples](#snmpdeviceconfiguration-examples)


**Properties:**
- [Address](#address)
- [Community](#community)
- [NetworkProtocol](#networkprotocol)
- [Port](#port)
- [ReadBatchSize](#readbatchsize)
- [Retries](#retries)
- [SnmpVersion](#snmpversion)
- [Timeout](#timeout)

---
### Address
The Address property specifies the IP address of the SNMP-enabled device that the adapter will communicate with. This is the network address where the SNMP agent is running on the target device.

**Type**: String

---
### Community
The Community property defines the SNMP community string used for authentication in SNMP versions 1 and 2c. It acts as a simple password mechanism for accessing the SNMP device. While the default value is "public", it's recommended to change this to a unique value for security purposes. The community string can contain alphanumeric characters, hyphens, and underscores,

**Type**: String

Default is "public"

---
### NetworkProtocol
The NetworkProtocol property specifies the transport protocol to be used for SNMP communication, accepting either `"UDP"` or `"TCP"` as valid values. UDP (User Datagram Protocol) is the default protocol for SNMP communications, though TCP (Transmission Control Protocol) can be used when more reliable delivery is required.

**Type**: String

Default is "UDP"

---
### Port
The Port property specifies the network port number where the SNMP agent is listening on the device. The default port for SNMP communication is 161, which is the standard SNMP port for queries and commands.

**Type**: Integer

Default is 161

---
### ReadBatchSize
The ReadBatchSize property determines how many SNMP values will be read in a single batch operation. It allows for more efficient network utilization by grouping multiple SNMP GET requests together. The default value is 100, meaning up to 100 values will be retrieved in a single batch request to the SNMP device.

**Type**: Integer

Default is 100

---
### Retries
The Retries property specifies how many additional attempts the adapter will make to read from the SNMP device if the initial request fails. With the default value of 2, the adapter will make a total of 3 attempts (initial attempt plus 2 retries) before considering the operation failed. 

**Type**: Integer

Default is 2

---
### SnmpVersion
The SnmpVersion property specifies which version of the SNMP protocol to use when communicating with the device. It accepts either 1 (for SNMPv1) or 2 (for SNMPv2c), with version 2 being the default. 

**Type**: Integer

---
### Timeout
The Timeout property defines how long (in milliseconds) the adapter will wait for a response from the SNMP device before considering the request failed. After this period elapses without receiving a response, the adapter will either retry the request (if retries are configured) or report a timeout error. 

**Type**: Integer

Default is 10000

### SnmpDeviceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for SNMP device",
  "properties": {
    "Address": {
      "type": "string",
      "description": "IP address or hostname of the SNMP device"
    },
    "Community": {
      "type": "string",
      "description": "SNMP community string"
      "default" : "public"
    },
    "NetworkProtocol": {
      "type": "string",
      "description": "Network protocol to use",
      "enum": ["UDP", "TCP"],
      "default" : "UDP"
    },
    "Port": {
      "type": "integer",
      "description": "SNMP port number",
      "default": 161
    },
    "ReadBatchSize": {
      "type": "integer",
      "description": "Number of OIDs to read in a single SNMP request"
    },
    "Retries": {
      "type": "integer",
      "description": "Number of retry attempts for failed requests"
    },
    "SnmpVersion": {
      "type": "integer",
      "description": "SNMP protocol version",
      "enum": [1, 2],
      "default" : 2
    },
    "Timeout": {
      "type": "integer",
      "description": "Timeout in milliseconds for SNMP requests"
    }
  },
  "required": ["Address"]
}

```

### SnmpDeviceConfiguration Examples

```json
{
  "Address": "192.168.1.100",
  "Community": "public",
  "SnmpVersion": "v2c",
  "Port": 161,
  "Timeout": 5000,
  "Retries": 3
}

```

[^top](#snmp-protocol-configuration)

