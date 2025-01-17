## ChangeFilterConfiguration

[SFC Configuration](./sfc-configuration.md) > [ChangeFilters](./sfc-configuration.md#ChangeFilters)

Change filter to apply to a channel value.

For more information see [DataFiltering](../sfc-data-processing-filtering.md#data-filtering)

- [Schema](#Schema)
- [Examples](#Examples)


**Properties:**
- [AtLeast](#AtLeast)
- [Type](#Type)
- [Value](#Value)

---
### AtLeast
Time interval in milliseconds in which at least a value is passed even the value has not changed or not beyond the specified value

**Type**: Long

---
### Type
Type of the change filter

**Type**: String, value must be any of these:

- "Absolute" (absolute change)
- "Percent" (relative change)
- "Always" (any change)


Default = "Always"

---
### Value
Change amount value

- Absolute value if type is "Absolute"
- Relative value in percent if type is "Percent"
- Ignored if type is always

**Type**: Double

Default is 0.0

[^top](#ChangeFilterConfiguration)



## Schema



```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "FilterType": {
      "type": "string",
      "enum": [
        "Absolute",
        "Percent",
        "Always"
      ],
      "default": "Always",
      "description": "Type of change filter to apply"
    },
    "FilterValue": {
      "type": "number",
      "default": 0.0,
      "description": "Threshold value for the filter"
    },
    "AtLeast": {
      "type": "number",
      "description": "Time interval to pas value even when not changed"
    }
  },
  "additionalProperties": false
}
```



## Examples

Absolute change filter:

```json
{
  "FilterType": "Absolute",
  "FilterValue": 5.0
}
```



Percentage change filter:

```json
{
  "FilterType": "Percent",
  "FilterValue": 10.0
}
```



Any change:

```json
{
  "FilterType": "Always"
}
```



Absolute change filter, with at least a value every 5 seconds even when value did not change

```json
{
  "FilterType": "Absolute",
  "FilterValue": 5.0,
  "Atleast" : 10000
}
```



[^top](#ChangeFilterConfiguration)
