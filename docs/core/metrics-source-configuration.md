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

