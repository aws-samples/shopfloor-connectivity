# AWS Kinesis Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

The Amazon [Kinesis](https://aws.amazon.com/kinesis/) target connector for Shop Floor Connectivity (SFC) enables streaming of industrial device data directly to Amazon Kinesis Data Streams. It provides configurable compression, batching , template based data transformations and delivery of device data to Kinesis streams for real-time processing and analytics.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWS-KINESIS": {
      "JarFiles" : ["<location of deployment>/aws-kinesis-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.awskinesis.AwsKinesisTargetWriter"
   }
}
```



## AwsKinesisTargetConfiguration

AwsKinesisTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending to a stream for the AWS Kinesis service. The [Targets](../core/sfc-configuration.md#targets) configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-KINESIS"**


Requires IAM permission `kinesis:PutRecords` for the stream the data is sent to.

- [Schema](#awskinesistargetconfiguration-schema)
- [Examples](#awskinesistargetconfiguration-examples)

**Properties:**
- [BatchSize](#batchsize)
- [Compression](#compression)
- [CredentialProviderClient](#credentialproviderclient)
- [Interval](#interval)
- [Region](#region)
- [StreamName](#streamname)

---
### BatchSize
The BatchSize property determines how many messages to accumulate before sending them in a single putRecordBatch API call to Amazon Kinesis. The default value is 10 messages, and there is a hard limit of 500 messages per batch as per Kinesis service limits. This batching mechanism helps optimize throughput and reduce API calls by grouping multiple records into a single request.

**Type**: Integer

Default is 10, Maximum is 500

---

### CredentialProviderClient

The CredentialProviderClient property specifies which AWS credential provider client to use for authentication. It references a client defined in the SFC's top-level configuration under [AwsIotCredentialProviderClients](../core/sfc-configuration.md#awsiotcredentialproviderclients) section. This client uses X.509 certificates to obtain temporary AWS credentials through the  [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---
### Compression
The Compression property specifies the compression algorithm to use when sending data to Kinesis. It accepts three possible values:

- "None": No compression is applied (default)
- "GZip": Uses GZIP compression to reduce data size 
- "Zip": Uses ZIP compression to reduce data size 

Using compression can help reduce bandwidth usage and costs, especially when sending large volumes of data, though it adds some processing overhead.

**Type**: String

Default is "None"

-- -
### Interval
The Interval property defines a time-based trigger (in milliseconds) for sending data to the Kinesis stream, even if the [BatchSize](#batchsize) hasn't been reached. When specified, the adapter will flush the buffer and send data either when the [BatchSize](#batchsize) is reached OR when this time interval has elapsed, whichever comes first. This ensures data freshness by preventing messages from sitting in the buffer for too long while waiting for the batch to fill up.

**Type**: Integer

Optional, if not set only [BatchSize](#batchsize) is used

---
### Region
The Region property specifies the AWS Region identifier where your Kinesis data stream is located (e.g., "us-east-1", "eu-west-1", "ap-southeast-2"). This setting determines which regional endpoint will be used for sending data to your Kinesis stream. The region must be one where Amazon Kinesis service is available and your AWS account has access to it.

**Type**: String

---
### StreamName
The StreamName property specifies the name of the Amazon Kinesis data stream where records will be sent. This must be the name of an existing Kinesis stream in your AWS account within the specified Region. The stream name is case-sensitive and must be between 1 and 128 characters long, containing only alphanumeric characters, hyphens, underscores, and periods.

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

