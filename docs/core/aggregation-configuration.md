## Aggregation

[SFC Configuration](./sfc-configuration.md) > [Sources](./sfc-configuration.md#sources) > [Schedule](./schedule-configuration.md) > [Aggregation](./schedule-configuration.md#aggregation)

Aggregation  defines how multiple read values from a schedule are combined into a single output message. It enables statistical processing of collected data points by applying aggregation functions (such as average, sum, minimum, maximum, or count) to the values before they are published. This helps reduce data volume and provide meaningful summaries of the collected measurements over the specified aggregation period.

- [Schema](#schema)
- [Examples](#examples)

**Properties**

- [Output](#output)
- [Size](#size)
- [Transformations](#transformations)

---
###  Size

The Count property specifies the number of values that must be collected before applying the aggregation functions and publishing the results to the targets. This determines how many data points will be combined into a single aggregated output message. The value must be 1 or higher, with a default value of 1 if not specified.

**Type**: Integer

---
###  Output

Output values and aggregations to apply to the output data. This element is a two-level map, where the first level contains is indexed by the source identifier.

Each entry in the map that is indexed by the channel identifier. Each entry  of the channel map contains the list output elements for the aggregated values for the channel. If the channel value is an array of values, then the aggregations are applied to the values in that array.

Wildcards can be applied at each level of the map. The "\*" wildcard can be used as a source identifier and/or channel identifier. If wildcards are used are combined with more specific entries the best matching entry will be applied. 

The matching will be applied in the following order: 

- Source and channel both match 
-  Source matches, channel wildcard
- Source wildcard, channel matches
- Source and channel are both wildcards

 For example, if there is an entry `source1/channel1` and an entry `source1/*`, then for aggregation of values for a value from source1, channel 1 the first entry will be used, and for any other channel values from source1 the second.   

A list with a single "*" wildcard entry can be used as the aggregation output values for an entry. In that case, all applicable aggregation outputs for the data type of the channel value will be generated. 

In the output, the aggregated values will be sub-elements that have the name of the applied aggregation.   So, if the output name of the channel is "a" and the count and stddev are configured as output for that channel, the "a" output element will have two sub-elements named "count" and "stddev" containing the aggregated values.   

Possible output elements are:  

- **"values"**:    All collected values as an array of values 

- **"avg"**:          Average value  
- **"count"**:      Number of collected values  

- **"first"**:         First collected value  

- **"last"**:          Last collected value  

- **"max"**:         Max collected value  

- **"min"**:          Min collected value  

- **"median"**:   Median for collected values  

- **"mode"**:      Mode for collected values (value van be a single value or array)  

- **"stddev"**:    Stddev for collected values  

- **"sum"**:         Sum of collected values   

- "*":	All output values


For numeric values, the following aggregations can be applied:  "avg", "count", "max", "median", "min", "mode", "stddev", "sum", "values".    

For timestamps values, the following aggregations can be applied:  "first", "last".  

For other data types, the following aggregations can be applied:  "count", "mode", "values".    

Besides aggregation of the collected data, reducing the volume of data sent to the targets, aggregation can also be used to reduce the number of calls to the targets by setting the size and just using the "values" aggregation.

**There must be at least one output**

**Type**: Map[String,Map[String,String[]]]

**Example**



```json
"Output": {

	"source1": {
       "channel1": [
          "count",
          "avg",
          "min",
          "max"
       ],
       "channel2": [
          "sum",
          "count"
       ],
       "channel3": [
          "*"
       ],
       "*": [
          "values"
       ]
    }
```

The aggregated values for source1, channel1 will contain the aggregated output count, avg, min, and max.

The aggregated values for source1, channel2 will contain the aggregated output sum and count.

The aggregated values for source1, channel3 will contain the aggregated output applicable to the datatype of the values.

For all other channels for source1, the values and timestamps will contain the values and timestamps.

Output aggregations first and last can have an optional timestamp, depending on the Timestamp level configured for the schedule.

The output values for the mod and values aggregation outputs are arrays of values.

The values aggregation output value is a list of the input values used for the aggregation, which can include a timestamp for each value depending on the Timestamp level configured for the schedule.



---
###  Transformations

Transformations applied to the aggregated data. 

This element is similar to the Output element, but it has an additional map level for the name of the output on which a transformation will be applied.  Transformations element is a three-level map, where the first level contains is indexed by the source identifier. Each entry is another map that is indexed by the channel identifier.  Each entry of the channel map at that level contains a map indexed by the aggregation output e.g., "values", "avg". Each entry contains a single transformation identifier of the transformation that will be applied to the aggregated output value. This transformation identifier must exist in the [Transformations](./sfc-configuration.md#transformations) section.   

Wildcards can be applied at each level of the map. The "\*" wildcard can be used at source identifier, channel identifier and/or output name. If wildcards are used are combined with more specific entries the best matching entry will be applied.  

The matching will be applied in the following order:    

- Source, channel and aggregation output name all match (`source/channel/output`)  
- Source matches and channel both match, aggregation output name is a wildcard. (`source/channel/*`) 
- Source matches, the channel is a wildcard, aggregation output name matches (`source/*/output`)  
- Source is wildcard, channel matches, and aggregation output name both match (`*/channel/ output`)  
- Source matches, channel, and aggregation output-name are both wildcards. (`source/*/*`)  The source is a wildcard, channel matches, aggregation output name is a wildcard. (`*/channel/*`)  
- Source and channel are wildcards, aggregation output name matches. (`*/*/output`) 
- Source, channel, and aggregation output names are all wildcards (`*/*/*`)  
- If there is no matching entry no transformations will be applied.



**Type:** Map[String,Map[String,Map[String,String]]



**Example:**

```json
"Transformations": {
   "source1": {
      "channel1": {
         "avg": "transformation1",
         "min": "transformation1",
         "max": "transformation2"
    },
    "channel2": {
        "sum": "transformation3"
    },
    "channel3": {
        "*": "transformation4"
    }
  }
}
```

Transformation "transformation1" will be applied to the aggregated "avg" output for the values of "source1", "channel1".

Transformation "transformation2" will be applied to the aggregated "min" and "max" output for the values of "source1", "channel1".

transformation3" will be applied to the aggregated "sum" output for the values of "source1", "channel2".

Transformation "transformation4" will be applied to all aggregated values of "source1", "channel3".

[^top](#aggregation)



## Schema:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "Size": {
      "type": "integer",
      "default": 1
    },
    "Output": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "type": "object",
          "patternProperties": {
            "^.*$": {
              "oneOf": [
                {
                  "type": "array",
                  "items": {
                    "const": "*"
                  },
                  "minItems": 1,
                  "maxItems": 1
                },
                {
                  "type": "array",
                  "items": {
                    "enum": [
                      "avg",
                      "count",
                      "first",
                      "last",
                      "max",
                      "median",
                      "min",
                      "mode",
                      "stddev",
                      "sum",
                      "values"
                    ]
                  },
                  "minItems": 1
                }
              ]
            }
          },
          "minProperties": 1
        }
      },
      "minProperties": 1,
      "additionalProperties": false
    },
    "Transformations": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "type": "object",
          "patternProperties": {
            "^.*$": {
              "type": "object",
              "patternProperties": {
                "^.*$": {
                  "type": "string"
                }
              },
              "propertyNames": {
                "oneOf": [
                  {
                    "const": "*"
                  },
                  {
                    "enum": [
                      "avg",
                      "count",
                      "first",
                      "last",
                      "max",
                      "median",
                      "min",
                      "mode",
                      "stddev",
                      "sum",
                      "values"
                    ]
                  }
                ]
              },
              "minProperties": 1,
              "additionalProperties": false
            }
          },
          "minProperties": 1,
          "additionalProperties": false
        }
      },
      "minProperties": 1,
      "additionalProperties": false
    }
  },
  "required": [
    "Size",
    "Output"
  ]
}
```



## Examples:



Basic example:

```json
{
  "Aggregation": {
    "Size": 10,
    "Output": {
      "sensor1": {
        "temperature": ["avg", "min", "max"],
        "humidity": ["sum"],
        "pressure": ["*"]
      }
    }
  }
}
```



Multiple sources example:

```json
{
  "Aggregation": {
    "Size": 5,
    "Output": {
      "sensor1": {
        "temperature": ["avg", "max"],
        "humidity": ["min", "max"]
      },
      "sensor2": {
        "pressure": ["median", "stddev"]
      }
    }
  }
}
```



Example with wildcard aggregation:

```json
{
  "Aggregation": {
    "Size": 20,
    "Output": {
      "sensor1": {
        "temperature": ["*"]
      }
    }
  }
}
```



Example with multiple aggregation types:

```json
{
  "Aggregation": {
    "Size": 15,
    "Output": {
      "machine1": {
        "vibration": ["min", "max", "avg", "stddev"],
        "speed": ["avg", "median"],
        "temperature": ["max", "min"]
      }
    }
  }
}
```



Basic example with single transformation:

```json
{
  "Aggregation": {
    "Size": 10,
    "Output": {
      "source1": {
        "channel1": ["avg"]
      }
    },
    "Transformations": {
      "source1": {
        "channel1": {
          "avg": "simpleTransform"
        }
      }
    }
  }
}
```



Multiple transformations for different aggregation types:

```json
{
  "Aggregation": {
    "Size": 5,
    "Output": {
      "sensor1": {
        "temperature": ["avg", "max", "min"],
        "humidity": ["avg", "stddev"]
      }
    },
    "Transformations": {
      "sensor1": {
        "temperature": {
          "avg": "tempAvgTransform",
          "max": "tempMaxTransform",
          "min": "tempMinTransform"
        },
        "humidity": {
          "avg": "humidityAvgTransform",
          "stddev": "humidityStdDevTransform"
        }
      }
    }
  }
}
```



Using wildcard transformation:

```json
{
  "Aggregation": {
    "Size": 20,
    "Output": {
      "sensor1": {
        "temperature": ["*"],
        "pressure": ["avg", "max"]
      }
    },
    "Transformations": {
      "sensor1": {
        "temperature": {
          "*": "allTempTransform"
        },
        "pressure": {
          "avg": "pressureAvgTransform",
          "max": "pressureMaxTransform"
        }
      }
    }
  }
}
```



Complex example with multiple sources and channels:

```json
{
  "Aggregation": {
    "Size": 15,
    "Output": {
      "machine1": {
        "vibration": ["min", "max", "avg"],
        "speed": ["avg", "median"]
      },
      "machine2": {
        "temperature": ["max", "min"],
        "pressure": ["avg", "stddev"]
      }
    },
    "Transformations": {
      "machine1": {
        "vibration": {
          "min": "vibrationMinTransform",
          "max": "vibrationMaxTransform",
          "avg": "vibrationAvgTransform"
        },
        "speed": {
          "avg": "speedAvgTransform",
          "median": "speedMedianTransform"
        }
      },
      "machine2": {
        "temperature": {
          "max": "tempMaxTransform",
          "min": "tempMinTransform"
        },
        "pressure": {
          "avg": "pressureAvgTransform",
          "stddev": "pressureStdDevTransform"
        }
      }
    }
  }
}
```

[^top](#aggregation)
