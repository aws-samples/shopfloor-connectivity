# File Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 



## FileConfiguration

FileConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for writing data to the local file system. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"FILE-TARGET".**

- [Schema](#fileconfiguration-schema)
- [Examples](#fileconfiguration-examples)

**Properties:**
- [BufferSize](#buffersize)
- [Compression](#compression)
- [Directory](#directory)
- [Extension](#extension)
- [Interval](#interval)
- [Json](#json)
- [UtcTime](#utctime)

---
### BufferSize
Size in KB after which the internal buffer is written to an output file

**Type**: Int

Must be in range 1-1024KB, default is 16KB

---
### Compression
Compression used to compress the data in the file

**Type**: "None" | "GZip" | "Zip"

Default is "None"

---
### Directory
Directory where the output files are created.

**Type**: String

The name of the output files in the directory will be yyyy/mm/dd/hh/mn/uuid.<extension>

---
### Extension
Extension used for the output files

**Type**: String

If no extension is specified, but the file is compressed then the corresponding extension for the compression method is used. For compression types that support entry names (e.g., zip) the extension of the entry will be set to ".json" if the Json field is true,

---
### Interval
Interval in seconds after which the internal buffer is written to an output file.

**Type**: Int

Must be in range 60-900 seconds, default is 60 seconds

---
### Json
Flag to indicate if the lines in the output file must form a valid JSON document. The target does this by wrapping the output in an '[' and ']' character and separating each line by a ',' character, making the output a JSON array.
If not set the output may be processed as JSONP or text file.

**Type**: Boolean

Default is true


---
### UtcTime
If set to true then UTC time is used to build the name of the output file, otherwise the local date and time of the system running the adapter is used.

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

