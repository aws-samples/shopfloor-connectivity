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
<td>Name or name template of the topic</td>
<td>String</td>
<td>
A template can be used for the topicName to render the actual topic name using placeholders. In this template, 
besides placeholders for environment variables (${name}) the following placeholders are available:

- %schedule%
- %target%
- %source%
- %channel%

To use the values of **metadata** at the top, source or channel level of the target data, the name af the metadata value can
be used with a '%' prefix and postfix.

Value placeholders can be used to add additional topic levels or grouping values to a specific topic.

Template examples:

- plant1-%source% : Values from each source will be published to a topic for that source
- plant1-%line%   : Values from all sources will be grouped by the value of the %line% metadata and published to a topic for that value

In case a placeholder is not resolved, when a value for a used placeholder is part of the data,
then an alternative topic name can be configured by setting the name of that topic to the **"AlternateTopiName"** setting.

Note that the use of placeholders to send data to specific topics will result in additional publish calls and may result in throttling. Enabling buffering
can be used to reduce the chance of throttling.

For AWS IoTCore the maximum number of topic levels is 8.

</td>
</tr>

<tr class="odd">
<td>AlternateTopicName</td>
<td>Name or name template of the topic values are published in case there are unmapped template placeholders in the TopicName</td>
<td>String</td>
<td>
</td>
</tr>


<tr class="even">
<td>WarnAlternateTopicName</td>
<td>Generate warning if data is published to AlternateTopicName</td>
<td>Boolean</td>
<td>
Default is tue
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