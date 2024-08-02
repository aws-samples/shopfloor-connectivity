# AWS SNS Target


AwsSnsTargetConfiguration extends the type TargetConfiguration with specific configuration data for sending data to an SNS topic queue. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-SNS"</strong>
<p>Requires IAM permission sqs:putMessage for the receiving topic.



[Targets](./targets.md)

## AwsSnsTargetConfiguration

<table>
<colgroup>
<col style="width: 22%" />
<col style="width: 26%" />
<col style="width: 28%" />
<col style="width: 22%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>

</tr>
<tr class="even">
<td>TopicArn</td>
<td>Arn of the receiving topic</td>
<td>String</td>
<td></td>

</tr>
<tr class="odd">
<td>Region</td>
<td>AWS Region for SNS service</td>
<td>String</td>
<td></td>

</tr>
<tr class="even">
<td>Subject</td>
<td>Topic message subject</td>
<td>String</td>
<td>Optional</td>

</tr>
<tr class="odd">
<td>MessageGroupId</td>
<td><p>This parameter applies only to FIFO (first-in-first-out) topics.</p>
<p>The tag that specifies that a message belongs to a specific message group</p></td>
<td>String</td>
<td>Optional</td>

</tr>
<tr class="even">
<td>SerialAsMessageDeduplicationId</td>
<td>Used the unique serial number of the SFC MessageDeduplicationId, if set to false ContentBasedDeduplication is used</td>
<td>Boolean</td>
<td>true</td>

</tr>
<tr class="odd">
<td>BatchSize</td>
<td>Number of output messages to combine in a publishBatch call. The data will be written to the queue before the batch size is reached if the maximum payload size will be exceeded.</td>
<td>Integer</td>
<td>Default is 10, maximum is 10</td>

</tr>
<tr class="even">
<td>Compression</td>
<td><p>Compression used to compress message.</p>
<p>The data in the messages is wrapped in structure with the following fields:</p>
<p>- "compression" : Used compression</p>
<p>- "payload": Compressed data as a base64 encoded string.</p>
<p>When using compression for the message verify if actual compression out weights the overhead of the base64 encoded of the compressed data.</p></td>
<td>"None" | "GZip" | "Zip"</td>
<td>Default is "None"</td>

</tr>
<tr class="odd">
<td>Interval</td>
<td>Interval in milliseconds after which data is sent to stream even if the buffer is not full</td>
<td>Integer</td>
<td>Optional, if not set only BatchSize is used</td>

</tr>
</tbody>
</table>

[^top](#aws-sns-target)