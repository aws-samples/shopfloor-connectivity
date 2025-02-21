## ProtocolAdapterConfiguration

[SFC Configuration](./sfc-configuration.md#metrics) > [ProtocolAdapters](./sfc-configuration.md#protocoladapters) 

Defines the base configuration structure for protocol adapters in SFC, specifying how adapters operate either in-process or as separate services. Includes essential settings for adapter type, server configuration, metrics collection, and custom descriptions. Serves as the foundation for protocol-specific adapter implementations.

- [Schema](#schema)
- [Examples](#examples)

**Properties:**

- [AdapterServer](#adapterserver)
- [AdapterType](#adaptertype)
- [Description](#description)
- [Metrics](#metrics)

---
### AdapterServer
Specifies the server reference for running the protocol adapter as a separate service. When set, it must match an entry in the [AdapterServers](./sfc-configuration.md#adapterservers)  section, enabling IPC-based communication between the SFC core and the adapter service. If not set, the adapter runs in-process within the SFC core. This property is mutually exclusive with [AdapterType](#adaptertype).

**Type**: String

---
### AdapterType
Defines the protocol adapter type for in-process execution, referencing a predefined type (like OPCUA, MQTT, MODBUS-TCP, SNMP, S7, ADS) from the [AdapterTypes](./sfc-configuration#adaptertypes) section. This property is used when the adapter runs within the SFC core process and is mutually exclusive with AdapterServer

**Type**: String

---

### Description

An optional free-form text field that allows users to provide a human-readable description of the protocol adapter, helping to document its purpose or specific configuration details.

**Type**: String

---

### Metrics

Defines the metrics collection configuration for the protocol adapter, specifying how performance and operational metrics should be gathered and processed from this adapter source.

Type: [MetricsSourceConfiguration](./metrics-source-configuration.md)

[^top](#protocoladapterconfiguration)



## Schema



```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "title": "Protocol Adapter Configuration Schema",
  "properties": {
    "AdapterServer": {
      "type": "string",
      "description": "Reference to an adapter server defined in the AdapterServers Section in SFC top level config"
    },
    "AdapterType": {
      "type": "string",
      "description": "Reference to an adapter type defined in the AdapterTypess Section in SFC top level config"
    },
    "Description": {
      "type": "string",
      "description": "Description of the protocol adapter configuration"
    },
    "Metrics": {
      "$ref": "#/definitions/MetricsSourceConfiguration",
      "description": "Configuration for metrics collection"
    }
  },
  "oneOf": [
    {
      "required": ["AdapterServer"],
      "not": {
        "required": ["AdapterType"]
      }
    },
    {
      "required": ["AdapterType"],
      "not": {
        "required": ["AdapterServer"]
      }
    }
  ]
}
```



## Examples



Example with AdapterType:

```json
{
  "AdapterType": "ModbusTCP",
  "Description": "Modbus connection to PLC using in-process adapter"
}
```



Example with just AdapterServer:

```json
{
  "AdapterServer": "S7AdapterServer"
}
```



Example with AdapterType and Metrics:

```json
{
  "AdapterType": "OPCUA",
  "Description": "Building automation controller",
  "Metrics": {
    "CommonDimensions": {
      "Environment": "Production",
      "Location": "Building2",
      "Device" : "Conveyor1"
    }
  }
}
```

[^top](#protocoladapterconfiguration)
