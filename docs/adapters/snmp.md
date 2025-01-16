# SNMP Protocol Configuration

SNMP Protocol adapter configuration

---
- [SnmpSourceConfiguration](#SnmpSourceConfiguration)
- [SnmpChannelConfiguration](#SnmpChannelConfiguration)
- [SnmpAdapterConfiguration](#SnmpAdapterConfiguration)
- [SnmpDeviceConfiguration](#SnmpDeviceConfiguration)

---

## SnmpSourceConfiguration

[SFC Configuration](../core/sfc-top-level-config.md) > [Sources](../core/sfc-top-level-config.md#Sources) >  [Source](../core/source-configuration.md) 



Source configuration for the SNMP protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type.

- [Schema](#SnmpSourceConfiguration-Schema)
- [Examples](#SnmpSourceConfiguration-Examples)


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

[SFC Configuration](../core/sfc-top-level-config.md) > [Sources](../core/sfc-top-level-config.md#Sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#Channels) > [Channel](../core/channel-configuration.md)



The SnmpChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the SNMP protocol adapter.

- [Schema](#SnmpChannelConfiguration-Schema)
- [Examples](#SnmpChannelConfiguration-Examples)

**Properties:**

- [ObjectId](#ObjectId)



---
### ObjectId
ID of the object to read

**Type**: string

Must be in valid dot format notation

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

[SFC Configuration](../core/sfc-top-level-config.md) > [ProtocolAdapters](../core/sfc-top-level-config.md#ProtocolAdapters) > [Adapter](../core/protocol-adapter-configuration.md) 



SnmpAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the SNMP Protocol adapter.

- [Schema](#SnmpAdapterConfiguration-Schema)
- [Examples](#SnmpAdapterConfiguration-Examples)

**Properties:**
- [Devices](#Devices)


---
### Devices
Snmp devices configured for this adapter. The modbus tcp source using the adapter must refer to one of these servers with the AdapterDevice attribute.

**Type**: Map[String,[SnmpDeviceConfiguration](#SnmpDeviceConfiguration)]

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
      "SnmpVersion": 2",
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

[SnmpAdapter](#SnmpAdapter) > [Devices](#devices])



- [Schema](#SnmpDeviceConfiguration-Schema)
- [Examples](#SnmpDeviceConfiguration-Examples)


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

