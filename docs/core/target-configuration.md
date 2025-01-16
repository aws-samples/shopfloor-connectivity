## TargetConfiguration

[SFC Configuration](./sfc-configuration) > [Targets](./sfc-configuration#Targets) 

Target Configuration defines common properties for [SFC target adapters](./sfc-configuration#Targets). Target adapter implementations extend this type with their specific additional properties.

- [Schema](#Schema)
- [Examples](#Examples)


**Properties:**
- [Active](#Active)
- [AsArrayWhenBuffered](#AsArrayWhenBuffered)
- [CredentialProviderClient](#CredentialProviderClient)
- [Description](#Description)
- [Metrics](#Metrics)
- [TargetServer](#TargetServer)
- [TargetChannelSize](#TargetChannelSize)
- [TargetChannelTimeout](#TargetChannelTimeout)
- [TargetType](#TargetType)
- [Template](#Template)
- [UnquoteNumericJsonValues](#UnquoteNumericJsonValues)

---
### Active
Output to a target can be suspended by setting the Active element to false.

**Type**: Boolean

Default is true

---
### AsArrayWhenBuffered
Set to value false to strip '[' prefix and ']' postfix and  ',' separator from buffered data for targets that output data as JSON or a transformed list of values.

**Type**: Boolean

Default is true

This setting can reduce the size of the output by stripping redundant double quotes.

Do not set this flag to true if there are any key names in the output that consist of numbers only.

---
### CredentialProviderClient
The client is used by the target to obtain session credentials from the AWS IoT Credential provider service.

**Type**: String

Must refer to an existing client configuration in [AwsIotCredentialProviderClients](./sfc-configuration#AwsIotCredentialProviderClients) section.

---
### Description

Description of the target

Type: String

---

### Metrics

Metrics configuration for the protocol adapter

Type: [MetricsSourceConfiguration](./metrics-source-configuration.md)

---

### TargetServer

Target server identifier of the server that is running the target as an IPC service in its process. The identifier must exist in the TargetServers section of the configuration.

If a server is used then no in-process instance of the target is created in the SFC core process and the target type does not have to be configured in the TargetTypes section.

The IPC server must implement the (gRPC) ProtocolAdapterService.

**Type**: String

Set to a configured target server in the [TargetServers](./sfc-configuration#TargetServers) section of the top level configuration to use IPC to send data to a target running as an external IPC service.

---
### TargetChannelSize
Size of channel used by target to process and write items. For more information see [SFC Tuning](../sfc-tuning.md).

**Type**: Int

Default is 1000,



---
### TargetChannelTimeout
Timeout in milliseconds for writing to internal target channel if it has reached it capacity. For more information see [SFC Tuning](../sfc-tuning.md)

**Type**: Int

Default is 1000

---
### TargetType
TargetType is a code that identifies the type of the target (e.g., "AWS-SQS", "AWS-KINESIS").
If a target runs in the same process as the SFC core then this type must be defined in the [TargetTypes](./sfc-configuration#TargetTypes) section of the configuration. The SFC core requires the information from that section to create instances of the target type.

Target implementations will typically define the target name, and use it to select and verify the configuration data that is passed to their instances.

**Type**: String

---
### Template
Pathname to file containing an [Apache velocity](https://velocity.apache.org/) template that can be applied to [transform the output data](../sfc-target-templates.md) of the target

**Type**: String

Optional

Context variables for template:

- $schedule
- $sources, 
- $metadata, 
- $serial, 
- $timestamp
- names specified in ElementNames configuration
- $tab can be used as a context variable to insert a '\t' character in the transformation output, as putting this character directly in a Velocity template is not supported.

---
### UnquoteNumericJsonValues
Set to true to strip double quotes from numeric values in JSON output.

**Type**: Boolean 

Default is false

This setting can reduce the size of the output by stripping redundant double quotes.

Do not set this flag to true if there are any key names in the output that consist of numbers only.



[^top](#TargetConfiguration)



## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "title": "Target Configuration",
  "description": "Configuration for a target",
  "properties": {
    "Active": {
      "type": "boolean",
      "default": true,
      "description": "Flag indicating if the target is active"
    },
    "AsArrayWhenBuffered": {
      "type": "boolean",
      "default": false,
      "description": "Flag indicating if buffered data should be sent as an array"
    },
    "CredentialProviderClient": {
      "type": "string",
      "description": "Reference to a CredentialsClient defined in AwsIotCredentialProviderClients"
    },
    "TargetServer": {
      "type": "string",
      "description": "Reference to a TargetServer defined in TargetServers"
    },
    "TargetChannelSize": {
      "type": "integer",
      "default" : 1000,
      "description": "Size of the target channel"
    },
    "TargetChannelTimeout": {
      "type": "integer",
      "default": 10000,
      "description": "Timeout for the target channel in milliseconds"
    },
    "TargetType": {
      "type": "string",
      "description": "Reference to a TargetType defined in TargetTypes"
    },
    "Template": {
      "type": "string",
      "description": "Template for target output formatting"
    },
    "UnquoteNumericJsonValues": {
      "type": "boolean",
      "default": false,
      "description": "Flag indicating if numeric JSON values should be unquoted"
    }, 
    "Metrics": {
      "$ref": "#/definitions/MetricsSourceConfiguration",
      "description": "Configuration for metrics collection"
    }
  },
  "oneOf": [
    {
      "required": ["TargetType"],
      "not": {
        "required": ["TargetServer"]
      }
    },
    {
      "required": ["TargetServer"],
      "not": {
        "required": ["TargetType"]
      }
    }
  ],
  "additionalProperties": false
}

```



## Examples

**<u>Note: TargetConfigurations always are instances of extended types with specific additional properties for the implementation of that type of target adapter.</u>**

Basic in-process configuration with local TargetType (not requiring AWS credentials)

```json
{
  "TargetType": "TargetTypeName"
}
```



Basic in-process configuration with TargetType:

```json
{
  "TargetType": "TargetTypeName",
  "CredentialProviderClient": "IotCredentialsClientName"
  
}
```



Configuration with TargetServer, does not need target-type:

```json
{
  "TargetServer": "TrargetServerName",
  "CredentialProviderClient": "IotCredentialsClientName"
}
```

