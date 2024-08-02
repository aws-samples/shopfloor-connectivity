
# AWS IoT Analytics Target

[Targets](./targets.md)

## AwsIotAnalyticsTargetConfiguration


AwsIotAnalyticsTargetConfiguration extends the type TargetConfiguration with specific configuration data for sending data to an IoT Analytics channel. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-IOT-ANALYTICS"</strong>
<p>Requires IAM permission iotanalytics:BatchPutMessage to write to the configured channel</p>
<br>

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
<td>ChannelName</td>
<td>Name of the IoT Analytics channel</td>
<td>String</td>
<td></td>

</tr>
<tr class="odd">
<td>Region</td>
<td>AWS Region for channel</td>
<td>String</td>
<td></td>

</tr>
<tr class="even">
<td>BatchSize</td>
<td>Number of output messages to combine in a single BatchPutMessage API call.</td>
<td>Integer</td>
<td>Default is 10</td>

</tr>
</tbody>
</table>

[^top](#aws-iot-analytics-target)

