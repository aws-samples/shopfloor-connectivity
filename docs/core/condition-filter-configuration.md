## ConditionFilterConfiguration

[SFC Configuration](./sfc-configuration.md) > [ConditionFilters](./sfc-configuration.md#conditionfilters)

The ConditionFilterConfiguration defines rules that evaluate whether specific channels have been read from a source, regardless of their actual values. This configuration checks for the presence or absence of channels in the data stream, rather than examining the data values within those channels. For example, it can verify if certain channels were successfully read, if they're missing, or create logical combinations of channel presence/absence conditions.

- [Schema](#schema)
- [Examples](#examples)

**Properties:**

- [Operator](#operator)
- [Value](#value)

  

---
### Operator
Filter operator to apply

**Type**: String, value must be any of these operators:

- "all" or "##"
- "any" or "**"
- "none" or "!!"
- "present" or "#"
- "absent" or "&!"
- "only" or "^"
- "notonly" or "$"
- "and" or "&&"
- "or" or "||"


A valid  operator must be specified.

---
### Value
Filter value.

If the operator is "and" ("&&") or "or" ("||")it is a nested list of Condition that all (and) or any (or) must match for the value to pass. Each filter that is part of an "and" or "or" list can have additional nested "and" ("&&") or "or" ("||") operators.

**Type**: String, String[], Boolean or list of Conditions.

**Note: The operands are the names of a channel, not the actual values for that channel that have been read from their source.**

Operand used by the filter operator, or a list of nested ConditionConfigurations if the operator is "and" ("&&") or "or" ("||"). If the operand for an operand is a channel name or a list of channel names, the name is the key of the channel in the channels table for a source. Valid JMESPath expressions van be used as well to specify channel names to match against.

[^top](#conditionfilterconfiguration)



## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "Operator": {
      "type": "string",
      "description": "The operator to use for the condition",
      "enum": [
        "all","##",
        "any","**",
        "none","!!",
        "present","#",
        "absent","&!",
        "only","^",
        "notonly","$",
        "and","&&",
        "or","||"
      ]
    },
    "Value": {
      "description": "Either a channel name or a list of nested conditions",
      "oneOf": [
        {
          "type": "string",
          "description": "Name of the channel"
        },
        {
          "type": "array",
          "items": {
            "$ref": "#/definitions/ConditionFilterConfiguration"
          },
          "description": "List of nested condition filters"
        }
      ]
    }
  },
  "required": [
    "Operator",
    "Value"
  ],
  "additionalProperties": false
}
```



## Examples



Simple channel condition, include if  "temperature_sensor" channel is present

```json
{
  "Operator": "present",
  "Value": "temperature_sensor"
}
```



Nested AND condition, include if both "temperature" and "humidity" channels are present

```json
{
  "Operator": "all",
  "Value": [
    {
      "Operator": "present",
      "Value": ["temperature", "humidity"]
    }
}
```



Nested AND condition, include if  channel "temperature" is present and "humidity" is absent

```json
{
  "Operator": "and",
  "Value": [
    {
      "Operator": "present",
      "Value": "temperature"
    },
    {
      "Operator": "absent",
      "Value": "humidity"
    }
  ]
}
```



Complex OR condition, include if any of "pressure", "temperature" channel  are present  and channel "error_state" is absent

```json
{
  "Operator": "and",
  "Value": [
    {
      "Operator": "any",
      "Value": ["pressure", "temperature"]
    },
    {
      "Operator": "present",
      "Value": "error_state"
    }
  ]
}
```



1. Deeply nested conditions:

```json
{
  "Operator": "and",
  "Value": [
    {
      "Operator": "any",
      "Value": [
        {
          "Operator": "present",
          "Value": "sensor1"
        },
        {
          "Operator": "absent",
          "Value": "sensor2"
        }
      ]
    },
    {
      "Operator": "none",
      "Value": "error_flag"
    }
  ]
}
```



Combined conditions

```json
{
  "Operator": "all",
  "Value": [
    {
      "Operator": "present",
      "Value": "temperature"
    },
    {
      "Operator": "and",
      "Value": [
        {
          "Operator": "present",
          "Value": "humidity"
        },
        {
          "Operator": "absent",
          "Value": "fault"
        }
      ]
    }
  ]
}
```



[^top](#conditionfilterconfiguration)

