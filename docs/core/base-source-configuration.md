# BaseSourceConfiguration

[SFC Configuration](./sfc-configuration.md) > [Sources](./sfc-configuration.md#sources)

The BaseSourceConfiguration serves as a foundational that defines common properties used by protocol adapter sources in the SFC  framework. It provides essential attributes like name, description, and protocol adapter identification that all source configurations share. Protocol-specific adapters inherit from this class and extend it with their own specialized configuration properties to support their unique protocol requirements

- [Schema](#schema)
- [Example](#example)

**Properties:**

- [Description](#description)
- [Name](#name)
- [ProtocolAdapter](#protocoladapter)

---

### Description

The Description property provides a human-readable text description of the source. It allows users to document the purpose, functionality, or any relevant details about the configured source.

Type: String

---

### Name

The Name property defines the identifier for the source in the output data. It only needs to be specified if you want the source name in the output to differ from the key used in the SFC top-level configuration's [Sources](./sfc-configuration.md#sources)  property. If not set, the source key from the configuration will be used as the source name in the output

---

### ProtocolAdapter

The ProtocolAdapter property specifies a reference to the protocol adapter that will be used for this source. This reference corresponds to an adapter defined in the SFC top-level configuration's [ProtocolAdapters](./sfc-configuration.md#protocoladapters)  property. It establishes the connection between the source and the specific protocol adapter that will handle the communication

---

[^top](#basesourceconfiguration)

## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "BaseSourceConfiguration",
  "type": "object",
  "properties": {
    "Description": {
      "type": "string".
      "description": "Description of the source"
    },
    "Name": {
      "type": "string",
      "description": "Name of the source"
    },
    "ProtocolAdapter": {
      "type": "string",
      "description": "Protocol adapter identifier for the source"
    }
  },
  "additionalProperties": false
}
```



## Example

```json
{
  "Description": "Production line sensor data source",
  "Name": "ProductionLineSensor1",
  "ProtocolAdapter": "OPCUA-SENSOR-ADAPTER"
}
```



[^top](#basesourceconfiguration)
