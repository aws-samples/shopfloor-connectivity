## ValueFilterConfiguration

[SFC Configuration](./sfc-configuration.md) > [ValueFilters](./sfc-configuration.md#valuefilters)

- [Schema](#schema)
- [Example](#examples)

**Properties:**

- [Operator](#operator)
- [Value](#value)

---
### Operator
Filter operator to apply

**Type**: String, value must be any of these operators:

- "eq" or "=="
- "ne" or "!="
- "gt" or ">"
- "ge" or ">="
- "lt" or "<"
- "le" or "<="
- "and" or "&&"
- "or" or "||"




---
### Value
Filter value.
If the operator is "and" ("&&") or "or" ("||")it is a nested list of ValueFilterConfigurations that all (and) or any (or) must match for the value to pass. Each filter that is part of an "and" or "or" list can have additional nested "and" ("&&") or "or" ("||") operators.

**Type**: Value to test against using the operator, or a list of nested ValueFilterConfigurations if the operator is "and" ("&&") or "or" ("||").



[^top](#valuefilterconfiguration)

## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "title": "Value Filter Configuration",
  "description": "Configuration for value filtering",
  "properties": {
    "Operator": {
      "type": "string",
      "enum": [
        "eq", "==",
        "ne", "!=",
        "gt", ">",
        "ge", ">=",
        "lt", "<",
        "le", "<=",
        "and", "&&",
        "or", "||"
      ],
      "description": "The operator to apply in the filter"
    },
    "Value": {
      "oneOf": [
        {
          "type": ["string", "number", "boolean", "null", "object"]
        },
        {
          "type": "array",
          "items": {
            "$ref": "#"
          }
        }
      ],
      "description": "The value to compare against or a list of ValueFilterConfiguration instances"
    }
  },
  "required": ["Operator", "Value"]
}

```

## Examples

Simple comparison with number:

```json
{
  "Operator": "gt",
  "Value": 100
}
```

String equality:

```json
{
  "Operator": "eq",
  "Value": "ACTIVE"
}
```



Compound AND condition:

```json
{
  "Operator": "and",
  "Value": [
    {
      "Operator": "gt",
      "Value": 20
    },
    {
      "Operator": "lt",
      "Value": 80
    }
  ]
}
```

Compound OR condition:

```json
{
  "Operator": "||",
  "Value": [
    {
      "Operator": "==",
      "Value": "ERROR"
    },
    {
      "Operator": "==",
      "Value": "CRITICAL"
    }
  ]
}
```

Nested conditions:

```json
{
  "Operator": "and",
  "Value": [
    {
      "Operator": ">=",
      "Value": 0
    },
    {
      "Operator": "or",
      "Value": [
        {
          "Operator": "<",
          "Value": 10
        },
        {
          "Operator": ">",
          "Value": 90
        }
      ]
    }
  ]
}
```

Example of a more complex filter that passes a value if the is equal to 0, or in the range 5 to 10 except when the value is 8:

```json
{
    "Operator": "or",
    "Value": [
       {
         "Operator": "eq",
         "Value": 0
       },
       {
          "Operator": "and",
          "Value": [
             {
               "Operator": "ge",
               "Value": 5
             },
             {
               "Operator": "ne",
               "Value": 8
            },
            {
             "Operator": "le",
             "Value": 10
            }
          ]
       }
    ]
}

```

