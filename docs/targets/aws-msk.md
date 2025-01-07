# AWS MSK Target



## AwsMskTargetConfiguration



AwsMskTargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for connecting to and sending to an AWS MSK topic. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-MSK"</strong>


**Properties:**
- [Acknowledgements](#Acknowledgements)
- [BatchSize](#BatchSize)
- [BootstrapBrokers](#BootstrapBrokers)
- [Compression](#Compression)
- [Headers](#Headers)
- [Interval](#Interval)
- [Key](#Key)
- [Partition](#Partition)
- [ProviderProperties](#ProviderProperties)
- [Serialization](#Serialization)
- [TopicName](#TopicName)

---
### Acknowledgements
Acknowledgements (acks)

**Type**: String



- "all" = 0
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

[^top](#aws-msk-target)

