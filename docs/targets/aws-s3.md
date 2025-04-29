# AWS S3 Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md)

The AWS [S3](https://aws.amazon.com/s3/) (Simple Storage Service) target adapter facilitates direct writing of industrial device data to Amazon S3 buckets via Shop Floor Connectivity. This adapter supports template-based transformations, configurable file prefixes, compression, and batching capabilities.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWS-S3": {
      "JarFiles" : ["<location of deployment>/aws-s3-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.awss3.AwsS3TargetWriter"
   }
}
```




## Aws3TargetConfiguration

AwsS3TargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to an S3 bucket. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-S3"**


Requires IAM permission `s3:putObject` to write to the configured bucket

- [Schema](#aws3targetconfiguration-schema)
- [Examples](#aws3targetconfiguration-examples)

**Properties:**
- [BucketName](#bucketname)
- [BufferSize](#buffersize)
- [Compression](#compression)
- [ContentType](#contenttype)
- [CredentialProviderClient](#credentialproviderclient)
- [Extension](#extension)
- [Formatter](#formatter)
- [Interval](#interval)
- [ObjectKey](#objectkey)
- [Prefix](#prefix)
- [Region](#region)
- [Template](#template)

---
### BucketName
Name of the bucket to write to.

The bucketname must comply to the following rules:

- Bucket names must be between 3 (min) and 63 (max) characters long.
- Bucket names can consist only of lowercase letters, numbers, periods (`.`), and hyphens (`-`).
- Bucket names must begin and end with a letter or number.
- Bucket names must not contain two adjacent periods.
- Bucket names must not be formatted as an IP address (for example, `192.168.5.4`).
- Bucket names must not start with the prefix `xn--`.
- Bucket names must not start with the prefix `sthree-`.
- Bucket names must not start with the prefix `amzn-s3-demo-`.
- Bucket names must not end with the suffix `-s3alias`. This suffix is reserved for access point alias names. For more information, see [Access point aliases](https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-points-naming.html#access-points-alias).
- Bucket names must not end with the suffix `--ol-s3`. This suffix is reserved for Object Lambda Access Point alias names. For more information, see [How to use a bucket-style alias for your S3 bucket Object Lambda Access Point](https://docs.aws.amazon.com/AmazonS3/latest/userguide/olap-use.html#ol-access-points-alias).
- Bucket names must not end with the suffix `.mrap`. This suffix is reserved for Multi-Region Access Point names. For more information, see [Rules for naming Amazon S3 Multi-Region Access Points](https://docs.aws.amazon.com/AmazonS3/latest/userguide/multi-region-access-point-naming.html).
- Bucket names must not end with the suffix `--x-s3`. This suffix is reserved for directory buckets. For more information, see [Directory bucket naming rules](https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html).

**Type**: String

---
### BufferSize
Specifies the size threshold in megabytes (MB) that triggers a write operation to S3. When the buffer reaches this size, the adapter will write the accumulated data to an S3 object. This setting helps optimize storage efficiency and API calls by controlling the size of objects written to S3. A larger buffer size results in fewer but larger objects, while a smaller buffer size creates more frequent writes of smaller objects.

**Type**: Integer

Default is 1, maximum is 128

---
### Compression
Specifies the compression algorithm used to compress data before writing to S3 objects.

Supported values:

- "None" (default): Data is stored uncompressed
- "GZip": Data is compressed using GZip compression [[2\]](https://docs.aws.amazon.com/iot-fleetwise/latest/APIReference/API_S3Config.html)
- "Zip": Data is compressed using Zip compression

Compression can significantly reduce storage costs and improve transfer speeds by reducing the size of stored data. The choice of compression format depends on your specific requirements for compression ratio, processing overhead, and compatibility with downstream applications.

**Type**: "None" | "GZip" | "Zip"

Default is "None"

---
### ContentType
Specifies the MIME type (media type) of the data stored in S3 objects. This property helps applications correctly interpret the stored data.

When compression is enabled, the content type is automatically set to the appropriate MIME type for the selected compression method:

- GZip: "application/gzip"
- Zip: "application/zip"

This setting is particularly useful when storing data in specific formats to ensure proper handling by downstream applications. Common examples include:

- XML: "application/xml"
- YAML: "application/yaml"
- JSON: "application/json"
- CSV: "text/csv"

Optional. If not specified, S3 will attempt to determine the content type automatically.

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

### Extension

Extension for the Created S3 Objects

Please note that the extension can also be included within the [ObjectKey](#objectkey) value.

**Type:** String

---

### Formatter

Configuration allows for custom formatting of data written by a target. A [custom formatter](../sfc-extending.md#custom-formatters), implemented as a JVM class, converts a sequence of target data messages into a specific format and returns the formatted data as an array of bytes.

When a formatter is used, a [template](#template) configured for that target is ignored.

**Type:** [InProcessConfiguration](../core/in-process-configuration.md)

---

### Interval
Specifies the time interval in seconds that triggers a write operation to S3.

The adapter writes data to S3 when either the [BufferSize](#buffersize) threshold is reached or this Interval period elapses, whichever occurs first. This ensures that data is written to S3 even during periods of low data volume.

**Type**: Integer

Default is 60, maximum is 900

---

### ObjectKey

Object key for the S3 object. This key can be a template in which the following placeholders can be utilized:

- `%year%`, 4 digit numeric year value from UTC time
- `%month%`, 2 digit numeric month value from UTC time
- `%day%`, 2 digit numeric day value from UTC time
- `%hour%`, 2 digit numeric hour value from UTC time
- `%minute%`, 2 digit numeric minute value from UTC time
- `%second%`, 2 digit numeric second value from UTC time
- `%millisecond%`, 2 digit numeric millisecond value from UTC time
- `%uuid%`, A UUID (Universally Unique Identifier) follows a standardized format consisting of 32 hexadecimal digits arranged in 5 groups, separated by hyphens. 

**The rendered object key must be unique to prevent existing objects from being overwritten.**

See https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html for guidelines for naming S3 objects.

Example template

`year=%year%/month=%month%/day=%day%/hour=%hour%/minute=%minute%/%uuid%.json`

Please note that the template above also specifies a file extension. Alternatively, the [Extension](#extension) property can be used to specify an extension for an object.

When the ObjectKey is not specified, the key of the object will have the value year/month/day/hour/minute/uuiud.

**Type :** String

---
### Prefix
Specifies a prefix that will be added to the beginning of all object keys created by the adapter in the S3 bucket. This helps organize objects in a hierarchical structure, similar to folders in a file system.

Optional. If not specified, objects will be created at the root level of the bucket.

**Type**:

---
### Region
Specifies the AWS Region where the S3 bucket is located. The Region should be provided using the standard AWS Region code format.

Examples:

- "us-east-1" (US East - N. Virginia) [[2\]](https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucketConfiguration.html)
- "eu-west-1" (Europe - Ireland)
- "ap-southeast-2" (Asia Pacific - Sydney)

This setting is used to ensure the adapter connects to the correct regional endpoint for the S3 bucket. Choosing the appropriate region can help optimize latency, costs, and comply with data residency requirements.

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
        "Extension":{
          "type": "string",
          "description": "Extension for S3 objets"
        },
        "Interval": {
          "type": "integer",
          "description": "Interval in seconds between uploads"
        },
        "ObjectKey":{
          "type" : "string",
          "description" : "object key name template"
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

