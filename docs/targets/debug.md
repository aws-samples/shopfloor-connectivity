# Debug Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration#Targets) >  [Target](../core/target-configuration.md) 



AwsDebugConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md). The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"DEBUG-TARGET".** This target type does not have additional elements. Output messages will be written to standard output.



Example

```json
{
  "Active" :true,
	"TargetType" : "DEBUG"
}
```

