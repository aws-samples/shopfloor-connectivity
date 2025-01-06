
## TuningConfiguration

[SFC tuning](../sfc-tuning.md) parameters.



**Properties:**

- [AggregatorChannelSize](#AggregatorChannelSize)
- [AllSourcesReadTimeout](#AllSourcesReadTimeout)
- [ChannelSizePerMetricsProvider](#ChannelSizePerMetricsProvider)
- [MaxConcurrentSourceReaders](#MaxConcurrentSourceReaders)
- [MetricsChannelTimeout](#MetricsChannelTimeout)
- [ScheduleReaderResultsChannelSize](#ScheduleReaderResultsChannelSize)
- [ScheduleReaderResultsChannelTimeout](#ScheduleReaderResultsChannelTimeout)
- [ScheduleReaderResultsChannelTimeout](#ScheduleReaderResultsChannelTimeout)
- [TargetForwardingChannelSize](#TargetForwardingChannelSize)
- [TargetForwardingChannelTimeout](#TargetForwardingChannelTimeout)
- [TargetResubmitChannelSize](#TargetResubmitChannelSize)
- [TargetResubmitChannelTimeout](#TargetResubmitChannelTimeout)
- [TargetResultsChannelSize](#TargetResultsChannelSize)
- [TargetResultsChannelTimeout](#TargetResultsChannelTimeout)
- [WriterInputChannelSize](#WriterInputChannelSize)
- [WriterInputChannelSizeTimeout](#WriterInputChannelSizeTimeout)

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

[^top](#TuningConfiguration)

