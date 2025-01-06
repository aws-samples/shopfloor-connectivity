[SFC Top Level]( ./sfc-top-level-config.md )>[Schedules](#./sfc-top-level-config.md#Schedules)>Schedule

## Schedule


**Properties:**
- [Active](#Active)
- [Aggregation](#Aggregation)
- [Description](#Description)
- [Interval](#Interval)
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
Optionally [aggregation](./aggregation-config-new) can be applied to the schedule output data by adding an Aggregation element. The collected values will be buffered and optionally one or more aggregation functions can be applied to these values before sending it to the targets.

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

[^top](#Schedule)

