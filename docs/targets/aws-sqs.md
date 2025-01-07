# AWS SQS Target



## AwsSqsTargetConfiguration

AwsSqsTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to an SQS queue. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-SQS"**

Requires IAM permission sqs:SendMessageBatch for the receiving queue.



**Properties:**

- [BatchSize](#BatchSize)
- [Compression](#Compression)
- [Interval](#Interval)

- [QueueUrl](#QueueUrl)
- [Region](#Region)

---
### BatchSize
Number of output messages to combine in a sendMessageBatch. The data will be written to the queue before the batch size is reached if the maximum payload size will be exceeded.

**Type**: Integer

Default is 10, maximum is 10

---
### Compression
Compression used to compress message payload.
The data in the messages is wrapped in structure with the following fields:
- "compression" : Used compression
- "payload": Compressed data as a base64 encoded string.
When using compression for the message verify if actual compression out weights the overhead of the base64 encoded of the compressed data.

**Type**: "None" | "GZip" | "Zip"

Default is "None"

---
### Interval
Interval in milliseconds after which data is sent to queue even if the buffer is not full

**Type**: Integer

Optional, if not set only BatchSize is used


---
### QueueUrl
Url of the receiving queue

**Type**: String

---
### Region
AWS Region for SQS service

**Type**: String

[^top](#aws-sqs-target)

