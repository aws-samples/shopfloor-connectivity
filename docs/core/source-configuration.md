## SourceConfiguration

[SFC Configuration](./sfc-configuration.md) > [Sources](./sfc-configuration.md#sources) 

Defines core configuration properties for [SFC source adapters](../adapters/README.md).. Includes settings for channel management, data transformation (composition/decomposition), timestamps, filtering, and metadata. Source adapters extend this base configuration with protocol-specific properties. Essential for configuring how data is collected and processed from input sources.

SourceConfiguration defines common properties for [SFC source adapters](../adapters/README.md). Source adapter implementations extend this type with their specific additional properties.

- [Schema](#schema)
- [Examples](#examples)

**Properties:**

- [ChangeFilter](#changefilter)
- [ChannelTimestampAdjustment](#channeltimestampadjustment)
- [Channels](#channels)
- [Compose](#compose)
- [Decompose](#decompose)
- [Description](#description)
- [Metadata](#metadata)
- [Name](#name)
- [ProtocolAdapter](#protocoladapter)
- [SourceTimestampAdjustment](#sourcetimestampadjustment)
- [Spread](#spread)

---
### ChangeFilter
The ChangeFilter property specifies a reference to a [change filter](./change-filter-configuration.md) that should be applied to all channel values of the source. The value should be a string that matches the ID of a filter defined in the ChangeFilters section of the top-level SFC configuration. This filter determines when values should be processed based on how they change over time. The property is optional - if not specified, no change filtering will be applied to the source's channel values  (unless a channel-level filter is active).

**Type**: String

---
### ChannelTimestampAdjustment
Specifies a time adjustment in milliseconds that will be applied to timestamps of all values from the source. Use positive numbers to move timestamps forward in time, or negative numbers to move them backward. This allows for systematic correction of timing offsets in the data collection process.

**Type**: Long

To set the timestamp to a later value use a positive value, for an earlier value use a negative value.

---

### Channels
Defines a mapping of channel identifiers to their configurations, representing the values read from a source. While each protocol implementation defines its specific channel attributes, the SFC core uses a common set of generic attributes for processing. The protocol implementation handles the specific details of reading and interpreting data from the source according to its configuration

**Type**: Map[String,[ChannelConfiguration](./channel-configuration.md)]

---
### Compose
Defines a mapping that combines multiple channel values into structured data. Each map entry specifies a structure name and a list of channel IDs to be merged. The resulting structure uses channel IDs (or their configured names) as field names, with the source timestamp. Original channel values are removed after composition. For example, combining "Input0" and "Output0" channels under an "IO" structure transforms individual boolean values into a single structured object with both values as fields

**Type**: Map[String, List[String]]

Examples: 

```json

   "Compose" : {
        "IO" : ["Input0", "Output0"]
  }
```

This will result in the Input0 and Output0 channel values being replaced by a new value named "IO" with both of these fields as fields of that structure.
```json
{
   "Input0" : true,
   "Output0" : false
}

```


```json
{
   "IO" : {
      "Input0" : true,
       "Output0" : false
   }
}
```




---
### Decompose
The Decompose property controls whether structured values from the channels in the source should be broken down into individual elements. When set to true:

- A structured value will be split into separate values for each sub-element
- Each decomposed value is named using the pattern "originalName.subElementName"
- The original structured value is removed after decomposition
- For lists of structures (when Spread is true), each structure is decomposed with names following the pattern "elementName.index.subElementName"

This property can be set at both [channel](./channel-configuration.md#decompose) and source level, with the channel-level setting taking precedence over the source-level setting. The default value is false.

This feature is particularly useful when working with complex data structures that need to be broken down into simpler individual values for processing or analysis

If the value is  list of structures and the value of the [Spread](#spread) setting is true then each structure in the list is decomposed. 

**Type**: Boolean

Default is false

---
### Description
Provides a free-form text field where users can add descriptive information about the source to document its purpose or characteristics.

**Type**: String

---
### Metadata

The optional [Metadata](../README.md#metadata) element can be used to add additional data to the output at the source level. If metadata is specified, which is a map of string indexed values, it will be added to the output at the channel level as an element that can be configured through the "Metadata" entry of the [ElementNames](./sfc-configuration.md#elementnames) configuration element.

**Type**: Map[String, String]

---
### Name
Defines an optional descriptive identifier for the source that will be used as the key in output value maps. If not specified, the system uses the source identifier instead. This allows meaningful naming of data sources (like "AC-Unit-1" or "Plant-1/Cooling-Pump") to improve readability and identification in the output data.

**Type**: String

Optional

---
### ProtocolAdapter
Specifies which protocol adapter should be used for this source by referencing its identifier. The referenced protocol adapter must be defined in the [ProtocolAdapters](./sfc-configuration.md#protocoladapters)  section of the configuration. This setting establishes the connection between the source and the specific protocol implementation used to communicate with the data source.

**Type**: String

---
### SourceTimestampAdjustment
Time in ms to adjust the value of the source timestamp value.

**Type**: Long

To set the timestamp to a later value use a positive value, for an earlier value use a negative value.

---
### Spread
If set to true and the value of the channel the value is a list then for each element in the list a new individual value is created.
The value of this setting overrules the setting of the Spread setting at source level.
The value of this setting can be overruled for specific channels by setting the [Spread](./channel-configuration.md#spread) setting for that channel.

**Type**: Boolean

Default is false

The names of the values for the fields in the structure start with the name of the value element with a sequence number, separated by a ".". After splitting the list value into individual values, it is removed from the dataset.



[^top](#sourceconfiguration)



## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "ChangeFilter": {
      "type": "string",
      "description": "Reference to a changefilter defined in ChangeFilters section at top level config"
    },
    "ChannelTimestampAdjustment": {
      "type": "integer",
      "description": "Adjustment value for timestamps in milliseconds"
    },
    "Channels": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/ChannelConfiguration"
      },
      "minItems": 1,
      "description": "List of channel configurations"
    },
    "Compose": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "minItems": 1
        }
      },
      "description": "Map of composed values"
    },
    "Decompose": {
      "type": "boolean",
      "default": false,
      "description": "Enable/disable data decomposition"
    },
    "Description": {
      "type": "string",
      "description": "Description of the source"
    },
    "Metadata": {
      "type": "object",
      "additionalProperties": {
        "type": "string"
      },
      "description": "Key-value pairs of metadata"
    },
    "Name": {
      "type": "string",
      "description": "Optional output name override for the source"
    },
    "ProtocolAdapter": {
      "$ref": "#/definitions/ProtocolAdapterConfiguration",
      "description": "Protocol adapter configuration"
    },
    "SourceTimestampAdjustment": {
      "type": "integer",
      "description": "Adjustment value for source timestamps in milliseconds"
    },
    "Spread": {
      "type": "integer",
      "minimum": 0,
      "description": "Spread interval for data collection in milliseconds"
    }
  },
  "required": ["Channels"]
}

```



## Examples

**<u>Note: In an SFC configuration sources will also be for a source type which extends the SourceConfiguration with their specific properties.</u>**



Minimal configuration:

```json
{
  "Channels": [
    {
      "Name": "Temperature"
    },
        {
      "Name": "Pressure"
    }
   
  ]
}
```



Basic configuration with metadata:

```json
{
  "Channels": [
    {
      "Name": "Temperature",
    },
    {
      "Name": "Pressure"
    }
  ],
  "Metadata": {
    "location": "Building1",
    "equipment": "Boiler3"
  }
}
```



Configuration with composition:

```json
{
  "Channels": [
    {
      "Name": "Flow1"
    },
    {
      "Name": "Flow2"
    }
  ],
  "Compose": {
    "TotalFlow": ["Flow1", "Flow2"],
    "ProcessMetrics": ["Flow1", "Flow2"]
  },
  "Name": "FlowMeter",
  "Description": "Flow measurement station"
}
```



Configuration with timestamp adjustment of -200 milliseconds

```json
{
  "Channels": [
    {
      "Name": "Level",
      "DataType": "Double",
      "ScanRate": 1000
    }
  ],
  "ChannelTimestampAdjustment": -200,
  "SourceTimestampAdjustment": 1000,
  "Spread": 100
}
```



1. Full configuration:

```json
{
  "Name": "ProcessUnit1",
  "Description": "Main process unit monitoring",
  "Channels": [
    {
      "Name": "Temperature1",
      "DataType": "Double",
      "ScanRate": 1000
    },
    {
      "Name": "Temperature2",
      "DataType": "Double",
      "ScanRate": 1000
    },
    {
      "Name": "Pressure",
      "DataType": "Double",
      "ScanRate": 500
    }
  ],
  "Compose": {
    "AverageTemp": ["Temperature1", "Temperature2"],
    "ProcessConditions": ["Temperature1", "Temperature2", "Pressure"]
  },
  "ChangeFilter": "DeadbandFilter",
  "ChannelTimestampAdjustment": -100,
  "SourceTimestampAdjustment": 500,
  "Spread": 250,
  "Decompose": false,
  "Metadata": {
    "area": "ProcessArea1",
    "criticality": "high",
    "maintainer": "Team1"
  }
}
```



Configuration with decomposition for all channels:

```json
{
  "Channels": [
    {
      "Name": "BatchData",
      "DataType": "JSON",
      "ScanRate": 5000
    }
  ],
  "Decompose": true,
  "ChangeFilter": "JsonFilter",
  "Description": "Batch process data collection"
}
```

