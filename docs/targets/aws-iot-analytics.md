# AWS IoT Analytics Target

## AwsIotAnalyticsTargetConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration#Targets) >  [Target](../core/target-configuration.md) 



- [Schema](#AwsIotAnalyticsTargetConfiguration-Schema)
- [Examples](#AwsIotAnalyticsTargetConfiguration-Examples)

AwsIotAnalyticsTargetConfiguration extends the types [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to an IoT Analytics channel. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-IOT-ANALYTICS"**

Requires IAM permission `iotanalytics:BatchPutMessage` to write to the configured channel

**Properties:**

- [BatchSize](#BatchSize)
- [ChannelName](#ChannelName)
- [CredentialProviderClient](#CredentialProviderClient)
- [Region](#Region)

---
### BatchSize
Number of output messages to combine in a single BatchPutMessage API call.

**Type**: Integer

Default is 10

---
### ChannelName
Name of the IoT Analytics channel

**Type**: String

---
### CredentialProviderClient

Name of the AWS credential provider client defined in the SFC top level configuration section [AwsIotCredentialProviderClients](../core/sfc-configuration#AwsIotCredentialProviderClients) obtaining credentials using X509 certificates from the [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---

### 

### Region

AWS Region for channel

**Type**: String

### AwsIotAnalyticsTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsIotAnalyticsTargetConfiguration",
  "type": "object",
  "allOf": [
    {
      "$ref": "#/definitions/TargetConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "BatchSize": {
          "type": "integer",
          "description": "Size of the batch for IoT Analytics messages",
          "minimum": 1
        },
        "ChannelName": {
          "type": "string",
          "description": "Name of the IoT Analytics channel"
        },
        "Region": {
          "type": "string",
          "description": "AWS region for the IoT Analytics channel"
        }
      },
      "required": ["ChannelName"]
    }
  ]
}

```

### AwsIotAnalyticsTargetConfiguration Examples

Configuration using CredentialProviderClient

```json
{
  "TargetType" : "AWS-IOT-ANALYTICS"
  "ChannelName": "production-data-channel",
  "Region": "us-east-1",
  "BatchSize": 50,
  "CredentialProviderClient": "aws-credentials-provider"
}

```

Configuration using  default AWS SDK credential provider chain.

```json
{
  "ChannelName": "production-data-channel",
  "Region": "us-east-1",
  "BatchSize": 50
}

```

[^top](#aws-iot-analytics-target)

