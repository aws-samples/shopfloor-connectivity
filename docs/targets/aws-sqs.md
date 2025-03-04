# AWS SQS Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

The SFC target adapter for Amazon [Simple Queue Service](https://aws.amazon.com/sqs/) (SQS) enables sending collected data to SQS queues.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWS-SQS": {
      "JarFiles" : ["<location of deployment>/aws-sqs-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.sqs.AwsSqsTargetWriter"
   }
}
```

## 

## AwsSqsTargetConfiguration

AwsSqsTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to an SQS queue. The Targets configuration element can contain entries of this type; the TargetType of these entries must be set to **"AWS-SQS"**.

Requires IAM permission `sqs:SendMessageBatch` for the receiving queue.

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
Number of output messages to combine in a single SendMessageBatch API call. The data will be written to the SQS queue before reaching the specified batch size if the maximum payload size (256 KB) would be exceeded. 

**Type**: Integer

Default is 10, maximum is 10

---
### Compression
Specifies the compression algorithm used for message payloads.  Consider the overhead of base64 encoding when choosing compression, as it may offset compression benefits for small payloads.

**Type**:  String

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
The time interval in milliseconds that triggers sending buffered messages to the SQS queue, even if the [batch size](#batchsize) hasn't been reached. When not specified, messages are only sent when the batch size limit is reached.

**Type**: Integer

Optional, if not set only [BatchSize](#batchsize) is used


---
### QueueUrl
The URL of the Amazon SQS queue where messages will be sent. This is the unique identifier for the queue, provided by AWS when the queue is created, in the format "https://sqs.{region}.amazonaws.com/{account-id}/{queue-name}".

**Type**: String

---
### Region
The AWS Region identifier where the SQS queue is located, such as "us-east-1" or "eu-west-2". Specifies which regional endpoint to use when sending messages to the queue.

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

