# AWS MSK Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 



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
- [Headers](#headers)
- [Interval](#interval)
- [Key](#key)
- [Partition](#partition)
- [ProviderProperties](#providerproperties)
- [Serialization](#serialization)
- [TopicName](#topicname)

---
### Acknowledgements
Acknowledgements (acks)

**Type**: String

- "None" = 0
- "leader" = 1 (default)
- "all" = -1



---
### BatchSize
Batch size (batch.size)

**Type**: Integer

---
### BootstrapBrokers
Addresses with port number for bootstrap brokers for AWS MSK cluster. (bootstrap.servers)

**Type**: List[String]

To get the broker addresses for a cluster use the CLI command 
```console
aws kafka get-bootstrap-brokers --cluster-arn `ClusterArn`
```
and use the addresses returned in "BootstrapBrokerStringPublicSaslIam".

See also 
[Getting the bootstrap brokers for an Amazon MSK cluster](https://docs.aws.amazon.com/msk/latest/developerguide/msk-get-bootstrap-brokers.html)

---
### Compression
Compression type (compression.type)

**Type**: String

Possible values:

- none (default)
- snappy
- lz4
- gzip
- zstd



---
### CredentialProviderClient

Name of the AWS credential provider client defined in the SFC top level configuration section [AwsIotCredentialProviderClients]
(../core/sfc-top-level-config.md#AwsIotCredentialProviderClients) obtaining credentials using X509 certificates from the [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---

### Headers
Map of headers set for written records

**Type**: Map[String,String]

Default = empty map

---
### Interval
Interval in milliseconds in which adapter will flush the producer even when the batch size is not reached.

**Type**: Integer

---
### Key
Key used for the written records

**Type**: String

Optional

---
### Partition
Optional partition key

**Type**: Integer

---
### ProviderProperties
Map of provider properties used to create the Kafka producer

**Type**: Map[String,String]

Default is an empty map

A description af producer options can be found in the 
Kafka documentation

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



---
### Serialization
Serialization (value.serializer)

**Type**: String

- "json" (default)
- "protobuf", see protobuf TargetAdapterService schema

If a Template is specified to transform the data for this target then this setting is not used
and the transformation output is written as a string to the topic.

---
### TopicName
Name of the MSK topic

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

