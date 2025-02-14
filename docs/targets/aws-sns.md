# AWS SNS Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

The SFC target adapter for [Amazon Simple Notification Service](https://aws.amazon.com/sns/) (SNS) enables publishing collected data as messages to SNS topics.

## AwsSnsTargetConfiguration

A configuration class that defines how industrial data should be published to Amazon SNS topics. It specifies the target SNS topic ARN, message format, and data transformation settings. 

AwsSnsTargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to an SNS topic queue. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-SNS"**


Requires IAM permission sqs:putMessage for the receiving topic.

- [Schema](#awssnstargetconfiguration-schema)
- [Examples](#awssnstargetconfiguration-examples)

**Properties:**

- [BatchSize](#batchsize)
- [Compression](#compression)
- [CredentialProviderClient](#credentialproviderclient)
- [Interval](#interval)
- [MessageGroupId](#messagegroupid)
- [Region](#region)
- [SerialAsMessageDeduplicationId](#serialasmessagededuplicationid)
- [Subject](#subject)
- [TopicArn](#topicarn)

---
### BatchSize
Number of output messages to combine in a single PublishBatch API call. The data will be written to the SNS topic before reaching the specified batch size if the maximum payload size (256 KB) would be exceeded.

**Type**: Integer

Default is 10, maximum is 10

---
### Compression
Specifies the compression algorithm used for message payloads.  Consider the overhead of base64 encoding when choosing compression, as it may offset compression benefits for smaller payloads.

**Type**: String

Possible valuesL

- "None" (Default)
- "GZip"
- "Zip"

---
### CredentialProviderClient



The CredentialProviderClient property specifies which AWS credential provider client to use for authentication. It references a client defined in the SFC's top-level configuration under [AwsIotCredentialProviderClients](../core/sfc-configuration.md#awsiotcredentialproviderclients) section. This client uses X.509 certificates to obtain temporary AWS credentials through the  [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---

### Interval
Time interval in milliseconds that triggers sending buffered data to SNS, even if the [batch size](#batchsize) hasn't been reached. When specified, messages will be sent after either this interval elapses or the batch size is reached, whichever occurs first.

**Type**: Integer

Optional, if not set only [BatchSize](#batchsize) is used

---
### MessageGroupId
A tag that identifies a specific message group for FIFO (first-in-first-out) topics only. Messages within the same message group are processed in strict order, while messages in different groups may be processed in parallel. 

**Type**: String

Optional

---
### Region
The AWS Region code where the SNS topic is located (for example, us-east-1, eu-west-1). This must match the region where your SNS topic is created. 

**Type**: String

---
### SerialAsMessageDeduplicationId
Controls how message deduplication is handled for FIFO topics. When true, uses the unique serial number from SFC as the MessageDeduplicationId. When false, enables ContentBasedDeduplication where SNS generates the deduplication ID based on message content.

**Type**: Boolean

---
### Subject
Optional subject for the SNS messages. 

**Type**: String

---
### TopicArn
The Amazon Resource Name (ARN) that uniquely identifies the SNS topic where messages will be published. Must be a valid SNS topic ARN in the format "arn:aws:sns:region:account-id:topic-name".

**Type**: String

### AwsSnsTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsSnsTargetConfiguration",
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
        "BatchSize": {
          "type": "integer",
          "description": "Size of the batch for SNS messages"
        },
        "Compression": {
          "type": "string",
          "description": "Compression type for messages",
          "enum": ["None", "Zip", "GZip"],
          "default": "None"
        },
        "Interval": {
          "type": "integer",
          "description": "Interval in milliseconds between publishes"
        },
        "MessageGroupId": {
          "type": "string",
          "description": "Message group ID for FIFO topics"
        },
        "Region": {
          "type": "string",
          "description": "AWS region for SNS"
        },
        "SerialAsMessageDeduplicationId": {
          "type": "boolean",
          "description": "Use message serial number as deduplication ID"
        },
        "Subject": {
          "type": "string",
          "description": "Subject of the SNS message"
        },
        "TopicArn": {
          "type": "string",
          "description": "ARN of the SNS topic"
        }
      },
      "required": ["TopicArn"]
    }
  ]
}
   
```

### AwsSnsTargetConfiguration Examples

Configuration using CredentialProviderClient.

```json
{
  "TargetType" : "AWS-SNS",    
  "TopicArn": "arn:aws:sns:us-east-1:123456789012:MyTopic",
  "Region": "us-east-1",
  "Subject": "Device Telemetry",
  "Compression": "None",
  "CredentialProviderClient": "aws-credentials-provider"
}
```

Configuration using  default AWS SDK credential provider chain.

```json
{
  "TargetType" : "AWS-SNS",  
  "TopicArn": "arn:aws:sns:us-east-1:123456789012:MyTopic",
  "Region": "us-east-1",
  "Subject": "Device Telemetry",
  "Compression": "None"
}
```



[^top](#aws-sns-target)

