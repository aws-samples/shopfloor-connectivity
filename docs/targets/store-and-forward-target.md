# Store and Forward Target



## StoreForwardTargetConfiguration

StoreForwardTargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for forwarding and buffering target data to next targets configured for this target. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"STORE-FORWARD".</strong>


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

[^top](#store-and-forward-target)

