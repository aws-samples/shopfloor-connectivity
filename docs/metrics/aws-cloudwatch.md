[SFC Configuration](../core/sfc-configuration.md) > [Metrics](../core/sfc-configuration.md#metrics) 

# AWS CloudWatch Metrics



The [Amazon CloudWatch Metrics](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/working_with_metrics.html) Writer is a target component for Shop Floor Connectivity (SFC) that publishes metrics data collected by SFC components to Amazon CloudWatch Metrics. This enables monitoring and analysis of industrial device and SFC operational data through CloudWatch's visualization, alerting, and analytics capabilities. The writer supports publishing both standard and high-resolution metrics, with configurable namespaces, dimensions, and units. 

## AwsCloudWatchConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Metrics](../core/sfc-configuration.md#metrics) 

The AwsCloudWatchConfiguration class defines the configuration settings for the AWS CloudWatch Metrics Writer target component. It specifies how metrics data should be published to CloudWatch, including the namespace for metrics, dimensions, storage resolution, batch size, and publishing interval. The configuration also includes AWS connectivity settings and credentials needed to authenticate with the CloudWatch service.

- [Schema](#schema)
- [Example](#example)

**Properties:**

- [BatchSize](#batchsize)
- [CredentialProviderClient](#credentialproviderclient)
- [Interval](#interval)
- [Region](#region)



---
### BatchSize
The BatchSize property determines how many data points are collected in a buffer before being written as a single batch to the CloudWatch service. This batching helps optimize API calls and improve throughput. The maximum allowed value is 1000 data points, which is also the default value if not specified.

**Type**: Int

Default and max value is 1000

---
### CredentialProviderClient
The CredentialProviderClient property specifies which AWS credential provider client to use for authentication. It references a client defined in the SFC's top-level configuration under [AwsIotCredentialProviderClients](../core/sfc-configuration.md#awsiotcredentialproviderclients) section. This client uses X.509 certificates to obtain temporary AWS credentials through the  [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---
### Interval
The Interval property defines the time period (in seconds) between writes to the CloudWatch service. Buffered metrics will be published when this interval expires, or earlier if the buffer reaches the configured [BatchSize](#batchsize). This helps optimize the frequency of API calls while ensuring timely delivery of metrics. If not specified, the default interval is 60 seconds.

**Type**: Integer

---
### Region
The Region property specifies the AWS Region where the CloudWatch metrics will be published. This should be set to the AWS Region identifier where you want your metrics to be stored and accessed (e.g., "us-east-1", "eu-west-1"). If not specified, the writer will use the default region configured in the AWS SDK through environment variables, configuration files, or instance metadata.

**Type**: String



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