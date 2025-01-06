# AWS IoT Core Target



## AwsIotCoreTargetConfiguration


AwsIotCoreTargetConfiguration extends the type <a href="../core/target-configuration.md" >TargetConfiguration</a> with specific configuration data for connecting to and sending to AWS IoT core topic using HTTP dataplane API. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-IOT-CORE"</strong>

Requires IAM permissions iot:Connect, iot:DescribeEndpoint, iot:Publish for the topic the data is published to and iot:RetainPublish if the Retain option is used.


**Properties:**
- [AlternateTopicName](#AlternateTopicName)
- [BatchCount](#BatchCount)
- [BatchInterval](#BatchInterval)
- [BatchSize](#BatchSize)
- [Compression](#Compression)
- [Region](#Region)
- [Retain](#Retain)
- [TopicName](#TopicName)
- [WarnAlternateTopicName](#WarnAlternateTopicName)

---
### AlternateTopiName
Name or name template of the topic values are published in case there are unmapped template placeholders in the TopicName

**Type**: String




---
### BatchCount
Number of messages to buffer before sending data as a batch to a topic.

**Type**: Int

Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.

---
### BatchInterval
Interval in milliseconds after which a batch of messages is sent to a topic, even when the BatchSize or BatchCount limit is not reached.

**Type**: Int

Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.


---
### BatchSize
Payload size in KB of messages to batch before sending data as a batch to a topic.

**Type**: Int

Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.
The size is calculated on the uncompressed payload of the messages.

---
### Compression
Compression method for MQTT message payloads.

**Type**: String

- "None"
- "Zip"
- "GZip"



Default is "None"


---
### Region
AWS Region for IoT Core service

**Type**: String

---
### Retain
Set to true to store a single message per a given MQTT topic for delivery to any current and future topic subscribers.

**Type**: Boolean


Default is false

As service limits for publishing retained messages are lower than publishing non-retained messages consider to enable buffering
using, BatchSize, BatchCount or BatchInterval.

Publishing messages with retain option requires the iot:RetainPublish permission


---
### TopicName
Name or name template of the topic

**Type**: String


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
then an alternative topic name can be configured by setting the name of that topic to the [AlternateTopiName](#AlternateTopiName) setting.

Note that the use of placeholders to send data to specific topics will result in additional publish calls and may result in throttling. Enabling buffering
can be used to reduce the chance of throttling.

For AWS IoTCore the maximum number of topic levels is 8.



---
### WarnAlternateTopicName
Generate warning if data is published to [AlternateTopiName](#AlternateTopiName).

**Type**: Boolean


Default is tue


[^top](#AWS IoT Core Target)

