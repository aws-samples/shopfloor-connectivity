## SFC top level configuration

SFC top level configuration structure	

- [Schema](#Schema)

- [Examples](#Examples)


**Properties:**

- [AWSVersion](#AWSVersion)
- [AwsIotCredentialProviderClients](#AwsIotCredentialProviderClients)
- [ChangeFilters](#ChangeFilters)
- [ConditionFilters](#ConditionFilters)
- [ConfigProvider](#ConfigProvider)
- [Description](#Description)
- [ElementNames](#ElementNames)
- [HealthProbe](#HealthProbe)
- [LogLevel](#LogLevel)
- [LogWriter](#LogWriter)
- [Metadata](#Metadata)
- [Metrics](#Metrics)
- [MonitorIncludedConfigContentInterval](#MonitorIncludedConfigContentInterval)
- [MonitorIncludedConfigFiles](#MonitorIncludedConfigFiles)
- [ProtocolAdapterServers](#ProtocolAdapterServers)
- [ProtocolAdapterTypes](#ProtocolAdapterTypes)
- [ProtocolAdapters](#ProtocolAdapters)
- [Schedules](#Schedules)
- [SecretsManager](#SecretsManager)
- [Sources](#Sources)
- [TargetServers](#TargetServers)
- [TargetTypes](#TargetTypes)
- [Targets](#Targets)
- [Templates](#Templates)
- [Transformations](#Transformations)
- [Tuning](#Tuning)
- [ValueFilters](#ValueFilters)
- [Version](#Version)

---
### AWSVersion
Software version must be set to "2022-04-02"

**Type**: String

Used for compatibility with future extensions and updates

---
### AwsIotCredentialProviderClients
Configuration for clients using the AWS IoT Credential Provider Service to obtain session credentials.

**Type**: Map[String,[AwsIotCredentialProviderClientConfiguration](./aws-iot-credential-provider-configuration.md)]

---
### ChangeFilters
Filters that can be applied at source or channel value level to let pass values only if they have changed at all or an absolute or percentage from the previously passed value, or since a time interval.

**Type**: Map[String,[ChangeFilterConfiguration](./change-filter-configuration.md)


Example:

```json

"ChangeFilters" : {
   "Change1PercentOrPer10Sec" : {
	   "Type": "Percent",
      "Value": 1,			
      "AtLeast": 10000
	},
   "AllChangesOrOncePer10Sec" : {
      "Type": "Always",
	   "AtLeast": 10000
   }				
}
```




---
### ConditionFilters
Filters that can be applied at channel values level. Values are passed if the value matches the filter expression

**Type**: Map[String,[ConditionFilterConfiguration](./condition-filter-configuration.md)]


---
### ConfigProvider
Configuration for custom configuration handler

**Type**: [InProcessConfiguration](./in-process-configuration.md)

---

### Description

User-defined description of the configuration

**Type**: String

---
### ElementNames

Names of the output elements. This is a map containing the following entries:

- Metadata: Name of metadata added at source level as configured in the optional "Metadata" configured for a schedule, source or channel
- Schedule: Name of the root element for the schedule.
- Sources: Name the of the element that contains the sources in the output of a schedule
- Timestamp: Name of the timestamp elements.
- Value: Name of the elements that contains a value.
- Values:  Name of the element in a source that contains the map of channel values for a source.
- Serial : Name of the element that contains a unique serial number for target data transmitted to the targets.

**Type**: 
Map(String, String)
indexed by name of the element

Optional, default values for missing element are:

- Metadata -> "metadata"

- Schedule -> "schedule"

- Sources -> "sources"

- Timestamp -> "timestamp"

- Value -> "value"

- Values -> "values"

- Serial -> "serial"

  


---
### HealthProbe
Configuration for main process health probe endpoint

**Type**: [HealthProbeConfiguration](./health-probe-configuration.md)

---
### LogLevel
Detail of logged output information

**Type**: String, 

Any of 

- "Trace"
- "Info"
- "Warning"
- "Error"

Default value is "Info"

---
### LogWriter
Configuration for custom log writer

**Type**: [InProcessConfiguration](./in-process-configuration.md)

Default built-in writer logs to console

---

### Metadata

The optional [Metadata](../README.md#Metadata) element can be used to add additional data to the output at the top level which is combined with the metadata of each schedule. If metadata is specified, which is a map of string indexed values, it will be added to the output at the source level as an element that can be configured through the "Metadata" entry of the ElementNames configuration element.

**Type**: Map[String, String]

---
### Metrics
Metrics collection configuration

**Type**: [MetricsConfiguration](metrics-configuration.md)

---
### MonitorIncludedConfigContentInterval
Set the interval in seconds of checking the content from external configuration sources loaded by configured urls, see Including configuration sections

**Type**: Integer

Default is 60, set to 0 to disable

---
### MonitorIncludedConfigFiles
Controls the monitoring of included configuration files see Including configuration sections

**Type**: Boolean

Default value is true, set the value false to disable monitoring

---
### Name
User-defined name of the configuration

**Type**: String

Optional

---
### ProtocolAdapterServers

Servers that run protocol adapter instances as separate processes. The SFC core will read the data from these servers using a streaming IPC protocol(gRPC) This section is a map indexed by the protocol adapter server identifier. The entries contain the address information that the SFC Core will use to connect and communicate with the IPC service.
The protocol-servers can be referenced by their identifier from the ProtocolAdapters section of the configuration.

**Type**: Map[String,[ServerConfiguration](./server-configuration.md)]

---
### ProtocolAdapterTypes

This section includes the information for each protocol adapter type that is used by the SFC core to create instances of that adapter type if that adapter runs in the same process as the SFC core.

The element is a map indexed by the adapter type (e.g., OPCUA, MODBUS). Each entry contains information on which jar files, that contain the protocol adapter implementation, to load and the factory class to create the instances.

The SFC core itself is not aware of the actual target implementations and only uses this configuration data to explicitly load the jar files to create and use the adapter instances. This makes it possible to add new protocol adapter types without modifications to the SFC core.

Only types that run in the same process as the SFC core need to be configured. If the core uses IPC to send the data to a target that runs in its process, the type does not have to be defined in the ProtocolAdapterTypes section.

Only JVM implementations of protocol adapters can be used to run in the same process as the SFC core.

**Type**: Map[String,[InProcessConfiguration](./in-process-configuration.md)]

---
### 	ProtocolAdapters
Protocol adapters are the sources to read data from and abstract the actual protocol that is us used to read the data. Each source used in a schedule must have a reference to a protocol adapter. As protocol adapters can be of different types, each inherited type has additional specific attributes for the protocol.

**Type**: Map[String, [ProtocolAdapterConfiguration](./protocol-adapter-configuration.md)]

---
### Schedules
List of one or more schedules that define how data is collected from their sources, processed, and send to the targets

**Type**: [[Schedule](./schedule-configuration)]

At least one active schedule needs to be present

---
### SecretsManager
Configuration to obtain secrets stored in AWS secrets manager which are used to replace placeholders in the configuration

**Type**: [SecretsManagerConfiguration](./secrets-manager-configuration.md)

---
### Sources

Input sources to read data from. This element is a map indexed by the source identifiers of the sources.

For controlling the schedule and processing the data from the source, the SFC core uses a set of generic configuration attributes which are common for all protocol implementations.

Implementations of input protocols will define their specific source configurations with additional specific attributes required for that protocol additionally to the common attributes (See SourceConfiguration type)

The entries of this Sources element will contain protocol-specific entries for the used protocol implementation. The protocol implementation is responsible for reading and handling the protocol-specific attributes.


**Type**: Map[String, [SourceConfiguration](./source-configuration.md)]

At least 1 source must be configured.

---
### TargetServers

Servers that run target instances as separate processes. The SFC core will send the data to these targets using IPC (gRPC) This section is a map indexed by the target server identifier. The entries contain the address information that the SFC Core will use to connect and communicate with the IPC service.
The targets-servers can be referenced by their identifier from the Targets section of the configuration.


**Type**: Map[String,[ServerConfiguration](./server-configuration.md)]

---
### TargetTypes

The TargetTypes section includes the information for each target type that is used by the SFC core to create instances of that target if that target runs in the same process as the SFC core.

The element is a map indexed by the TargetType (e.g., AWS-SQS). Each entry contains information on which jar files, that contain the target implementation, to load and the factory class to create the instances.

The SFC core itself is not aware of the actual target implementations and only uses this configuration data to explicitly load the jar files to create and use the target instances. This makes it possible to add new target types without modifications to the SFC core.

Note Only types that run in the same process as the SFC core need to be included in the configuration. If the core uses IPC to send the data to a target that runs in its process, the type does not have to be defined in the TargetTypes section.

Only JVM implementations of targets can be used to run in the same process as the SFC core.

**Type**: Map[String,[InProcessConfiguration](./in-process-configuration.md)]

---
### Targets

Targets are the destinations for data collected and processed by the SFC. Targets defined in this section can be referred to by the target identifier in schedules as their output destinations.
The element is a map, indexed by the target identifier. The entries contain the target configuration data.
Targets can be of different types that have specific configuration attributes. Target implementations define their specific configuration types containing the attributes required for communicating with the target.
For sending the data to the targets the SFC core only uses a subset of attributes that are common between all target types.


**Type**: Map[String,[TargetConfiguration](./target-configuration.md)]

---
### Templates
Configuration Templates

**Type**: Map[String,String]

Map indexed by template names containing JSON objects used as [SFC configuration templates](../sfc-configuration.md#configuration-templates)

---
### Transformations

Transformations are a sequence of one or more transformation operators that can be applied to values read from input channels and/or aggregated output values.
This element is a map indexed by transformation identifiers which can be referred to in case a transformation needs to be applied to the data.
Each entry is a list of one or more transformation operators. An operator consists of the name of the operator and operator-specific parameters. The operators are applied in the order in which they are listed. The output type of the operator must be compatible with the input type of the next operator in the list.
If a value the transformation is applied to is an array of values, the transformation will be applied to each value in the array.

**Type**: Map[String,[TransformationOperator](./transformation-operator-configuration.md)[]]


Example:

```json
{
   "DivBy2Add1Round": [
       {
           "Operator" : "Divide",
           "Operand" : 2
       },
       {
          "Operator" : "Add",
          "Operand" : 1
       },
       {
         "Operator" : "Round"
       }
    ]
 }
```

The transformation with identifier DivBy2Add1Round above Divides the input value by 2, then adds 1 and rounds the result.




---
### Tuning
SFC tuning parameters

**Type**: [TuningConfiguration](./tuning-configuration.md)

---
### ValueFilters
Filters that can be applied at channel values level. Values are passed if the value matches the filter expression

**Type**: Map[String,[ValueFilterConfiguration](./value-filter-configuration.md)


Example:

```json
"ValueFilters": {
   "ValueEqual5": {
      "Operator": "eq",
      "Value": 5
   },
   "ValueEqualYes": {
      "Operator": "eq",
      "Value": "Yes"
   },
   "ValueInRange0-10": {
      "Operator": "and",
      "Value": [
         {
           "Operator": "ge",
           "Value": 0
         },
         {
           "Operator": "le",
           "Value": 10
         }
      ]
   }
}
```



---
### Version
User-defined version

**Type**: Integer

Optional

[^top](#sfc-top-level-configuration)

## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "AWSVersion": {
      "type": "string",
      "const": "2022-04-02"
    },
    "Schedules": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/Schedule"
      },
      "minItems": 1
    },
    "Sources": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "$ref": "#/definitions/SourceConfiguration"
        }
      },
      "minProperties": 1
    },
    "Targets": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "$ref": "#/definitions/TargetConfiguration"
        }
      },
      "minProperties": 1
    },
    "AwsIotCredentialProviderClients": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "$ref": "#/definitions/AwsIotCredentialProviderClientConfiguration"
        }
      }
    },
    "ChangeFilters": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "$ref": "#/definitions/ChangeFilterConfiguration"
        }
      }
    },
    "ConditionFilters": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "$ref": "#/definitions/ConditionFilterConfiguration"
        }
      }
    },
    "ConfigProvider": {
      "$ref": "#/definitions/InProcessConfiguration"
    },
    "Description": {
      "type": "string"
    },
    "ElementNames": {
      "type": "object",
      "propertyNames": {
        "enum": [
          "Metadata",
          "Schedule",
          "Sources",
          "Timestamp",
          "Value",
          "Values",
          "Serial"
        ]
      },
      "patternProperties": {
        "^.*$": {
          "type": "string"
        }
      },
      "default": {
        "Metadata": "metadata",
        "Schedule": "schedule",
        "Sources": "sources",
        "Timestamp": "timestamp",
        "Value": "value",
        "Values": "values",
        "Serial": "serial"
      }
    },
    "HealthProbe": {
      "$ref": "#/definitions/HealthProbeConfiguration"
    },
    "LogLevel": {
      "type": "string",
      "enum": [
        "trace",
        "info",
        "warn",
        "error"
      ]
    },
    "LogWriter": {
      "$ref": "#/definitions/InProcessConfiguration"
    },
    "Metadata": {
      "type": "object",
      "additionalProperties": {
        "type": "string"
      }
    },
    "Metrics": {
      "$ref": "#/definitions/MetricsConfiguration"
    },
    "MonitorIncludedConfigContentInterval": {
      "type": "integer",
      "default": 60
    },
    "MonitorIncludedConfigFiles": {
      "type": "boolean",
      "default": true
    },
    "Name": "string",
    "ProtocolAdapterServers": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "$ref": "#/definitions/ServerConfiguration"
        }
      }
    },
    "ProtocolAdapterTypes": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "$ref": "#/definitions/InProcessConfiguration"
        }
      }
    },
    "ProtocolAdapters": {
      "type": "object"
    },
    "SecretsManager": {
      "$ref": "#/definitions/SecretsManagerConfiguration"
    },
    "TargetServers": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "$ref": "#/definitions/ServerConfiguration"
        }
      }
    },
    "TargetTypes": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "$ref": "#/definitions/InProcessConfiguration"
        }
      }
    },
    "Templates": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "type": "string"
        }
      }
    },
    "Transformations": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/TransformationOperator"
          },
          "minItems": 1
        }
      }
    },
    "Tuning": {
      "$ref": "#/definitions/TuningConfiguration"
    },
    "ValueFilters": {
      "type": "object",
      "patternProperties": {
        "^.*$": {
          "$ref": "#/definitions/ValueFilterConfiguration"
        }
      }
    },
    "Version": {
      "type": "integer"
    }
  },
  "required": [
    "AWSVersion",
    "Schedules",
    "Sources",
    "Targets"
  ],
  "allOf": [
    {
      "anyOf": [
        {
          "required": [
            "TargetTypes"
          ],
          "properties": {
            "TargetTypes": {
              "minProperties": 1
            }
          }
        },
        {
          "required": [
            "TargetServers"
          ],
          "properties": {
            "TargetServers": {
              "minProperties": 1
            }
          }
        }
      ]
    },
    {
      "anyOf": [
        {
          "required": [
            "ProtocolAdapterTypes"
          ],
          "properties": {
            "ProtocolAdapterTypes": {
              "minProperties": 1
            }
          }
        },
        {
          "required": [
            "ProtocolAdapterServers"
          ],
          "properties": {
            "ProtocolAdapterServers": {
              "minProperties": 1
            }
          }
        }
      ]
    }
  ]
}
```



## Examples




S7 data to (debug) terminal  and OPCUA targets, in-process configuration

```json
{
  "AWSVersion": "2022-04-02",
  "Schedules": [
    {
      "Name": "ConveyorData",
      "Interval": 1000,
      "TimestampLevel": "Both",
      "Sources": {
        "S7-SOURCE": ["*"]
      },
      "Targets": [
        "DebugTarget",
        "OpcuaTarget"
      ]
    }
  ],
  
  "Sources": {
    "S7-SOURCE": {
      "Name": "FluidConveyor",
      "ProtocolAdapter": "S7",
      "AdapterController": "S7-PLC-1",
      "Channels": {
        "Power": {
          "Address": "%DB120:230:DINT"
        },
        "Speed": {
          "Address": "%DB120:234:DINT"
        },
        "Temperature": {
          "Address": "%DB120:242:REAL",
          "Metadata": {
            "Unit": "Celsius"
          }
        },
        "Pressure": {
          "Address": "%DB120:246:REAL"
        }
      }
    }
  },
  
  "Targets": {
    "DebugTarget": {
      "TargetType": "DEBUG-TARGET"
    },
    "OpcuaTarget": {
      "LogLevel": "Info",
      "TargetType": "OPCUA-TARGET",
      "AutoCreate" : true
    }
  },
  
  "TargetTypes": {
    "DEBUG-TARGET": {
      "JarFiles": [
        "./debug-target/lib"
      ],
      "FactoryClassName": "com.amazonaws.sfc.debugtarget.DebugTargetWriter"
    },
    
    "OPCUA-TARGET": {
      "JarFiles": [
        "./opcua-target/lib"
      ],
      "FactoryClassName": "com.amazonaws.sfc.opcuatarget.OpcuaTargetWriter"
    }
  },
  
  "AdapterTypes": {
    "S7": {
      "JarFiles": [
        "./s7/lib"
      ],
      "FactoryClassName": "com.amazonaws.sfc.s7.S7Adapter"
    }
  },
  
  "ProtocolAdapters": {
    "S7": {
      "AdapterType": "S7",
      "Controllers": {
        "S7-PLC-1": {
          "Address": "192.168.1.130",
          "ControllerType": "S7-1200"
        }
      }
    }
  }
}
```



Example with OPCUA source and AWS IoT Core target with metadata, filtering and transformations, in process configuration. Using a credentials client to obtain credentials using X509 certificates required to make IoT Core service calls

```json
{
  "AWSVersion": "2022-04-02",
  "Schedules": [
    {
      "Name": "Pumpdata",
      "Interval": 100,
      "TimestampLevel": "Both",
      "Sources": {
        "MainPump": ["*"]
      },
      "Targets": [ "IoTCoreTarget" ]
    }
  ],
  
  "ChangeFilters": {
    "ChangedBy10Percent": {
      "Type": "Percent",
      "Value": 10,
      "AtLeast": 60000
    }
  },
  
  "ValueFilters": {
    "GreaterThan0": {
      "Operator": "gt",
      "Value": 0
    }
  },
  
  "Transformations": {
    "TwoDigits": [
      {
        "Operator": "TruncAt",
        "Operand": 2
      }
    ]
  },
  
 "Sources": {
   
    "Pump": {
      "Name": "MainLiquidPump",
      "ProtocolAdapter": "OPCUA",
      "AdapterOpcuaServer": "PUMP-OPCUA-SERVER",
      "SourceReadingMode": "Subscription",
      
      "Metadata" : {
        "location" : "AMS",
        "environment" : "Production",
        "line" : "Prod-1"
      },
      
      "Channels": {
        "Pressure": {
          "Name": "MainPressure",
          "NodeId": "ns=3;i=1001",
          "ChangeFilter": "ChangedBy10Percent",
          "Transformation": "TwoDigits",
          "Metadata": {
            "Units": "Bar"
          }
        },
        
        "Flow": {
          "NodeId": "ns=3;i=1002",
          "Transformation": "TwoDigits",
          "Metadata": {
            "Units": "meter/sec"
          }
        },
        
        "Power": {
          "NodeId": "ns=3;i=1003",
          "Transformation": "TwoDigits",
          "ValueFilter": "GreaterThan0",
          "Metadata": {
            "Units": "watt"
          }
        }
      }
    }
  },
  
  "Targets": {
    "IoTCoreTarget": {
      "TargetType": "AWS-IOT-CORE",
      "Region": "eu-west-1",
      "TopicName": "pump-data-topic",
      "CredentialProviderClient": "AwsIotClient"
    }
  },
  
  "TargetTypes": {
    "AWS-IOT-CORE": {
      "JarFiles": [ "./sfc/aws-iot-core-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.awsiotcore.AwsIotCoreTargetWriter"
    }
  },
  "AdapterTypes": {
    "OPCUA": {
      "JarFiles": ["./sfc/opcua/lib" ],
      "FactoryClassName": "com.amazonaws.sfc.opcua.OpcuaAdapter"
    }
  },
  
  "ProtocolAdapters": {
    "OPCUA": {
      "AdapterType": "OPCUA",
      "OpcuaServers": {
        "PUMP-OPCUA-SERVER": {
          "Address": "opc.tcp://uademo.prosysopc.com",
          "Path": "OPCUA/SimulationServer",
          "Port": 53530
        }
      }
    }
  },
  
  "AwsIotCredentialProviderClients": {
    "AwsIotClient": {
      "IotCredentialEndpoint": "aaaaaaaaaa.credentials.iot.eu-west-1.amazonaws.com",
      "RoleAlias": "PumpTokenExchangeRoleAlias",
      "ThingName": "PumpThing-1",
      "Certificate": "./certificates/thingCert.crt",
      "PrivateKey": "./certificates/privKey.key",
      "RootCa": "./certificates/rootCA.pem"
    }
  }
}
```



Example with 2 OPCUA sources and AWS IoT Core and Amazon S3 targets using IPC configuration.

Configuration is using Templates for repeating channel sections and region value.

```json
{
  "AWSVersion": "2022-04-02",
  "Schedules": [
    {
      "Name": "Pumpdata",
      "Interval": 100,
      "TimestampLevel": "Both",
      "Sources": {
        "OPCUA-SOURCE": ["*" ]
      },
      "Targets": ["IoTCoreTarget", "S3Target"]
    }
  ],
  
  
  "Sources": {
    
    "Pump1": {
      "Name": "MainLiquidPump1",
      "ProtocolAdapter": "OPCUA",
      "AdapterOpcuaServer": "PUMP1-OPCUA-SERVER",
      "SourceReadingMode": "Subscription",
      "Metadata": {
        "location": "AMS",
        "environment": "Production",
        "line": "Prod-1"
      },
      "Channels": "$(PUMPDATA-CHANNELS-TEMPLATE)"
    },
    
    "Pump2": {
      "Name": "MainLiquidPump2",
      "ProtocolAdapter": "OPCUA",
      "AdapterOpcuaServer": "PUMP2-OPCUA-SERVER",
      "SourceReadingMode": "Subscription",
      "Metadata": {
        "location": "AMS",
        "environment": "Production",
        "line": "Prod-2"
      },
      "Channels": "$(PUMPDATA-CHANNELS-TEMPLATE)"
    }
  },
  
  "Targets": {
    "IoTCoreTarget": {
      "TargetServer": "IotCoreTargetServer",
      "TargetType": "AWS-IOT-CORE",
      "Region": "$(REGION-TEMPLATE)",
      "TopicName": "pump-data-topic",
      "CredentialProviderClient": "AwsIotClient"
    },
    "S3Target": {
      "Active": true,
      "TargetType": "AWS-S3",
      "TargetServer": "S3TargetServer",
      "Region": "$(REGION)",
      "BucketName": "ams-production",
      "Interval": 60,
      "BufferSize": 1,
      "Prefix": "pumpdata",
      "CredentialProviderClient": "AwsIotClient",
      "Compression": "Zip"
    }
  },
  
  "ProtocolAdapters": {
    "OPCUA": {
      "AdapterType": "OPCUA",
      "AdapterServer": "OpcuaProtocolAdapterServer",
      "OpcuaServers": {
        "PUMP1-OPCUA-SERVER": {
          "Address": "opc.tcp://production1",
          "Path": "/pumpdata",
          "Port": 53530
        },
        "PUMP2-OPCUA-SERVER": {
          "Address": "opc.tcp://production2",
          "Path": "/pumpdata",
          "Port": 53530
        }
      }
    }
  },
  "AdapterServers": {
    "OpcuaProtocolAdapterServer": {
      "Address": "192.168.1.10",
      "Port": 50000
    }
  },
  "TargetServers": {
    "IotCoreTargetServer": {
      "Address": "192.168.1.11",
      "Port": 40000
    },
    "S3TargetServer": {
      "Address": "192.168.1.11",
      "Port": 40001
    }
  },
  
  "AwsIotCredentialProviderClients": {
    "AwsIotClient": {
      "IotCredentialEndpoint": "aaaaaaaaaa.credentials.iot.$(REGION-TEMPLATE).amazonaws.com",
      "RoleAlias": "PumpTokenExchangeRoleAlias",
      "ThingName": "PumpThing-1",
      "Certificate": "./certificates/thingCert.crt",
      "PrivateKey": "./certificates/privKey.key",
      "RootCa": "./certificates/rootCA.pem"
    }
  },
  
  "Templates": {
    
    "REGION-TEMPLATE": "eu-west1",
    
    "PUMPDATA-CHANNELS-TEMPLATE": {
      "Pressure": {
        "NodeId": "ns=3;i=1001",
        "Metadata": {
          "Units": "bar"
        }
      },
      "Flow": {
        "NodeId": "ns=3;i=1002",
        "Metadata": {
          "Units": "meter/sec"
        }
      },
      "Power": {
        "NodeId": "ns=3;i=1003",
        "Metadata": {
          "Units": "watt"
        }
      }
    }
  }
}
```

[^top](#sfc-top-level-configuration)
