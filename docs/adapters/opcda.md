
# OPCDA Protocol Configuration

Configuration for OPCDA protocol adapter.

---
- [OpcdaSourceConfiguration](#OpcdaSourceConfiguration)
- [OpcdaChannelConfiguration](#OpcdaChannelConfiguration)
- [OpcdaAdapterConfiguration](#OpcdaAdapterConfiguration)
- [OpcdaServerConfiguration](#OpcdaServerConfiguration)

---

## OpcdaSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration#Sources) >  [Source](../core/source-configuration.md)



Source configuration for the OPCDA protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type.

- [Schema](#OpcdaSourceConfiguration-Schema)
- [Examples](#OpcdaSourceConfiguration-Examples)

**Properties:**
- [AdapterOpcdaServer](#AdapterOpcdaServer)
- [Channels](#Channels)
- [SourceReadingMode](#SourceReadingMode)

---
### AdapterOpcdaServer
Server Identifier for the OPCDA server to read from. This referenced server must be present in the dServers section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a server in the OpcdaServers section of the OPCDA adapter used by the source.

---
### Channels
The channels configuration for an OPCDA source holds configuration data to read values from items on the source OPCDA server.
The element is a map indexed by the channel identifier.
Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,[OpcdaChannelConfiguration](#OpcdaChannelConfiguration)]

At least 1 channel must be configured.

---
### SourceReadingMode
Mode for reading values from OPCDA server.

- "Subscription": connector will create a subscription and will monitor the items configured in the channels for the source. When reading from the adapter in this mode, only items that have been changed in the schedule interval period will be returned, except for the initial read that will return all monitored items.
- "Polling", the connector will batch-read all items configured in the channels for the source with the interval defined in the schedule.


**Type**: String 
Possible values are "Subscription" or "Polling".

Default is "Subscription".

[^top](#opcda-protocol-configuration)

### OpcdaSourceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for OPC DA source",
  "allOf": [
    {
      "$ref": "#/definitions/SourceConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AdapterOpcdaServer": {
          "type": "string",
          "description": "Reference to the OPC DA server configuration to be used by this source"
        },
        "Channels": {
          "type": "object",
          "description": "Map of OPC DA channel configurations",
          "additionalProperties": {
            "$ref": "#/definitions/OpcdaChannelConfiguration"
          },
          "minProperties": 1
        },
        "SourceReadingMode": {
          "type": "string",
          "description": "Reading mode for the OPC DA source",
          "enum": ["Subscription", "Polling"],
          "default": "Subscription"
        }
      },
      "required": [
        "AdapterOpcdaServer",
        "Channels"
      ]
    }
  ]
}

```

### OpcdaSourceConfiguration Examples

Basic configuration with subscription:

```json
{
  "ProtocolAdapter" : "OpddaAdapter",
  "AdapterOpcdaServer": "MainServer",
  "Channels": {
    "Temperature": {
      "Item": "Building1.Floor1.Room1.Temperature"
    }
  }
}
```


Multiple channels with polling

```json
{
  "ProtocolAdapter" : "OpddaAdapter",
  "AdapterOpcdaServer": "ProductionServer",
  "SourceReadingMode": "Polling",
  "Channels": {
    "Speed": {
      "Item": "Line1.Conveyor.Speed",
      "Name": "ConveyorSpeed"
    },
    "Pressure": {
      "Item": "Line1.Tank.Pressure",
      "Name": "TankPressure"
    },
    "Temperature": {
      "Item": "Line1.Oven.Temperature",
      "Name": "OvenTemp"
    }
  }
}
```



Multiple channels with subscription

```json
{
  "ProtocolAdapter" : "OpddaAdapter",
  "AdapterOpcdaServer": "ProcessControl",
  "SourceReadingMode": "Subscription",
  "Channels": {
    "FlowRate": {
      "Item": "Process.Flow.Rate",
      "Name": "ProcessFlowRate",
      "Description": "Main process flow rate"
    },
    "Level": {
      "Item": "Process.Tank.Level",
      "Name": "TankLevel",
      "Description": "Tank level measurement"
    }
  }
}
```

Copy

## OpcdaChannelConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration#Sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#Channels) > [Channel](../core/channel-configuration.md)



The OpcdaChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the OPCDA protocol adapter.

- [Schema](#OpcdaChannelConfiguration-Schema)
- [Examples](#OpcdaChannelConfiguration-Examples)

**Properties:**
- [Item](#Item)

---
### Item
A string containing the name of the item to read the value from or to monitor.

**Type**: String

[^top](#opcda-protocol-configuration)

### OpcdaChannelConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for OPC DA channel",
  "allOf": [
    {
      "$ref": "#/definitions/ChannelConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Item": {
          "type": "string",
          "description": "OPC DA item identifier"
        }
      },
      "required": [
        "Item"
      ]
    }
  ]
}

```

### OpcdaChannelConfiguration Examples

Basic configuration:

```json
{
  "Item": "Channel1.Device1.Tag1"
}
```

Temperature sensor:

```json
{
  "Item": "Building1.Floor2.Room3.Temperature",
  "Name": "Room3Temperature",
  "Description": "Temperature sensor in Room 3"
}
```



## OpcdaAdapterConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [ProtocolAdapters](../core/sfc-configuration#ProtocolAdapters) > [Adapter](../core/protocol-adapter-configuration.md) 



OpcdaAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the OPCDA Protocol adapter.

- [Schema](#OpcdaAdapterConfiguration-Schema)
- [Examples](#OpcdaAdapterConfiguration-Examples)

**Properties:**

- [OpcdaServers](#OpcdaServers)

---
### OpcdaServers
Opcda servers configured for this adapter. The Opcda source using the adapter must refer to one of these servers with the AdapterOpcdaServer attribute.

**Type**: Map[String,[OpcdaServerConfiguration](#OpcdaServerConfiguration)]

[^top](#opcda-protocol-configuration)

### OpcdaAdapterConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for OPC DA adapter",
  "allOf": [
    {
      "$ref": "#/definitions/AdapterConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Servers": {
          "type": "object",
          "description": "Map of OPC DA server configurations",
          "additionalProperties": {
            "$ref": "#/definitions/OpcdaServerConfiguration"
          },
          "minProperties": 1
        }
      },
      "required": [
        "Servers"
      ]
    }
  ]
}

```

### OpcdaAdapterConfiguration Examples

```json
{
  "AdapterType" : "OpcdaAdapter",
  "Servers": {
    "MainServer": {
      "Url": "opcda://localhost/Matrikon.OPC.Simulation"
    }
  }
}

```

## OpcdaServerConfiguration

[OpcdaAdapter](#OpcdaAdapterConfiguration)> [OpcdaServers](#OpcdaServers)



- [Schema](#OpcdaServerConfiguration-Schema)
- [Examples](#OpcdaServerConfiguration-Examples)



**Properties:**

- [ConnectTimeout](#ConnectTimeout)
- [ReadBatchSize](#ReadBatchSize)
- [ReadTimeout](#ReadTimeout)
- [SamplingRate](#SamplingRate)
- [Url](#Url)
- [WaitAfterConnectError](#WaitAfterConnectError)

---
### ConnectTimeout
Timeout in milliseconds connecting to the server/td>
Integer

**Type**: Integer

Default is 10000, the minimum value is 1000



---
### ReadBatchSize
Max number of items to read in a single batch read from the server

**Type**: Integer

If not specified all configured items are read in a single read

---
### ReadTimeout
Timeout in milliseconds reading from the server

**Type**: Integer

Default is 10000

---
### SamplingRate
Time in milliseconds for sampling items in subscription mode.

**Type**: Integer

If not specified then the shorted interval will be used from all active schedules that have a source using this server.

---
### Url
Url	Address of the OPCDA server

**Type**: String

e.g., "opcda://192.168.1.145/Simulation"

---
### WaitAfterConnectError
Time in milliseconds to wait to reconnect after a connection error

**Type**: Integer

Default is 10000, the minimum value is 1000



### OpcdaServerConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for OPC DA server connection",
  "properties": {
    "ConnectTimeout": {
      "type": "integer",
      "description": "Timeout in milliseconds for establishing connection",
      "minimum": 1000,
      "default": 10000
    },
    "ReadBatchSize": {
      "type": "integer",
      "description": "Number of items to read in a single batch"
    },
    "ReadTimeout": {
      "type": "integer",
      "description": "Timeout in milliseconds for read operations",
      "default": 1000
    },
    "SamplingRate": {
      "type": "integer",
      "description": "Rate in milliseconds at which to sample data"
    },
    "Url": {
      "type": "string",
      "description": "URL of the OPC DA server",
      "pattern": "^opcda://.+"
    },
    "WaitAfterConnectError": {
      "type": "integer",
      "description": "Wait time in miliseconds after connection error before retry",
      "minimum": 1000,
      "default": 10000
    }
  },
  "required": [
    "Url"
  ]
}

```



### OpcdaServerConfiguration Examples

```json
{
  "Url": "opcda://opc.server.com/Prosys.OPC.Simulation",
  "ReadBatchSize": 100,
  "ConnectTimeout": 45
}

```



[^top](#opcda-protocol-configuration)

