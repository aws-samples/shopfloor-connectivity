
## ValueFilterConfiguration

**Properties:**

- [Operator](#Operator)
- [Value](#Value)

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



[^top](#ValueFilterConfiguration)

