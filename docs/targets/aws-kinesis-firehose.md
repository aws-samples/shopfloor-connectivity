
# AWS Kinesis Firehose Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md)

The Amazon [Kinesis Firehose](https://aws.amazon.com/firehose/) Target adapter for Shop Floor Connectivity facilitates data streaming from industrial devices to Amazon Kinesis Data Firehose. This adapter collects and transmits data to Kinesis Firehose delivery streams. The adapter supports batching and template-based transformations.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWS-FIREHOSE": {
      "JarFiles" : ["<location of deployment>/aws-kinesis-firehose-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.awsfirehose.AwsKinesisFirehoseTargetWriter"
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
- [Endpoint](#endpoint)
- [Formatter](#formatter)
- [Region](#region)
- [StreamName](#streamname)
- [Template](#template)

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

### Endpoint

The EndPoint property specifies the VPC endpoint URL used to access AWS services privately through AWS PrivateLink without requiring an internet gateway or NAT device. When not specified, the service's default public endpoint for the configured region will be used.

https://docs.aws.amazon.com/vpc/latest/privatelink/aws-services-privatelink-support.html

**Type:** String

---

### Formatter

Configuration allows for custom formatting of data written by a target. A [custom formatter](../sfc-extending.md#custom-formatters), implemented as a JVM class, converts a sequence of target data messages into a specific format and returns the formatted data as an array of bytes.

When a formatter is used, a [template](#template) configured for that target is ignored.

**Type:** [InProcessConfiguration](../core/in-process-configuration.md)

---

### Region
The Region property specifies the AWS Region identifier where the Kinesis Firehose delivery stream is located (e.g., "us-east-1", "eu-west-1", "ca-west-1"). This setting determines which regional endpoint will be used for sending data to your Firehose delivery stream. The region must be one where Kinesis Firehose service is available and your AWS account has access.

**Type**: String

---
### StreamName
The StreamName property specifies the name of the Kinesis Firehose delivery stream where data will be sent. This is the unique identifier of an existing Firehose delivery stream in your AWS account that will receive and process the data. The stream name must match an active delivery stream that has been previously created in the specified AWS Region.

**Type**: String



---

### Template

Specifies the file path to an [Apache velocity](https://velocity.apache.org/)  template used for  [transforming the output data](../sfc-target-templates.md) target output data. This optional setting enables custom formatting of data before it is sent to the target. Available context variables include:

- $schedule
- $sources
- $metadata
- $serial
- $timestamp
- names specified in ElementNames configuration
- $tab (for inserting tab characters)

Pathname to file containing an [Apache velocity](https://velocity.apache.org/) template that can be applied to [transform the output data](../sfc-target-templates.md) of the target.

The following [Velocity tools](https://velocity.apache.org/tools/3.1/tools-summary.html) can be used in the transformation template:

- $datetool
- $collection
- $context
- $math
- $number

Additional epoch timestamp values can be added to the data used for the transformation by setting the [TemplateEpochTimestamp](../core/target-configuration.md#templateepochtimestamp) property to true,

For targets where the data does not require specific output format, the data is serialized as [JSON data](../sfc-data-format.md#sfc-output-data-schemas).

When a custom [formatter](#formatter) is configured for a target then this property is ignored.

**Type**: String

---

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

