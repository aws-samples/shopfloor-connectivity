



# AWS SiteWise Target


---
- [AwsSitewiseTargetConfiguration](#AwsSitewiseTargetConfiguration)
- [AwsSiteWiseAssetCreationConfiguration](#AwsSiteWiseAssetCreationConfiguration)
- [AwsSiteWiseAssetConfiguration](#AwsSiteWiseAssetConfiguration)
- [AwsSiteWiseAssetPropertyConfiguration](#AwsSiteWiseAssetPropertyConfiguration)

---

## AwsSitewiseTargetConfiguration

AwsSitewiseTargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for sending data to SiteWise assets. The Targets configuration element can contain entries of this type, the TargetType of 
these entries must be set to **"AWS-SITEWISE"**



Required IAM permissions:

- `iotsitewise:BatchPutAssetPropertyValue`
- `iotsitewise:CreateAsset` (*)
- `iotsitewise:CreateAssetModel` (*)
- `iotsitewise:DescribeAsset` (*) (**)
- `iotsitewise:DescribeAssetModel` (*) (**)
- `iotsitewise:DescribeEndpoint`
- `iotsitewise:ListAssetModels` (*) (**)
- `iotsitewise:ListAssetModelProperties` (*) (**)
- `iotsitewise:ListAssets` (*) (**)
- `iotsitewise:UpdateAssetModel` (*)
- `iotsitewise:UpdateAssetModelProperty` (*)
- `iotsitewise:TagResource` (*)

(*) required when using Asset creation

(**) required when  using AssetName, AssetExternalId, AssetPropertyName,AssetPropertyExternalId in asset and asset property configuration


- [Schema](#AwsSitewiseTargetConfiguration-Schema)
- [Examples](#AwsSitewiseTargetConfiguration-Examples)

**Properties:**
- [AssetCreation](#AssetCreation)
- [Assets](#Assets)
- [Batch Size](#BatchSize)
- [CredentialProviderClient](#CredentialProviderClient)
- [Interval](#Interval)
- [Region](#Region)

---
### AssetCreation
Settings for AssetModels and Assets automatically created by the adapter.

**Type**:  [AwsSiteWiseAssetCreationConfiguration](#AwsSiteWiseAssetCreationConfiguration)

When present automatic creation of AssetModels and Assets is enabled, can be empty when using the default settings.

---
### Assets
Assets to write to

**Type**: List of [AwsSiteWiseAssetConfiguration](#AwsSiteWiseAssetConfiguration)

This setting is used to map data to existing assets and asset properties. It is possible to combine these with assets 
 which are automatically created by the target adapter using the 
AssetCreation setting. 


---
### BatchSize
Batch size for writing asset data

**Type**: Integer

Default is 10

---
### CredentialProviderClient

Name of the AWS credential provider client defined in the SFC top level configuration section [AwsIotCredentialProviderClients]
(../core/sfc-top-level-config.md#AwsIotCredentialProviderClients) obtaining credentials using X509 certificates from the [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---

### Interval

Interval in milliseconds after which data is sent to stream even if the buffer is not full

**Type**: Integer

Optional, if not set only BatchSize is used, minimum value is 10


---
### Region
AWS Region for SiteWise service

**Type**: String

---



### AwsSitewiseTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsSitewiseTargetConfiguration",
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
        "AssetCreation": {
          "$ref": "#/definitions/AwsSiteWiseAssetCreationConfiguration",
          "description": "Configuration for asset creation"
        },
        "Assets": {
          "type": "array",
          "description": "List of asset configurations",
          "items": {
            "$ref": "#/definitions/AwsSiteWiseAssetConfiguration"
          }
        },
        "BatchSize": {
          "type": "integer",
          "description": "Size of the batch for SiteWise operations"
        },
        "Interval": {
          "type": "integer",
          "description": "Interval in milliseconds between operations"
        },
        "Region": {
          "type": "string",
          "description": "AWS region for SiteWise"
        }
      },
      "oneOf": [
        {
          "required": ["AssetCreation"]
        },
        {
          "required": ["Assets"],
          "properties": {
            "Assets": {
              "minItems": 1
            }
          }
        }
      ]
    }
  ]
}

```

### AwsSitewiseTargetConfiguration Examples

 Config with full asset creation for all:

```json
{
  "TargetType" : "AWS-SITEWISE",     
  "Region": "us-east-1",
  "AssetCreation": {
    "AssetName": "Production Line %source%",
    "AssetDescription": "Main production line %source%",
    "AssetModelName": "ProductionLineModel  %source%",
    "AssetPropertyName": "Temperature",
    "AssetTags": {
      "Location": "%plant%",
      "Department": "Production"
    }
  },
  "CredentialProviderClient": "aws-credentials-provider"
}
```



Configuration using existing model and assets:

```json
{
  "TargetType" : "AWS-SITEWISE",       
  
  "Region": "eu-west-1",
  
  "AssetCreation": {
    "AssetName": "%plant%-%source%",
    "AssetModelName": "%plant%-%source%-model",
    "AssetPropertyName": "%plant%-%source%-%channel%",
    "AssetTags": {
      "Location": "%plant%-%source%",
      "Department": "Production"
    }
  },
  
  "Assets": [
    {
      "AssetName": "AMS-Motor-1",
      "Properties": [
        {
          "PropertyId": "speed",
          "DataType": "double",
          "DataPath": "@.sources.Motor1.values.Speed.value"
        },
        {
          "PropertyId": "power",
          "DataType": "double",
          "DataPath": "@.sources.Motor1.values.Power.value"
        },
      ]
    },
    {
      "AssetName": "AMS-Motor-2",
      "Properties": [
        {
          "PropertyId": "speed",
          "DataType": "double",
          "DataPath": "@.sources.Motor2.values.Speed.value"
        },
        {
          "PropertyId": "power",
          "DataType": "double",
          "DataPath": "@.sources.Motor2.values.Power.value"
        },
      ]
    },
  ],
  "CredentialProviderClient": "aws-credentials-provider"
}
```



Mixed Configuration:

```json
{
  
  "TargetType" : "AWS-SITEWISE",     
  
  "Region": "eu-west-1",
  "Assets": [
    {
      "AssetName": "Motor-1",
      "Properties": [
        {
          "PropertyId": "speed",
          "DataType": "double",
          "DataPath": "@.sources.Motor1.values.Speed.value"
        },
        {
          "PropertyId": "power",
          "DataType": "double",
          "DataPath": "@.sources.Motor1.values.Power.value"
        },
      ]
    },
    {
      "AssetName": "Motor-2",
      "Properties": [
        {
          "PropertyId": "speed",
          "DataType": "double",
          "DataPath": "@.sources.Motor2.values.Speed.value"
        },
        {
          "PropertyId": "power",
          "DataType": "double",
          "DataPath": "@.sources.Motor2.values.Power.value"
        },
      ]
    },
  ],
  "CredentialProviderClient": "aws-credentials-provider"
}
```

Copy

[^top](#aws-sitewise-target)

## AwsSiteWiseAssetCreationConfiguration



The SiteWise target adapter can automatically create and update AssetModels and Assets using the target data received by the adapter.
Each source in the target  data will be mapped to a SiteWise AssetModel and Asset using configurable naming templates.

- [Schema](#AwsSiteWiseAssetCreationConfiguration-Schema)
- [Examples](#AwsSiteWiseAssetCreationConfiguration-Examples)

**Properties:**

- [AssetDescription ](#assetdescription )

- [AssetExternalId](#AssetExternalId)

- [AssetModelDescription ](#assetmodeldescription )

- [AssetModelExternalId](#AssetModelExternalId)

- [AssetModelName](#AssetModelName)

- [AssetModelTags](#AssetModelTags)

- [AssetName](#AssetName)

- [AssetPropertyAlias](#AssetPropertyAlias)

- [AssetPropertyName](#AssetPropertyName)

- [AssetPropertyTimestamp](#AssetPropertyTimestamp)

- [AssetTags](#AssetTags)



---
### AssetDescription
Template for description of created assets.

**Type**: String


The value is as template used tro create the name for a created asset.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%
- %datetime%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

The default value is "Asset for target %target%, schedule %schedule%, source %source%"


---
### AssetExternalId
Template for external ID  of created or updated assets.

**Type**: String


The value is as template used to create the external ID for the created or updated asset.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

If this setting is not used then no external ID will be created for the asset.


---
### AssetModelDescription
Template for description of created asset models.

**Type**: String


The value is as template used tro create the name for a created asset model.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%
- %datetime%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.


The default value is "Asset model for target %target%, schedule %schedule%, source %source%"


---
### AssetModelExternalId
Template for external ID  of created or updated asset models.

**Type**: String


The value is as template used to create the external ID for the created or updated asset model.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

If this setting is not used then no external ID will be created for the asset model.


---
### AssetModelName
Template for name of created or updated asset models.

**Type**: String


The value is as template used tro create the name for the created or updated asset model.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

The default value is "%target%-%schedule%-%source%-model"


---
### AssetModelTags
Map containing the names and value templates to add to an AssetModel when it is created by the adapter.

**Type**: Map[String,String]


Optional

The value is as template used tro create the tag values for the created measurement asset models,
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.



---
### AssetName
Template for name of created or updated assets.

**Type**: String


The value is as template used to create the name for the created or updated asset.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

The default value is "%target%-%schedule%-%source%"


---
### AssetPropertyAlias
Template for alias  of created or updated asset properties.

**Type**: String


The value is as template used to create the external ID for the created asset property.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%
- %channel%
- %uuid% (random uuid)
- %assetid% (ID of the asset of the property)

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

If this setting is not used then no alias will be created for the asset property.


---
### AssetPropertyName
Template for name of created measurement asset properties.

**Type**: String


The value is as template used tro create the name for the created measurement asset properties,
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%
- %channel%

To use the values of metadata at the top, source or channel level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

The default value is "%target%-%schedule%-%source%-%channel%"


---
### AssetPropertyTimestamp
Specified which value to use for the timestamp of the measurement values written to the asset properties.

**Type**: String



The value specifies the starting point in the target output data from where a timestamp is searched for. The following values
can be used. If no timestamp is available at the level in the output data the next level up is tried. Depending on configuration
and availability at the source it can happen that a timestamp is not available at source or channel level. The Schedule timestamp, which is at the top level of the
target output data, is always available as it is added by the SFC core.

- "Channel" : Value timestamp, Source timestamp, Schedule timestamp
- "Source" : Source timestamp, Schedule timestamp
- "Schedule" : Schedule timestamp
- "System": Current UTC date and time


Default value is "Channel"



---
### AssetTags
Map containing the names and value templates to add to an Asset when it is created by the adapter.

**Type**: Map[String,String]


Optional

The value is as template used tro create the tag values for the created measurement assets,
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.


**Type**: Map[String.String]



[^top](#aws-sitewise-target)

### AwsSiteWiseAssetCreationConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsSiteWiseAssetCreationConfiguration",
  "type": "object",
  "properties": {
    "AssetDescription": {
      "type": "string",
      "description": "Description of the asset"
    },
    "AssetExternalId": {
      "type": "string",
      "description": "External ID of the asset"
    },
    "AssetModelDescription": {
      "type": "string",
      "description": "Description of the asset model"
    },
    "AssetModelExternalId": {
      "type": "string",
      "description": "External ID of the asset model"
    },
    "AssetModelName": {
      "type": "string",
      "description": "Name of the asset model"
    },
  "AssetModelTags": {
    "type": "object",
    "description": "Tags for the asset model",
    "patternProperties": {
      "^.*$": {
        "type": "string"
      }
    },
    "additionalProperties": false
  },
  "AssetName": {
    "type": "string",
    "description": "Name of the asset"
  },
  "AssetPropertyAlias": {
    "type": "string",
    "description": "Alias for the asset property"
  },
  "AssetPropertyName": {
    "type": "string",
    "description": "Name of the asset property"
  },
  "AssetPropertyTimestamp": {
    "type": "string",
    "description": "Timestamp type for the asset property",
    "enum": ["Channel", "Source", "Schedule", "System"],
    "default": "System"
  },
  "AssetTags": {
    "type": "object",
    "description": "Tags for the asset",
    "patternProperties": {
      "^.*$": {
        "type": "string"
      }
    },
    "additionalProperties": false
  }
}
}

```

### AwsSiteWiseAssetCreationConfiguration Examples

Config using all defaults

```json
{
}
```


Configuration overwriting defaults for AssetPropertyName and alias using values from target- and meta-data.

```json
{
  "AssetPropertyName": "%plant%-%source%-%channel%",
  "AssetPropertyAlias": "%plant%-%source%-%channel%-alias",
  "AssetTags":{
     "environment" : "production",
     "location" : "%plant%",
     "batch" : "%batch-number%"
   }
}
```

Setting all possible values and adding tags for assetmodel and asset

```json
{
  "AssetName": "Assembly-Robot-%source%",
  "AssetDescription": "Robotic assembly unit for schedule %schedule% for lacoaction %location",
  "AssetExternalId": "%source%-external",
  "AssetModelName": "RoboticAssemblyModel",
  "AssetModelDescription": "Standard model for robotic assembly units from source %source%",
  "AssetModelExternalId": "%source%-external",
  "AssetPropertyName": "%source%-%channel%",
  "AssetPropertyAlias": "%source%-%channel%-alias",
  "AssetPropertyTimestamp": "Channel",
  "AssetTags": {
    "Type": "Robot %source%",
    "Function": "Assembly"
  },
  "AssetModelTags": {
    "Manufacturer": "Robot %source%",
    "Version": "2.0"
  }
}
```



## AwsSiteWiseAssetConfiguration

- [Schema](#AwsSiteWiseAssetConfiguration-Schema)
- [Examples](#AwsSiteWiseAssetConfiguration-Examples)

**Properties:**
- [AssetExternalId](#AssetExternalId)
- [AssetId](#AssetId)
- [AssetName](#AssetName)
- [Properties](#Properties)

---
### AssetExternalId
External id of the asset

**Type**: String

The asset's id, name or external id must be specified, not both. If all properties for the asset use the property alias then ExternalId must NOT be specified.

---
### AssetId
ID of the asset

**Type**: String

Either the asset's id, name or external id must be specified. If all properties for the asset use the property alias then AssetId must NOT be specified.

---
### AssetName
Name of the asset

**Type**: String

The asset's id, name OR external id must be specified, not both. If all properties for the asset use the property alias then AssetName must NOT be specified.

---
### Properties
Properties to write to the asset

**Type**: List of [AwsSiteWiseAssetPropertyConfiguration](#awssitewiseassetpropertyconfiguration)

Either property id or alias must be specified, but not both

### AwsSiteWiseAssetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsSiteWiseAssetConfiguration",
  "type": "object",
  "properties": {
    "AssetExternalId": {
      "type": "string",
      "description": "External ID of the asset"
    },
    "AssetId": {
      "type": "string", 
      "description": "ID of the asset"
    },
    "AssetName": {
      "type": "string",
      "description": "Name of the asset"
    },
    "Properties": {
      "type": "array",
      "description": "List of asset property configurations",
      "items": {
        "$ref": "#/definitions/AwsSiteWiseAssetPropertyConfiguration"
      },
      "minItems": 1
    }
  },
  "oneOf": [
    {
      "required": ["AssetId"],
      "not": {
        "required": ["AssetExternalId"]
      },
      "allOf": [
        {
          "required": ["Properties"]
        }
      ]
    },
    {
      "required": ["AssetExternalId"],
      "not": {
        "required": ["AssetId"]
      },
      "allOf": [
        {
          "required": ["Properties"]
        }
      ]
    }
  ]
}

```

### AwsSiteWiseAssetConfiguration Examples

```json
{
  "AssetId": "a1b2c3d4-5678-90ef-ghij-klmnopqrstuv",
  "AssetName": "Production Line 1",
  "Properties": [
    {
      "PropertyName": "IsActive",
      "DataType": "boolean",
      "DataPath": "@.sources.PumpMotor.values.Active.value",
      "WarnIfNotPresent": true
    },
    {
      "PropertyId": "Speed",
      "DataType": "double",
      "DataPath": "@.sources.PumpMotor.values.Speed.value"
    }
  ]
}

```

[^top](#aws-sitewise-target)




## AwsSiteWiseAssetPropertyConfiguration

- [Schema](#AwsSiteWiseAssetPropertyConfiguration-Schema)
- [Examples](#AwsSiteWiseAssetPropertyConfiguration-Examples)

**Properties:**
- [DataPath](#DataPath)
- [DataType](#DataType)
- [PropertyAlias](#PropertyAlias)
- [PropertyExternalId](#PropertyExternalId)
- [PropertyId](#PropertyId)
- [PropertyName](#PropertyName)
- [TimestampPath](#TimestampPath)
- [WarnIfNotPresent](#WarnIfNotPresent)

---
### DataPath
JMES path that selects the value to write to the property from the data received by the target writer

**Type**: String

https://jmespath.org/
A path typically has the format "sources.< source name >.values< value name >.value or sourcename.valuename.value"
Note that JMESPath syntax treats characters like '-' as special characters and therefore the element in the path must be in quotes,

---
### DataType
SiteWise data type

**Type**: string, integer, double, boolean

If no type is specified the type of the value is used to determine type that is used

---
### PropertyAlias
Alias of the asset property

**Type**: String

Only one of the property id, name, external id or alias must be specified
If PropertyAlias is used for all properties of an asset then the AssetId, AssetName and AssetExternalId must not be configured for that asset.

---
### PropertyExternalId
External id of the asset property

**Type**: String

Only one of the property id, name, external id or alias must be specified

---
### PropertyId
Id of the asset property

**Type**: String

Only one of the property id, name, external id or alias must be specified

---
### PropertyName
Name of the asset property

**Type**: String

Only one of the property id, name, external id or alias must be specified

---
### TimestampPath
JMES path that selects the timestamp to use with to the property value from the data received by the target writer.
String

**Type**: https://jmespath.org/

A path typically has the format "sources.< source name >.values< value name >.timestamp"
If not specified the adapter will look for a timestamp in the order, value level, source level, root level.
Note that JMESPath syntax treats characters like '-' as special characters and therefore the element in the path must be in quotes,


---
### WarnIfNotPresent
A warning is generated if the data path does not return a value for the data being handled by the adapter. This warning can be disabled for 
fields that are not always present by setting this setting to false.
Boolean

**Type**: 
Default is true

### AwsSiteWiseAssetPropertyConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsSiteWiseAssetPropertyConfiguration",
  "type": "object",
  "properties": {
    "DataPath": {
      "type": "string",
      "description": "JMES Path to the data value in the source message"
    },
    "DataType": {
      "type": "string",
      "description": "Data type of the asset property",
      "enum": ["string", "integer", "double", "boolean"]
    },
    "PropertyAlias": {
      "type": "string",
      "description": "Alias of the asset property"
    },
    "PropertyExternalId": {
      "type": "string",
      "description": "External ID of the asset property"
    },
    "PropertyId": {
      "type": "string",
      "description": "ID of the asset property"
    },
    "PropertyName": {
      "type": "string",
      "description": "Name of the asset property"
    },
    "TimestampPath": {
      "type": "string",
      "description": "JMES Path to the timestamp value in the source message"
    },
    "WarnIfNotPresent": {
      "type": "boolean",
      "description": "Whether to generate a warning if the property is not present"
    }
  },
  "oneOf": [
    {
      "required": ["PropertyId"],
      "not": {
        "anyOf": [
          { "required": ["PropertyName"] },
          { "required": ["PropertyExternalId"] },
          { "required": ["PropertyAlias"] }
        ]
      }
    },
    {
      "required": ["PropertyName"],
      "not": {
        "anyOf": [
          { "required": ["PropertyId"] },
          { "required": ["PropertyExternalId"] },
          { "required": ["PropertyAlias"] }
        ]
      }
    },
    {
      "required": ["PropertyExternalId"],
      "not": {
        "anyOf": [
          { "required": ["PropertyId"] },
          { "required": ["PropertyName"] },
          { "required": ["PropertyAlias"] }
        ]
      }
    },
    {
      "required": ["PropertyAlias"],
      "not": {
        "anyOf": [
          { "required": ["PropertyId"] },
          { "required": ["PropertyName"] },
          { "required": ["PropertyExternalId"] }
        ]
      }
    }
  ]
}


```

### AwsSiteWiseAssetPropertyConfiguration Examples

```json
{
  "PropertyName": "IsActive",
  "DataType": "boolean",
  "DataPath": "@.sources.PumpMotor.values.Active.value",
  "TimestampPath": "@.sources.Pump.values.Active.timestamp",
  "WarnIfNotPresent": true
}

```


[^top](#aws-sitewise-target)

