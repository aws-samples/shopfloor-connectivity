## ChannelConfiguration

[SFC Configuration](./sfc-configuration.md) > [Sources](./sfc-configuration.md#sources) > [Channels](./source-configuration.md#channels)

- [Schema](#schema)

- [Examples](#examples)

The ChannelConfiguration serves as a base that defines common attributes for protocol adapter channels. It provides an abstraction for input addresses in a data source. Protocol-specific adapters extend this class to add their own specific properties.

This base configuration includes core properties for channel configuration including:

- Channel naming and description
- Value transformation settings
- Metadata handling
- Various filtering capabilities (change, value, and condition filters)
- Value decomposition and spreading options
- Configuration validation

The configuration  is designed to be extended by specific protocol implementations to add protocol-specific channel configuration properties while maintaining a consistent base set of functionality across different adapter types.

**Properties:**

- [ChangeFilter](#changefilter)

- [ConditionFilter](#conditionfilter)

- [Decompose](#decompose)

- [Description](#description)

- [Metadata](#metadata)

- [Name](#name)

- [Spread](#spread)

- [Transformation](#transformation)

- [ValueFilter](#valuefilter)

  

---
### ChangeFilter
The ChangeFilter property specifies a reference to a [change filter](./change-filter-configuration.md) that should be applied to the channel's values. If set, it overrides any change filter that might be configured at the source level. The value should be a string that matches the ID of a filter defined in the ChangeFilters section of the top-level SFC configuration. This filter determines when values should be processed based on how they change over time. The property is optional - if not specified, no change filtering will be applied to the channel (unless a source-level filter is active).

**Type**: String

---
### ConditionFilter
The ConditionFilter property specifies a reference to a [condition filter](./condition-filter-configuration.md) that should be applied to the channel's values. It accepts a string value that must match an ID of a filter defined in the [ConditionFilters](./sfc-configuration.md#conditionfilters)  section of the top-level SFC configuration. This filter evaluates whether values should be processed based on specified conditions. The property is optional - if not specified, no condition filtering will be applied to the channel. This allows for selective processing of values based on defined conditions.

**Type**: String

---
### Decompose

The Decompose property controls whether structured values from the channel should be broken down into individual elements. When set to true:

- A structured value will be split into separate values for each sub-element
- Each decomposed value is named using the pattern "originalName.subElementName"
- The original structured value is removed after decomposition
- For lists of structures (when Spread is true), each structure is decomposed with names following the pattern "elementName.index.subElementName"

This property can be set at both channel and [source levels](./source-configuration.md#decompose), with the channel-level setting taking precedence over the source-level setting. The default value is false.

This feature is particularly useful when working with complex data structures that need to be broken down into simpler individual values for processing or analysis

If the value is  list of structures and the value of the [Spread](#spread) setting is true then each structure in the list is decomposed. 

**Type**: Boolean

Default is false

---
### Description
The Description property allows users to provide a human-readable text description of the channel. It accepts a string value that can be used to document the purpose, function, or any other relevant information about the channel. This property helps in maintaining clear documentation and understanding of the channel's role within the configuration.

**Type**: String

---

### Metadata

The optional [Metadata](../README.md#metadata) element can be used to add additional data to the output at the channel level. If metadata is specified, which is a map of string indexed values, it will be added to the output at the channel level as an element that can be configured through the "Metadata" entry of the [ElementNames](./sfc-configuration.md#elementnames) configuration element.

**Type**: Map[String, String]

---
### Name
 Name of the channel. If this element is specified, it is used as the channel key in the map of output values for its source. If no value is specified then the channel identifier is used. This name can be used to give a descriptive name in the output the data read from the channel (e.g., "InputTemperature", "RotationSpeed/RPM"

**Type**: String

---
### Spread

The Spread property determines how list values from the channel are handled. When set to true:

- Each element in a list value will be converted into a separate individual value
- The new values are named using the pattern "originalName.index" where index is a sequence number
- The original list value is removed from the dataset after spreading
- This setting overrides any [Spread](./source-configuration.md#spread)  setting configured at the source level

The default value is false. This feature is useful when you need to process list elements as individual values rather than handling them as a single list structure.

**Type**: Boolean

Default is false

---
### Transformation

The Transformation property specifies a reference to a [transformation](./transformation-operator-configuration.md)  that should be applied to the channel's values. It accepts a string value that must match the name of a transformation defined in the  [Transformations](./sfc-configuration.md#transformations)  section of the top-level SFC configuration. This property allows values to be modified or converted before further processing. The property is optional - if not specified, no transformation will be applied to the channel's values. The transformation is applied to the raw channel value before any other processing like filtering or spreading occurs

**Type**: String

---
### ValueFilter
The [ValueFilter](./value-filter-configuration.md) property specifies a reference to a value filter that should be applied to the channel's values. It accepts a string value that must match an ID of a filter defined in the [ValueFilters](./sfc-configuration.md#valuefilters)  section of the top-level SFC configuration. This filter determines whether values should be processed based on their actual content or value. The property is optional - if not specified, no value filtering will be applied to the channel. Value filters can use operators like ==, !=, >, >=, <, <= for numeric values, and == and != for non-numeric values to determine if a value should be processed

**Type**: String



[^top](#channelconfiguration)



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



[^top](#channelconfiguration)
