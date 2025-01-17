# Store and Forward Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration#Targets) >  [Target](../core/target-configuration.md) 



## StoreForwardTargetConfiguration

StoreForwardTargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for forwarding and buffering target data to next targets configured for this target. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"STORE-FORWARD".**

- [Schema](#StoreForwardTargetConfiguration-Schema)
- [Examples](#StoreForwardTargetConfiguration-Examples)

**Properties:**

- [CleanupInterval](#CleanupInterval)
- [Directory](#Directory)
- [Fifo](#Fifo)
- [RetainFiles](#RetainFiles)
- [RetainPeriod](#RetainPeriod)
- [RetainSize](#RetainSize)
- [Targets](#Targets)
- [WriteTimeout](#WriteTimeout)

---
### CleanupInterval
Interval in seconds in which the internal cleanup procedure is executed when the target is in buffering mode.

**Type**: Int

Default is 60

---
### Directory
Pathname of the directory in which to store buffered messages.

**Type**: String

This directory must already exist and the process running the Store and Forward target must have read and write access

---
### Fifo
If true the buffer operates in FIFO mode, meaning oldest messages that fall into the retention strategy are resubmitted first. If false then the most recent messages are sent first.

**Type**: Boolean

Default is true


---
### RetainFiles
Number of files per target to buffer for a target before they are deleted from the buffer.

**Type**: Int

Minimum is 100
At least one, but not more than one retention strategy must be used.

---
### RetainPeriod
Period in minutes in which buffered messages are kept in the buffer before deleted.

**Type**: Int

Minimum is 1 (minute)
At least one, but not more than one retention strategy must be used.

---
### RetainSize
Size of files in MB per target to buffer for a target before they are deleted from the buffer.

**Type**: Int

Minimum is 1 (MB)
At least one, but not more than one retention strategy must be used.

---
### Targets
Targets for which to store and forward target data messages.

**Type**: String[]

The targets must be targets that are configured either as in-process or IPC service targets in the same configuration.

---
### WriteTimeout
Timeout for write actions to the storage device in seconds

**Type**: Int

Default is 10

### StoreForwardTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "StoreForwardTargetConfiguration",
  "type": "object",
  "allOf": [
    {
      "$ref": "#/definitions/TargetConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "CleanupInterval": {
          "type": "integer",
          "description": "Interval in seconds between cleanup operations"
        },
        "Directory": {
          "type": "string",
          "description": "Directory path for storing files"
        },
        "Fifo": {
          "type": "boolean",
          "description": "Use FIFO (First In First Out) processing order",
          "default": true
        },
        "RetainFiles": {
          "type": "integer",
          "description": "Maximum number of files to retain"
        },
        "RetainPeriod": {
          "type": "integer",
          "description": "Period in minutes to retain files"
        },
        "RetainSize": {
          "type": "integer",
          "description": "Maximum total size in MBto retain",
          "minimum": 0
        },
        "Targets": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "description": "List of target IDs to forward data to",
          "minItems": 1
        },
        "WriteTimeout": {
          "type": "integer",
          "description": "Timeout in seconds for write operations",
          "minimum": 0
        }
      },
      "required": ["Directory", "Targets"]
    }
  ]
}

```

### StoreForwardTargetConfiguration Examples

```json

{
  "TargetType" : "STORE-FORWARD",
  "Directory": "./store",
  "Targets": ["s3-target", "iot-core-target"],
  "Fifo": true,
  "RetainSize": 10240,
  "RetainFiles": 1000,
  "CleanupInterval": 60
}

```

[^top](#store-and-forward-target)

