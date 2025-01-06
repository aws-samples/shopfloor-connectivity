[SFC Top Level]( ./sfc-top-level-config.md )>[Sources](./sfc-top-level-config.md#Sources)>[Channels](./source-configuration.md#Channels)(

## ChannelConfiguration


**Properties:**
- [ChangeFilter](#ChangeFilter)
- [ChangeFilter](#ChangeFilter)
- [ConditionFilter](#ConditionFilter)
- [Decompose](#Decompose)
- [Description](#Description)
- [Name](#Name)
- [Name](#Name)
- [Spread](#Spread)
- [Transformation](#Transformation)
- [ValueFilter](#ValueFilter)

---
### ChangeFilter
[ChangeFilter](./change-filter-configuration.md)  to apply to this channel value. (Overwrites change filter at source level if any)

**Type**: String

Optional, if used it must refer to a configured filter in the [ChangeFilters](./sfc-top-level-config.md#ChangeFilters) element at the sec top level configuration.

---
### ConditionFilter
ConditionFilter to apply to this channel, see condition filters

**Type**: String

Optional, if used it must refer to a configured filter in the [ConditionFilters](./sfc-top-level-config.md#ConditionFilters) element  at the sec top level configuration.

---
### Decompose
If set to true and the value of the channel the value is a structured value then the value is decomposed into a set of individual values for each (sub) element in  the structure.
If the value is  list of structures and the value of the [Spread](#Spread) setting is true then each structure in the list is decomposed. 

**Type**: Boolean

Default is false

The names of the values for the fields in the structure start with the name of the value appended by the names of the sub elements, separated by a ".". 
After decomposing the structured value into individual values, it is removed from the dataset. If the structure was an element in a list of structures the nameis the name of the element, followed by a zero indexed order number of the element in the list and the name of the sub element, all separated by a ".".

---
### Description
User-defined description of the channel

**Type**: String

---
### Name
 Name of the channel. If this element is specified, it is used as the channel key in the map of output values for its source. If no value is specified then the channel identifier is used. This name can be used to give a descriptive name in the output the data read from the channel (e.g., "InputTemperature", "RotationSpeed/RPM"

**Type**: String

---
### Spread
If set to true and the value of the channel the value is a list then for each element in the list a new individual value is created.
The value of this setting overrules the setting of the Spread setting at source level.

**Type**: Boolean

Default is false

The names of the values for the fields in the structure start with the name of the value element with a sequence number, separated by a ".". After splitting the list value into individual values, it is removed from the dataset.

---
### Transformation
[Transformation](./transformation-operator-configuration.md) to apply to this channel value, the name must be an existing entry in the [Transformations](./sfc-top-level-config.md#Transformations) element at the top level of the SFC configuration.

**Type**: String

Optional, if used it must refer to a configured filter in the  [Transformations](./sfc-top-level-config.md#Transformations)  element in the SFC top level configuration.

---
### ValueFilter
[ValueFilter](./value-filter-configuration.md) to apply to this channel value.

**Type**: String

Optional, if used it must refer to a configured filter in the [ValueFilters](./sfc-top-level-config.md#ValueFilters) element  at the sec top level configuration.

[^top](#ChannelConfiguration)

