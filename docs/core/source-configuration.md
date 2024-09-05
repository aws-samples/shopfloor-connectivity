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


<tr class="even">
<td>Compose</td>
<td>This setting is a map, indexed by a structure name, with a list of Channel IDs in this source. The channels listed for a structured are
composed into a new value with the name of the structure. Each channel listed in a structure becomes a field of the structured value. The field name id the ID of
the channel or value of the "Name" setting when it is specified in the configuration of the channel. The channels that become fields in the composed structured values
are removed from the dataset.

The timestamp of the source will be used as the timestamp of the new structured value.

</td>
<td>Map[String, List[String]]</td>
<td>

```json

   "Compose" : {
        "IO" : ["Input0", "Output0"]
  }
```

will result in the Input0 and Output0 channel values being replaced by a new value named "IO" with both of these fields as fields of that structure.
```json
{
   "Input0" : true,
   "Output0" : false
}

```


```json
{
   "IO" : {
      "Input0" : true,
       "Output0" : false
   }
}
```


</td>

</tr>


<tr class="odd">
<td>Decompose</td>
<td>If set to true and the value of the channel the value is a structured value then the value is decomposed into a set of individual values for each (sub) element in  the structure.
If the value is  list of structures and the value of the "Spread" setting is true then each structure in the list is decomposed. </td>
The value of this setting can be overruled for specific channels by setting the Decompose setting for that channel.
<td>Boolean</td>
<td>Default is false

The names of the values for the fields in the structure start with the name of the value appended by the names of the sub elements, separated by a ".".
After decomposing the structured value into individual values, it is removed from the dataset. If the structure was an element in a list of structures the name
is the name of the element, followed by a zero indexed order number of the element in the list and the name of the sub element, all separated by a ".".

</td>
</tr>

<tr class="even">
<td>Spread</td>
<td>If set to true and the value of the channel the value is a list then for each element in the list a new individual value is created.
The value of this setting overrules the setting of the Spread setting at source level.
The value of this setting can be overruled for specific channels by setting the Spread setting for that channel.</td>
<td>Boolean</td>
<td>Default is false

The names of the values for the fields in the structure start with the name of the value element with a sequence number, separated by a ".". After splitting the list value into individual values, it is removed from the dataset.

</td>
</tr>

</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)
