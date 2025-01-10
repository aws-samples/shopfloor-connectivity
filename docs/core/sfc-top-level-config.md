## SFC top level configuration


**Properties:**
- [AWSVersion](#AWSVersion)
- [AwsIotCredentialProviderClients](#AwsIotCredentialProviderClients)
- [ChangeFilters](#ChangeFilters)
- [ConditionFilters](#ConditionFilters)
- [ConfigProvider](#ConfigProvider)
- [ElementNames](#ElementNames)
- [HealthProbe](#HealthProbe)
- [LogLevel](#LogLevel)
- [LogWriter](#LogWriter)
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

**Type**: Map[String,AwsIotCredentialProviderClientConfiguration]

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

Default value is true, set the value to false to disable monitoring

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
### ProtocolAdapters
Protocol adapters are the sources to read data from and abstract the actual protocol that is us used to read the data. Each source used in a schedule must have a reference to a protocol adapter. As protocol adapters can be of different types, each inherited type has additional specific attributes for the protocol.

**Type**: Map[String, [ProtocolAdapterConfiguration](./protocol-adapter-configuration.md)]

---
### Schedules
List of one or more schedules that define how data is collected from their sources, processed, and send to the targets

**Type**: [[Schedule](./schedule-config.md)]

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


**Type**: Map[String,ServerConfiguration]

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

