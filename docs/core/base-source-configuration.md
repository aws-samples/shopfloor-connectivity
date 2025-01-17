# BaseSourceConfiguration

[SFC Configuration](./sfc-configuration.md) > [Sources](./sfc-configuration.md#sources)

The BaseSourceConfiguration class contains the common properties for protocol adapter sources. Protocol adapters extend this class with specific properties for that adapter.

-[Schema](#schema)
-[Example](#example)

**Properties:**

- [Description](#description)
- [Name](#name)
- [ProtocolAdapter](#protocoladapter)

---

### Description

Description of the source.

Type: String

---

### Name

Name of the source in the output data. This property only needs to be set if the sourcename in the output must be different from the key for the source in the SFC toplevel configuration [Sources](./sfc-configuration.md#sources) property.

---

### ProtocolAdapter

Reference to the adapter for the source in the SFC toplevel configuration [Sources](./sfc-configuration.md#sources) property.

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
