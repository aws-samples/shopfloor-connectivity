# AWS IoT Core Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

The [AWS IoT Core](https://aws.amazon.com/iot-core/) target adapter facilitates secure transmission of industrial data to the AWS IoT Core service via service API calls. It obtains temporary credentials using X.509 certificates or configured AWS credentials for authenticating service publish calls. The adapter supports batching of messages for efficient transmission, data compression to reduce bandwidth, payload transformation using templates, and dynamic topic names generated from target configuration and source metadata.



In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWS-IOT-CORE": {
      "JarFiles" : ["<location of deployment>/aws-iot-core-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.awsiotcore.AwsIotCoreTargetWriter"
   }
}
```



## 

## AwsIotCoreTargetConfiguration

The AwsIotCoreTargetConfiguration class extends  [TargetConfiguration](../core/target-configuration.md) with specific settings for publishing data to AWS IoT Core topics using the HTTP dataplane API. When used in the [Targets](../core/sfc-configuration.md#targets) configuration, entries must specify the TargetType as **"AWS-IOT-CORE"**. 

Requires IAM permissions `iot:Connect`, `iot:DescribeEndpoint`, `iot:Publish` for the topic the data is published to and `iot:RetainPublish` if the [Retain](#retain) option is used.

- [Schema](#awsiotcoretargetconfiguration-schema)
- [Examples](#awsiotcoretargetconfiguration-examples)

**Properties:**

- [AlternateTopicName](#alternatetopicname)
- [BatchCount](#batchcount)
- [BatchInterval](#batchinterval)
- [BatchSize](#batchsize)
- [Compression](#compression)
- [CredentialProviderClient](#credentialproviderclient)
- [Endpoint](#endpoint)
- [Formatter](#formatter)
- [Region](#region)
- [Retain](#retain)
- [Template](#template)
- [TopicName](#topicname)
- [WarnAlternateTopicName](#warnalternatetopicname)

---
### AlternateTopicName
The AlternateTopicName property specifies a fallback topic name or template that is used when the [TopicName](#topicname) contains template placeholders that cannot be resolved from the source metadata. This ensures messages are still published even when dynamic topic name generation fails

**Type**: String


---
### BatchCount
The BatchCount property specifies the maximum number of messages to accumulate before triggering a batch publish to AWS IoT Core. When this count is reached, all buffered messages are sent as a single array. This property works in conjunction with [BatchSize](#batchsize) and [BatchInterval](#batchinterval) - whichever threshold (message count, total size, or time interval) is reached first will trigger the batch transmission.

**Type**: Int

---
### BatchInterval
The BatchInterval property defines the maximum time in milliseconds that messages can be buffered before being published to AWS IoT Core, regardless of whether [BatchSize](#batchsize) or [BatchCount](#batchcount) limits have been reached. This ensures messages are sent even during periods of low message volume, maintaining data freshness. When the interval elapses, all currently buffered messages are published as a batch.

**Type**: Int


---
### BatchSize
The BatchSize property defines the maximum total payload size in kilobytes (KB) of buffered messages before triggering a batch publish to AWS IoT Core. This size is calculated based on the uncompressed message payloads. When the cumulative size of buffered messages reaches this limit, all messages are sent as a single batch. The property works alongside [BatchCount](#batchcount)  and [BatchInterval](#batchinterval)  - the first threshold reached (size, count, or time) triggers the batch transmission. 

**Type**: Int

---
### Compression
The Compression property specifies the compression algorithm to be applied to message payloads before publishing to AWS IoT Core. Supported compression methods are "None" (default, no compression), "Zip", or "GZip". Compression can help reduce bandwidth usage and costs when transmitting large payloads.

**Values:**

- "None" (Default)
- "Zip"
- "GZip"

**Type**: String


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

The Region property specifies the AWS Region identifier where the AWS IoT Core service endpoint is located (e.g., "us-east-1", "eu-west-1"). This setting determines which regional endpoint will be used for publishing messages. The region must be one where AWS IoT Core service is available and the AWS account has access.

**Type**: String

---
### Retain
The Retain property determines whether messages should be stored by AWS IoT Core as retained messages. When set to true (default is false), AWS IoT Core will store the most recent message for each topic and automatically deliver it to new subscribers when they subscribe to that topic. Due to lower service limits for retained messages, it's recommended to enable message buffering using BatchSize, BatchCount, or BatchInterval. 

The `iot:RetainPublish` IAM permission is required when this feature is enabled.

**Type**: Boolean

Default is false



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
### TopicName
The TopicName property defines the MQTT topic name or topic name template where messages will be published. It supports both static names and dynamic templates using placeholders. The available placeholders include:

- Environment variables: ${name}

- Built-in variables:

  - `%schedule%`

  - `%target%`

  - `%source%`

  - `%channel%`

- Metadata values:  Metadata values can be referenced using the name of metadata value, e.g.  `%metadata name%`

Example templates:

- `plant1-%source%` - Creates separate topics for each source
- `plant1-%line%` \- Groups messages by the line metadata value

Important considerations:

- If a placeholder cannot be resolved, the [AlternateTopicName](#alternatetopicname) will be used as fallback
- Using dynamic topics may increase publish calls and risk throttling - consider using message buffering
- AWS IoT Core has a limit of 8 topic levels (separated by forward slashes)

Name or name template of the topic

**Type**: String

---
### WarnAlternateTopicName
The WarnAlternateTopicName property is a boolean flag that controls whether a warning should be generated when messages are published to the [AlternateTopicName](#alternatetopicname) instead of the primary TopicName. When set to true, the system will log a warning whenever a message falls back to using the alternate topic, which typically occurs when placeholders in the primary TopicName template cannot be resolved

**Type**: Boolean


Default is true

### AwsIotCoreTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsIotCoreTargetConfiguration",
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
        "AlternateTopicName": {
          "type": "string",
          "description": "Alternate topic name to publish messages to when there arte unresolved placeholders"
        },
        "BatchCount": {
          "type": "integer",
          "description": "Number of messages to batch before publishing"
        },
        "BatchInterval": {
          "type": "integer",
          "description": "Interval in milliseconds between batch publishes"
        },
        "BatchSize": {
          "type": "integer",
          "description": "Maximum size of batched messages in bytes"
        },
        "Compression": {
          "type": "string",
          "description": "Compression type for messages",
          "enum": ["None", "Zip", "GZip"],
          "default" : "None"
        },
        "CredentialProviderClient": {
          "type": "string",
          "description": "The credential provider client name"
        },
        "Region": {
          "type": "string",
          "description": "AWS region for IoT Core"
        },
        "Retain": {
          "type": "boolean",
          "description": "Whether to retain messages"
        },
        "TopicName": {
          "type": "string",
          "description": "Main topic name to publish messages to"
        },
        "WarnAlternateTopicName": {
          "type": "boolean",
          "description": "Whether to warn when using alternate topic name"
        }
      },
      "required": ["TopicName"]
    }
  ]
}

```

### AwsIotCoreTargetConfiguration Examples

Configuration using CredentialProviderClient

```json
{
  "TargetType" : "AWS-IOT-CORE",
  "TopicName": "device/data",
  "Region": "us-east-1",
  "BatchSize": 1024,
  "BatchCount": 100,
  "BatchInterval": 5000,
  "Compression": "GZIP",
  "Retain": true,
  "CredentialProviderClient": "aws-credentials-provider"
}
```

Configuration using dynamic topic name based on target- (%source%) and metadata (%line%, %plant%)

```json
{
  "TargetType" : "AWS-IOT-CORE",
  "TopicName": "sensordata/%plant%/%line%/%source%",
  "Region": "us-east-1",
  "Compression": "ZIP",
  "Retain": true,
  "CredentialProviderClient": "aws-credentials-provider"
}
```

Configuration using  default AWS SDK credential provider chain.

```json
{
  "TargetType" : "AWS-IOT-CORE",
  "TopicName": "device/data",
  "Region": "us-east-1",
  "Compression": "GZIP",
  "Retain": true,
}
```



[^top](#aws-iot-core-target)



