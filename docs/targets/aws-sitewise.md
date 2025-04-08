# AWS SiteWise Target

The AWS IoT [SiteWise](https://aws.amazon.com/iot-sitewise/) target adapter enables Shop Floor Connectivity's uninterrupted streaming of industrial device data directly to AWS IoT SiteWise assets and measurements. This adapter facilitates the mapping of device data to SiteWise asset properties, with the option of automatically creating the necessary SiteWise assets. Additionally, it handles data types and timestamps. 

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWS-SITEWISE": {
      "JarFiles" : ["<location of deployment>/aws-sitewise-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.awssitewise.AwsSiteWiseTargetWriter"
   }
}
```



**Configuration:**

- [AwsSitewiseTargetConfiguration](#awssitewisetargetconfiguration)
- [AwsSiteWiseAssetCreationConfiguration](#awssitewiseassetcreationconfiguration)
- [AwsSiteWiseAssetConfiguration](#awssitewiseassetconfiguration)
- [AwsSiteWiseAssetPropertyConfiguration](#awssitewiseassetpropertyconfiguration)

---

## AwsSitewiseTargetConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

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


- [Schema](#awssitewisetargetconfiguration-schema)
- [Examples](#awssitewisetargetconfiguration-examples)

**Properties:**
- [AssetCreation](#assetcreation)
- [Assets](#assets)
- [Batch Size](#batchsize)
- [CredentialProviderClient](#credentialproviderclient)
- [Endpoint](#endpoint)
- [Interval](#interval)
- [Region](#region)

---
### AssetCreation
Controls the automatic creation of AWS IoT SiteWise asset models and assets by the adapter. When this configuration is present, the adapter will automatically create the necessary asset models and assets based on the incoming device data structure. 

- Automatically generates asset models from device data schemas
- Creates corresponding assets from the generated models
- Handles property definitions and hierarchies
- Supports asset naming conventions and property configurations

The configuration can be empty to use default settings, or can be customized to control:

- Asset and model naming patterns
- Property configurations
- Hierarchy definitions
- Model versioning behavior

When this property is absent, even if it's not set to an empty  [AwsSiteWiseAssetCreationConfiguration](#awssitewiseassetcreationconfiguration) value, automatic asset and model creation is disabled. In such cases, existing assets must be explicitly referenced in the configuration.

**Type**:  [AwsSiteWiseAssetCreationConfiguration](#awssitewiseassetcreationconfiguration)

---
### Assets
Defines the mapping configuration for writing data to existing AWS IoT SiteWise assets. This setting allows you to specify how source data should be mapped to asset properties in your IoT SiteWise asset hierarchy.

Each asset configuration in the list specifies:

- The target asset identifier
- Property mappings for measurements, attributes, or transforms
- Data type conversions and transformations
- Timestamp handling

This configuration can be used alongside automatically created assets (defined in AssetModelCreation), providing flexibility to:

- Write to existing asset structures
- Combine with dynamically created assets
- Support hybrid deployment scenarios

Required if writing to existing assets. Optional if using only automatically created assets through AssetModelCreation.

**Type**: List of [AwsSiteWiseAssetConfiguration](#awssitewiseassetconfiguration)




---
### BatchSize
Specifies the maximum number of property values to include in a single BatchPutAssetPropertyValue request to AWS IoT SiteWise. This setting helps optimize data ingestion performance and manage API quotas.

Constraints:

- Maximum allowed value: 10 (AWS IoT SiteWise service limit)
- Each batch entry can contain up to 10 property values
- Timestamps must be within 7 days in the past and 10 minutes in the future

The batch size setting helps:

- Optimize network utilization
- Reduce API calls
- Balance throughput and latency
- Manage service quotas efficiently

Optional. If not specified, the default value of 10 will be used.

**Type**: Integer

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

### Interval

Specifies a time-based trigger for sending data to AWS IoT SiteWise, ensuring data is sent even when the [BatchSize](#batchsize) threshold is not met. This helps maintain data freshness during periods of low data volume.

- Triggers data transmission after specified milliseconds
- Works in conjunction with BatchSize
- Ensures timely data delivery regardless of buffer fullness
- Helps optimize real-time monitoring scenarios

Optional. When not specified, data transmission is controlled solely by BatchSize, which may lead to increased latency during low-volume periods.

**Type**: Integer

Optional, if not set only [BatchSize](#batchsize) is used, minimum value is 10


---
### Region
Specifies the AWS Region where the IoT SiteWise service is deployed. The Region must be one where AWS IoT SiteWise is available and supported. [[1\]](https://docs.aws.amazon.com/govcloud-us/latest/UserGuide/govcloud-iotsitewise.html)

Examples:

- "us-east-1" (US East - N. Virginia)
- "eu-west-1" (Europe - Ireland)
- "ap-southeast-2" (Asia Pacific - Sydney)

Important considerations:

- Must match the region where your assets are defined
- Affects data residency and compliance
- Impacts latency between data source and SiteWise service
- Should align with your organization's AWS infrastructure

Required. The adapter will connect to the AWS IoT SiteWise endpoint in the specified region.

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
    "AssetName": "Production %line% %source%",
    "AssetDescription": "Production line %source%",
    "AssetModelName": "Production %line%  %source%",
    "AssetPropertyName": "%source%-%channel%",
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

[AwsSitewiseTarget](#awssitewisetargetconfiguration) > [AssetCreation](#assetcreation)

Configuration for automatic creation and management of AWS IoT SiteWise asset models and assets. Defines how the adapter generates and maintains asset hierarchies, property definitions, and model relationships based on incoming device data structure. When enabled, the adapter automatically creates and updates the necessary SiteWise resources while following specified naming conventions and configuration patterns.



- [Schema](#awssitewiseassetcreationconfiguration-schema)
- [Examples](#awssitewiseassetcreationconfiguration-examples)

**Properties:**

- [AssetDescription ](#assetdescription )
- [AssetExternalId](#assetexternalid)
- [AssetModelDescription ](#assetmodeldescription )
- [AssetModelExternalId](#assetmodelexternalid)
- [AssetModelName](#assetmodelname)
- [AssetModelTags](#assetmodeltags)
- [AssetName](#assetname)
- [AssetPropertyAlias](#assetpropertyalias)
- [AssetPropertyName](#assetpropertyname)
- [AssetPropertyTimestamp](#assetpropertytimestamp)
- [AssetTags](#assettags)



---
### AssetDescription
Defines the template used to generate descriptions for automatically created assets. The template supports dynamic content through placeholders and metadata values.

Available placeholders:

- %schedule% - Schedule identifier
- %target% - Target identifier
- %source% - Source identifier
- %datetime% - Current date/time
- ${name} - Environment variables
- %metadataName% - Source/target metadata values

Default: "Asset for target %target%, schedule %schedule%, source %source%"

Optional. When not specified, the default template is used. The description helps identify and organize assets within AWS IoT SiteWise

**Type**: String


---
### AssetExternalId
Defines the template used to generate external IDs for assets. External IDs provide a way to link SiteWise assets with external systems and maintain consistent identification across platforms.

Available placeholders:

- %schedule% - Schedule identifier
- %target% - Target identifier
- %source% - Source identifier
- ${name} - Environment variables
- %metadataName% - Source/target metadata values

Pattern requirements:

- Must start with a letter or number
- Can contain letters, numbers, hyphens, underscores
- Must be 2-128 characters long
- Must end with a letter or number

Optional. If not specified, no external ID will be assigned to the asset.

**Type**: String


---
### AssetModelDescription
Defines the template used to generate descriptions for automatically created asset models. The template supports dynamic content through placeholders and metadata values to provide context about the model's purpose and origin.

Available placeholders:

- %schedule% - Schedule identifier
- %target% - Target identifier
- %source% - Source identifier
- %datetime% - Current date/time
- ${name} - Environment variables
- %metadataName% - Source/target metadata values

Default: "Asset model for target %target%, schedule %schedule%, source %source%"

Optional. When not specified, the default template is used. The description helps identify and document asset models within AWS IoT SiteWise.

**Type**: String


---
### AssetModelExternalId
Template for external ID of created or updated asset models.

**Type** : String

Defines the template used to generate external IDs for asset models. External IDs enable integration with external systems by providing a consistent identifier across different platforms and systems. External IDs help maintain referential integrity when synchronizing with external systems.

Available placeholders:

- %schedule% - Schedule identifier
- %target% - Target identifier
- %source% - Source identifier
- ${name} - Environment variables
- %metadataName% - Source/target metadata values

Optional. If not specified, no external ID will be assigned to the asset model. 

Pattern requirements:

- Must be unique within your AWS account
- Must follow AWS IoT SiteWise naming conventions
- Case-sensitive

**Type**: String


---
### AssetModelName
Defines the template used to generate names for asset models. The template supports dynamic content through placeholders and metadata values to create unique and meaningful model names.

Available placeholders:

- %schedule% - Schedule identifier
- %target% - Target identifier
- %source% - Source identifier
- ${name} - Environment variables
- %metadataName% - Source/target metadata values

Default: "%target%-%schedule%-%source%-model"

Required. Must follow AWS IoT SiteWise naming constraints: [[2\]](https://docs.aws.amazon.com/iot-sitewise/latest/userguide/update-asset-models.html)

- Maximum length of 256 characters
- Cannot contain control characters or certain special characters
- Must be unique within your AWS account

**Type**: String


---
### AssetModelTags
Defines key-value pairs of tags to be applied to created asset models. Tag values support dynamic content through templates, allowing for automated and consistent tagging based on context.

Available placeholders in value templates:

- %schedule% - Schedule identifier
- %target% - Target identifier
- %source% - Source identifier
- ${name} - Environment variables
- %metadataName% - Source/target metadata values

Optional. When specified, these tags are automatically applied during asset model creation, enabling better resource organization and management in AWS IoT SiteWise.

**Type**: Map[String,String]

---
### AssetName
Defines the template used to generate names for assets. The template supports dynamic content through placeholders and metadata values to create unique and meaningful asset names.

Available placeholders:

- %schedule% - Schedule identifier
- %target% - Target identifier
- %source% - Source identifier
- ${name} - Environment variables
- %metadataName% - Source/target metadata values

Default: "%target%-%schedule%-%source%"

Required. Must follow AWS IoT SiteWise naming constraints:

- Maximum length of 256 characters
- Must be unique within your asset hierarchy
- Cannot contain control characters or certain special characters

**Type**: String


---
### AssetPropertyAlias
Defines the template used to generate aliases for asset properties. 

Available placeholders:

- %schedule% - Schedule identifier
- %target% - Target identifier
- %source% - Source identifier
- %channel% - Channel identifier
- %uuid% - Random UUID
- %assetid% - ID of the asset containing the property
- ${name} - Environment variables
- %metadataName% - Source/target metadata values

Optional. If not specified, no alias will be created for the asset property.

Constraints:

- Minimum length of 1 character 
- Maximum length of 1000 characters
- Cannot contain control characters
- Must follow AWS IoT SiteWise alias naming conventions

**Type**: String


---
### AssetPropertyName
Defines the template used to generate names for measurement asset properties. The template supports dynamic content through placeholders and metadata values to create descriptive and unique property names.

Available placeholders:

- %schedule% - Schedule identifier
- %target% - Target identifier
- %source% - Source identifier
- %channel% - Channel identifier
- ${name} - Environment variables
- %metadataName% - Metadata values from top, source, or channel level

Default: "%target%-%schedule%-%source%-%channel%"

Required. Must follow AWS IoT SiteWise naming constraints:

- Maximum length of 256 characters
- Must be unique within the asset
- Cannot contain control characters or certain special characters

**Type**: String




---
### AssetPropertyTimestamp
Specified which value to use for the timestamp of the measurement values written to the asset properties.

The value specifies the starting point in the target output data from where a timestamp is searched for. The following values
can be used. If no timestamp is available at the level in the output data, the next level up is tried. Depending on configuration
and availability at the source, it can happen that a timestamp is not available at source or channel level. The Schedule timestamp, which is at the top level of the target output data, is always available as it is added by the SFC core.

- "Channel" : Value timestamp, Source timestamp, Schedule timestamp
- "Source" : Source timestamp, Schedule timestamp
- "Schedule" : Schedule timestamp
- "System": Current UTC date and time


Default value is "Channel"

**Type**: String

---
### AssetTags
Defines key-value pairs of tags to be applied to created assets. Tag values can be dynamically generated using templates, allowing for consistent and automated asset tagging.

Available placeholders in value templates:

- %schedule% - Schedule identifier
- %target% - Target identifier
- %source% - Source identifier
- ${name} - Environment variables
- %metadataName% - Source/target metadata values

Optional. When specified, these tags are automatically applied during asset creation, enabling better resource organization and management in AWS IoT SiteWise.

**Type**: Map[String,String]



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

[AwsSitewiseTarget](#awssitewisetargetconfiguration) > [Assets](#assets) 

Configuration class that defines how data should be mapped to AWS IoT SiteWise assets and their properties.


- [Schema](#awssitewiseassetconfiguration-schema)
- [Examples](#awssitewiseassetconfiguration-examples)

**Properties:**

- [AssetExternalId](#assetexternalid)
- [AssetId](#assetid)
- [AssetName](#assetname)
- [Properties](#properties)

---
### AssetExternalId
Identifies an existing AWS IoT SiteWise asset using its external ID. This is an alternative to using the asset's UUID or name. The asset's id, name or external id must be specified, not both. If all properties for the asset use the property alias then ExternalId must NOT be specified.

**Type**: String

---
### AssetId
Identifies an existing AWS IoT SiteWise asset using its ID. Either the asset's id, name, or external id must be specified. If all properties for the asset use the property alias, then AssetId must NOT be specified.

**Type**: String

---
### AssetName
Identifies an existing AWS IoT SiteWise asset using its name. The asset's id, name OR external id must be specified, not both. If all properties for the asset use the property alias, then AssetName must NOT be specified.

**Type**: String

---
### Properties
Defines the list of property configurations that map data to AWS IoT SiteWise asset properties.

**Type**: List of [AwsSiteWiseAssetPropertyConfiguration](#awssitewiseassetpropertyconfiguration)

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

[AwsSitewiseTarget](#awssitewisetargetconfiguration) > [Assets](#assets) > [Asset](#awssitewiseassetconfiguration) > [Properties](#properties)

Configuration class that defines how data values should be mapped to a specific AWS IoT SiteWise asset property. Specifies how to identify the target property (using ID or alias) and defines the mapping rules for data values and their timestamps.

- [Schema](#awssitewiseassetpropertyconfiguration-schema)
- [Examples](#awssitewiseassetpropertyconfiguration-examples)

**Properties:**

- [DataPath](#datapath)
- [DataType](#datatype)
- [PropertyAlias](#propertyalias)
- [PropertyExternalId](#propertyexternalid)
- [PropertyId](#propertyid)
- [PropertyName](#propertyname)
- [TimestampPath](#timestamppath)
- [WarnIfNotPresent](#warnifnotpresent)

---
### DataPath
[JMES](https://jmespath.org/) path expression that selects the value to write to the AWS IoT SiteWise asset property from the received data structure.

A path typically has the format 

`"sources.< source name >.values< value name>.value"` 

Important notes:

- Special characters (like '-') must be enclosed in quotes
- Path must follow JMESPath syntax rules
- Must resolve to a single value in the data structure
- Case-sensitive

**Type**: String

---
### DataType
Specifies the AWS IoT SiteWise data type for the property value.

Possible values:

- "string"

- "integer"

- "double"

- "boolean"

If no type is specified the type of the value is used to determine type that is used

**Type**: String

---
### PropertyAlias
Alias that identifies the AWS IoT SiteWise asset property.

Only one of the property id, name, external id or alias must be specified.
If PropertyAlias is used for all properties of an asset, then the AssetID, AssetName, and AssetExternalID must not be configured for that asset.

**Type**: String

---
### PropertyExternalId
External ID of the AWS IoT SiteWise asset property.

Only one of the property id, name, external id or alias must be specified.

**Type**: String

---
### PropertyId
ID of the AWS IoT SiteWise asset property. 

Only one of the property id, name, external id or alias must be specified.

**Type**: String

---
### PropertyName
Name of the AWS IoT SiteWise asset property.

Only one of the property id, name, external id or alias must be specified.

**Type**: String

---
### TimestampPath
Defines the path to extract timestamp information using JMESPath syntax.

A path typically has the format 

`"sources.< source name >.values< value name >.timestamp"`

If not specified, the adapter will look for a timestamp in the order: value level, source level, root level.
Note that JMESPath syntax treats characters like '-' as special characters, and therefore the element in the path must be in quotes.

**Type**: String


---
### WarnIfNotPresent
Controls whether a warning is generated when the specified data path doesn't return a value.

**Type**: Boolean

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

