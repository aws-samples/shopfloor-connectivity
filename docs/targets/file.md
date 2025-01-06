# File Target



## FileConfiguration

FileConfiguration extends the type <a href="../core/target-configuration.md" >TargetConfiguration</a> with specific configuration data for writing data to the local file system. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"FILE-TARGET".</strong>


**Properties:**
- [BufferSize](#BufferSize)
- [Compression](#Compression)
- [Directory](#Directory)
- [Extension](#Extension)
- [Interval](#Interval)
- [Json](#Json)

- [UtcTime](#UtcTime)

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
Flag to indicate if the lines in the output file must form a valid JSON document. The target does this by wrapping the output in a '[' and ']' character and separating each line by a ',' character, making the output a JSON array.
If not set the output may be processed as JSONP or text file.

**Type**: Boolean

Default is true


---
### UtcTime
If set to true then UTC time is used to build the name of the output file, otherwise the local date and time of the system running the adapter is used.

**Type**: Boolean

Default is false

[^top](#File Target)

