## SourceConfiguration


**Properties:**
- [ChangeFilter](#ChangeFilter)
- [ChannelTimestampAdjustment](#ChannelTimestampAdjustment)
- [Channels](#Channels)
- [Compose](#Compose)
- [Decompose](#Decompose)
- [Description](#Description)
- [Metadata](#Metadata)
- [Name](#Name)
- [ProtocolAdapter](#ProtocolAdapter)
- [SourceTimestampAdjustment](#SourceTimestampAdjustment)
- [Spread](#Spread)

---
### ChangeFilter
Change filter to apply on every channel value in this source.

**Type**: String

Optional, if used it must refer to a configured filter in the [ChangeFilters](./sfc-top-level-config.md#ChangeFilters) element

---
### ChannelTimestampAdjustment
Time in ms to adjust the value of the timestamp for all values in the source.

**Type**: Long

To set the timestamp to a later value use a positive value, for an earlier value use a negative value.

---
### Channels
Channels are an abstraction of the values read from the source. For processing the value from these channels, the SFC core only uses a small set of generic attributes which are common for all protocols.

This element is a map indexed by the channel identifiers. The entries contain the actual protocol specific source configuration data
Implementations of input protocols will define their channel configuration types containing protocol-specific attributes to read values from their source. 

The protocol implementation is responsible for reading and handling the protocol-specific attributes.

**Type**: Map[String,[ChannelConfiguration](./channel-configuration.md)]

---
### Compose
This setting is a map, indexed by a structure name, with a list of Channel IDs in this source. The channels listed for a structured are
composed into a new value with the name of the structure. Each channel listed in a structure becomes a field of the structured value. The field name id the ID of the channel or value of the "Name" setting when it is specified in the configuration of the channel. The channels that become fields in the composed structured values are removed from the dataset.

The timestamp of the source will be used as the timestamp of the new structured value.

**Type**: Map[String, List[String]]

Examples: 

```json

   "Compose" : {
        "IO" : ["Input0", "Output0"]
  }
```

This will result in the Input0 and Output0 channel values being replaced by a new value named "IO" with both of these fields as fields of that structure.
```json
{
   "Input0" : true,
   "Output0" : false
}

```


```json
{
   "IO" : {
      "Input0" : true,
       "Output0" : false
   }
}
```




---
### Decompose
If set to true and the value of the channel the value is a structured value then the value is decomposed into a set of individual values for each (sub) element in  the structure.
If the value is  list of structures and the value of the [Spread](#Spread) setting is true then each structure in the list is decomposed. 

**Type**: Boolean

**Default,Constraints,Examples**: Default is false

The names of the values for the fields in the structure start with the name of the value appended by the names of the sub elements, separated by a ".".
After decomposing the structured value into individual values, it is removed from the dataset. If the structure was an element in a list of structures the name is the name of the element, followed by a zero indexed order number of the element in the list and the name of the sub element, all separated by a ".".

---
### Description
User-defined description of the source

**Type**: String

---
### Metadata
The optional [Metadata](../README.md#Metadata) element can be used to add additional data to the output at the source level. If metadata is specified, which is a map of string indexed values, it will be added to the output at the source level as an element that can be configured through the "Metadata" entry of the ElementNames configuration element.

**Type**: Map[String, String]

---
### Name
Name of the source. If this element is specified, it is used as the source key in the map of output values. If no value is specified then the source identifier is used. This name can be used to give a descriptive name in the output for the source the data is read from. (e.g., "AC-Unit-1", "Plant-1/Cooling-Pump")

**Type**: String

Optional

---
### ProtocolAdapter
Reference to the used protocol adapter.

**Type**: String

**Default,Constraints,Examples**: Must refer to an existing protocol adapter in the [ProtocolAdapters](./sfc-top-level-config.md#ProtocolAdapters) section.

---
### SourceTimestampAdjustment
Time in ms to adjust the value of the source timestamp value.

**Type**: Long

To set the timestamp to a later value use a positive value, for an earlier value use a negative value.

---
### Spread
If set to true and the value of the channel the value is a list then for each element in the list a new individual value is created.
The value of this setting overrules the setting of the Spread setting at source level.
The value of this setting can be overruled for specific channels by setting the Spread setting for that channel.

**Type**: Boolean

Default is false

The names of the values for the fields in the structure start with the name of the value element with a sequence number, separated by a ".". After splitting the list value into individual values, it is removed from the dataset.



[^top](#SourceConfiguration)

