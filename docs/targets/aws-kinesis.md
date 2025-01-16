# AWS Kinesis Target

[SFC Configuration](../core/sfc-top-level-config.md) > [Targets](../core/sfc-top-level-config.md#Targets) >  [Target](../core/target-configuration.md) 



## AwsKinesisTargetConfiguration

AwsKinesisTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending to a stream for the AWS Kinesis service. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-KINESIS"**


Requires IAM permission `kinesis:PutRecords` for the stream the data is sent to.

- [Schema](#AwsKinesisTargetConfiguration-Schema)
- [Examples](#AwsKinesisTargetConfiguration-Examples)

**Properties:**
- [BatchSize](#BatchSize)
- [Compression](#Compression)
- [CredentialProviderClient](#CredentialProviderClient)
- [Interval](#Interval)

- [Region](#Region)
- [StreamName](#StreamName)

---
### BatchSize
Number of output messages to combine in a single putRecordBatch API call.

**Type**: Integer

Default is 10, Maximum is 500

---

### CredentialProviderClient

Name of the AWS credential provider client defined in the SFC top level configuration section [AwsIotCredentialProviderClients]
(../core/sfc-top-level-config.md#AwsIotCredentialProviderClients) obtaining credentials using X509 certificates from the [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---
### Compression
Compression used to compress the data in the submitted items

**Type**: "None" | "GZip" | "Zip"

Default is "None"

-- -
### Interval
Interval in milliseconds after which data is sent to stream even if the buffer is not full

**Type**: Integer

Optional, if not set only BatchSize is used

---
### Region
AWS Region for Kinesis service

**Type**: String

---
### StreamName
Name of the Kinesis stream

**Type**: String

### AwsKinesisTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsKinesisTargetConfiguration",
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
          "description": "Size of the batch for Kinesis messages"
        },
        "Compression": {
          "type": "string",
          "description": "Compression type for messages",
          "enum": ["None", "GZIP"]
        },
        "CredentialProviderClient": {
          "type": "string",
          "description": "The credential provider client name"
        },
        "Interval": {
          "type": "integer",
          "description": "Interval in milliseconds between batch publishes"
        },
        "Region": {
          "type": "string",
          "description": "AWS region for Kinesis"
        },
        "StreamName": {
          "type": "string",
          "description": "Name of the Kinesis stream"
        }
      },
      "required": ["StreamName"]
    }
  ]
}

```

### AwsKinesisTargetConfiguration Examples

Configuration using CredentialProviderClient.

```json
{
  "TargetType" : "AWS-KINESIS",  
  "StreamName": "data-stream",
  "Region": "us-east-1",
  "Compression": "GZip",
  "CredentialProviderClient": "aws-credentials-provider"
}

```



Configuration using  default AWS SDK credential provider chain.

```json
{
  "TargetType" : "AWS-KINESIS",    
  "StreamName": "data-stream",
  "Region": "us-east-1"
  "Compression": "ZIP"
}

```





[^top](#aws-kinesis-target)

