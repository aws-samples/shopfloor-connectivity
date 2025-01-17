[SFC Configuration](../core/sfc-configuration.md) > [Metrics](../core/sfc-configuration.md#metrics) 

# AWS CloudWatch Metrics


---
- [AwsCloudWatchConfiguration](#awscloudwatchconfiguration)



## AwsCloudWatchConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Metrics](../core/sfc-configuration.md#metrics) 



- [Schema](#schema)
- [Example](#example)

**Properties:**

- [BatchSize](#batchsize)
- [CredentialProviderClient](#credentialproviderclient)
- [Interval](#interval)
- [Region](#region)



---
### BatchSize
Number of data points to buffer to write as a batch to CloudWatch service

**Type**: Int

Default and max value is 1000

---
### CredentialProviderClient
Name of configured credentials client that will be used to read secrets stored in the AWS Secrets Manager service.

**Type**: String

If not set the AWS SDK credential provider chain is used.

---
### Interval
Interval in seconds in which metrics are written to the service (or earlier if buffer size is reached)
Integer

**Type**: Integer

Default is 60

---
### Region
AWS CloudWatch service region

**Type**: String

Default is region setup for AWS SDK

[^top](#aws-cloudwatch-metrics)



## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsCloudWatchConfiguration",
  "type": "object",
  "properties": {
    "BatchSize": {
      "type": "integer",
      "description": "Size of the batch for CloudWatch metrics",
      "minimum": 1,
      "maximum": 1000,
      "default": 1000
    },
    "CredentialProviderClient": {
      "type": "string",
      "description": "Name of the AWS IoT credentials provider client"
    },
    "Interval": {
      "type": "integer",
      "description": "Interval in seconds between metrics submissions",
      "minimum": 1,
      "default": 60
    },
    "Region": {
      "type": "string",
      "description": "AWS region for CloudWatch"
    }
  }
}

```

## Example

```json
{
  "BatchSize": 500,
  "CredentialProviderClient": "MyAwsCredentialsProvider",
  "Interval": 120,
  "Region": "us-west-2"
}

```