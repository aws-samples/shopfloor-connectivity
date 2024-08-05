
# AWS CloudWatch Metrics

AwsCloudWatchConfiguration configures the settings used by the AWS CloudWatch Metrics writer. It is used as a section names "CloudWatch" in the Metrics section of the SFC configuration

[Meteric Writers](./README.md)


## AwsCloudWatchConfiguration

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
<td>Region</td>
<td>AWS CloudWatch service region</td>
<td>String</td>
<td>Default is region setup for AWS SDK</td>
</tr>

<tr class="odd">
<td>Interval/td>
<td>Interval in seconds in which metrics are written to the service (or earlier if buffer size is reached)</td>
<td>Integer</td>
<td>Default is 60</td>
</tr>

<tr class="even">
<td>BatchSize</td>
<td>Number of data points to buffer to write as a batch to CloudWatch service</td>
<td>Int</td>
<td>Default and max value is 1000</td>
</tr>

<tr class="odd">
<td>CredentialProviderClient</td>
<td>Name of configured <a href="../core/aws-iot-credential-provider-configuration.md">credentials client</a> that will be used to read secrets stored in the AWS Secrets Manager service.</td>
<td>String</td>
<td>If not set the AWS SDK credential provider chain is used.</td>
</tr>

<tr class="even">
<td>CloudWatchMetricsChannelSize</td>
<td>Size of internal buffer to send metrics data to CloudWatch</td>
<td>Int</td>
<td>Default is 1000</td>
</tr>

<tr class="odd">
<td>CloudWatchMetricsChannelTimeout</td>
<td>Time in milliseconds to send data to internal buffer</td>
<td>Int</td>
<td>Default is 1000</td>
</tr>

</tbody>
</table>
[^top](#aws-cloudwatch-metrics)

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
<td>Region</td>
<td>AWS CloudWatch service region</td>
<td>String</td>
<td>Default is region setup for AWS SDK</td>
</tr>

<tr class="odd">
<td>Interval/td>
<td>Interval in seconds in which metrics are written to the service (or earlier if buffer size is reached)</td>
<td>Integer</td>
<td>Default is 60</td>
</tr>

<tr class="even">
<td>BatchSize</td>
<td>Number of data points to buffer to write as a batch to CloudWatch service</td>
<td>Int</td>
<td>Default and max value is 1000</td>
</tr>

<tr class="odd">
<td>CredentialProviderClient</td>
<td>Name of configured <a href="../core/aws-iot-credential-provider-configuration.md">credentials client</a> that will be used to read secrets stored in the AWS Secrets Manager service.</td>
<td>String</td>
<td>If not set the AWS SDK credential provider chain is used.</td>
</tr>

<tr class="even">
<td>CloudWatchMetricsChannelSize</td>
<td>Size of internal buffer to send metrics data to CloudWatch</td>
<td>Int</td>
<td>Default is 1000</td>
</tr>

<tr class="odd">
<td>CloudWatchMetricsChannelTimeout</td>
<td>Time in milliseconds to send data to internal buffer</td>
<td>Int</td>
<td>Default is 1000</td>
</tr>

</tbody>
</table>