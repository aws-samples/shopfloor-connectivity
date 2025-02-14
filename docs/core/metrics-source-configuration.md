## MetricsSourceConfiguration

[SFC Configuration](./sfc-configuration.md) > [Metrics](./sfc-configuration.md#metrics)

[SFC Configuration](./sfc-configuration.md) > [ProtocolAdapters](./sfc-configuration.md#protocoladapters) > [ProtocolAdapter](./protocol-adapter-configuration.md) > [Metrics](./protocol-adapter-configuration.md#metrics)

[SFC Configuration](./sfc-configuration.md) > [Targets](./sfc-configuration.md#targets) > [TargetAdapter](./target-configuration.md) > [Metrics](./target-configuration.md#metrics)

Configuration for individual metrics sources within SFC, controlling collection at the source level. Includes an enable/disable flag and the ability to add custom dimensions specific to the source. Provides granular control over metrics collection for protocol adapters, targets, and other components.

- [Schema](#schema)
- [Examples](#schema)

**Properties:**

- [CommonDimensions](#commondimensions)
- [Enabled](#enabled)


---
### CommonDimensions
Defines source-specific key-value pairs that are automatically added to every metric from this particular source. These dimensions help identify and categorize metrics from individual components or data sources within the system.

**Type**: Map(String,String)

Optional

---
### Enabled
Controls whether metrics collection is active for this specific source. When true (default), metrics are collected from this source. When false, metrics collection is disabled for this source only, without affecting other sources in the system.

**Type**: Boolean

Default is true

[^top](#metricssourceconfiguration)

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

[^top](#metricssourceconfiguration)
