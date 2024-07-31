## ChangeFilterConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4">Configuration for change filter that let only pass values when they have changed, have changes with an absolute or percentage amount or at least one per period.</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>
<tr class="even">
<td>Type</td>
<td>Type of filter</td>
<td><p>String, value must be any of these:</p>
<ul>
<li><p>"Absolute" (absolute change)</p></li>
<li><p>"Percent" (relative change)</p></li>
<li><p>"Always" (any change)</p></li>
</ul></td>
<td>Default = "Always"</td>
</tr>
<tr class="odd">
<td>Value</td>
<td><p>Change amount value<br />
Absolute value if type is "Absolute"</p>
<p>Relative value in percent if type is "Percent"</p>
<p>Ignored if type is always</p></td>
<td>Double</td>
<td>Default is 0.0</td>
</tr>
<tr class="even">
<td>AtLeast</td>
<td>Time interval in milliseconds in which at least a value is passed even the value has not changed or not beyond the specified value</td>
<td>Long</td>
<td></td>
</tr>
</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)

[^top](../../README.md#toc)