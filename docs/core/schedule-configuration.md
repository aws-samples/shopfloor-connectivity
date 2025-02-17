## ScheduleConfiguration

[SFC Configuration](./sfc-configuration.md) > [Schedules](./sfc-configuration.md#schedules) 

Defines how data is collected from sources and sent to targets on a specified interval. It includes configuration for data aggregation, timestamps, metadata, and mapping between sources and targets. Each schedule must have at least one active source and target.


- [Schema](#schema)
- [Examples](#examples)


**Properties:**
- [Active](#active)
- [Aggregation](#aggregation)
- [Description](#description)
- [Interval](#interval)
- [Metadata](#metadata)
- [Name](#name)
- [Sources](#sources)
- [Targets](#targets)
- [TimestampLevel](#timestamplevel)

---
### Active
Controls whether the schedule is operational. When true, the schedule actively collects data from its configured sources. At least one schedule in the configuration must be active. Defaults to true if not specified

**Type**: Boolean

Default is true

---
### Aggregation
Optional configuration that enables data [aggregation](./aggregation-configuration.md) for the schedule's output. When configured, collected values are buffered and can have one or more aggregation functions applied before being sent to targets. If not specified, raw data is sent without aggregation.

**Type**: Aggregation

---
### Description
An optional free-form text field that allows users to provide a human-readable description of the schedule's purpose or functionality.

**Type**: String

---
### Interval
Specifies how frequently the schedule reads values from its sources, expressed in milliseconds. The default polling interval is 1000 milliseconds (1 second) if not explicitly set.

**Type**: Integer

Default is 1000

---

### Metadata

Optional key-value pairs that add contextual information to the schedule's output data. When specified, these metadata values are included at the schedule level and can be customized using the [ElementNames](./sfc-configuration.md#elementnames) configuration's Metadata entry

**Type**: Map[String, String]

---

### Name

A required unique identifier for the schedule that is included with all collected data sent to targets. The name must be unique across all schedules in the configuration.

**Type**: String

Must be specified and unique in the configuration

---
### Sources
Defines which data points to collect from each source using a map of source IDs to channel lists. Each source must be defined in the [Sources](./sfc-configuration.md#sources) configuration section. Use "*" to read all channels from a source. At least one source and channel must be specified

**Type**: Map [String, String[]]

---
### Targets
List of target identifiers to send the output of the schedule to. The target identifiers must exist in the Targets section of the configuration.

**Type**: String[]

Must at least contain one active target.

---
### TimestampLevel

Controls timestamp inclusion in the output data. Timestamp field names are configurable via [ElementNames](./sfc-configuration.md#elementnames).

- "None": No timestamps will be included in the output data.
- "Channel": A timestamp will be included with every channel output value. The output values will be an element that contains both the value and timestamp. The names of the fields in this element can be specified in the Value and Timestamp fields of the ElementNames entry of the configuration
- "Source": A single timestamp will be included in the output at the source level. The name of the element that contains the timestamp can be specified in the Timestamp field at the ElementNames entry of the configuration.
- "Both": Timestamps will be added at both source-level and channel value levels. See Channel and Source level for more information on the name of the elements containing the timestamps and values.

**Type**: String

Default is "None"

[^top](#scheduleconfiguration)



## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "Active": {
      "type": "boolean",
      "default": true,
      "description": "Indicates if the schema is active"
    },
    "Aggregation": {
      "$ref": "#/definitions/AggregationConfiguration",
      "description": "Aggregation settings for the schema"
    },
    "Description": {
      "type": "string",
      "description": "Description of the schema configuration"
    },
    "Interval": {
      "type": "integer",
      "description": "Data collection interval in milliseconds"
    },
    "Name": {
      "type": "string",
      "minLength": 1,
      "description": "Name of the schedule"
    },
    "Sources": {
      "type": "object",
      "patternProperties": {
        "^[A-Za-z0-9_-]+$": {
          "type": "object",
        }
      },
      "minProperties": 1,
      "description": "Map of data sources configurations"
    },
    "Targets": {
      "type": "array",
      "items": {
        "type": "string",
        "minLength": 1
      },
      "minItems" : 1,
      "description": "List of target references for the collected data"
    },
    "TimestampLevel": {
      "type": "string",
      "enum": ["None", "Channel", "Source", "Both"],
      "default": "None",
      "description": "Level at which timestamps are applied to the data"
    },
    "Metadata": {
      "type": "object",
      "description": "Metadata key-value pairs for the schema",
      "additionalProperties": {
        "type": "string"
      }
    }
  },
  "required": ["Name", "Sources", "Targets"]
}
```



## Examples


Basic configuration, collecting all channels for source

```json
{
  "Name": "TankData",
  "Sources": {
    "Tank1": {
      "Channels": ["*"]
    }
  },
  "Targets": ["S3TargetBucket"],
  "Interval": 5000,
  "TimestampLevel": "Source",
}
```



Basic configuration, collecting selected channels for source and adding metadata at schedule level

```json
{
  "Name": "TankData",
  "Sources": {
    "Tank1": {
      "Channels": ["Temperature", "Pressure", "Level"]
    }
  },
  "Targets": ["S3TargetBucket"],
  "Interval": 5000,
  "TimestampLevel": "Source",
    "Metadata": {
    "location": "Factory-1",
    "line": "Production-A",
    "criticality": "high",
    "owner": "manufacturing-team"
  }
}
```



Configuration with aggregation to collect average, minimum and maximum values over 10-second period for all collected values.

```json
{
  "Name": "ProductionLine1",
  "Description": "Production line monitoring schedule",
  "Active": true,
  "Sources": {
    "Assembly-A": {
      "Channels": ["Speed", "Temperature"],
    },
    "Assembly-B": {
      "Channels": ["Pressure", "Flow"],
    }
  },
  "Targets": ["IoTSiteWise", "Timestream"],
  "Interval": 1000,
  "TimestampLevel": "Both",
  "Aggregation": {
    "Size": 10,
    "Output": {
      "*": {
        "*": ["avg", "min", "max"]
      }
    }
  },
  "Metadata": {
    "location": "Factory-1",
    "line": "Production-A",
    "criticality": "high",
    "owner": "manufacturing-team"
  }
}
```
