## Schedule

[SfcTopLevelConfiguration](sfc-top-level-config.md)

<br>
A schedule defines a unit of work for the SFC core. It defines the values to read from sources, the read interval, data processing, and output targets to send the data to.
<br>
<br>
<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 25%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>

</tr>
<tr class="even">
<td>Name</td>
<td>Name of the schedule, this name is passed with the collected data to the configured targets.</td>
<td>String</td>
<td>Must be specified and unique in the configuration</td>

</tr>
<tr class="odd">
<td>Description</td>
<td>Optional description of the schedule</td>
<td>String</td>
<td>Optional</td>

</tr>
<tr class="even">
<td>Active</td>
<td>State of the schedule. The schedule will collect data if the flag is set to true. Note that a configuration must at least contain one active schedule.</td>
<td>Boolean</td>
<td>Default is true</td>

</tr>
<tr class="odd">
<td>Interval</td>
<td>Interval period in milliseconds for schedule reading values from source.</td>
<td>Integer</td>
<td>Default is 1000</td>

</tr>
<tr class="even">
<td>TimestampLevel</td>
<td><p>Included timestamps in the schedule output.</p>
<p>"<strong>None</strong>": No timestamps will be included in the output data.</p>
<p>"<strong>Channel</strong>": A timestamp will be included with every channel output value. The output values will be an element that contains both the value and timestamp. The names of the fields in this element can be specified in the Value and Timestamp fields of the ElementNames entry of the configuration</p>
<p>"<strong>Source</strong>": A single timestamp will be included in the output at the source level. The name of the element that contains the timestamp can be specified in the Timestamp field at the ElementNames entry of the configuration.</p>
<p>"<strong>Both</strong>": Timestamps will be added at both source-level and channel value levels. See Channel and Source level for more information on the name of the elements containing the timestamps and values.</p></td>
<td>String, any of "None", "Channel", "Source", "Both"</td>
<td>Default is "None"</td>

</tr>
<tr class="odd">
<td>Sources</td>
<td>Input source values to read. This value is a map indexed by the source identifier. Each entry is a list of values (channels) to read from that source. A value of "*" can be used to read all values from a source. The Source Identifier must exist in the Sources section of the configuration and the channel names specified, must exist for that source.</td>
<td><p>Map [String, String[]]</p>
<p>Map indexed by source identifier, entries containing a list of channel identifiers.</p></td>
<td>Must contain at least one source with one channel.</td>

</tr>
<tr class="even">
<td>Targets</td>
<td>List of target identifiers to send the output of the schedule to. The target identifiers must exist in the Targets section of the configuration.</td>
<td>String[]</td>
<td>Must at least contain one active target.</td>

</tr>
<tr class="odd">
<td>Aggregation</td>
<td>Optionally aggregation can be applied to the schedule output data by adding an Aggregation element. The collected values will be buffered and optionally one or more aggregation functions can be applied to these values before sending it to the targets.</td>
<td><a href="./aggregation-config.md">Aggregation</a></td>
<td>Default is no aggregation of data</td>

</tr>


</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)