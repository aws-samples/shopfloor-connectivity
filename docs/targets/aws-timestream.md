
# AWS Timestream Target


- [AwsTimestreamTargetConfiguration](#awstimestreamtargetconfiguration)
- [AwsTimestreamRecordConfiguration](#awstimestreamrecordconfiguration)
- [AwsTimestreamDimensionConfiguration](#awstimestreamdimensionconfiguration)


[Targets](./targets.md)

## AwsTimestreamTargetConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 29%" />
<col style="width: 23%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p>AwsSTimestreamTargetConfiguration extends the type TargetConfiguration with specific configuration data for sending data to a Timestream table. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-TIMESTREAM"</strong></p>
<p>Requires IAM timestream:WriteRecords permission for the configures table as well timestream:DescribeEndpoints</p></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>
<tr class="even">
<td>Database</td>
<td>Timestream database</td>
<td>String</td>
<td></td>
</tr>
<tr class="odd">
<td>TableName</td>
<td>Timestream table</td>
<td>String</td>
<td></td>
</tr>
<tr class="even">
<td>Batch Size</td>
<td>Batch size for writing records to table</td>
<td>Integer</td>
<td>Default is 10</td>
</tr>
<tr class="odd">
<td>Interval</td>
<td>Interval in milliseconds after which data is written even if the buffer is not full</td>
<td>Integer</td>
<td>Optional, if not set only BatchSize is used, minimum value is 10</td>
</tr>
<tr class="even">
<td>Records</td>
<td>Records to write to table</td>
<td>List of <a href="#awstimestreamrecordconfiguration">AwsTimestreamRecordConfiguration</a></td>
<td></td>
</tr>
</tbody>
</table>

[^top](#aws-timestream-target)

## AwsTimestreamRecordConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 29%" />
<col style="width: 23%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4">Timestream records to write</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>
<tr class="even">
<td>MeasureName</td>
<td>Measure name for the value</td>
<td>String</td>
<td></td>
</tr>
<tr class="odd">
<td>MeasureValuePath</td>
<td>JMES path that selects the value to write to the record from the data received by the target writer</td>
<td>String</td>
<td><p><a href="https://jmespath.org/">https://jmespath.org/</a></p>
<p>A path typically has the format sourcename.valuename or sourcename.valuename.value (If the data entries contain both value and timestamp, in case TimestampLevel "channel" or "both" is used in the root of the configuration file) the writer will automatically look for a field with the name used for data values specified in "ElementNames" at the top level of the configuration for a path sourcename.valuename)</p></td>
</tr>
<tr class="even">
<td>MeasureValueType</td>
<td>Type of the value</td>
<td>DOUBLE, BIGINT,VARCHAR,BOOLEAN</td>
<td></td>
</tr>
<tr class="odd">
<td>MeasureTimePath</td>
<td>JMES path that selects the timestamp to use with to the property value from the data received by the target writer</td>
<td>String</td>
<td><p><a href="https://jmespath.org/">https://jmespath.org/</a></p>
<p>If the data entries contain both value and timestamp, in case TimestampLevel "channel" or "both" is used in the root of the configuration file) the writer will automatically look for a field with the name used for timestamp values specified in "ElementNames" at the top level of the configuration for a path sourcename.valuename)</p></td>
</tr>
<tr class="even">
<td>Dimensions</td>
<td>Record dimensions</td>
<td>List of <a href="#awstimestreamdimensionconfiguration">AwsTimestreamDimensionConfiguration</a></td>
<td></td>
</tr>
</tbody>
</table>

[^top](#aws-timestream-target)

## AwsTimestreamDimensionConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 29%" />
<col style="width: 23%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4">Timestream record dimensions</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>
<tr class="even">
<td>DimensionName</td>
<td>Name for the dimensions</td>
<td>String</td>
<td></td>
</tr>
<tr class="odd">
<td>DimensionValue</td>
<td>Fixed dimension value</td>
<td>String</td>
<td>Either DimensionValue or DimensionValuePath (see below) can be used.</td>
</tr>
<tr class="even">
<td>DimensionValuePath</td>
<td>JMES path that selects the value to write to the dimensions from the data received by the target writer</td>
<td>String</td>
<td><p><a href="https://jmespath.org/">https://jmespath.org/</a></p>
<p>A path typically has the format sourcename.valuename or sourcename.valuename.value (If the data entries contain both value and timestamp, in case TimestampLevel "channel" or "both" is used in the root of the configuration file) the writer will automatically look for a field with the name used for data values specified in "ElementNames" at the top level of the configuration for a path sourcename.valuename)</p></td>
</tr>
</tbody>
</table>

[^top](#aws-timestream-target)