## ChannelConfiguration
<br>
The ChannelConfiguration element contains a set of generic source configuration attributes used to process the read data. Each input protocol implementation should implement its specific channel configuration type, and include that in its source type, which contains the required configuration data for that protocol.
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
<td>Name</td>
<td> Name of the channel. If this element is specified, it is used as the channel key in the map of output values for its source. If no value is specified then the channel identifier is used. This name can be used to give a descriptive name in the output the data read from the channel (e.g., "InputTemperature", "RotationSpeed/RPM"</td>
<td>String</td>
<td>Optional</td>
</tr>

<tr class="odd">
<td>Description</td>
<td>User-defined description of the channel</td>
<td>String</td>
<td></td>
</tr>

<tr class="even">
<td>Transformation</td>
<td>Transformation to apply to this channel value, the name must be an existing entry in the "Transformation" element at the <a href="sfc-top-level-config.md">top level</a> of the SFC configuration.</td>
<td>String</td>
<td>Optional, if used it must refer to a configured filter in the Transformations element.</td>
</tr>

<tr class="odd">
<td>ChangeFilter</td>
<td>ChangeFilter to apply to this channel value. (Overwrites change filter at source level if any)</td>
<td>String</td>
<td>Optional, if used it must refer to a configured filter in the ChangeFilters element.</td>
</tr>

<tr class="even">
<td>ValueFilter</td>
<td>ValueFilter to apply to this channel value, see <a href="./value-filter-configuration.md">value filters</a></td>
<td>String</td>
<td></td>
</tr>

<tr class="odd">
<td>ChangeFilter</td>
<td>Change filter to apply to the value of the channel, see <a href="./change-filter-configuration.md">change filters</a></td>
<td>String</td>
<td>Optional, if used it must refer to a configured filter in the ChangeFilters element</td>
</tr>

<tr class="even">
<td>ConditionFilter</td>
<td>ConditionFilter to apply to this channel, see <a href="./condition-filter-configuration.md">condition filters</a></td>
<td>String</td>
<td>Optional, if used it must refer to a configured filter in the ConditionFilters element </td>
</tr>

<tr class="odd">
<td>Decompose</td>
<td>If set to true and the value of the channel the value is a structured value then the value is decomposed into a set of individual values for each (sub) element in  the structure.</td>
<td>Boolean</td>
<td>Default is false

The names of the values for the fields in the structure start with the name of the value appended by the names of the sub elements, separated by a ".". After decomposing the structured value into individual values, it is removed from the dataset.

</td>
</tr>

<tr class="even">
<td>Spread</td>
<td>If set to true and the value of the channel the value is a list then for each element in the list a new individual value is created .</td>
<td>Boolean</td>
<td>Default is false

The names of the values for the fields in the structure start with the name of the value element with a sequence number, separated by a ".". After splitting the list value into individual values, it is removed from the dataset.

</td>
</tr>


</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)
