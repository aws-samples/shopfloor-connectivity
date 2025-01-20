# AWS SQS Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 



## AwsSqsTargetConfiguration

AwsSqsTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to an SQS queue. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-SQS"**

Requires IAM permission sqs:SendMessageBatch for the receiving queue.

- [Schema](#awssqstargetconfiguration-schema)
- [Examples](#awssqstargetconfiguration-examples)

**Properties:**

- [BatchSize](#batchsize)
- [Compression](#compression)
- [CredentialProviderClient](#credentialproviderclient)
- [Interval](#interval)
- [QueueUrl](#queueurl)
- [Region](#region)

---
### BatchSize
Number of output messages to combine in a sendMessageBatch. The data will be written to the queue before the batch size is reached if the maximum payload size will be exceeded.

**Type**: Integer

Default is 10, maximum is 10

---
### Compression
Compression used to compress message payload.
The data in the messages is wrapped in structure with the following fields:
- "compression" : Used compression
- "payload": Compressed data as a base64 encoded string.
When using compression for the message verify if actual compression out weights the overhead of the base64 encoded of the compressed data.

**Type**: "None" | "GZip" | "Zip"

Default is "None"

---
### CredentialProviderClient

Name of the AWS credential provider client defined in the SFC top level configuration section [AwsIotCredentialProviderClients]
(../core/sfc-top-level-config.md#AwsIotCredentialProviderClients) obtaining credentials using X.509 certificates from the [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---

### Interval
Interval in milliseconds after which data is sent to queue even if the buffer is not full

**Type**: Integer

Optional, if not set only BatchSize is used


---
### QueueUrl
Url of the receiving queue

**Type**: String

---
### Region
AWS Region for SQS service

**Type**: String

### AwsSqsTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsSqsTargetConfiguration",
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
          "description": "Size of the batch for SQS messages"
        },
        "Compression": {
          "type": "string",
          "description": "Compression type for messages",
          "enum": ["None", "Zip", "GZip"],
          "default": "None"
        },
        "Interval": {
          "type": "integer",
          "description": "Interval in milliseconds between sends"
        },
        "QueueUrl": {
          "type": "string",
          "description": "URL of the SQS queue"
        },
        "Region": {
          "type": "string",
          "description": "AWS region for SQS"
        }
      },
      "required": ["QueueUrl"]
    }
  ]
}
  
```

### AwsSqsTargetConfiguration Examples

Configuration using CredentialProviderClient.

```json
{
  "TargetType" : "AWS-SQS", 
  "QueueUrl": "https://sqs.us-west-2.amazonaws.com/123456789012/BatchQueue",
  "Region": "us-west-2",
  "BatchSize": 100,
  "Interval": 10000,
  "Compression": "GZip",
  "CredentialProviderClient": "aws-credentials-provider"
}
```

Configuration using  default AWS SDK credential provider chain.

```json
{
  "TargetType" : "AWS-SQS", 
  "QueueUrl": "https://sqs.us-west-2.amazonaws.com/123456789012/BatchQueue",
  "Region": "us-west-2",
  "BatchSize": 100,
  "Interval": 10000,
  "Compression": "GZip"
}
```



[^top](#aws-sqs-target)

