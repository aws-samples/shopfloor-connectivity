## SourceConfiguration
<br>
The SourceConfiguration element contains a set of generic source configuration attributes used to process the read data. Each input protocol implementation must extend this type with its source configuration type that contains additional attributes required for that protocol.
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
<td>Name of the source. If this element is specified, it is used as the source key in the map of output values. If no value is specified then the source identifier is used. This name can be used to give a descriptive name in the output for the source the data is read from. (e.g., "AC-Unit-1", "Plant-1/Cooling-Pump")</td>
<td>String</td>
<td>Optional</td>

</tr>
<tr class="odd">
<td>Description</td>
<td>User-defined description of the source</td>
<td>String</td>
<td></td>

</tr>
<tr class="even">
<td>ProtocolAdapter</td>
<td>Reference to the used protocol adapter.</td>
<td>String</td>
<td>Must refer to an existing protocol adapter in the protocol adapter section.</td>

</tr>
<tr class="odd">
<td>Channels</td>
<td><p>Channels are an abstraction of the values read from the source. For processing the value from these channels, the SFC core only uses a small set of generic attributes which are common for all protocols.</p>
<p>This element is a map indexed by the channel identifiers. The entries contain the actual protocol specific source configuration data</p>
<p>Implementations of input protocols will define their channel configuration types containing protocol-specific attributes to read values from their source. The protocol implementation is responsible for reading and handling the protocol-specific attributes.</p></td>
<td>Map[String,<a href="./channel-configuration.md">ChannelConfiguration</a>]</td>
<td></td>

</tr>
<tr class="even">
<td>ChangeFilter</td>
<td>Change filter to apply on every channel value in this source.</td>
<td>String</td>
<td>Optional, if used it must refer to a configured filter in the ChangeFilters element</td>

</tr>
<tr class="odd">
<td>Metadata</td>
<td>The optional Metadata element can be used to add additional data to the output at the source level. If metadata is specified, which is a map of string indexed values, it will be added to the output at the source level as an element that can be configured through the "Metadata" entry of the ElementNames configuration element.</td>
<td>Map(String, Any)</td>
<td></td>

</tr>

<tr class="even">
<td>SourceTimestampAdjustment</td>
<td>Time in ms to adjust the value of the source timestamp value.</td>
<td>Long</td>
<td>To set the timestamp to a later value use a positive value, for an earlier value use a negative value.</td>

</tr>
<tr class="odd">
<td>ChannelTimestampAdjustment</td>
<td>Time in ms to adjust the value of the timestamp for all values in the source.</td>
<td>Long</td>
<td>To set the timestamp to a later value use a positive value, for an earlier value use a negative value.</td>

</tr>

</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)
