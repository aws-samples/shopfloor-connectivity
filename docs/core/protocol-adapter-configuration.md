[SFC Configuration](./sfc-top-level-config.md#Metrics) > [ProtocolAdapters](./sfc-top-level-config.md#ProtocolAdapters) 

## ProtocolAdapterConfiguration

ProtocolAdapterConfiguration which is extended by the implementation of a protocol adapter with specific properties for that adapter.

- [Schema](#Schema)
- [Examples](#Examples)

**Properties:**

- [AdapterServer](#AdapterServer)
- [AdapterType](#AdapterType)
- [Description](#Description)
- [Metrics](#Metrics)

---
### AdapterServer
If the adapter runs as a service in a separate process, then this attribute must refer to an entry for that server in the [ProtocolAdapterServers](./sfc-top-level-config.md#protocoladapterservers) section.

**Type**: String

 If this attribute is not set then the SFC core will load and execute the protocol adapter in the SFC core process. If set then and IPC client will be used to communicate with the service that runs the protocol adapter. If an adapter server is specified, then the [AdapterType](#AdapterType) setting is not used.

---
### AdapterType
Type of the adapter. These types are predefined for each adapter type (e.g., OPCUA, MQTT,MODBUS-TCP, SNMP, S7, ADS ).
If the adapter is running in the same process as the SFC core module, then it must refer to an entry in the [ProtocolAdapterTypes](./sfc-top-level-config.md#ProtocolAdapterTypes) section. 

**Type**: String

---

### Description

User defined description of the adapter

**Type**: String

Optional

---

### Metrics

Metrics configuration for the protocol adapter

Type: [MetricsSourceConfiguration](./metrics-source-configuration.md)

[^top](#ProtocolAdapterConfiguration)



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

[^top](#ProtocolAdapterConfiguration)
