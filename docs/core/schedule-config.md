## ScheduleConfiguration


- [Schema](#Schema)
- [Examples](#Examples)


**Properties:**
- [Active](#Active)
- [Aggregation](#Aggregation)
- [Description](#Description)
- [Interval](#Interval)
- [Metadata](#Metadata)
- [Name](#Name)
- [Sources](#Sources)
- [Targets](#Targets)
- [TimestampLevel](#TimestampLevel)

---
### Active
State of the schedule. The schedule will collect data if the flag is set to true. Note that a configuration must at least contain one active schedule.

**Type**: Boolean

Default is true

---
### Aggregation
Optionally [aggregation](./aggregation-config.md) can be applied to the schedule output data by adding an Aggregation element. The collected values will be buffered and optionally one or more aggregation functions can be applied to these values before sending it to the targets.

**Type**: Aggregation

Default is no aggregation of data

---
### Description
Optional description of the schedule

**Type**: String

Optional

---
### Interval
Interval period in milliseconds for schedule reading values from source.

**Type**: Integer

Default is 1000

---

### Metadata

The optional [Metadata](../README.md#Metadata) element can be used to add additional data to the output at the schedule level. If metadata is specified, which is a map of string indexed values, it will be added to the output at the schedule level as an element that can be configured through the "Metadata" entry of the ElementNames configuration element.

**Type**: Map[String, String]

---

### Name

Name of the schedule, this name is passed with the collected data to the configured targets.

**Type**: String

Must be specified and unique in the configuration

---
### Sources
Input source values to read. This value is a map indexed by the source identifier. Each entry is a list of values (channels) to read from that source. A value of "*" can be used to read all values from a source. The Source Identifier must exist in the Sources section of the configuration and the channel names specified, must exist for that source.

**Type**: Map [String, String[]]
Map indexed by source identifier, entries containing a list of channel identifiers.

Must contain at least one source with one channel.

---
### Targets
List of target identifiers to send the output of the schedule to. The target identifiers must exist in the Targets section of the configuration.

**Type**: String[]

Must at least contain one active target.

---
### TimestampLevel
Included timestamps in the schedule output.

- "None": No timestamps will be included in the output data.
- "Channel": A timestamp will be included with every channel output value. The output values will be an element that contains both the value and timestamp. The names of the fields in this element can be specified in the Value and Timestamp fields of the ElementNames entry of the configuration
- "Source": A single timestamp will be included in the output at the source level. The name of the element that contains the timestamp can be specified in the Timestamp field at the ElementNames entry of the configuration.
- "Both": Timestamps will be added at both source-level and channel value levels. See Channel and Source level for more information on the name of the elements containing the timestamps and values.

**Type**: String, any of "None", "Channel", "Source", "Both"

Default is "None"

[^top](#ScheduleConfiguration)



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
