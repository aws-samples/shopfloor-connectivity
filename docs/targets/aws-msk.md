# AWS MSK Target
<br>
<p>AwsMskTargetConfiguration extends the type TargetConfiguration with specific configuration data for connecting to and sending to an AWS MSK topic. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-MSK"</strong>
<br>
<br>

[Targets](./targets.md)

## AwsMskTargetConfiguration

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 17%" />
<col style="width: 13%" />
<col style="width: 55%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>

</tr>
<tr class="even">
<td>TopicName</td>
<td>Name of the MSK topic</td>
<td>String</td>
<td></td>

</tr>
<tr class="odd">
<td>BootstrapBrokers</td>
<td>Addresses with port number for bootstrap brokers for AWS MSK cluster. (bootstrap.servers)</td>
<td>List[String]</td>
<td><p>To get the broker addresses for a cluster use the CLI command <br />
aws kafka get-bootstrap-brokers --cluster-arn `ClusterArn` and use the addresses returned in `"BootstrapBrokerStringPublicSaslIam"'.</p>
<p><a href="https://docs.aws.amazon.com/msk/latest/developerguide/msk-get-bootstrap-brokers.html">Getting the bootstrap brokers for an Amazon MSK cluster"</a></p></td>

</tr>
<tr class="even">
<td>Key</td>
<td>Key used for the written records</td>
<td>String</td>
<td>Optional</td>

</tr>
<tr class="odd">
<td>Partition</td>
<td>Optional partition key</td>
<td>Integer</td>
<td></td>

</tr>
<tr class="even">
<td>Serialization</td>
<td>Serialization <a href="https://kafka.apache.org/documentation/#producerconfigs_value.serializer">(value.serializer)</a></td>
<td>String</td>
<td>

- "json" (default)
- "protobuf", <a href="../../../core/sfc-ipc/src/main/proto/TargetAdapterService.proto">see protobuf TargetAdapterService schema</a>

If a <a href="../core/target-configuration.md">Template</a> is specified to transform the data for this target then this setting is not used
and the transformation output is written as a string to the topic.</td>

</tr>
<tr class="odd">
<td>Acknowledgements</td>
<td>Acknowledgements <a href="https://kafka.apache.org/documentation/#producerconfigs_acks">(acks)</a></td>
<td>String</td>
<td>

- "all" = 0
- "leader" = 1 (default)
- "all" = -1

</td>

</tr>
<tr class="even">
<td>ProviderProperties</td>
<td>Map of provider properties used to create the Kafka producer</td>
<td>Map[String,String]</td>
<td>Default is an empty map

A description af producer options can be found in the <a href="https://kafka.apache.org/documentation/#producerconfigs">
Kafka documentation</a>

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

</td>

</tr>
<tr class="odd">
<td>Headers</td>
<td>Map of headers set for written records</td>
<td>Map[String,String]</td>
<td>Default = empty map</td>

</tr>
<tr class="even">
<td>Compression</td>
<td>Compression type <a href="https://kafka.apache.org/documentation/#producerconfigs_compression.type">(compression.type)</a></td>
<td>String</td>
<td>Possible values:

- none (default)
- snappy
- lz4
- gzip
- zstd

</td>

</tr>
<tr class="odd">
<td>BatchSize</td>
<td>Batch size <a href="https://kafka.apache.org/documentation/#producerconfigs_batch.size">(batch.size)</a></td>
<td>Integer</td>
<td></td>

</tr>
<tr class="even">
<td>Interval</td>
<td>Interval in milliseconds in which adapter will flush the producer even when the batch size is not reached.</td>
<td>Integer</td>
<td></td>

</tr>
</tbody>
</table>

[^top](#aws-msk-target)
