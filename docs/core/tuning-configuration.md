## TuningConfiguration
<br>
Tuning parameters for SFC components
<br>
<br>
<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 25%" />
<col style="width: 24%" />
<col style="width: 30%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>

</tr>
<tr class="even">
<td>MaxConcurrentSourceReaders</td>
<td>Max number of sources read concurrently by an SFC Schedule</td>
<td>Int</td>
<td>Default is 5</td>
</tr>


<tr class="even">
<td>AllSourcesReadTimeout</td>
<td>Timeout in which reading from all sources must be completed. </td>
<td>Int</td>
<td>Default is 60000</td>
</tr>

<tr class="odd">
<td>ScheduleReaderResultsChannelSize</td>
<td>Internal buffer size for reading from sources</td>
<td>Int</td>
<td>Default is 5000

Increment when getting timeouts on ScheduleReader:writerInputChannel, reduce to limit memory use by reader</td>
</tr><tr class="even">
<td>ScheduleReaderResultsChannelTimeout</td>
<td>Timeout writing to internal buffer for reading from sources in milliseconds</td>
<td>Int</td>
<td>Default is 1000

Increment when getting timeouts on ScheduleReader:resultsChannel and available memory is limited</td>
</tr>
<tr class="odd">
<td>AggregatorChannelSize</td>
<td>Internal buffer size for sending data to aggregator</td>
<td>Int</td>
<td>Default is 1000

Increment when getting timeouts on ScheduleReader:aggregationChannel, reduce to limit memory used for aggregation</td>
</tr>
<tr class="even">
<td>ScheduleReaderResultsChannelTimeout</td>
<td>Timeout writing to internal buffer used to send data to aggregation in milliseconds</td>
<td>Int</td>
<td>Default is 1000

Increment when getting timeouts on ScheduleReader:aggregationChannel and available memory is limited</td>
</tr>

<tr class="odd">
<td>WriterInputChannelSize</td>
<td>Internal buffer size for sending data to writers</td>
<td>Int</td>
<td>Default is 10000

Increment when getting timeouts on ScheduleController:writerInputChannel, reduce to limit memory use </td>
</tr>

<tr class="even">
<td>WriterInputChannelSizeTimeout</td>
<td>Timeout writing to internal buffer used to send data to writers in milliseconds</td>
<td>Int</td>
<td>Default is 1000

Increment when getting timeouts on writerInputChannel and available memory is limited</td>
</tr>

<tr class="odd">
<td>ChannelSizePerMetricsProvider</td>
<td>Buffer size per metrics provider used for internal metrics processor</td>
<td>Int</td>
<td>Default is 1000

Increment when getting timeouts on MetricsProcessor:metricsChannel, reduce to limit memory use </td>
</tr>
<tr class="even">
<td>MetricsChannelTimeout</td>
<td>Timeout writing to internal metrics processor buffer in milliseconds</td>
<td>Int</td>
<td>Default is 5000

Increment when getting timeouts on metrics channels and available memory is limited</td>
</tr>


<tr class="odd">
<td>TargetResultsChannelSize</td>
<td>Buffer size for internal buffer to send target results</td>
<td>Int</td>
<td>Default is 1000

Increment when getting timeouts on resultChannels, reduce to limit memory use </td>
</tr>
<tr class="even">
<td>TargetResultsChannelTimeout</td>
<td>Timeout writing to target results buffer in milliseconds</td>
<td>Int</td>
<td>Default is 5000

Increment when getting timeouts on result channels and available memory is limited</td>
</tr>


<tr class="odd">
<td>TargetForwardingChannelSize</td>
<td>Buffer size for internal buffer to forward target data used by chained targets</td>
<td>Int</td>
<td>Default is 1000

Increment when getting timeouts on forwarding channels, reduce to limit memory use </td>
</tr>
<tr class="even">
<td>TargetForwardingChannelTimeout</td>
<td>Timeout writing to forwarding buffer in milliseconds</td>
<td>Int</td>
<td>Default is 1000

Increment when getting timeouts on forwarding channels and available memory is limited</td>
</tr>

<tr class="odd">
<td>TargetResubmitChannelSize</td>
<td>Buffer size for internal buffer to resubmit target data used by chained targets</td>
<td>Int</td>
<td>Default is 1000

Increment when getting timeouts on resubmit channels, reduce to limit memory use </td>
</tr>
<tr class="even">
<td>TargetResubmitChannelTimeout</td>
<td>Timeout writing to resubmit buffer in milliseconds</td>
<td>Int</td>
<td>Default is 1000

Increment when getting timeouts on resubmit channels and available memory is limited</td>
</tr>
</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)
