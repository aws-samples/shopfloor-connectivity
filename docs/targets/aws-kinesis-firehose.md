
# AWS Kinesis Firehose Target

AwsKinesisFirehoseTargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending to a delivery stream for the AWS Kinesis Firehose service. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-FIREHOSE"</strong>

<p>Requires IAM permission firehose:PutRecordBatch for the delivery stream the data is sent to.</p>


[Targets](./README.md)

## AwsKinesisFirehoseTargetConfiguration

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
<td>Name of the delivery stream</td>
<td>String</td>
<td></td>
</tr>
<tr class="odd">
<td>Region</td>
<td>AWS Region for Kinesis Firehose service</td>
<td>String</td>
<td></td>
</tr>
<tr class="even">
<td>BatchSize</td>
<td><p>Number of output messages to combine in a single putRecordBatch API call.</p>
<p>The target will send buffered data before the batch size is reached if the entire size of the message will exceed the maximum size for a single request.</p></td>
<td>Integer</td>
<td>Default is 10</td>
</tr>
</tbody>
</table>

[^top](#aws-kinesis-firehose-target)
