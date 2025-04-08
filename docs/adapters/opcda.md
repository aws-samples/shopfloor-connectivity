
# OPCDA Protocol Configuration

The OPC Data Access (DA) adapter for SFC enables integration with legacy industrial automation systems by connecting to OPC DA servers. It allows reading and writing of real-time process data from devices and systems that support the classic OPC DA specification, commonly found in manufacturing and process control environments. The adapter supports browsing of available tags, synchronous and asynchronous data access, and handles data type conversions between OPC DA and SFC's internal format

Due to the OPC DA dependency on Windows DCOM this adapter can only be executed as an IPC server.

---
- [OpcdaSourceConfiguration](#opcdasourceconfiguration)
- [OpcdaChannelConfiguration](#opcdachannelconfiguration)
- [OpcdaAdapterConfiguration](#opcdaadapterconfiguration)
- [OpcdaServerConfiguration](#opcdaserverconfiguration)

---

## OpcdaSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) >  [Source](../core/source-configuration.md)

Configuration class that defines settings for connecting to and reading data from an OPC DA server source. It specifies server connection details, tag browsing parameters, and data collection settings for retrieving real-time process values from OPC DA-compliant devices and systems

Source configuration for the OPCDA protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type.

- [Schema](#opcdasourceconfiguration-schema)
- [Examples](#opcdasourceconfiguration-examples)

**Properties:**
- [AdapterOpcdaServer](#adapteropcdaserver)
- [Channels](#channels)
- [SourceReadingMode](#sourcereadingmode)

---
### AdapterOpcdaServer
Identifier that references a specific OPC DA server configuration defined in the adapter's Servers section, linking the source to its corresponding server connection settings.

**Type**: String

Must be an identifier of a server in the OpcdaServers section of the OPCDA adapter used by the source.

---
### Channels
Map of data channels defined for this source, where each channel is identified by a unique key. Channels can be temporarily disabled by prefixing their identifier with '#'.

**Type**: Map[String,[OpcdaChannelConfiguration](#opcdachannelconfiguration)]

At least 1 channel must be configured.

---
### SourceReadingMode
- Defines the data collection process from the OPC DA server, offering two modes: "Subscription" and "Polling."

  **Subscription Mode:** In this mode, the connector establishes a subscription to monitor changes in the configured items in the source channels. Upon reading from the adapter, only items that have been modified within the specified schedule interval period are returned. The initial read returns all monitored items.

  **Polling Mode:** In this mode, the connector performs batch-reading of all items configured in the source channels at the interval defined in the schedule.


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

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#channels) > [Channel](../core/channel-configuration.md)

Configuration class that defines settings for an individual OPC DA channel, specifying how to read and process data from a specific item or tag on the OPC DA server

The OpcdaChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the OPCDA protocol adapter.

- [Schema](#opcdachannelconfiguration-schema)
- [Examples](#opcdachannelconfiguration-examples)

**Properties:**
- [Item](#item)

---
### Item
The name or identifier of the OPC DA item (tag) from which values will be read or monitored on the server

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

[SFC Configuration](../core/sfc-configuration.md) > [ProtocolAdapters](../core/sfc-configuration.md#protocoladapters) > [Adapter](../core/protocol-adapter-configuration.md) 

Configuration class that defines the overall settings for the OPC DA adapter, including server connections, security settings, and communication parameters for interacting with OPC DA data sources

OpcdaAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the OPCDA Protocol adapter.

- [Schema](#opcdaadapterconfiguration-schema)
- [Examples](#opcdaadapterconfiguration-examples)

**Properties:**

- [OpcdaServers](#opcdaservers)

---
### OpcdaServers
Collection of OPC DA server configurations available to the adapter, each identified by a unique key that sources can reference through their AdapterOpcdaServer property.

**Type**: Map[String,[OpcdaServerConfiguration](#opcdaserverconfiguration)]

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

[OpcdaAdapter](#opcdaadapterconfiguration)> [OpcdaServers](#opcdaservers)

Configuration class that defines connection parameters for a specific OPC DA server, including server identification, connection settings, and authentication details. Each server configuration can be referenced by OPC DA sources to establish connections and access data from the specified server

- [Schema](#opcdaserverconfiguration-schema)
- [Examples](#opcdaserverconfiguration-examples)



**Properties:**

- [ConnectTimeout](#connecttimeout)
- [ReadBatchSize](#readbatchsize)
- [ReadTimeout](#readtimeout)
- [SamplingRate](#samplingrate)
- [Url](#url)
- [WaitAfterConnectError](#waitafterconnecterror)

---
### ConnectTimeout
Maximum time in milliseconds allowed for establishing a connection to the OPC DA server.

**Type**: Integer

Default is 10000, the minimum value is 1000



---
### ReadBatchSize
Maximum number of items that can be requested in a single batch read operation from the OPC DA server.

**Type**: Integer

If not specified all configured items are read in a single read

---
### ReadTimeout
Maximum time in milliseconds to wait for a response when reading data from the OPC DA server.

**Type**: Integer

Default is 10000

---
### SamplingRate
Interval in milliseconds between consecutive samples when monitoring items in subscription mode.

**Type**: Integer

If not specified then the shorted interval will be used from all active schedules that have a source using this server.

---
### Url
Network address or connection endpoint of the OPC DA server.

**Type**: String

e.g., "opcda://192.168.1.145/Simulation"

---
### WaitAfterConnectError
Delay in milliseconds before attempting to reconnect following a failed connection attempt.

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

