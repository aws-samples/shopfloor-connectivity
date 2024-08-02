## MetricsConfiguration

Configuration for metrics configuration. In order to collect and write metrics this section must include a metrics writers
<br>
<br>
<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Enabled</td>
<td>Collection enabled or disabled</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>

<tr class="odd">
<td>CommonDimensions</td>
<td>Set of extra dimensions added to every datapoint</td>
<td>Map(String,String)</td>
<td>Optional</td>
</tr>

<tr class="even">
<td>CollectCoreMetrics</td>
<td>Collection of core detailed metrics enabled or disabled</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>

<tr class="odd">
<td>Interval</td>
<td>Interval in seconds for reading metrics from adapters, targets and core</td>
<td>Integer</td>
<td>Default is 10</td>
</tr>

<tr class="even">
<td>Writer</td>
<td>Writer for writing collect metrics data</td>
<td><a href="in-process-configuration.md">InprocessConfiguration</a></td>
<td></td>
</tr>

<tr class="odd">
<td>Namespace</td>
<td>Namespace for collected metrics</td>
<td>String</td>
<td>Default is "SFC"</td>
</tr>
</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)

[^top](../../README.md#toc)

