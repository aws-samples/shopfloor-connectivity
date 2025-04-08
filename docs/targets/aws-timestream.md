# AWS Timestream Target

The SFC target adapter for Amazon [Timestream](https://aws.amazon.com/timestream/) enables storing industrial device data in AWS's purpose-built time series database service. It supports efficient ingestion of time series data with configurable timestamp handling, dimension mapping, and measure value formatting. The adapter automatically handles data batching and can stream device measurements directly into Timestream tables for real-time analytics and historical data analysis.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWS-TIMESTREAM": {
      "JarFiles" : ["<location of deployment>/aws-timestream-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.timestream.AwsTimestreamTargetWriter"
   }
}
```

**Configuration:**


- [AwsTimestreamTargetConfiguration](#awstimestreamtargetconfiguration)
- [AwsTimestreamRecordConfiguration](#awstimestreamrecordconfiguration)
- [AwsTimestreamDimensionConfiguration](#awstimestreamdimensionconfiguration)

---

## AwsTimestreamTargetConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

AwsTimestreamTargetConfiguration extends the type TargetConfiguration with specific configuration data for sending data to a Timestream table. The Targets configuration element can contain entries of this type; the TargetType of these entries must be set to **"AWS-TIMESTREAM"**.

Requires IAM `timestream:WriteRecords` permission for the configures table as well `timestream:DescribeEndpoints`.

- [Schema](#awstimestreamtargetconfiguration-schema)
- [Examples](#awstimestreamtargetconfiguration-examples)

**Properties:**

- [BatchSize](#batchsize)
- [CredentialProviderClient](#credentialproviderclient)
- [Database](#database)
- [Endpoint](#endpoint)
- [Interval](#interval)
- [Records](#records)
- [TableName](#tablename)

---
### BatchSize
The number of records to accumulate before writing them as a batch to the Timestream table. Batching records optimizes write operations and reduces API calls to the service.

**Type**: Integer

Default is 10

---
### CredentialProviderClient

The CredentialProviderClient property specifies which AWS credential provider client to use for authentication. It references a client defined in the SFC's top-level configuration under [AwsIotCredentialProviderClients](../core/sfc-configuration.md#awsiotcredentialproviderclients) section. This client uses X.509 certificates to obtain temporary AWS credentials through the  [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---

### Database
The name of the Amazon Timestream database where the time series data will be stored. This database must exist before writing records. 

**Type**: String

---
### Endpoint

The EndPoint property specifies the VPC endpoint URL used to access AWS services privately through AWS PrivateLink without requiring an internet gateway or NAT device. When not specified, the service's default public endpoint for the configured region will be used.

https://docs.aws.amazon.com/vpc/latest/privatelink/aws-services-privatelink-support.html

**Type:** String

---

### Interval

The time interval in milliseconds that triggers writing buffered records to Timestream, even if the [batch size](#batchsize) hasn't been reached. If not specified, records are only written when the [batch size](#batchsize) is reached. The interval cannot be less than 10 milliseconds.

**Type**: Integer

Optional, if not set only [BatchSize](#batchsize)#batchsize is used.


---
### Records
A list of record configurations that define how source data is mapped to Timestream records. Each record configuration specifies the dimensions, measure names, and measure values to be written to the Timestream table.

**Type**: List of [AwsTimestreamRecordConfiguration](#awstimestreamrecordconfiguration)

---
### TableName
The name of the table within the specified Timestream database where the time series records will be written. This table must exist in the database before writing records.

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

[AwsTimestreamTarget](#awstimestreamtargetconfiguration) > [Records](#records)



- [Schema](#awstimestreamrecordconfiguration-schema)
- [Examples](#awstimestreamrecordconfiguration-examples)

**Properties:**

- [Dimensions](#dimensions)
- [MeasureName](#measurename)
- [MeasureTimePath](#measuretimepath)
- [MeasureValuePath](#measurevaluepath)
- [MeasureValueType](#measurevaluetype)


---
### Dimensions
Record dimensions

**Type**: List of [AwsTimestreamDimensionConfiguration](#awstimestreamdimensionconfiguration)

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

[AwsTimestreamTarget](#awstimestreamtargetconfiguration) > [Records](#records) > [Record](#awstimestreamrecordconfiguration) > [Dimensions](#dimensions)



- [Schema](#awstimestreamdimensionconfiguration-schema)
- [Examples](#awstimestreamdimensionconfiguration-examples)

**Properties:**
- [DimensionName](#dimensionname)
- [DimensionValue](#dimensionvalue)
- [DimensionValuePath](#dimensionvaluepath)


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

