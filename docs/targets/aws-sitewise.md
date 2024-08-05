# AWS SiteWise Target

AwsSitewiseTargetConfiguration extends the type TargetConfiguration with specific configuration data for sending data to SIteWise assets. The Targets configuration element can contain entries of this type, the TargetType of 
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


<p style='text-align: justify;'>(*) required when using Asset creation</p>
<p style='text-align: justify;'>(**) required when  using AssetName, AssetExternalId, AssetPropertyName,AssetPropertyExternalId in asset and asset property configuration.</p>

- [AwsSitewiseTargetConfiguration](#awssitewisetargetconfiguration)
- [AwsSiteWiseAssetCreationConfiguration](#awssitewiseassetcreationconfiguration)
- [AwsSiteWiseAssetConfiguration](#awssitewiseassetconfiguration)
- [AwsSiteWiseAssetPropertyConfiguration](#awssitewiseassetpropertyconfiguration)

[Targets](./README.md)

## AwsSitewiseTargetConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 29%" />
<col style="width: 23%" />
</colgroup>

<tbody>

<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>

<tr class="even">
<td>Assets</td>
<td>Assets to write to</td>
<td>List of <a href="#awssitewiseassetconfiguration">AwsSiteWiseAssetConfiguration</a></td>
<td>This setting is used to map data to existing assets and asset properties. It is possible to combine these with assets 
 which are automatically created by the target adapter using the <a href="#AwsSiteWiseAssetCreationConfiguration">
AssetCreation</a> setting. 
</td>
</tr>

<tr class="odd">
<td>AssetCreation</td>
<td>Settings for AssetModels and Assets automatically created by the adapter.</td>
<td><a href="#AwsSiteWiseAssetCreationConfiguration">
AwsSiteWiseAssetCreationConfiguration</a></td>
<td>When present automatic creation of AssetModels and Assets is enabled, can be empty when using the default settings.</td>
</tr>
<tr class="even">
<td>Region</td>
<td>AWS Region for SiteWise service</td>
<td>String</td>
<td></td>
</tr>
<tr class="odd">
<td>Interval</td>
<td>Interval in milliseconds after which data is sent to stream even if the buffer is not full</td>
<td>Integer</td>
<td>Optional, if not set only BatchSize is used, minimum value is 10</td>
</tr>
<tr class="even">
<td>Batch Size</td>
<td>Batch size for writing asset data</td>
<td>Integer</td>
<td>Default is 10</td>
</tr>
</tbody>
</table>

[^top](#aws-sitewise-target)

## AwsSiteWiseAssetCreationConfiguration

<table>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 29%" />
<col style="width: 23%" />
<tbody>
<tr>
<td colspan="4"><p>The SiteWise target adapter can automatically create and update AssetModels and Assets using the target data received by the adapter.
Each source in the target  data will be mapped to a SiteWise AssetModel and Asset using configurable naming templates.</p>

</td>

<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>

<tr class="even">
<td>AssetName</td>
<td>Template for name of created or updated assets.</td>
<td>String</td>
<td>
The value is as template used to create the name for the created or updated asset.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

The default value is "%target%-%schedule%-%source%"
</td>
</tr>

<tr class="odd">
<td>AssetExternalId</td>
<td>Template for external ID  of created or updated assets.</td>
<td>String</td>
<td>
The value is as template used to create the external ID for the created or updated asset.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

If this setting is not used then no external ID will be created for the asset.
</td>
</tr>


<tr class="even">
<td>AssetDescription </td>
<td>Template for description of created assets.</td>
<td>String</td>
<td>
The value is as template used tro create the name for a created asset.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%
- %datetime%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

The default value is "Asset for target %target%, schedule %schedule%, source %source%"
</td>
</tr>


<tr class="odd">
<td>AssetTags</td>
<td>Map containing the names and value templates to add to an Asset when it is created by the adapter.</td>
<td>Map[String,String]</td>
<td>
Optional

The value is as template used tro create the tag values for the created measurement assets,
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

</td>
</tr>


<tr class="even">
<td>AssetPropertyName</td>
<td>Template for name of created measurement asset properties.</td>
<td>String</td>
<td>
The value is as template used tro create the name for the created measurement asset properties,
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%
- %channel%

To use the values of metadata at the top, source or channel level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

The default value is "%target%-%schedule%-%source%-%channel%"
</td>
</tr>

<tr class="odd">
<td>AssetPropertyAlias</td>
<td>Template for alias  of created or updated asset properties.</td>
<td>String</td>
<td>
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
</td>
</tr>


<tr class="odd">
<td>AssetPropertyTimestamp</td>
<td>Specified which value to use for the timestamp of the measurement values written to the asset properties.</td>
<td>String</td>
<td>

The value specifies the starting point in the target output data from where a timestamp is searched for. The following values
can be used. If no timestamp is available at the level in the output data the next level up is tried. Depending on configuration
and availability at the source it can happen that a timestamp is not available at source or channel level. The Schedule timestamp, which is at the top level of the
target output data, is always available as it is added by the SFC core.

- "Channel" : Value timestamp, Source timestamp, Schedule timestamp
- "Source" : Source timestamp, Schedule timestamp
- "Schedule" : Schedule timestamp
- "System": Current UTC date and time


Default value is "Channel"

</td>
</tr>


<tr class="odd">
<td>AssetModelName</td>
<td>Template for name of created or updated asset models.</td>
<td>String</td>
<td>
The value is as template used tro create the name for the created or updated asset model.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

The default value is "%target%-%schedule%-%source%-model"
</td>
</tr>
<tr class="odd">
<td>AssetModelDescription </td>
<td>Template for description of created asset models.</td>
<td>String</td>
<td>
The value is as template used tro create the name for a created asset model.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%
- %datetime%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.


The default value is "Asset model for target %target%, schedule %schedule%, source %source%"
</td>
</tr>

<tr class="odd">
<td>AssetModelExternalId</td>
<td>Template for external ID  of created or updated asset models.</td>
<td>String</td>
<td>
The value is as template used to create the external ID for the created or updated asset model.
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

If this setting is not used then no external ID will be created for the asset model.
</td>
</tr>

<tr class="even">
<td>AssetModelTags</td>
<td>Map containing the names and value templates to add to an AssetModel when it is created by the adapter.</td>
<td>Map[String,String]</td>
<td>
Optional

The value is as template used tro create the tag values for the created measurement asset models,
In template, besides placeholders for environment variables (${name}) the following placeholders are
available:

- %schedule%
- %target%
- %source%

To use the values of metadata at the top or source level of the target data, the name af the metadata value can be used with a '%' prefix and postfix.

</td>
</tr>



</tbody>
</table>

[^top](#aws-sitewise-target)

## AwsSiteWiseAssetConfiguration
<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 29%" />
<col style="width: 23%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4">SiteWise asset to write to</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>

<tr class="even">
<td>AssetId</td>
<td>ID of the asset</td>
<td>String</td>
<td>Either the asset's id, name or external id must be specified. If all properties for the asset use the property alias then AssetId must NOT be specified.</td>
</tr>

<tr class="odd">
<td>AssetName</td>
<td>Name of the asset</td>
<td>String</td>
<td>The asset's id, name OR external id must be specified, not both. If all properties for the asset use the property alias then AssetName must NOT be specified.</td>
</tr>

<tr class="even">
<td>AssetExternalId</td>
<td>External id of the asset</td>
<td>String</td>
<td>The asset's id, name or external id must be specified, noth both. If all properties for the asset use the property alias then ExternalId must NOT be specified.</td>
</tr>

<tr class="odd">
<td>Properties</td>
<td>Properties to write to the asset</td>
<td>List of AwsSiteWiseAssetPropertyConfiguration]</td>
<td>Either property id or alias must be specified, but not both</td>
</tr>

</tbody>
</table>

[^top](#aws-sitewise-target)

## AwsSiteWiseAssetPropertyConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 29%" />
<col style="width: 23%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4">SiteWise asset property to write to</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>

<tr class="even">
<td>PropertyId</td>
<td>Id of the asset property</td>
<td>String</td>
<td>Only one of the property id, name, external id or alias must be specified</td>
</tr>

<tr class="odd">
<td>PropertyName</td>
<td>Name of the asset property</td>
<td>String</td>
<td>Only one of the property id, name, external id or alias must be specified</td>
</tr>

<tr class="even">
<td>PropertyExternalId</td>
<td>External id of the asset property</td>
<td>String</td>
<td>Only one of the property id, name, external id or alias must be specified</td>
</tr>

<tr class="odd">
<td>PropertyAlias</td>
<td>Alias of the asset property</td>
<td>String</td>
<td>Only one of the property id, name, external id or alias must be specified
If ProperyAlias is used for all properties of an asset then the AssetId, AssetName and AssetExternalId must not be configured for that asset.</td>
</tr>

<tr class="even">
<td>DataType</td>
<td>SiteWise data type</td>
<td>string, integer, double, boolean</td>
<td>If no type is specified the type of the value is used to determine type that is used</td>
</tr>

<tr class="odd">
<td>DataPath</td>
<td>JMES path that selects the value to write to the property from the data received by the target writer</td>
<td>String</td>
<td><p><a href="https://jmespath.org/">https://jmespath.org/</a></p>
<p>A path typically has the format "sources.< source name >.values< value name >.value or sourcename.valuename.value"
<p>Note that JMESPath syntax treats chacteres like '-' as special characters and therefore the element in the path must be in quotes,</p></td>
</tr>

<tr class="even">
<td>TimestampPath</td>
<td>JMES path that selects the timestamp to use with to the property value from the data received by the target writer.
<td>String</td>
<td><p><a href="https://jmespath.org/">https://jmespath.org/</a></p>
A path typically has the format "sources.< source name >.values< value name >.timestamp"
<p>If not specified the adapter will look for a timestamp in the order, value level, source level, root level.
<p>Note that JMESPath syntax treats characters like '-' as special characters and therefore the element in the path must be in quotes,</p>

<tr>
<td>WarnIfNotPresent</td>
<td>A warning is generated if the data path does not return a value for the data being handled by the adapter. This warning can be dissabled for 
fields that are not always present by setting this setting to false.
<td>Boolean</td>
<td>
Default is true
</td>
</tr>
</tbody>
</table>



[^top](#aws-sitewise-target)
