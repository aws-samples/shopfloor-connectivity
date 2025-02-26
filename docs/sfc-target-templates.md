# SFC Target data transformation templates

- [Output Structure Transformation Examples](#output-structure-transformation)
    - [CSV output](#csv-output)
    - [XML format](#xml-format)
    - [YAML format](#yaml-format)
    

# **Output Structure Transformation**

For situations where the structure of the data needs to be converted, this can be another JSON format, XML, CSV etc.,
targets can have a configurable [template](./core/target-configuration.md#template). This template is the name of
an [Apache Velocity template file](https://velocity.apache.org/engine/2.3/user-guide.html). Before the data is transmitted the actual destination of the target the template is applied to transform the data.

The context of the input data contains 5 variables:

- "$schedule": Only contains the name of the schedule
- "$sources": Map with an element for each source containing all its values
- "$metadata": Metadata at (schedule) top-level.
- "$serial" : Output data message serial number
- "$timestamp" : : Output data message serial timestamp

The following [Velocity tools](https://velocity.apache.org/tools/3.1/tools-summary.html) can be used in the transformation template:

- $datetool
- $collection
- $context
- $math
- $number

Below are examples of templates that transform the data (not-aggregated) into different formats.

## CSV output

This template flattens the data into CSV format. Each line consists of the name of the source, the name of the value,
the actual value and its timestamp.

```vtl
#foreach($sourceName in $sources.keySet())
#foreach($valueName in $sources[$sourceName]["values"].keySet())
#set( $value = $sources[$sourceName]["values"][$valueName])
"$sourceName","$valueName",$value["value"],"$value["timestamp"]"
#end
#end
```

The template below flattens the values for the "`count`", "`avg`", "`min`", "`max`", "`stddev`" aggregations of a
dataset into CSV format.

```vtl
#foreach($sourceName in $sources.keySet())
#foreach($valueName in $sources[$sourceName]["values"].keySet())
#set( $values = $sources[$sourceName]["values"][$valueName])
#set($aggregatedValues="")
#foreach($aggrName in ["count", "avg", "min", "max", "stddev"])
    #set($aggregatedValues = $aggregatedValues + "," + $values["value"][$aggrName]["value"])
#end
"$sourceName","$valueName"$aggregatedValues
    #set($aggregatedValues="")
#end
#end
```

## XML format

The following example template converts the data into XML format, including timestamps and metadata at each level if
these are available

```vtl

<schedule id="$schedule" #metadata_attributes($metadata)>
    #foreach($sourceName in $sources.keySet())
        #set( $source = $sources[$sourceName])
        <source name="sourceName" #metadata_attributes($source["metadata"]) #timestamp_attr($source)>
    #foreach($valueName in $source["values"].keySet())
    #set($value = $source["values"][$valueName])
             <value name="$valueName" #metadata_attributes($value["metadata"])#timestamp_attr($value)>$value["value"]</value>
    #end
        </source>
    #end
</schedule>

#macro(metadata_attributes $metadata)
#set($attrs = "")
#foreach($key in $metadata.keySet())
#set( $attrs = $attrs + $key + "=""" + $metadata[$key] +  """ " )
#end
$attrs#end

#macro(timestamp_attr $item)
#set($timestamp=$item["timestamp"])
#if ($timestamp != "")
#set($timestamp = "timestamp=""" + $timestamp + """")
$timestamp#end
#set($timestamp = "")
#end
```

## YAML format

The following example template converts the data into YAML format, including timestamps and metadata at each level if
these are available

```vtl
---
    $schedule:
sources:
#foreach($sourceName in $sources.keySet())
#set( $source = $sources[$sourceName])
    $sourceName:
      values:
#foreach($valueName in $source["values"].keySet())
#set($value = $source["values"][$valueName])
        $valueName:
          value: $value["value"]
#set($val_timestamp = $value["timestamp"])
#if ($val_timestamp != "")
          timestamp: $val_timestamp
#end
#set($val_metadata = $value["metadata"])
#if( $val_metadata != "")
          metadata:
#foreach($key in $val_metadata.keySet())
            $key: $val_metadata[$key]
#end
#end
#set($val_metadata = "")
#end
#set($src_timestamp = $source["timestamp"])
#if ($src_timestamp != "")
      timestamp: $src_timestamp
#end
#set($src_metadata = $source["metadata"])
#if( $src_metadata != "")
      metadata:
#foreach($key in $src_metadata.keySet§())
        $key: $src_metadata[$key]
#end
#end
#set($src_metadata = "")
#set($src_timestamp = "")
#end
#if( $metadata != "")
metadata:
    #foreach($key in $metadata.keySet())
        $key: $metadata[$key]
    #end
#end
```

[^top](#sfc-target-data-transformation-templates)
