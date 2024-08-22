# AWS S3 Target

AwsS3TargetConfiguration extends the type <a href="../core/target-configuration.md" >TargetConfiguration</a> with specific configuration data for sending data to an S3 bucket. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-S3"</strong>
<p>Requires IAM permission s3:putObject to write to the configured bucket</p>



[Targets](./README.md)

## Aws3TargetConfiguration

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
<td>BucketName</td>
<td>Name of the bucket to write to</td>
<td>String</td>
<td></td>
</tr>
<tr class="odd">
<td>Prefix</td>
<td>S3 key objects prefix</td>
<td></td>
<td>optional</td>
</tr>
<tr class="even">
<td>Region</td>
<td>AWS Region for S3 Bucket</td>
<td>String</td>
<td></td>
</tr>
<tr class="odd">
<td>BufferSize</td>
<td>Size in MB that triggers writing data to an S3 Object</td>
<td>Integer</td>
<td>Default is 1, maximum is 128</td>
</tr>
<tr class="even">
<td>Interval</td>
<td>Interval in seconds that triggers writing data to an S3 Object</td>
<td>Integer</td>
<td>Default is 60, maximum is 900</td>
</tr>
<tr class="odd">
<td>Compression</td>
<td>Compression used to compress the data in the S3 object</td>
<td>"None" | "GZip" | "Zip"</td>
<td>Default is "None"</td>
</tr>
<tr class="even">
<td>ContentType</td>
<td>Content Type Mime type of S3 object</td>
<td>String</td>
<td>When using compression, the value is set to the mime type of the used compression method. The main purpose of this setting is to explicitly set the value in case the data is transformed into a specific format, e.g. xml, yaml</td>
</tr>
</tbody>
</table>

[^top](#aws-s3-target)
