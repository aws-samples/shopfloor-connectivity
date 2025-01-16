[SFC Configuration](./sfc-top-level-config.md) > [Sources](./sfc-top-level-config.md#Sources) > [Channels](./source-configuration.md#channels)

## ChannelConfiguration

- [Schema](#Schema)

- [Examples](#Examples)

**Properties:**

- [ChangeFilter](#ChangeFilter)

- [ConditionFilter](#ConditionFilter)

- [Decompose](#Decompose)

- [Description](#Description)

- [Metadata](#Metadata)

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
If set to true and the value of the channel  is a structured value then the value is decomposed into a set of individual values for each (sub) element in  the structure. Decomposition can also be set for all channels for a source by setting its [Decompose](./source-configuration.md#Decompose) value to true. The value of the Decompose setting at channel level will override the setting at source level.

If the value is  list of structures and the value of the [Spread](#Spread) setting is true then each structure in the list is decomposed. 

**Type**: Boolean

Default is false

The names of the values for the fields in the structure start with the name of the value appended by the names of the sub elements, separated by a ".". 
After decomposing the structured value into individual values, it is removed from the dataset. If the structure was an element in a list of structures the name is the name of the element, followed by a zero indexed order number of the element in the list and the name of the sub element, all separated by a ".".

---
### Description
User-defined description of the channel

**Type**: String



---

### Metadata

The optional [Metadata](../README.md#Metadata) element can be used to add additional data to the output at the channel level. If metadata is specified, which is a map of string indexed values, it will be added to the output at the channel level as an element that can be configured through the "Metadata" entry of the ElementNames configuration element.

**Type**: Map[String, String]

---
### Name
 Name of the channel. If this element is specified, it is used as the channel key in the map of output values for its source. If no value is specified then the channel identifier is used. This name can be used to give a descriptive name in the output the data read from the channel (e.g., "InputTemperature", "RotationSpeed/RPM"

**Type**: String

---
### Spread
If set to true and the value of the channel the value is a list then for each element in the list a new individual value is created.
The value of this setting overrules the setting of the [Spread](./source-configuration.md#Spread) setting at source level.

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



## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "Name": {
      "type": "string",
      "description": "Name of the channel"
    },
    "Description": {
      "type": "string",
      "description": "Description of the channel"
    },
    "Transformation": {
      "type": "string",
      "description": "Name of transformation to apply to the channel data"
    },
    "ChangeFilter": {
      "type": "string",
      "description": "Reference to a change filter transformation"
    },
    "ConditionFilter": {
      "type": "string",
      "description": "Reference to a condition filter"
    },
    "ValueFilter": {
      "type": "string",
      "description": "Reference to a value filter"
    },
    "Decompose": {
      "type": "boolean",
      "description": "Flag indicating if the properties of a channel value should be decomposed",
      "default": false
    },
    "Spread": {
      "type": "boolean",
      "description": "Flag indicating if list values the channels should be spread into separate values",
      "default": false
    },
    "Metadata": {
      "type": "object",
      "description": "Additional metadata key-value pairs for the channel",
      "additionalProperties": {
        "type": "string"
      }
    }
  }
}
```





## Examples

<u>**Note that the ChannelConfiguration is an abstract configuration class. Source adapters extend this type with additional properties specific for their adapter implementation.**</u>



Minimal configuration:

```json

  "Channel": {}

```



Basic channel with name, overwriting the key for the channel in its source,  and description:

```json
{
  "Temperature": {
    "Name": "temperature_sensor_1",
    "Description": "Primary temperature sensor in zone A"
  }
}
```



Basic channel with name and metadata:

```json
{
  "Temperature": {
    "Name": "temperature_sensor",
    "Metadata": {
      "location": "Building A",
      "unit": "celsius",
      "manufacturer": "Siemens"
    }
  }
}
```



Channel with filter:

```json
{
  "Pressure": {
    "Name": "pressure_sensor",
    "ChangeFilter": "pressureChangeFilter",
    "Metadata": {
      "unit": "PSI",
      "range": "0-1000",
      "calibration_date": "2024-01-15"
    }
  }
}
```



Channel with decompose enabled:

```json
{
  "ComposedStatusValue": {
    "Name": "device_status",
    "Decompose": true,
    "Metadata": {
      "device_type": "PLC",
      "model": "S7-1200",
      "protocol": "ModbusTCP"
    }
  }
}
```

Channel with spread enabled:

```json
{
  "TemparatureList": {
    "Name": "temperature_array",
    "Spread": true,
    "Metadata": {
      "sensor_count": "4",
      "sampling_rate": "1Hz",
      "array_type": "linear"
    }
  }
}
```



Complete configuration:

```json
{
  "Name": "production_line_sensor",
  "Description": "Main production line monitoring sensor",
  "Transformation": "productionDataTransform",
  "ValueFilter": "validRangeFilter",
  "Decompose": true,
  "Spread": false,
  "Metadata": {
    "line_id": "PL-123",
    "location": "Factory-1",
    "department": "Assembly",
    "criticality": "high"
  }
}
```



Channel with only transformation:

```json
{
  "Transformation": "normalizeData",
  "Metadata": {
    "transform_type": "linear",
    "scale_factor": "1.5"
  }
}
```



Channel with filters and transformation:

```json
{
  "Transformation": "flowNormalization",
  "ChangeFilter": "deltaFilter",
  "ValueFilter": "rangeValidator",
  "Metadata": {
    "fluid_type": "water",
    "pipe_size": "2inch",
    "flow_unit": "m3/h"
  }
 
}
```



[^top](#ChannelConfiguration)
