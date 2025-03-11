# AWS IoT Analytics Target

The Shop Floor Connectivity for  [AWS IoT Analytics](https://aws.amazon.com/iot-analytics/) Target adapter enables you to send industrial device data directly to AWS IoT Analytics. This adapter receives data from the SFC Core and forwards it to AWS IoT Analytics channels, where the data can be processed, stored, and analyzed using AWS IoT Analytics' capabilities for IoT data analytics, machine learning, and visualization. 

**AWS IoT Analytics will no longer accept new customers starting July 25, 2024. While existing customers can continue using this service .**

https://aws.amazon.com/blogs/iot/unlocking-scalable-iot-analytics-on-aws/



In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWS-IOT-ANALYTICS": {
      "JarFiles" : ["<location of deployment>/aws-iot-analytics-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.awsiota.AwsIotAnalyticsTargetWriter"
   }
}
```



## AwsIotAnalyticsTargetConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

AwsIotAnalyticsTargetConfiguration is a configuration class for the AWS IoT Analytics target adapter that defines settings for connecting to and sending data to AWS IoT Analytics channels. AwsIotAnalyticsTargetConfiguration extends the types [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to an IoT Analytics channel. 

The [Targets](../core/sfc-configuration.md#targets) configuration element contain entries of this type, the TargetType of these entries must be set to **"AWS-IOT-ANALYTICS"**

Requires IAM permission `iotanalytics:BatchPutMessage` to write to the configured channel

- [Schema](#awsiotanalyticstargetconfiguration-schema)
- [Examples](#awsiotanalyticstargetconfiguration-examples)

**Properties:**

- [BatchSize](#batchsize)
- [ChannelName](#channelname)
- [CredentialProviderClient](#credentialproviderclient)
- [Region](#region)
- [Template](#template)

---
### BatchSize
The BatchSize property specifies how many output messages should be combined into a single BatchPutMessage API call to AWS IoT Analytics. It can be set from 1 to 10 messages, with a default value of 10. Batching messages can improve throughput and reduce API calls.

**Type**: Integer

Default is 10

---
### ChannelName
The ChannelName property specifies the name of the AWS IoT Analytics channel where the data will be sent. This must be the name of an existing channel in your AWS IoT Analytics configuration. The channel serves as the entry point for your IoT data, collecting and archiving the raw, unprocessed messages before they are published to a pipeline for further processing.

**Type**: String

---
### CredentialProviderClient

The CredentialProviderClient property specifies which AWS credential provider client to use for authentication. It references a client defined in the SFC's top-level configuration under [AwsIotCredentialProviderClients](../core/sfc-configuration.md#awsiotcredentialproviderclients) section. This client uses X.509 certificates to obtain temporary AWS credentials through the  [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---

### 

### Region

The Region property specifies the AWS region where the IoT Analytics channel is located (e.g., us-east-1, eu-west-1). This must match the region where your IoT Analytics channel was created, as IoT Analytics resources are region-specific. If not specified, the adapter will use the default region from your AWS configuration.

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

**Type**: String

---

### AwsIotAnalyticsTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsIotAnalyticsTargetConfiguration",
  "type": "object",
  "allOf": [
    {
      "$ref": "#/definitions/TargetConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "BatchSize": {
          "type": "integer",
          "description": "Size of the batch for IoT Analytics messages",
          "minimum": 1
        },
        "ChannelName": {
          "type": "string",
          "description": "Name of the IoT Analytics channel"
        },
        "Region": {
          "type": "string",
          "description": "AWS region for the IoT Analytics channel"
        }
      },
      "required": ["ChannelName"]
    }
  ]
}

```

### AwsIotAnalyticsTargetConfiguration Examples

Configuration using CredentialProviderClient

```json
{
  "TargetType" : "AWS-IOT-ANALYTICS"
  "ChannelName": "production-data-channel",
  "Region": "us-east-1",
  "BatchSize": 50,
  "CredentialProviderClient": "aws-credentials-provider"
}

```

Configuration using  default AWS SDK credential provider chain.

```json
{
  "ChannelName": "production-data-channel",
  "Region": "us-east-1",
  "BatchSize": 50
}

```

[^top](#aws-iot-analytics-target)

