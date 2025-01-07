# AWS IoT Analytics Target



## AwsIotAnalyticsTargetConfiguration


AwsIotAnalyticsTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to an IoT Analytics channel. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-IOT-ANALYTICS"</strong>

<p>Requires IAM permission iotanalytics:BatchPutMessage to write to the configured channel</p>


**Properties:**
- [BatchSize](#BatchSize)
- [ChannelName](#ChannelName)
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
### Region
AWS Region for channel

**Type**: String

[^top](#aws-iot-analytics-target)

