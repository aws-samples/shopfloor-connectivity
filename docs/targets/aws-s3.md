# AWS S3 Target



## Aws3TargetConfiguration

AwsS3TargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to an S3 bucket. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-S3"</strong>

<p>Requires IAM permission s3:putObject to write to the configured bucket</p>


**Properties:**
- [BucketName](#BucketName)
- [BufferSize](#BufferSize)
- [Compression](#Compression)
- [ContentType](#ContentType)
- [Interval](#Interval)
- [Prefix](#Prefix)
- [Region](#Region)

---
### BucketName
Name of the bucket to write to

**Type**: String

---
### BufferSize
Size in MB that triggers writing data to an S3 Object

**Type**: Integer

Default is 1, maximum is 128

---
### Compression
Compression used to compress the data in the S3 object

**Type**: "None" | "GZip" | "Zip"

Default is "None"

---
### ContentType
Content Type Mime type of S3 object

**Type**: String

When using compression, the value is set to the mime type of the used compression method. The main purpose of this setting is to explicitly set the value in case the data is transformed into a specific format, e.g. xml, yaml

---
### Interval
Interval in seconds that triggers writing data to an S3 Object

**Type**: Integer

Default is 60, maximum is 900

---
### Prefix
S3 key objects prefix

**Type**: 

optional

---
### Region
AWS Region for S3 Bucket

**Type**: String

[^top](#aws-s3-target)

