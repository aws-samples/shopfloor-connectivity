# AWS IoT Core Target



## AwsIotCoreTargetConfiguration

AwsIotCoreTargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for connecting to and sending to AWS IoT core topic using HTTP dataplane API. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-IOT-CORE"**

Requires IAM permissions `iot:Connect`, `iot:DescribeEndpoint`, `iot:Publish` for the topic the data is published to and `iot:RetainPublish` if the Retain option is used.

- [Schema](#AwsIotCoreTargetConfiguration-Schema)
- [Examples](#AwsIotCoreTargetConfiguration-Examples)

**Properties:**

- [AlternateTopicName](#AlternateTopicName)
- [BatchCount](#BatchCount)
- [BatchInterval](#BatchInterval)
- [BatchSize](#BatchSize)
- [Compression](#Compression)
- [CredentialProviderClient](#CredentialProviderClient)
- [Region](#Region)
- [Retain](#Retain)
- [TopicName](#TopicName)
- [WarnAlternateTopicName](#WarnAlternateTopicName)

---
### AlternateTopicName
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

**Values:**

- "None" (Default)
- "Zip"
- "GZip"

**Type**: String


---
### CredentialProviderClient

Name of the AWS credential provider client defined in the SFC top level configuration section [AwsIotCredentialProviderClients](../core/sfc-top-level-config.md#AwsIotCredentialProviderClients) obtaining credentials using X509 certificates from the [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

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
then an alternative topic name can be configured by setting the name of that topic to the [AlternateTopiName](#AlternateTopicName) setting.

Note that the use of placeholders to send data to specific topics will result in additional publish calls and may result in throttling. Enabling buffering
can be used to reduce the chance of throttling.

For AWS IoTCore the maximum number of topic levels is 8.



---
### WarnAlternateTopicName
Generate warning if data is published to [AlternateTopiName](#AlternateTopicName).

**Type**: Boolean


Default is true

### AwsIotCoreTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsIotCoreTargetConfiguration",
  "type": "object",
  "allOf": [
    {
      "$ref": "#/definitions/TargetConfiguration"
    },
    {
      "$ref": "#/definitions/AwsServiceConfig"
    },
    {
      "type": "object",
      "properties": {
        "AlternateTopicName": {
          "type": "string",
          "description": "Alternate topic name to publish messages to when there arte unresolved placeholders"
        },
        "BatchCount": {
          "type": "integer",
          "description": "Number of messages to batch before publishing"
        },
        "BatchInterval": {
          "type": "integer",
          "description": "Interval in milliseconds between batch publishes"
        },
        "BatchSize": {
          "type": "integer",
          "description": "Maximum size of batched messages in bytes"
        },
        "Compression": {
          "type": "string",
          "description": "Compression type for messages",
          "enum": ["None", "Zip", "GZip"],
          "default" : "None"
        },
        "CredentialProviderClient": {
          "type": "string",
          "description": "The credential provider client name"
        },
        "Region": {
          "type": "string",
          "description": "AWS region for IoT Core"
        },
        "Retain": {
          "type": "boolean",
          "description": "Whether to retain messages"
        },
        "TopicName": {
          "type": "string",
          "description": "Main topic name to publish messages to"
        },
        "WarnAlternateTopicName": {
          "type": "boolean",
          "description": "Whether to warn when using alternate topic name"
        }
      },
      "required": ["TopicName"]
    }
  ]
}

```

### AwsIotCoreTargetConfiguration Examples

Configuration using CredentialProviderClient

```json
{
  "TargetType" : "AWS-IOT-CORE",
  "TopicName": "device/data",
  "Region": "us-east-1",
  "BatchSize": 1024,
  "BatchCount": 100,
  "BatchInterval": 5000,
  "Compression": "GZIP",
  "Retain": true,
  "CredentialProviderClient": "aws-credentials-provider"
}
```

Configuration using dynamic topic name based on target- (%source%) and metadata (%line%, %plant%)

```json
{
  "TargetType" : "AWS-IOT-CORE",
  "TopicName": "sensordata/%plant%/%line%/%source%",
  "Region": "us-east-1",
  "Compression": "ZIP",
  "Retain": true,
  "CredentialProviderClient": "aws-credentials-provider"
}
```

Configuration using  default AWS SDK credential provider chain.

```json
{
  "TargetType" : "AWS-IOT-CORE",
  "TopicName": "device/data",
  "Region": "us-east-1",
  "Compression": "GZIP",
  "Retain": true,
}
```



[^top](#aws-iot-core-target)



