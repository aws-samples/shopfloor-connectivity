# AWS MSK Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

The AWS [MSK](https://aws.amazon.com/msk/) (Amazon Managed Streaming for Apache Kafka) target adapter for Shop Floor Connectivity enables data streaming from industrial devices directly to Amazon MSK clusters. This adapter transforms collected device data into the required format and publishes it to specified Kafka topics in your MSK cluster. The adapter supports configurable batching, compression,data transformations using Apache Velocity templates and handles the authentication and connection management to your MSK clusters.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWS-MSK": {
      "JarFiles" : ["<location of deployment>/aws-msk-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.msk.AwsMskTargetWriter"
   }
}
```



## AwsMskTargetConfiguration

AwsMskTargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for connecting to and sending to an AWS MSK topic. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-MSK"**

Required IAM permissions are `kafka-cluster:WriteDataIdempotently`, `kafka-cluster:CreateTopic`, `kafka-cluster:DescribeTopic` `,kafka-cluster:Connect`, `kafka-cluster:WriteData,`

- [Schema](#awsmsktargetconfiguration-schema)
- [Examples](#awsmsktargetconfiguration-examples)

**Properties:**
- [Acknowledgements](#acknowledgements)
- [BatchSize](#batchsize)
- [BootstrapBrokers](#bootstrapbrokers)
- [Compression](#compression)
- [CredentialProviderClient](#credentialproviderclient)
- [Endpoint](#endpoint)
- [Formatter](#formatter)
- [Headers](#headers)
- [Interval](#interval)
- [Key](#key)
- [Partition](#partition)
- [ProviderProperties](#providerproperties)
- [Serialization](#serialization)
- [Template](#template)
- [TopicName](#topicname)

---
### Acknowledgements
Acknowledgements (acks) controls the durability and reliability of message delivery to the Kafka cluster.

**Type** : String

- "None" = 0: No acknowledgement required - fastest but may lose data
- "leader" = 1 (default): Leader acknowledgement only - balanced between durability and performance
- "all" = -1: All replicas must acknowledge - highest durability but slower performance



---
### BatchSize
Batch size (batch.size)

Number of records to accumulate before sending to MSK cluster. Larger batch sizes can improve throughput and reduce network overhead, but increase latency and memory usage.

**Type**: Integer

---
### BootstrapBrokers
Addresses with port number for bootstrap brokers for AWS MSK cluster. (bootstrap.servers)

**Type**: List[String]

To get the broker addresses for a cluster use the CLI command 
```console
aws kafka get-bootstrap-brokers --cluster-arn ClusterArn
```
and use the addresses returned in "BootstrapBrokerStringPublicSaslIam".

See also 
[Getting the bootstrap brokers for an Amazon MSK cluster](https://docs.aws.amazon.com/msk/latest/developerguide/msk-get-bootstrap-brokers.html)

---
### Compression
Compression type (compression.type)

Specifies the compression algorithm used for data sent to the MSK cluster. Compression reduces network bandwidth usage and storage at the cost of some CPU overhead.

Possible values:

- "none" (default): No compression
- "snappy" : Fast compression/decompression with good compression ratio
- "lz4" : Very fast compression/decompression
- "gzip" : High compression ratio but more CPU intensive
- "zstd" : High compression ratio with good performance

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

### Headers

Map of headers set for written records.

Allows setting custom key-value pairs as Kafka message headers. These headers are metadata that will be attached to each record written to the MSK cluster. Headers can be used for message filtering, routing, or carrying additional metadata alongside the message payload.

**Type**: Map[String,String]

Default = empty map

---
### Interval
Interval in milliseconds in which adapter will flush the producer even when the batch size is not reached.

Controls how long the producer will wait to accumulate messages before sending them to MSK, even if the [batch size](#batchsize) has not been reached. This ensures messages are sent within a reasonable timeframe during periods of low message volume. A lower interval reduces latency but may decrease throughput.

**Type**: Integer

---
### Key
Optional key used for the written records.

Specifies the key that will be attached to all messages written to MSK. The key is used by Kafka for message partitioning and maintaining message order within partitions. When not specified, messages will be distributed across partitions in a round-robin manner.

**Type**: String

---
### Partition
Optional partition key

Specifies the target partition number in the Kafka topic where messages will be written. When specified, all messages will be sent to this specific partition. If not specified, Kafka will distribute messages across available partitions based on the message key (if provided) or using its default partitioning strategy.

**Type**: Integer

---
### ProviderProperties
Map of provider properties used to create the Kafka producer

Additional configuration properties for the Kafka producer client. These properties will be passed directly to the underlying Kafka producer instance.

**Type**: Map[String,String]

Default is an empty map

A description af producer options can be found in the Kafka documentation

The following properties are set by the adapter

- bootstrap.servers from `BootstrapBrokers`
- client.id = "sfc-msk-target_" + hostname
- security.protocol = "SASL_SSL"
- acks from `Acknowledgements`
- compression.type from `Compression`
- key.serializer = "org.apache.kafka.common.serialization.StringSerializer"
- value.serializer = "org.apache.kafka.common.serialization.ByteArraySerializer"
- sasl.client.callback.handler.class = "software.amazon.msk.auth.iam.IAMClientCallbackHandler"
- sasl.jaas.config =  "software.amazon.msk.auth.iam.IAMLoginModule required;"
- sasl.mechanism = "AWS_MSK_IAM"
- batch.size from `BatchSize`

Any additional valid Kafka producer properties can be specified in this map to customize the producer behavior.



---
### Serialization
Serialization (value.serializer)

Specifies the format used to serialize message values before sending them to MSK.

Supported values:

- "json" (default): Messages are serialized as JSON format
- "protobuf": Messages are serialized using Protocol Buffers format. When using this option, the message structure must conform to the protobuf schema defined in the [TargetAdapterService schema](../../core/sfc-ipc/src/main/proto/TargetAdapterService.proto) 

If a Template is specified to transform the data for this target then this setting is not used and the transformation output is written as a string to the topic.

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
### TopicName
Specifies the name of the Kafka topic in the MSK cluster where messages will be written. The topic must exist in the MSK cluster before messages can be written to it. Topic names must be between 1 and 255 characters in length and can contain alphanumeric characters, dots (.), underscores (_), and hyphens (-).

**Type**: String

### AwsMskTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsMskTargetConfiguration",
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
        "Acknowledgements": {
          "type": "string",
          "description": "Acknowledgement level for messages",
          "enum": ["all", "one", "leader"]
        },
        "BatchSize": {
          "type": "integer",
          "description": "Size of the batch for MSK messages"
        },
        "BootstrapBrokers": {
          "type": "array",
          "description": "List of bootstrap broker addresses",
          "items": {
            "type": "string"
          },
          "minItems": 1
        },
        "Compression": {
          "type": "string",
          "description": "Compression type for messages",
          "enum": ["none", "snappy", "lz4", "gzip", "zstd"],
          "default": "none"
        },
        "CredentialProviderClient": {
          "type": "string",
          "description": "The credential provider client name"
        },
        "Headers": {
          "type": "object",
          "description": "Message headers"
        },
        "Interval": {
          "type": "integer",
          "description": "Interval in milliseconds between batch publishes"
        },
        "Key": {
          "type": "string",
          "description": "Message key"
        },
        "Partition": {
          "type": "integer",
          "description": "Partition number"
        },
        "ProviderProperties": {
          "type": "object",
          "description": "Provider specific properties",
          "additionalProperties": {
            "type": "string"
          }
        },
        "Serialization": {
          "type": "string",
          "description": "Message serialization format",
          "enum": ["json", "protobuf"],
          "default" : "json"
        },
        "TopicName": {
          "type": "string",
          "description": "Name of the MSK topic"
        }
      },
      "required": ["TopicName", "BootstrapBrokers"]
    }
  ]
}

```

### AwsMskTargetConfiguration Examples



```json
{
  "TargetType" : "AWS-MSK",    
  "TopicName": "data-topic",
  "BootstrapBrokers": ["broker1.example.com:9092"],
  "Compression": "gzip",
  "Acknowledgements": "all",
  "Serialization": "json",
  "CredentialProviderClient": "aws-credentials-provider"
}

```

[^top](#aws-msk-target)

