# AWS IoT Core Target

[Targets](./targets.md)

## AwsIotCoreTargetConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 29%" />
<col style="width: 23%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p>AwsIotCoreTargetConfiguration extends the type TargetConfiguration with specific configuration data for connecting to and sending to AWS IoT core topic using HTTP dataplane API. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-IOT-CORE"</strong></p>
<p>Requires IAM permission iot:Publish for the topic the data is published to.</p></th>
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
<td>TopicName</td>
<td>Name of the topic</td>
<td>String</td>
<td>Topic names must not start with "$" as these are reserved for topics used only by AWS IoT Core</td>
</tr>
<tr class="odd">
<td>Region</td>
<td>AWS Region for IoT Core service</td>
<td>Integer</td>
<td></td>
</tr>



<tr class="even">  
<td>BatchCount</td>  
<td>Number of messages to buffer before sending data as a batch  to topic.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.</td>
</tr>

<tr class="odd">  
<td>BatchSize</td>  
<td>Payload size in KB of messages to batch before sending data as a batch to topic.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.
The size is calculated on the uncompressed payload of the messages.</td>
</tr>

<tr class="even">  
<td>BatchInterval</td>  
<td>Interval in milliseconds after which a batch of messages is sent to the topic, even when the BatchSize or BatchCount limit is not reached.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.
</td>
</tr>

<tr class="odd">  
<td>Compression</td>  
<td>Compression method for MQTT message payloads.</td>  
<td>String

- "None"
- "Zip"
- "GZip"

</td>  
<td>Default is "None"</td>
</tr> 

</tbody>
</table>

[^Top](#aws-iot-core-target)