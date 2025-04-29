# File Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

The SFC File target adapter enables writing collected data to files in the local file system.

In order to use this adapter as in [in-process](../sfc-running-targets.md#running-targets-in-process) type adapter the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "FILE-TARGET": {
      "JarFiles" : ["<location of deployment>/file-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.filetarget.FileTargetWriter"
   }
}
```



## FileConfiguration

FileConfiguration extends the type  TargetConfiguration with specific configuration data for writing data to the local file system. The Targets configuration element can contain entries of this type; the TargetType of these entries must be set to **"FILE_TARGET"**

- [Schema](#fileconfiguration-schema)
- [Examples](#fileconfiguration-examples)

**Properties:**

- [BufferCount](#buffercount)
- [BufferSize](#buffersize)
- [Compression](#compression)
- [Directory](#directory)
- [Extension](#extension)
- [Formatter](#formatter)
- [Interval](#interval)
- [Json](#json)
- [Template](#template)
- [UtcTime](#utctime)

---
### BufferCount

The maximum number of messages to accumulate in the buffer before triggering a batch publish to the MQTT topic. When this count is reached, all buffered messages are written to a file.

Batching is triggered when any configured threshold (BufferCount, [BufferSize](#buffersize), or [Interval](#interval)) is reached

---

### BufferSize

The size of the internal write buffer in kilobytes (KB) that determines when buffered data is flushed to the output file. When the buffer reaches this size, its contents are written to disk.

**Type**: Int

Must be in range 1-1024KB, default is 16KB

---
### Compression
The type of compression algorithm used to compress data written to the output file. 

**Type**: String

Possible values are:

- "None" (Default)
- "GZip"
- "Zip"

---
### Directory
The filesystem path where output files will be stored. Files are automatically organized in a hierarchical directory structure based on timestamp (year/month/day/hour/minute) with a unique UUID filename and appropriate extension.

**Type**: String

The name of the output files in the directory will be yyyy/mm/dd/hh/mn/uuid.[extension](#extension)

---
### Extension
The file extension to be used for output files. If no extension is specified, but the file is compressed, then the corresponding extension for the compression method is used. For compression types that support entry names (e.g., zip), the extension of the entry will be set to ".json" if the [Json](#json) field is true.

**Type**: String

---

### Formatter

Configuration allows for custom formatting of data written by a target. A [custom formatter](../sfc-extending.md#custom-formatters), implemented as a JVM class, converts a sequence of target data messages into a specific format and returns the formatted data as an array of bytes.

When a formatter is used, a [template](#template) configured for that target is ignored.

**Type:** [InProcessConfiguration](../core/in-process-configuration.md)

---
### Interval
The time interval in seconds that determines how often the internal buffer is flushed and written to the output file, regardless of  [buffer size](#buffersize).

**Type**: Int

Must be in range 60-900 seconds, default is 60 seconds

---
### Json
Determines whether the output file should be formatted as a valid JSON array document. When enabled, the target wraps all output lines with square brackets and separates entries with commas. When disabled, the output can be processed as JSONP or plain text with individual JSON lines.

**Type**: Boolean

Default is true

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


---
### UtcTime

Controls whether UTC or local system time is used when generating the timestamp-based directory structure and filenames. When true, UTC time is used; when false, the local time of the system running the adapter is used. 

**Type**: Boolean

Default is false

### FileConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "FileConfiguration",
  "type": "object",
  "allOf": [
    {
      "$ref": "#/definitions/TargetConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "BufferSize": {
          "type": "integer",
          "description": "Buffer size in MB",
          "minimum": 1,
          "maximum": 1024,
          "default": 16
        },
        "Compression": {
          "type": "string",
          "description": "Type of compression to use",
          "enum": ["None", "Zip", "GZip"],
          "default": "None"
        },
        "Directory": {
          "type": "string",
          "description": "Directory path where files will be written"
        },
        "Extension": {
          "type": "string",
          "description": "File extension"
        },
        "Interval": {
          "type": "integer",
          "description": "Interval in seconds between file writes",
          "minimum": 60,
          "maximum": 900,
          "default": 60
        },
        "Json": {
          "type": "boolean",
          "description": "Whether to write in JSON format"
        },
        "UtcTime": {
          "type": "boolean",
          "description": "Whether to use UTC time for timestamps"
        }
      },
      "required": ["Directory"]
    }
  ]
}

```

### FileConfiguration Examples

```json

{
  "TargetType" : "FILE-TARGET",
  "Directory": "/data/logs",
  "Extension": ".json",
  "Json": true,
  "UtcTime": true,
  "Interval": 300,
  "BufferSize": 32
}
```

 Compressed Files:

```json
{
  "TargetType" : "FILE-TARGET",
  "Directory": "/var/log/sensors",
  "Extension": ".json",
  "Compression": "GZip",
  "BufferSize": 64,
  "Interval": 600,
  "UtcTime": true
}
```

[^top](#file-target)

