# AWS SNS Target



## AwsSnsTargetConfiguration

AwsSnsTargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to an SNS topic queue. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-SNS"</strong>

<p>Requires IAM permission sqs:putMessage for the receiving topic.



**Properties:**

- [BatchSize](#BatchSize)
- [Compression](#Compression)
- [Interval](#Interval)
- [MessageGroupId](#MessageGroupId)
- [Region](#Region)
- [SerialAsMessageDeduplicationId](#SerialAsMessageDeduplicationId)
- [Subject](#Subject)
- [TopicArn](#TopicArn)

---
### BatchSize
Number of output messages to combine in a publishBatch call. The data will be written to the queue before the batch size is reached if the maximum payload size will be exceeded.

**Type**: Integer

Default is 10, maximum is 10

---
### Compression
Compression used to compress message.
The data in the messages is wrapped in structure with the following fields:
- "compression" : Used compression
- "payload": Compressed data as a base64 encoded string.
When using compression for the message verify if actual compression out weights the overhead of the base64 encoded of the compressed data.

**Type**: "None" | "GZip" | "Zip"

Default is "None"

---
### Interval
Interval in milliseconds after which data is sent to stream even if the buffer is not full

**Type**: Integer

Optional, if not set only BatchSize is used

---
### MessageGroupId
This parameter applies only to FIFO (first-in-first-out) topics.
The tag that specifies that a message belongs to a specific message group

**Type**: String

Optional

---
### Region
AWS Region for SNS service

**Type**: String

---
### SerialAsMessageDeduplicationId
Used the unique serial number of the SFC MessageDeduplicationId, if set to false ContentBasedDeduplication is used

**Type**: Boolean

true

---
### Subject
Topic message subject

**Type**: String

Optional

---
### TopicArn
Arn of the receiving topic

**Type**: String

[^top](#aws-sns-target)

