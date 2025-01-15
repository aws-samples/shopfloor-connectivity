# AWS S3 Target




## Aws3TargetConfiguration

AwsS3TargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to an S3 bucket. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-S3"**


Requires IAM permission `s3:putObject` to write to the configured bucket

- [Schema](#Aws3TargetConfiguration-Schema)
- [Examples](#Aws3TargetConfiguration-Examples)

**Properties:**
- [BucketName](#BucketName)
- [BufferSize](#BufferSize)
- [Compression](#Compression)
- [ContentType](#ContentType)
- [CredentialProviderClient](#CredentialProviderClient)
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
### CredentialProviderClient

Name of the AWS credential provider client defined in the SFC top level configuration section [AwsIotCredentialProviderClients]
(../core/sfc-top-level-config.md#AwsIotCredentialProviderClients) obtaining credentials using X509 certificates from the [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

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

### Aws3TargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Aws3TargetConfiguration",
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
        "BucketName": {
          "type": "string",
          "description": "Name of the S3 bucket"
        },
        "BufferSize": {
          "type": "integer",
          "description": "Size of the buffer for S3 uploads"
        },
        "Compression": {
          "type": "string",
          "description": "Compression type for objects",
          "enum": ["None", "Zip", "GZip"],
          "default": "None"
        },
        "ContentType": {
          "type": "string",
          "description": "Content type of the S3 objects"
        },
        "CredentialProviderClient": {
          "type": "string",
          "description": "The credential provider client name"
        },
        "Interval": {
          "type": "integer",
          "description": "Interval in seconds between uploads"
        },
        "Prefix": {
          "type": "string",
          "description": "Prefix for S3 object keys"
        },
        "Region": {
          "type": "string",
          "description": "AWS region for S3"
        }
      },
      "required": ["BucketName"]
    }
  ]
}

```

### Aws3TargetConfiguration Examples

Configuration using CredentialProviderClient.

```json
{
  "TargetType" : "AWS-S3",    
  "BucketName": "your-bucket-name",
  "Region": "us-east-1",
  "BufferSize": 10,
  "Interval": 60,
  "Compression": "GZip",
  "Prefix": "data/",
  "CredentialProviderClient": "aws-credentials-provider"
}

```

Configuration using  default AWS SDK credential provider chain.

```json
{
  "TargetType" : "AWS-S3",     
  "BucketName": "your-bucket-name",
  "Region": "us-east-1",
  "BufferSize": 16,
  "Interval": 300,
  "Compression": "Zip",
  "Prefix": "data/"
}

```



[^top](#aws-s3-target)

