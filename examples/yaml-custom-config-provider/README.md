# SFC Example custom config provider for YAML

This is an example of a custom config provider allowing the use of YAML SFC configuration files.

The initial SFC configuration file passed to sfc-main only contains the configuration for the custom provider and the name of the actual YAML configuration file.

```json
{ 
    "AWSVersion": "2022-04-02",
    "Name": "Configuration using custom YAML configurations",
    "ConfigProvider": {
        "JarFiles": ["examples/yaml-custom-config-provider/build/libs/yaml-custom-config-provider-1.0.0.jar"],
        "FactoryClassName": "com.amazonaws.sfc.config.YamlConfigProvider"
    },
    "YamlConfigFile" : "examples/yaml-custom-config-provider/config.yaml"
}
```

When sfc-main is started with the command

`sfc-main -config json.config`

it will load the custom config provider which will read the configuration from the specified YAML file, convert it to JSON and pass it to SFC-Core. The provider will also detect updates to the YAML config file.