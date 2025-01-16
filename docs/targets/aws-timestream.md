# AWS Timestream Target


- [AwsTimestreamTargetConfiguration](#AwsTimestreamTargetConfiguration)
- [AwsTimestreamRecordConfiguration](#AwsTimestreamRecordConfiguration)
- [AwsTimestreamDimensionConfiguration](#AwsTimestreamDimensionConfiguration)

---

## AwsTimestreamTargetConfiguration

[SFC Configuration](../core/sfc-configuration) > [Targets](../core/sfc-configuration#Targets) >  [Target](../core/target-configuration.md) 



AwsSTimestreamTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to a Timestream table. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-TIMESTREAM"**

Requires IAM timestream:WriteRecords permission for the configures table as well timestream:DescribeEndpoints

- [Schema](#AwsTimestreamTargetConfiguration-Schema)
- [Examples](#AwsTimestreamTargetConfiguration-Examples)

**Properties:**
- [BatchSize](#BatchSize)
- [CredentialProviderClient](#CredentialProviderClient)
- [Database](#Database)
- [Interval](#Interval)

- [Records](#Records)
- [TableName](#TableName)

---
### BatchSize
Batch size for writing records to table

**Type**: Integer

Default is 10

---
### CredentialProviderClient

Name of the AWS credential provider client defined in the SFC top level configuration section [AwsIotCredentialProviderClients]
(../core/sfc-top-level-config.md#AwsIotCredentialProviderClients) obtaining credentials using X509 certificates from the [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

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

**Type**: List of [AwsTimestreamRecordConfiguration](#AwsTimestreamRecordConfiguration)

---
### TableName
Timestream table

**Type**: String

### AwsTimestreamTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsTimestreamTargetConfiguration",
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
        "BatchSize": {
          "type": "integer",
          "description": "Size of the batch for Timestream writes"
        },
        "Database": {
          "type": "string",
          "description": "Name of the Timestream database"
        },
        "Interval": {
          "type": "integer",
          "description": "Interval in milliseconds between writes"
        },
        "Records": {
          "type": "array",
          "description": "List of record configurations",
          "items": {
            "$ref": "#/definitions/AwsTimestreamRecordConfiguration"
          },
          "minItems": 1
        },
        "TableName": {
          "type": "string",
          "description": "Name of the Timestream table"
        }
      },
      "required": [
        "Database",
        "TableName",
        "Records"
      ]
    }
  ]
}

```

### AwsTimestreamTargetConfiguration Examples

```json
{
  "TargetType": "AWS-TIMESTREAM",
  "TargetServer": "TimestreamTargetServer",
  "BatchSize": 10,
  "Database": "sfc",
  "TableName": "sfc-data",
  "Region": "eu-west-1",
  "Records": [
    {
      "MeasureName": "temperature",
      "MeasureValuePath": "@.sources.Motor.values.temperature",
      "MeasureValueType": "DOUBLE",
      "Dimensions": [
        {
          "DimensionName": "plant",
          "DimensionValuePath": "@.sources.Motor.metadata.Plant"
        },
        {
          "DimensionName": "line",
          "DimensionValuePath": "@.sources.Motor.values.temperature.metadata.Line"
        },
        {
          "DimensionName": "version",
          "DimensionValue": 1
        }
      ]
    },
    {
      "MeasureName": "pressure",
      "MeasureValuePath": "@.sources.Motor.values.pressure",
      "MeasureValueType": "DOUBLE",
      "Dimensions": [
        {
          "DimensionName": "plant",
          "DimensionValuePath": "@.sources.Motor.metadata.Plant"
        },
        {
          "DimensionName": "line",
          "DimensionValuePath": "@.sources.Motor.values.pressure.metadata.Line"
        },
        {
          "DimensionName": "version",
          "DimensionValue": 1
        }
      ]
    }
  ],
  "CredentialProviderClient": "aws-credentials-provider"
}
```


[^top](#aws-timestream-target)

## AwsTimestreamRecordConfiguration

[AwsTimstreamTarget](#AwsTimestreamTargetConfiguration) > [Records](#Records)



- [Schema](#AwsTimestreamRecordConfiguration-Schema)
- [Examples](#AwsTimestreamRecordConfiguration-Examples)

**Properties:**
- [Dimensions](#Dimensions)
- [MeasureName](#MeasureName)
- [MeasureTimePath](#MeasureTimePath)
- [MeasureValuePath](#MeasureValuePath)
- [MeasureValueType](#MeasureValueType)


---
### Dimensions
Record dimensions

**Type**: List of [AwsTimestreamDimensionConfiguration](#AwsTimestreamDimensionConfiguration)

---
### MeasureName
Measure name for the value

**Type**: String

---
### MeasureTimePath
JMES path that selects the timestamp to use with to the property value from the data received by the target writer

**Type**: String

https://jmespath.org/
If the data entries contain both value and timestamp, in case TimestampLevel "channel" or "both" is used in the root of the configuration file, the writer will automatically look for a field with the name used for timestamp values specified in "ElementNames" at the top level of the configuration for a path sourcename.valuename.

---
### MeasureValuePath
JMES path that selects the value to write to the record from the data received by the target writer

**Type**: String

https://jmespath.org/
A path typically has the format sourcename.valuename or sourcename.valuename.value If the data entries contain both value and timestamp, in case TimestampLevel "channel" or "both" is used in the root of the configuration file, the writer will automatically look for a field with the name used for data values specified in "ElementNames" at the top level of the configuration for a path sourcename.valuename

---
### MeasureValueType
Type of the value

Any of "DOUBLE", "BIGINT", "VARCHAR","BOOLEAN"

**Type**: String

### AwsTimestreamRecordConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsTimestreamRecordConfiguration",
  "type": "object",
  "properties": {
    "Dimensions": {
      "type": "array",
      "description": "List of dimension configurations",
      "items": {
        "$ref": "#/definitions/AwsTimestreamDimensionConfiguration"
      },
      "minItems": 1
    },
    "MeasureName": {
      "type": "string",
      "description": "Name of the measure"
    },
    "MeasureTimePath": {
      "type": "string",
      "description": "Path to the timestamp value in the source message"
    },
    "MeasureValuePath": {
      "type": "string",
      "description": "Path to the measure value in the source message"
    },
    "MeasureValueType": {
      "type": "string",
      "description": "Data type of the measure value",
      "enum": ["DOUBLE", "BIGINT", "VARCHAR", "BOOLEAN"]
    }
  },
  "required": [
    "Dimensions",
    "MeasureName",
    "MeasureValuePath",
    "MeasureValueType"
  ]
}

```

### AwsTimestreamRecordConfiguration Examples

```json
{
  "MeasureName": "temperature",
  "MeasureValuePath": "@.sources.Motor.values.temperature",
  "MeasureValueType": "DOUBLE",
  "Dimensions": [
    {
      "DimensionName": "plant",
      "DimensionValuePath": "@.sources.Motor.metadata.Plant"
    },
    {
      "DimensionName": "line",
      "DimensionValuePath": "@.sources.Motor.values.temperature.metadata.Line"
    },
    {
      "DimensionName": "version",
      "DimensionValue": 1
    }
  ]
}
```



[^top](#aws-timestream-target)

## AwsTimestreamDimensionConfiguration

[AwsTimstreamTarget](#AwsTimestreamTargetConfiguration) > [Records](#Records) > [Record](#AwsTimestreamRecordConfiguration) > [Dimensions](#dimensions)



- [Schema](#AwsTimestreamDimensionConfiguration-Schema)
- [Examples](#AwsTimestreamDimensionConfiguration-Examples)

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
A path typically has the format sourcename.valuename or sourcename.valuename.value. If the data entries contain both value and timestamp, in case TimestampLevel "channel" or "both" is used in the root of the configuration file, the writer will automatically look for a field with the name used for data values specified in "ElementNames" at the top level of the configuration for a path sourcename.valuename.

### AwsTimestreamDimensionConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsTimestreamDimensionConfiguration",
  "type": "object",
  "properties": {
    "DimensionName": {
      "type": "string",
      "description": "Name of the dimension"
    },
    "DimensionValue": {
      "type": "string",
      "description": "Static value for the dimension"
    },
    "DimensionValuePath": {
      "type": "string",
      "description": "Path to the dimension value in the source message"
    }
  },
  "required": ["DimensionName"],
  "oneOf": [
    {
      "required": ["DimensionValue"],
      "not": {
        "required": ["DimensionValuePath"]
      }
    },
    {
      "required": ["DimensionValuePath"],
      "not": {
        "required": ["DimensionValue"]
      }
    }
  ]
}

```

### AwsTimestreamDimensionConfiguration Examples

 Static Dimension Value:

```json
{
  "DimensionName": "device-id",
  "DimensionValue": "sensor-001"
}
```

Dynamic Dimension Value from Path selecting a metadata value from the Motor source

```json
{
  "DimensionName": "plant",
  "DimensionValuePath": "@.sources.Motor1.metadata.%plant%"
}
```



[^top](#aws-timestream-target)

