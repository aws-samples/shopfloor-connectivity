
# AWS Kinesis Firehose Target



## AwsKinesisFirehoseTargetConfiguration

AwsKinesisFirehoseTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending to a delivery stream for the AWS Kinesis Firehose service. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-FIREHOSE"**

Requires IAM permission firehose:PutRecordBatch for the delivery stream the data is sent to.


**Properties:**
- [BatchSize](#BatchSize)
- [Region](#Region)
- [StreamName](#StreamName)

---
### BatchSize
Number of output messages to combine in a single putRecordBatch API call.
The target will send buffered data before the batch size is reached if the entire size of the message will exceed the maximum size for a single request.

**Type**: Integer

Default is 10

---
### Region
AWS Region for Kinesis Firehose service

**Type**: String

---
### StreamName
Name of the delivery stream

**Type**: String

[^top](#aws-kinesis-firehose-target)

