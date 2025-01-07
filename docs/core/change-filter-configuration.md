[SFC Top Level]( ./sfc-top-level-config.md)>[ChangeFilterConfiguration](./sfc-top-level-config.md#ChangeFilters )

## ChangeFilterConfiguration

Change filter to apply to a channel value.

For more information see [DataFiltering](../sfc-data-processing-filtering.md#data-filtering)




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

