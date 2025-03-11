# SFC Output data formats



## Output data format

```
[schedule]  -- schedule name
[serial]    -- serial number
[timestamp] -– processing timestamp
[sources]   -- source name* --- [values] -- value name* --- [value]-- value
                            |                           |- [metadata]--name* -- meta value
                            |                           |- [timestamp]-- value timestamp
                            |
                            |- [timestamp] -- source timestamp
                            |- [metadata] -- name* -- value
[metadata] --name* -- value
```



## Aggregated output data format 

```

[schedule]  -- schedule name
[serial]    -- serial number
[sources]   -- source name* --- [values] -- value name* --- [value]-- **aggregation name*** --  [value] --- value
                            |                           |                                       [timestamp] -timestamp
                            |                           |- [metadata] -- name* -- meta value
                            |                           
                            |- [metadata] --- name* --- value
[metadata] --name* -- value
```

Custom element names in brackets can be set for all elements above in brackets using the [ElementNames](../docs/core/sfc-configuration.md#elementnames) configuration
setting. The name keys for the sources and value maps get the value of the "Name" element for the source and channel in
their configuration (default is the key used as the id for the source/value in the configuration).

The root contains 6 elements

- **schedule**: This element contains the name of the schedule that outputs the data
- **serial**: A unique serial number for the target data
- **timestamp**: Timestamp when the target output data was created
- **sources**: This element contains a map with a node for each source of the schedule that has output data
    - **values**: The values node contains a map for each channel of its source that has an output value
    
        - **value**: This node contains the actual value of a channel or an aggregated value
    
        - **metadata**: This node contains a map with (optional) metadata for a channel
    
        - **timestamp**: Timestamp for the value (only if timestamp level = "value" or "both")
          For aggregated data the timestamp is only available for the aggregation outputs first, last and values.


- **metadata**: This node contains a map with (optional) metadata for a schedule

When using output transformations using a [velocity template](./core/target-configuration.md#template) for a target additional epoch timestamp fields, at message can be used in the transformation by setting the [TemplateEpochTimestamp](./core/target-configuration.md#templateepochtimestamp) property for the configuration of that target to true.



## SFC output data schemas



When target adapters write data in JSON format, the following schema is employed to structure the data of a  message or a list of messages if batching is enabled. 

Note that the names for the properties can be customized using the [ElementNames](./core/sfc-configuration.md#elementnames) property in the SFC Configuration.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "description": "SFC Target data",
  
  "oneOf": [
    {
      "$ref": "#/definitions/targetdata"
    },
    {
      "type": "array",
      "description": "Array of SFC Target data items",
      "items": {
        "$ref": "#/definitions/sfcTarget"
      }
    }
  ],
  
  "definitions": {
    
    "targetdata": {
      "type": "object",
      "description": "Single SFC Target data item",
      "required": ["schedule", "serial", "timestamp", "sources"],
      "properties": {
        "schedule": {
          "type": "string",
          "description": "Name of the schedule"
        },
        "serial": {
          "type": "string",
          "description": "Unique identifier for the data message",
          "format": "uuid"
        },
        "timestamp": {
          "$ref": "#/definitions/timestamp",
          "description": "Timestamp when the data was collected"
        },
        "sources": {
          "type": "object",
          "description": "Data sources containing channel values",
          "minProperties": 0,
          "additionalProperties": {
            "type": "object",
            "description": "Source containing channel values and optional metadata and timestamp",
            "required": ["values"],
            "properties": {
              "values": {
                "type": "object",
                "description": "Channel values for the source",
                "minProperties": 1,
                "additionalProperties": {
                  "type": "object",
                  "description": "Channel containing value and optional metadata and timestamp",
                  "required": ["value"],
                  "properties": {
                    "value": {
                      "type" : "any",
                      "$ref": "#/definitions/any"
                    },
                    "metadata": {
                      "$ref": "#/definitions/metadata"
                    },
                    "timestamp": {
                      "$ref": "#/definitions/timestamp"
                    }
                  }
                }
              },
              "metadata": {
                "$ref": "#/definitions/metadata"
              },
              "timestamp": {
                "$ref": "#/definitions/timestamp"
              }
            }
          }
        },
        "metadata": {
          "$ref": "#/definitions/metadata",
          "description": "Optional metadata for the schedule"
        }
      }
    },
    
    "any": {
      "description": "Value which can be of any JSON type (string, number, boolean, array or object)""
    },
    
    "metadata": {
      "type": "object",
      "description": "String key-value pairs containing metadata",
      "additionalProperties": {
        "type": "string"
      }
    },
    
    "timestamp": {
      "type": "string",
      "description": "ISO-8601 formatted timestamp",
      "format": "date-time"
    }
  }
}

```



When [aggregation](./core/aggregation-configuration.md) is enabled for a schedule, an additional level is added for every statistical value defined in the [output definition](./core/aggregation-configuration.md#output) for the aggregation.



```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "description": "SFC Target aggregated data",
  
  "oneOf": [
    {
      "$ref": "#/definitions/aggrgatedData"
    },
    {
      "type": "array",
      "description": "Array of aggregated SFC Target data items",
      "items": {
        "$ref": "#/definitions/sfcTarget"
      }
    }
  ],
  
  "definitions": {
    "aggrgatedData": {
      "type": "object",
      "required": [
        "schedule",
        "serial",
        "timestamp",
        "sources"
      ],
      "properties": {
        
        "schedule": {
          "type": "string",
          "description": "Name of the schedule"
        },
        
        "serial": {
          "type": "string",
          "description": "Unique identifier for the data message",
          "format": "uuid"
        },
        
        "timestamp": {
          "$ref": "#/definitions/timestamp",
          "description": "Timestamp when the data was aggregated"
        },
        
        "sources": {
          "type": "object",
          "description": "Data sources containing channel values",
          "additionalProperties": {
            "type": "object",
            "required": [
              "values"
            ],
            "properties": {
              
              "values": {
                "type": "object",
                "description": "Channel values for the source",
                "additionalProperties": {
                  
                  "type": "object",
                  "required": [
                    "value"
                  ],
                  "properties": {
                    
                    "value": {
                      "type": "object",
                      "description": "Statistical values",
                      "minProperties": 1,
                      "properties": {
                        
                        "avg": {
                          "$ref": "#/definitions/aggregatedValue"
                        },
                        
                        "count": {
                          "type": "integer"
                        },
                        
                        "max": {
                          "$ref": "#/definitions/aggregatedValue"
                        },
                        
                        "median": {
                          "$ref": "#/definitions/aggregatedValue"
                        },
                        
                        "min": {
                          "$ref": "#/definitions/aggregatedValue"
                        },
                        
                        "mode": {
                          "type": "object",
                          "required": [
                            "value"
                          ],
                          "properties": {
                            "value": {
                              "type": "array",
                              "minItems": 1,
                              "items": {
                                "$ref": "#/definitions/any"
                              }
                            },
                            "timestamp": {
                              "$ref": "#/definitions/timestamp"
                            }
                          }
                        },
                        
                        "stddev": {
                          "$ref": "#/definitions/aggregatedValue"
                        },
                        
                        "sum": {
                          "$ref": "#/definitions/aggregatedValue"
                        },
                        
                        "values": {
                          "type": "object",
                          "required": [
                            "value"
                          ],
                          "properties": {
                            "value": {
                              "type": "array",
                              "minItems": 1,
                              "items": {
                                "$ref": "#/definitions/any"
                              }
                            }
                          }
                        },
                        
                        "first": {
                          "type": "object",
                          "required": [
                            "value"
                          ],
                          "properties": {
                            "value": {
                              "$ref": "#/definitions/any"
                            },
                            "timestamp": {
                              "$ref": "#/definitions/timestamp"
                            }
                          }
                        },
                        
                        "last": {
                          "type": "object",
                          "required": [
                            "value"
                          ],
                          "properties": {
                            "value": {
                              "$ref": "#/definitions/any"
                            },
                            "timestamp": {
                              "$ref": "#/definitions/timestamp"
                            }
                          }
                        }
                      }
                    },
                    "metadata": {
                      "$ref": "#/definitions/metadata"
                    }
                  }
                }
              },
              "metadata": {
                "$ref": "#/definitions/metadata"
              }
            }
          }
        },
        "metadata": {
          "$ref": "#/definitions/metadata"
        }
      },

  
      "aggregatedValue": {
        "type": "object",
         "description": "Numeric value or array of numeric values representing an aggregation result",
        "required": [
          "value"
        ],
        "properties": {
          "value": {
            "oneOf": [
              {
                "type": "number"
              },
              {
                "type": "array",
                "items": {
                  "type": "number"
                }
              }
            ]
          }
        }
      }
    },
    "any": {
      "description": "Any valid JSON value"
    },
    "metadata": {
      "type": "object",
      "description": "String key-value pairs containing metadata",
      "additionalProperties": {
        "type": "string"
      }
    },
    "timestamp": {
      "type": "string",
      "description": "ISO-8601 formatted timestamp",
      "format": "date-time"
    }
  }
}
```



The following targets serialize the SFC data, except when a transformation template is applied to the target, resulting in JSON format.

- [**AWS IoT Analytics Target**](./targets/aws-iot-analytics.md)

- **[AWS IoT Core Service Target](./targets/aws-iot-core.md)**
- **[AWS Kinesis Firehose Target](./targets/aws-kinesis-firehose.md)**
- [**AWS Lambda  Target**](./targets/aws-lambda.md)
- **[AWS MSK Target](./targets/aws-msk.md)**
- **[AWS S3 Target](./targets/aws-s3.md)**
- **[AWS SNS Target](./targets/aws-sns.md)**
- **[AWS SQS Service Target](./targets/aws-sqs.md)**
- **[Debug Target](./targets/debug.md)**
- **[File Target](./targets/file.md)**
- **[MQTT Target](./targets/mqtt.md)**
- **[NATS Target](./targets/nats.md)**
