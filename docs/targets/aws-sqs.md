# AWS SQS Target
AwsSqsTargetConfiguration extends the type TargetConfiguration with specific configuration data for sending data to an SQS queue. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-SQS"</strong>
<p>Requires IAM permission sqs:SendMessageBatch for the receiving queue.</p>

[Targets](./targets.md)

## AwsSqsTargetConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 28%" />
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
<td>QueueUrl</td>
<td>Url of the receiving queue</td>
<td>String</td>
<td></td>

</tr>
<tr class="odd">
<td>Region</td>
<td>AWS Region for SQS service</td>
<td>String</td>
<td></td>

</tr>
<tr class="even">
<td>BatchSize</td>
<td>Number of output messages to combine in a sendMessageBatch. The data will be written to the queue before the batch size is reached if the maximum payload size will be exceeded.</td>
<td>Integer</td>
<td>Default is 10, maximum is 10</td>

</tr>
<tr class="odd">
<td>Compression</td>
<td><p>Compression used to compress message payload.</p>
<p>The data in the messages is wrapped in structure with the following fields:</p>
<p>- "compression" : Used compression</p>
<p>- "payload": Compressed data as a base64 encoded string.</p>
<p>When using compression for the message verify if actual compression out weights the overhead of the base64 encoded of the compressed data.</p></td>
<td>"None" | "GZip" | "Zip"</td>
<td>Default is "None"</td>

</tr>
<tr class="even">
<td>Interval</td>
<td>Interval in milliseconds after which data is sent to queue even if the buffer is not full</td>
<td>Integer</td>
<td>Optional, if not set only BatchSize is used</td>

</tr>
</tbody>
</table>

[^top](#aws-sqs-target)
