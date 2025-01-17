## TuningConfiguration

[SFC Configuration](./sfc-configuration.md) > [Tuning](./sfc-configuration.md#tuning) 

[SFC tuning](../sfc-tuning.md) parameters. Tuning parameters can be set to adjust sizes of internal channels and timeouts. The SFC log output will output messages when internal channels start blocking when these are at their maximum capacity or when timeouts occur waiting for a channel to unblock.

- [Schema](#schema)
- [Examples](#examples)


**Properties:**

- [AggregatorChannelSize](#aggregatorchannelsize)
- [AllSourcesReadTimeout](#allsourcesreadtimeout)
- [ChannelSizePerMetricsProvider](#channelsizepermetricsprovider)
- [MaxConcurrentSourceReaders](#maxconcurrentsourcereaders)
- [MetricsChannelTimeout](#metricschanneltimeout)
- [ScheduleReaderResultsChannelSize](#schedulereaderresultschannelsize)
- [ScheduleReaderResultsChannelTimeout](#schedulereaderresultschanneltimeout)
- [ScheduleReaderResultsChannelTimeout](#schedulereaderresultschanneltimeout)
- [TargetForwardingChannelSize](#targetforwardingchannelsize)
- [TargetForwardingChannelTimeout](#targetforwardingchanneltimeout)
- [TargetResubmitChannelSize](#targetresubmitchannelsize)
- [TargetResubmitChannelTimeout](#targetresubmitchanneltimeout)
- [TargetResultsChannelSize](#targetresultschannelsize)
- [TargetResultsChannelTimeout](#targetresultschanneltimeout)
- [WriterInputChannelSize](#writerinputchannelsize)
- [WriterInputChannelSizeTimeout](#writerinputchannelsizetimeout)

---
### AggregatorChannelSize
Internal buffer size for sending data to aggregator

**Type**: Int

Default is 1000

Increment when getting timeouts on ScheduleReader:aggregationChannel, reduce to limit memory used for aggregation

---
### AllSourcesReadTimeout
Timeout in which reading from all sources must be completed. 

**Type**: Int

Default is 60000

---
### ChannelSizePerMetricsProvider
Buffer size per metrics provider used for internal metrics processor

**Type**: Int

Default is 1000

Increment when getting timeouts on MetricsProcessor:metricsChannel, reduce to limit memory use 

---
### MaxConcurrentSourceReaders
Max number of sources read concurrently by an SFC Schedule

**Type**: Int

Default is 5

---
### MetricsChannelTimeout
Timeout writing to internal metrics processor buffer in milliseconds

**Type**: Int

Default is 5000

Increment when getting timeouts on metrics channels and available memory is limited

---
### ScheduleReaderResultsChannelSize
Internal buffer size for reading from sources

**Type**: Int

Default is 5000

Increment when getting timeouts on ScheduleReader:writerInputChannel, reduce to limit memory use by reader

---
### ScheduleReaderResultsChannelTimeout
Timeout writing to internal buffer for reading from sources in milliseconds

**Type**: Int

Default is 1000

Increment when getting timeouts on ScheduleReader:resultsChannel and available memory is limited

---
### ScheduleReaderResultsChannelTimeout
Timeout writing to internal buffer used to send data to aggregation in milliseconds

**Type**: Int

Default is 1000

Increment when getting timeouts on ScheduleReader:aggregationChannel and available memory is limited

---
### TargetForwardingChannelSize
Buffer size for internal buffer to forward target data used by chained targets

**Type**: Int

Default is 1000

Increment when getting timeouts on forwarding channels, reduce to limit memory use 

---
### TargetForwardingChannelTimeout
Timeout writing to forwarding buffer in milliseconds

**Type**: Int

Default is 1000

Increment when getting timeouts on forwarding channels and available memory is limited

---
### TargetResubmitChannelSize
Buffer size for internal buffer to resubmit target data used by chained targets

**Type**: Int

Default is 1000

Increment when getting timeouts on resubmit channels, reduce to limit memory use 

---
### TargetResubmitChannelTimeout
Timeout writing to resubmit buffer in milliseconds

**Type**: Int

Default is 1000

Increment when getting timeouts on resubmit channels and available memory is limited

---
### TargetResultsChannelSize
Buffer size for internal buffer to send target results

**Type**: Int

Default is 1000

Increment when getting timeouts on resultChannels, reduce to limit memory use 

---
### TargetResultsChannelTimeout
Timeout writing to target results buffer in milliseconds

**Type**: Int

Default is 5000

Increment when getting timeouts on result channels and available memory is limited

---
### WriterInputChannelSize
Internal buffer size for sending data to writers

**Type**: Int

Default is 10000

Increment when getting timeouts on ScheduleController:writerInputChannel, reduce to limit memory use 

---
### WriterInputChannelSizeTimeout
Timeout writing to internal buffer used to send data to writers in milliseconds

**Type**: Int

Default is 1000

Increment when getting timeouts on writerInputChannel and available memory is limited

[^top](#tuningconfiguration)



## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "AggregatorChannelSize": {
      "type": "integer",
      "default": 1000,
      "description": "Size of the aggregator channel"
    },
    "AllSourcesReadTimeout": {
      "type": "integer",
      "default": 60000,
      "description": "Timeout for reading from all sources in milliseconds"
    },
    "ChannelSizePerMetricsProvider": {
      "type": "integer",
      "default": 1000,
      "description": "Channel size for each metrics provider"
    },
    "MaxConcurrentSourceReaders": {
      "type": "integer",
      "default": 5,
      "description": "Maximum number of concurrent source readers"
    },
    "MetricsChannelTimeout": {
      "type": "integer",
      "default": 5000,
      "description": "Timeout for metrics channel in milliseconds"
    },
    "ScheduleReaderResultsChannelSize": {
      "type": "integer",
      "default": 1,
      "description": "Size of the schedule reader results channel"
    },
    "ScheduleReaderResultsChannelTimeout": {
      "type": "integer",
      "default": 0,
      "description": "Timeout for schedule reader results channel in milliseconds"
    },
    "TargetForwardingChannelSize": {
      "type": "integer",
      "default": 5000,
      "description": "Size of the target forwarding channel"
    },
    "TargetForwardingChannelTimeout": {
      "type": "integer",
      "default": 1000,
      "description": "Timeout for target forwarding channel in milliseconds"
    },
    "TargetResubmitChannelSize": {
      "type": "integer",
      "default": 1000,
      "description": "Size of the target resubmit channel"
    },
    "TargetResubmitChannelTimeout": {
      "type": "integer",
      "default": 1000,
      "description": "Timeout for target resubmit channel in milliseconds"
    },
    "TargetResultsChannelSize": {
      "type": "integer",
      "default": 1000,
      "description": "Size of the target results channel"
    },
    "TargetResultsChannelTimeout": {
      "type": "integer",
      "default": 5000,
      "description": "Timeout for target results channel in milliseconds"
    },
    "WriterInputChannelSize": {
      "type": "integer",
      "default": 10000,
      "description": "Size of the writer input channel"
    },
    "WriterInputChannelSizeTimeout": {
      "type": "integer",
      "default": 1000,
      "description": "Timeout for writer input channel in milliseconds"
    }
  }
}

```



## Examples

Increase number of concurrent source readers

```json
{
  "MaxConcurrentSourceReaders": 10
}
```



Full configuration

```json
{
  "AggregatorChannelSize": 1000,
  "AllSourcesReadTimeout": 1000,
  "ChannelSizePerMetricsProvider": 100,
  "MaxConcurrentSourceReaders": 16,
  "MetricsChannelTimeout": 1000,
  "ScheduleReaderResultsChannelSize": 1000,
  "ScheduleReaderResultsChannelTimeout": 1000,
  "TargetForwardingChannelSize": 1000,
  "TargetForwardingChannelTimeout": 1000,
  "TargetResubmitChannelSize": 1000,
  "TargetResubmitChannelTimeout": 1000,
  "TargetResultsChannelSize": 1000,
  "TargetResultsChannelTimeout": 1000,
  "WriterInputChannelSize": 1000,
  "WriterInputChannelSizeTimeout": 1000
}
```



