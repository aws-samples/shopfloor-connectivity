
# AWS Kinesis Firehose Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

The Amazon [Kinesis Firehose](https://aws.amazon.com/firehose/) Target adapter for Shop Floor Connectivity facilitates data streaming from industrial devices to Amazon Kinesis Data Firehose. This adapter collects and transmits data to Kinesis Firehose delivery streams. The adapter supports batching and template-based transformations.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWS-IOT-FIREHOSE": {
      "JarFiles" : ["<location of deployment>/aws-kinesis-firehose-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.awsfirehose.AwsFirehoseTargetWriter"
   }
}
```



## AwsKinesisFirehoseTargetConfiguration

AwsKinesisFirehoseTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending to a delivery stream for the AWS Kinesis Firehose service. The [Targets](../core/sfc-configuration.md#targets) configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-FIREHOSE"**

Requires IAM permission `firehose:PutRecordBatch` for the delivery stream the data is sent to.

- [Schema](#awskinesisfirehosetargetconfiguration-schema)
- [Examples](#awskinesisfirehosetargetconfiguration-examples)

**Properties:**
- [BatchSize](#batchsize)
- [CredentialProviderClient](#credentialproviderclient)
- [Region](#region)
- [StreamName](#streamname)

---
### BatchSize
The BatchSize property specifies the maximum number of messages to accumulate before sending them in a single putRecordBatch API call to Kinesis Firehose. The default value is 10 messages per batch. If adding another message would cause the batch to exceed Firehose's maximum request size limit, the adapter will automatically send the current batch before the BatchSize limit is reached. This batching mechanism helps optimize network usage and reduce API calls while ensuring compliance with Firehose's size constraints

**Type**: Integer

Default is 10

---
### CredentialProviderClient

The CredentialProviderClient property specifies which AWS credential provider client to use for authentication. It references a client defined in the SFC's top-level configuration under [AwsIotCredentialProviderClients](../core/sfc-configuration.md#awsiotcredentialproviderclients) section. This client uses X.509 certificates to obtain temporary AWS credentials through the  [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---

### Region
The Region property specifies the AWS Region identifier where the Kinesis Firehose delivery stream is located (e.g., "us-east-1", "eu-west-1", "ca-west-1"). This setting determines which regional endpoint will be used for sending data to your Firehose delivery stream. The region must be one where Kinesis Firehose service is available and your AWS account has access.

**Type**: String

---
### StreamName
The StreamName property specifies the name of the Kinesis Firehose delivery stream where data will be sent. This is the unique identifier of an existing Firehose delivery stream in your AWS account that will receive and process the data. The stream name must match an active delivery stream that has been previously created in the specified AWS Region.

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

