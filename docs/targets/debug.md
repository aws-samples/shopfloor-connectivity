# Debug Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

The SFC Debug target adapter provides a simple way to output collected data to the system console for debugging and development purposes. It can display source values, metadata, and timestamps in a readable format, helping developers verify data collection and transformation processes. The adapter is particularly useful for building and testing transformation templates, allowing developers to validate template output before configuring production targets. It supports configurable output formatting to facilitate troubleshooting of data flows.

## AwsDebugConfiguration

AwsDebugConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md). The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"DEBUG-TARGET".** This target type does not have additional elements. Output messages will be written to standard output.

Example

```json
{
  "Active" :true,
	"TargetType" : "DEBUG"
}
```

