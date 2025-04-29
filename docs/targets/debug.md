# Debug Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md)

The SFC Debug target adapter provides a simple way to output collected data to the system console for debugging and development purposes. It can display source values, metadata, and timestamps in a readable format, helping developers verify data collection and transformation processes. The adapter is particularly useful for building and testing transformation templates, allowing developers to validate template output before configuring production targets. It supports configurable output formatting to facilitate troubleshooting of data flows.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "DEBUG-TARGET": {
      "JarFiles" : ["<location of deployment>/degbug-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.debugtarget.DebugTargetWriter"
   }
}
```



## AwsDebugConfiguration

AwsDebugConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md). The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"DEBUG-TARGET".** This target type does not have additional elements. Output messages will be written to standard output.

FileConfiguration extends the type  TargetConfiguration with specific configuration data for writing data to the local file system. The Targets configuration element can contain entries of this type; the TargetType of these entries must be set to **"FILE_TARGET"**

- [Schema](#schema)
- [Example](#example)

**Properties:**

- [Formatter](#formatter)

- [Template](#template)

---

### Formatter

Configuration allows for custom formatting of data written by a target. A [custom formatter](../sfc-extending.md#custom-formatters), implemented as a JVM class, converts a sequence of target data messages into a specific format and returns the formatted data as an array of bytes.

When a formatter is used, a [template](#template) configured for that target is ignored.

**Type:** [InProcessConfiguration](../core/in-process-configuration.md)

---

### Template

Specifies the file path to an [Apache velocity](https://velocity.apache.org/)  template used for  [transforming the output data](../sfc-target-templates.md) target output data. This optional setting enables custom formatting of data before it is sent to the target. Available context variables include:

- $schedule
- $sources
- $metadata
- $serial
- $timestamp
- names specified in ElementNames configuration
- $tab (for inserting tab characters)

Pathname to file containing an [Apache velocity](https://velocity.apache.org/) template that can be applied to [transform the output data](../sfc-target-templates.md) of the target.

The following [Velocity tools](https://velocity.apache.org/tools/3.1/tools-summary.html) can be used in the transformation template:

- $datetool
- $collection
- $context
- $math
- $number

Additional epoch timestamp values can be added to the data used for the transformation by setting the [TemplateEpochTimestamp](../core/target-configuration.md#templateepochtimestamp) property to true,

For targets where the data does not require specific output format, the data is serialized as [JSON data](../sfc-data-format.md#sfc-output-data-schemas).

When a custom [formatter](#formatter) is configured for a target then this property is ignored.

**Type**: String



## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "DebugConfiguration",
  "type": "object",
  "allOf": [
    {
      "$ref": "#/definitions/TargetConfiguration"
    }
  ]
}
```



## Example

```json
{
  "Active" :true,
  "TargetType" : "DEBUG"
}
```

