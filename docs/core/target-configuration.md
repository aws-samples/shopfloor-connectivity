## TargetConfiguration


**Properties:**
- [Active](#Active)
- [AsArrayWhenBuffered](#AsArrayWhenBuffered)
- [CredentialProviderClient](#CredentialProviderClient)
- [Name](#Name)
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

Must refer to an existing client configuration in [AwsIotCredentialProviderClients](./sfc-top-level-config.md#AwsIotCredentialProviderClients) section.

---
### TargetServer
Target server identifier of the server that is running the target as an IPC service in its process. The identifier must exist in the TargetServers section of the configuration.

If a server is used then no in-process instance of the target is created in the SFC core process and the target type does not have to be configured in the TargetTypes section.

The IPC server must implement the (gRPC) ProtocolAdapterService.

**Type**: String

Set to a configured target server in the [TargetServers](./sfc-top-level-config.md#TargetServers) section of the top level configuration to use IPC to send data to a target running as an external IPC service.

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
If a target runs in the same process as the SFC core then this type must be defined in the [TargetTypes](./sfc-top-level-config.md#TargetTypes) section of the configuration. The SFC core requires the information from that section to create instances of the target type.

Target implementations will typically define the target name, and use it to select and verify the configuration data that is passed to their instances.

**Type**: String

---
### Template
Pathname to file containing an [Apache velocity](https://velocity.apache.org/) template that can be applied to transform the output data of the target

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

