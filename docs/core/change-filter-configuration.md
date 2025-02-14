## ChangeFilterConfiguration

[SFC Configuration](./sfc-configuration.md) > [ChangeFilters](./sfc-configuration.md#changefilters)

The ChangeFilterConfiguration defines settings for filtering data based on value changes. It allows configuration of how and when data should be filtered by comparing changes in values. The class includes validation logic to ensure proper configuration of filter parameters. It contains properties for the filter value threshold, filter type, and minimum time interval between changes. This configuration is used to determine when data should be passed through or filtered out based on how its value changes over time

For more information see [DataFiltering](../sfc-data-processing-filtering.md#data-filtering)

- [Schema](#schema)
- [Examples](#examples)


**Properties:**
- [AtLeast](#atleast)
- [Type](#type)
- [Value](#value)

---
### AtLeast
The AtLeast property specifies a minimum time interval in milliseconds during which at least one value will be passed through the filter, regardless of whether the value has changed or met the filter value threshold. This ensures that data is reported at a minimum frequency even when values remain stable. For example, if set to 60000 (1 minute), it guarantees that at least one value will be passed through every minute, even if no significant changes have occurred

**Type**: Long

---
### Type
The Type property defines how changes in values are evaluated by the filter. It accepts three possible values:

- "Absolute": Filters based on the absolute numerical difference between values
- "Percent": Filters based on the relative percentage change between values
- "Always": Passes through any change in value, regardless of magnitude

If not specified, it defaults to "Always". This property determines the method used to compare current and previous values when deciding whether to pass or filter the data

**Type**: String

---
### Value
- The Value property specifies the threshold amount that determines when a change should be filtered. Its interpretation depends on the Type setting:
  - For "Absolute" type: Represents the minimum absolute numerical difference required between values
  - For "Percent" type: Represents the minimum percentage change required between values
  - For "Always" type: This value is ignored as any change will be passed through

The default value is 0.0. The value must be greater than or equal to 0

**Type**: Double

Default is 0.0

[^top](#changefilterconfiguration)



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



[^top](#changefilterconfiguration)
