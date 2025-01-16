
# AWS Kinesis Firehose Target

[SFC Configuration](../core/sfc-top-level-config.md) > [Targets](../core/sfc-top-level-config.md#Targets) >  [Target](../core/target-configuration.md) 



## AwsKinesisFirehoseTargetConfiguration

AwsKinesisFirehoseTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending to a delivery stream for the AWS Kinesis Firehose service. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-FIREHOSE"**

Requires IAM permission `firehose:PutRecordBatch` for the delivery stream the data is sent to.

- [Schema](#AwsKinesisFirehoseTargetConfiguration-Schema)
- [Examples](#AwsKinesisFirehoseTargetConfiguration-Examples)

**Properties:**
- [BatchSize](#BatchSize)
- [CredentialProviderClient](#CredentialProviderClient)
- [Region](#Region)
- [StreamName](#StreamName)

---
### BatchSize
Number of output messages to combine in a single putRecordBatch API call.
The target will send buffered data before the batch size is reached if the entire size of the message will exceed the maximum size for a single request.

**Type**: Integer

Default is 10

---
### CredentialProviderClient

Name of the AWS credential provider client defined in the SFC top level configuration section [AwsIotCredentialProviderClients]
(../core/sfc-top-level-config.md#AwsIotCredentialProviderClients) obtaining credentials using X509 certificates from the [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---

### Region
AWS Region for Kinesis Firehose service

**Type**: String

---
### StreamName
Name of the delivery stream

**Type**: String

### AwsKinesisFirehoseTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsKinesisFirehoseTargetConfiguration",
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
          "description": "Size of the batch for Kinesis Firehose messages"
        },
        "CredentialProviderClient": {
          "type": "string",
          "description": "The credential provider client name"
        },
        "Region": {
          "type": "string",
          "description": "AWS region for Kinesis Firehose"
        },
        "StreamName": {
          "type": "string",
          "description": "Name of the Kinesis Firehose delivery stream"
        }
      },
      "required": ["StreamName"]
    }
  ]
}

```

### AwsKinesisFirehoseTargetConfiguration Examples

Configuration using CredentialProviderClient

```json
{
  "TargetType" : "AWS-FIREHOSE",
  "StreamName": "data-delivery-stream",
  "Region": "us-east-1",
  "BatchSize": 500,
  "CredentialProviderClient": "aws-credentials-provider"
}

```

Configuration using  default AWS SDK credential provider chain.

```json
{
  "TargetType" : "AWS-FIREHOSE",
  "StreamName": "data-delivery-stream",
  "Region": "us-east-1",
  "BatchSize": 500
}

```

[

[^top](#aws-kinesis-firehose-target)

