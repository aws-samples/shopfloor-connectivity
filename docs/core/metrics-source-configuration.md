[SFC Configuration](./sfc-top-level-config.md) > [Metrics](./sfc-top-level-config.md#Metrics)

[SFC Configuration](./sfc-top-level-config.md) > [ProtocolAdapters](./sfc-top-level-config.md#ProtocolAdapters) > [ProtocolAdapter](./protocol-adapter-configuration.md) > [Metrics](./protocol-adapter-configuration.md#Metrics)

[SFC Configuration](./sfc-top-level-config.md) > [Targets](./sfc-top-level-config.md#Targets) > [TargetAdapter](./target-configuration.md) > [Metrics](./target-configuration.md#Metrics)

## MetricsSourceConfiguration

- [Schema](#Schema)
- [Examples](#Schema)

**Properties:**

- [CommonDimensions](#CommonDimensions)

- [Enabled](#Enabled)


---
### CommonDimensions
Set of extra dimensions added to every datapoint

**Type**: Map(String,String)

Optional

---
### Enabled
Collection enabled or disabled

**Type**: Boolean

Default is true

[^top](#MetricsSourceConfiguration)

## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "CommonDimensions": {
      "type": "object",
      "description": "Map of dimension names to values, both strings",
      "patternProperties": {
        "^.*$": {
          "type": "string"
        }
      },
      "additionalProperties": false
    },
    "Enabled": {
      "type": "boolean",
      "description": "Flag to enable/disable metrics collection",
      "default": true
    }
  }
}
```



Examples

```json
{
  "CommonDimensions": {
    "Environment": "Production",
    "Location": "Building2",
    "Device" : "Conveyor1"
  }
}
```

[^top](#MetricsSourceConfiguration)
