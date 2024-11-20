# AWS IoT Core Target
<br>
AwsIotCoreTargetConfiguration extends the type <a href="../core/target-configuration.md" >TargetConfiguration</a> with specific configuration data for connecting to and sending to AWS IoT core topic using HTTP dataplane API. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-IOT-CORE"</strong><br>
<br>
Requires IAM permissions iot:Connect, iot:DescribeEndpoint, iot:Publish for the topic the data is published to and iot:RetainPublish if the Retain option is used.


[Targets](./README.md)

## AwsIotCoreTargetConfiguration

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
<td>TopicName</td>
<td>Name of the topic</td>
<td>String</td>
<td>Topic names must not start with "$" as these are reserved for topics used only by AWS IoT Core

A template can be used for the topicName to render the actual topic name using placeholders. In this template, 
besides placeholders for environment variables (${name}) the following placeholders are available:

- %target%
- %source%
- %channel%

To use the values of metadata at the top, source or channel level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

Note that the use of placeholders to send data to specific topics will result in additional publish calls and may result in throttling. Enabling buffering
can be used to reduce the chance of throttling.


</td>
</tr>
<tr class="odd">
<td>Region</td>
<td>AWS Region for IoT Core service</td>
<td>Integer</td>
<td></td>
</tr>



<tr class="even">  
<td>BatchCount</td>  
<td>Number of messages to buffer before sending data as a batch to a topic.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.</td>
</tr>

<tr class="odd">  
<td>BatchSize</td>  
<td>Payload size in KB of messages to batch before sending data as a batch to a topic.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.
The size is calculated on the uncompressed payload of the messages.</td>
</tr>

<tr class="even">  
<td>BatchInterval</td>  
<td>Interval in milliseconds after which a batch of messages is sent to a topic, even when the BatchSize or BatchCount limit is not reached.</td>  
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

<tr class="even">  
<td>Retain</td>  
<td>Set to true to store a single message per a given MQTT topic for delivery to any current and future topic subscribers.</td>  
<td>Boolean
</td>  
<td>Default is false

As service limits for publishing retained messages are lower than publishing non-retained messages consider to enable buffering
using, BatchSize, BatchCount or BatchInterval.
Publishing messages with retain option requires the iot:RetainPublish permission
</td>
</tr> 

</tbody>
</table>

[^Top](#aws-iot-core-target)