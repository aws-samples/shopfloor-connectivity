# AWS Kinesis Target



## AwsKinesisTargetConfiguration



AwsKinesisTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending to a stream for the AWS Kinesis service. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-KINESIS"**</p>

Requires IAM permission kinesis:PutRecords for the stream the data is sent to.




**Properties:**
- [BatchSize](#BatchSize)
- [Compression](#Compression)
- [Interval](#Interval)

- [Region](#Region)
- [StreamName](#StreamName)

---
### BatchSize
Number of output messages to combine in a single putRecordBatch API call.

**Type**: Integer

Default is 10, Maximum is 500

---
### Compression
Compression used to compress the data in the submitted items

**Type**: "None" | "GZip" | "Zip"

Default is "None"

---
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

[^top](#AWS Kinesis Target)

