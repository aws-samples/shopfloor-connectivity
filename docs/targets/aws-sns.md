# AWS SNS Target



## AwsSnsTargetConfiguration

AwsSnsTargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to an SNS topic queue. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-SNS"**


Requires IAM permission sqs:putMessage for the receiving topic.

- [Schema](#AwsSnsTargetConfiguration-Schema)
- [Examples](#AwsSnsTargetConfiguration-Examples)

**Properties:**

- [BatchSize](#BatchSize)
- [Compression](#Compression)
- [CredentialProviderClient](#CredentialProviderClient)
- [Interval](#Interval)
- [MessageGroupId](#MessageGroupId)
- [Region](#Region)
- [SerialAsMessageDeduplicationId](#SerialAsMessageDeduplicationId)
- [Subject](#Subject)
- [TopicArn](#TopicArn)

---
### BatchSize
Number of output messages to combine in a publishBatch call. The data will be written to the queue before the batch size is reached if the maximum payload size will be exceeded.

**Type**: Integer

Default is 10, maximum is 10

---
### Compression
Compression used to compress message.
The data in the messages is wrapped in structure with the following fields:
- "compression" : Used compression
- "payload": Compressed data as a base64 encoded string.
When using compression for the message verify if actual compression out weights the overhead of the base64 encoded of the compressed data.

**Type**: "None" | "GZip" | "Zip"

Default is "None"

---
### CredentialProviderClient

Name of the AWS credential provider client defined in the SFC top level configuration section [AwsIotCredentialProviderClients]
(../core/sfc-top-level-config.md#AwsIotCredentialProviderClients) obtaining credentials using X509 certificates from the [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---

### Interval
Interval in milliseconds after which data is sent to stream even if the buffer is not full

**Type**: Integer

Optional, if not set only BatchSize is used

---
### MessageGroupId
This parameter applies only to FIFO (first-in-first-out) topics.
The tag that specifies that a message belongs to a specific message group

**Type**: String

Optional

---
### Region
AWS Region for SNS service

**Type**: String

---
### SerialAsMessageDeduplicationId
Used the unique serial number of the SFC MessageDeduplicationId, if set to false ContentBasedDeduplication is used

**Type**: Boolean

true

---
### Subject
Topic message subject

**Type**: String

Optional

---
### TopicArn
Arn of the receiving topic

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

