# AWS Timestream Target



- [AwsTimestreamTargetConfiguration](#AwsTimestreamTargetConfiguration)
- [AwsTimestreamRecordConfiguration](#AwsTimestreamRecordConfiguration)
- [AwsTimestreamDimensionConfiguration](#AwsTimestreamDimensionConfiguration)

---

## AwsTimestreamTargetConfiguration

AwsSTimestreamTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to a Timestream table. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-TIMESTREAM"**

Requires IAM timestream:WriteRecords permission for the configures table as well timestream:DescribeEndpoints


**Properties:**
- [Batch Size](#Batch Size)
- [Database](#Database)
- [Interval](#Interval)

- [Records](#Records)
- [TableName](#TableName)

---
### Batch Size
Batch size for writing records to table

**Type**: Integer

Default is 10

---
### Database
Timestream database

**Type**: String

---
### Interval
Interval in milliseconds after which data is written even if the buffer is not full

**Type**: Integer

Optional, if not set only BatchSize is used, minimum value is 10


---
### Records
Records to write to table

**Type**: List of AwsTimestreamRecordConfiguration

---
### TableName
Timestream table

**Type**: String

[^top](#AWS Timestream Target)


## AwsTimestreamRecordConfiguration


**Properties:**
- [Dimensions](#Dimensions)
- [MeasureName](#MeasureName)
- [MeasureTimePath](#MeasureTimePath)
- [MeasureValuePath](#MeasureValuePath)
- [MeasureValueType](#MeasureValueType)


---
### Dimensions
Record dimensions

**Type**: List of AwsTimestreamDimensionConfiguration

---
### MeasureName
Measure name for the value

**Type**: String

---
### MeasureTimePath
JMES path that selects the timestamp to use with to the property value from the data received by the target writer

**Type**: String

https://jmespath.org/
If the data entries contain both value and timestamp, in case TimestampLevel "channel" or "both" is used in the root of the configuration file) the writer will automatically look for a field with the name used for timestamp values specified in "ElementNames" at the top level of the configuration for a path sourcename.valuename)

---
### MeasureValuePath
JMES path that selects the value to write to the record from the data received by the target writer

**Type**: String

https://jmespath.org/
A path typically has the format sourcename.valuename or sourcename.valuename.value (If the data entries contain both value and timestamp, in case TimestampLevel "channel" or "both" is used in the root of the configuration file) the writer will automatically look for a field with the name used for data values specified in "ElementNames" at the top level of the configuration for a path sourcename.valuename)

---
### MeasureValueType
Type of the value

**Type**: DOUBLE, BIGINT,VARCHAR,BOOLEAN


[^top](#AWS Timestream Target)


## AwsTimestreamDimensionConfiguration


**Properties:**
- [DimensionName](#DimensionName)
- [DimensionValue](#DimensionValue)
- [DimensionValuePath](#DimensionValuePath)


---
### DimensionName
Name for the dimensions

**Type**: String

---
### DimensionValue
Fixed dimension value

**Type**: String

Either DimensionValue or DimensionValuePath (see below) can be used.

---
### DimensionValuePath
JMES path that selects the value to write to the dimensions from the data received by the target writer

**Type**: String

https://jmespath.org/
A path typically has the format sourcename.valuename or sourcename.valuename.value (If the data entries contain both value and timestamp, in case TimestampLevel "channel" or "both" is used in the root of the configuration file) the writer will automatically look for a field with the name used for data values specified in "ElementNames" at the top level of the configuration for a path sourcename.valuename)


[^top](#AWS Timestream Target)

