# AWS Kinesis Target


AwsKinesisTargetConfiguration extends the type <a href="../core/target-configuration.md" >TargetConfiguration</a> with specific configuration data for sending to a stream for the AWS Kinesis service. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-KINESIS"</strong></p>
<p>Requires IAM permission kinesis:PutRecords for the stream the data is sent to.


[Targets](./README.md)

## AwsKinesisTargetConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 29%" />
<col style="width: 23%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>

</tr>
<tr class="even">
<td>StreamName</td>
<td>Name of the Kinesis stream</td>
<td>String</td>
<td></td>

</tr>
<tr class="odd">
<td>Region</td>
<td>AWS Region for Kinesis service</td>
<td>String</td>
<td></td>

</tr>
<tr class="even">
<td>BatchSize</td>
<td>Number of output messages to combine in a single putRecordBatch API call.</td>
<td>Integer</td>
<td>Default is 10, Maximum is 500</td>

</tr>
<tr class="odd">
<td>Compression</td>
<td>Compression used to compress the data in the submitted items</td>
<td>"None" | "GZip" | "Zip"</td>
<td>Default is "None"</td>

</tr>
<tr class="even">
<td>Interval</td>
<td>Interval in milliseconds after which data is sent to stream even if the buffer is not full</td>
<td>Integer</td>
<td>Optional, if not set only BatchSize is used</td>

</tr>
</tbody>
</table>

[^top](#aws-kinesis-target)
