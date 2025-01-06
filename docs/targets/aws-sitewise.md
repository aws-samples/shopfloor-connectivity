# AWS SiteWise Target


---
- [AwsSitewiseTargetConfiguration](#AwsSitewiseTargetConfiguration)
- [AwsSiteWiseAssetCreationConfiguration](#AwsSiteWiseAssetCreationConfiguration)
- [AwsSiteWiseAssetConfiguration](#AwsSiteWiseAssetConfiguration)
- [AwsSiteWiseAssetPropertyConfiguration](#AwsSiteWiseAssetPropertyConfiguration)

---

## AwsSitewiseTargetConfiguration



AwsSitewiseTargetConfiguration extends the type <a href="../core/target-configuration.md" >TargetConfiguration</a> with specific configuration data for sending data to SiteWise assets. The Targets configuration element can contain entries of this type, the TargetType of 
these entries must be set to <strong>"AWS-SITEWISE"</strong></p>

<p></p>
<p style='text-align: justify;'>Required IAM permissions</p>

- <p style='text-align: justify;'>iotsitewise:BatchPutAssetPropertyValue</p>
- <p style='text-align: justify;'>iotsitewise:CreateAsset (*)</p>
- <p style='text-align: justify;'>iotsitewise:CreateAssetModel (*)</p>
- <p style='text-align: justify;'>iotsitewise:DescribeAsset (*) (**)</p>
- <p style='text-align: justify;'>iotsitewise:DescribeAssetModel (*) (**)</p>
- <p style='text-align: justify;'>iotsitewise:DescribeEndpoint</p>
- <p style='text-align: justify;'>iotsitewise:ListAssetModels (*) (**)</p>
- <p style='text-align: justify;'>iotsitewise:ListAssetModelProperties (*) (**)</p>
- <p style='text-align: justify;'>iotsitewise:ListAssets (*) (**)</p>
- <p style='text-align: justify;'>iotsitewise:UpdateAssetModel (*)</p>
- <p style='text-align: justify;'>iotsitewise:UpdateAssetModelProperty (*)</p>
- <p style='text-align: justify;'>iotsitewise:TagResource (*)</p>

(*) required when using Asset creation
(**) required when  using AssetName, AssetExternalId, AssetPropertyName,AssetPropertyExternalId in asset and asset property configuration




**Properties:**
- [AssetCreation](#AssetCreation)
- [Assets](#Assets)
- [Batch Size](#Batch Size)
- [Interval](#Interval)
- [Region](#Region)

---
### AssetCreation
Settings for AssetModels and Assets automatically created by the adapter.

**Type**: 
AwsSiteWiseAssetCreationConfiguration

When present automatic creation of AssetModels and Assets is enabled, can be empty when using the default settings.

---
### Assets
Assets to write to

**Type**: List of AwsSiteWiseAssetConfiguration

This setting is used to map data to existing assets and asset properties. It is possible to combine these with assets 
 which are automatically created by the target adapter using the 
AssetCreation setting. 


---
### Batch Size
Batch size for writing asset data

**Type**: Integer

Default is 10

---
### Interval
Interval in milliseconds after which data is sent to stream even if the buffer is not full

**Type**: Integer

Optional, if not set only BatchSize is used, minimum value is 10


---
### Region
AWS Region for SiteWise service

**Type**: String

[^top](#AWS SiteWise Target)

## AwsSiteWiseAssetCreationConfiguration



The SiteWise target adapter can automatically create and update AssetModels and Assets using the target data received by the adapter.
Each source in the target  data will be mapped to a SiteWise AssetModel and Asset using configurable naming templates.



**Properties:**

- [AssetDescription ](#AssetDescription )

- [AssetExternalId](#AssetExternalId)

- [AssetModelDescription ](#AssetModelDescription )

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



[^top](#AWS SiteWise Target)




## AwsSiteWiseAssetConfiguration


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

**Type**: List of AwsSiteWiseAssetPropertyConfiguration]

Either property id or alias must be specified, but not both

[^top](#AWS SiteWise Target)




## AwsSiteWiseAssetPropertyConfiguration


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
Note that JMESPath syntax treats chacterers like '-' as special characters and therefore the element in the path must be in quotes,

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
If ProperyyAlias is used for all properties of an asset then the AssetId, AssetName and AssetExternalId must not be configured for that asset.

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


[^top](#AWS SiteWise Target)

