## ConditionFilterConfiguration


**Properties:**
- [Operator](#Operator)
- [Value](#Value)



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

[^top](#ConditionFilterConfiguration)

