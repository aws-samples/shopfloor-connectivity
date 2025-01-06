# AWS CloudWatch Metrics


---
- [AwsCloudWatchConfiguration](#AwsCloudWatchConfiguration)



## AwsCloudWatchConfiguration


**Properties:**
- [BatchSize](#BatchSize)
- [CloudWatchMetricsChannelSize](#CloudWatchMetricsChannelSize)
- [CloudWatchMetricsChannelTimeout](#CloudWatchMetricsChannelTimeout)
- [CredentialProviderClient](#CredentialProviderClient)
- [Interval](#Interval)
- [Region](#Region)

---
### BatchSize
Number of data points to buffer to write as a batch to CloudWatch service

**Type**: Int

Default and max value is 1000

---
### CloudWatchMetricsChannelSize
Size of internal buffer to send metrics data to CloudWatch

**Type**: Int

Default is 1000

---
### CloudWatchMetricsChannelTimeout
Time in milliseconds to send data to internal buffer

**Type**: Int

Default is 1000

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

[^top](#AWS CloudWatch Metrics)



