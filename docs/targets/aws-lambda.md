# AWS Lambda Target

AwsLambdaFunctionConfiguration extends the type TargetConfiguration with specific configuration data for calling an AWS lambda function. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-LAMBDA"</strong>
<p>Requires IAM permission lambda:InvokeFunction for the lambda function that is called.</p>


[Targets](./targets.md)

## AwsLambdaTargetConfiguration

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
<td>FunctionName</td>
<td>Name of the Lambda function</td>
<td>String</td>
<td></td>

</tr>
<tr class="odd">
<td>Qualifier</td>
<td>AWS Region for Lambda service</td>
<td>String</td>
<td>Default is latest</td>

</tr>
<tr class="even">
<td>Region</td>
<td>AWS Region for Lambda service</td>
<td>String</td>
<td></td>

</tr>
<tr class="odd">
<td>BatchSize</td>
<td>Number of output messages to combine in a single invoke request for the lambda function. If BatchSize is greater than 1, then the output records are combined in a JSON array. The function will be called before the batch size is reached if the maximum payload size will be exceeded.</td>
<td>Integer</td>
<td>Default is 10</td>

</tr>
<tr class="even">
<td>Compression</td>
<td><p>Compression used to compress invocation payload.</p>
<p>As this payload needs to be valid JSON.</p>
<p>The data is wrapped in structure with the following fields:</p>
<p>- "compression" : Used compression</p>
<p>- "payload": Compressed data as a base64 encoded string.</p>
<p>When using compression for the lambda payload verify if actual compression out weights the overhead of the base64 encoded of the compressed data.</p></td>
<td>"None" | "GZip" | "Zip"</td>
<td>Default is "None"</td>

</tr>
<tr class="odd">
<td>Interval</td>
<td>Interval in milliseconds after which data is sent to stream even if the buffer is not full</td>
<td>Integer</td>
<td>Optional, if not set only BatchSize is used, minimum value is 10</td>

</tr>
</tbody>
</table>

[^top](#aws-lambda-target)