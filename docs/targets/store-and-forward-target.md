# Store and Forward Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

The SFC Store and Forward Target acts as an intermediate buffer between SFC-Core and downstream targets, providing reliable message delivery through persistent storage. When configured targets become unavailable, this target automatically stores data to disk, preventing message loss during network interruptions or target system outages. Once connectivity is restored, the stored messages are forwarded to their intended destinations in the order they were received, ensuring no data is lost in the processing pipeline.

Buffering will take place in situations where the next targets in the chain are IPC targets which cannot be reached by the store and forwarding targets due to network issues.

The store and forward target using to following logic:

- In normal situations the target will forward the target data to the next targets.
- For messages that can be delivered to their destinations these targets will send ACKs containing the serial number of
  the delivered messages.
- When the targets cannot deliver messages, NACKS, including the full message will be returned.
- When receiving NACKs the store and forward target will go into buffering mode and will start buffering data received
  by the core to disk.
- In buffering mode, the store and forward target will periodically send a buffered message, which is the oldest message
  that falls in the retention strategy (see below) of the buffer if the buffer is configured to operate in FIFO mode,
  which is the default. In LIFO mode the most recent message is used. An internal flag is set in the message to indicate
  to the target that this message should not be buffered but send directly to their destinations.
- The target will try to deliver this message to the destination and report an ACK or NACK for that message.
- When an ACK is received the store and forward target will switch back from buffering mode into normal mode after
  submitting the buffered data. This will happen in FIFO or LIFO mode based on configuration.
- Messages for which an ERROR is received are not stored and in case they are buffered removed from the store as this
  means they cannot be processed by the target.


## Retention strategies

In order to prevent running out of disk space of the device that is used to store the buffered messages a retention
strategy must be defined for a store and forward target. This can either be a period in minutes, a number of messages
per target the total size in MB per target. Data in the buffer that falls outside the used retention criteria will not
be resubmitted and automatically deleted from the storage device.

In order to reduce the storage of buffered messages the target will try to use hard links for messages that need to be
stored for multiple end targets, if the file system of that device supports it.

**PLEASE NOTE**  

Storing messages to a physical device can reduce the throughput of the SFC deployment. It is strongly recommended to run
process that contains the store and forward target, in memory or as an IPC service, on a device that has a fast storage
device.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "STORE-FORWARD": {
      "JarFiles" : ["<location of deployment>/store-forward-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.storeforward.StoreForwardTargetWriter"
   }
}
```



**Configuration:**

---

## StoreForwardTargetConfiguration

StoreForwardTargetConfiguration extends the type [TargetConfiguration](file:///Applications/Typora.app/Contents/Resources/core/target-configuration.md) with specific configuration data for forwarding and buffering target data to the next targets configured for this target. The Targets configuration element can contain entries of this type; the TargetType of these entries must be set to **"STORE-FORWARD"**.



- [Schema](#storeforwardtargetconfiguration-schema)
- [Examples](#storeforwardtargetconfiguration-examples)

**Properties:**

- [CleanupInterval](#cleanupinterval)
- [Directory](#directory)
- [Fifo](#fifo)
- [RetainFiles](#retainfiles)
- [RetainPeriod](#retainperiod)
- [RetainSize](#retainsize)
- [Targets](#targets)
- [WriteTimeout](#writetimeout)

---
### CleanupInterval
The CleanupInterval property defines how frequently (in seconds) the target executes its internal cleanup procedure while operating in buffering mode. This maintenance process helps manage stored data and system resources. If not specified, the cleanup procedure runs every 60 seconds.

**Type**: Int

---
### Directory
The Directory property specifies the filesystem path where buffered messages will be stored when targets are unavailable. The specified directory must exist prior to target initialization, and the process running the Store and Forward target must have both read and write permissions for this location.

**Type**: String

---
### Fifo
The Fifo property controls the order in which buffered messages are resubmitted to targets. When set to true (default), messages are processed in First-In-First-Out order, ensuring the oldest stored messages are sent first. When set to false, the most recent messages take priority in resubmission.

**Type**: Boolean


---
### RetainFiles
The RetainFiles property sets the maximum number of buffered files to retain per target before implementing deletion. The minimum allowed value is 100 files. This property is part of the retention strategy system - at least one retention strategy (RetainFiles, [RetainFiles](#retainfiles), or [RetainSize](#retainsize)) must be configured, but only one can be active at a time.

**Type**: Int

---
### RetainPeriod
The RetainPeriod property defines how long (in minutes) buffered messages are kept before deletion. The minimum retention period is 1 minute. This property is part of the retention strategy system - at least one retention strategy ([RetainFiles](#retainfiles), RetainPeriod, or [RetainSize](#retainsize)) must be configured, but only one can be active at a time.

**Type**: Int

---
### RetainSize
The RetainSize property specifies the maximum total size (in megabytes) of buffered files to retain per target before implementing deletion. The minimum allowed value is 1 MB. This property is part of the retention strategy system - at least one retention strategy ([RetainFiles](#retainfiles), [RetainPeriod](#retainperiod), or RetainSize) must be configured, but only one can be active at a time.

**Type**: Int

---
### Targets
The Targets property defines an array of target IDs for which message buffering and forwarding will be enabled. These specified targets must be already configured within the same configuration file, either as in-process targets or as IPC service targets, and must be listed in the [Targets](../core/sfc-configuration.md#targets) property of the SFC configuration. 

**Type**: Array of String

---
### WriteTimeout
The WriteTimeout property specifies the maximum time (in seconds) allowed for write operations to complete when storing messages to the storage device. If a write operation exceeds this timeout, it will be considered failed. If not specified, the default timeout is 10 seconds.

**Type**: Int



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

