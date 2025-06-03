## InProcessConfiguration

[SFC Configuration](./sfc-configuration.md) > [AdapterTypes](./sfc-configuration.md#adaptertypes)

[SFC Configuration](./sfc-configuration.md) > [TargetTypes](./sfc-configuration.md#targettypes) 

[SFC Configuration](./sfc-configuration.md) > [Metrics](./sfc-configuration.md#metrics) > [Writer](./metrics-writer-configuration.md#metricswriter)

[SFC Configuration](./sfc-configuration.md) > [LogWriter](./sfc-configuration.md#logwriter)

[SFC Configuration](./sfc-configuration.md) > [ConfigProvider](./sfc-configuration.md#configprovider)

[SFC Configuration](./sfc-configuration.md) > [Targets](./sfc-configuration.md#targets) > [Target](./target-configuration.md ) > [Formatter](./target-configuration.md#formatter)

The InProcessConfiguration class defines settings for loading and instantiating Java components (like protocol adapters or targets) that run within the SFC process. It specifies the factory class responsible for creating component instances and the locations of required JAR files, supporting both individual JAR files and directories containing multiple JARs.

- [Schema](#schema)
- [Examples](#examples)

**Properties:**

- [FactoryClassName](#factoryclassname)
- [JarFiles](#jarfiles)

---
### FactoryClassName
The FactoryClassName property specifies the fully qualified name of the factory class responsible for creating instances of protocol adapters or targets. 

**Type**: String

---
### JarFiles
The JarFiles property is an array of strings that specifies the locations of JAR files containing target type implementations that the SFC core requires to load. The property accepts two types of path entries:

1. Direct JAR file paths - Paths pointing to specific JAR files
2. Directory paths - Paths to directories containing JAR files. When no directory other than  is specified, the system will automatically include all JAR files found in that directory.

If there are no files or directories, the class will be loaded using the classpath.

This property is optional and allows for flexible JAR file organization, whether you prefer specifying individual JAR files or grouping them in directories.

If `JarFiles' is not specified the current classpath is used to load the factory class and its dependencies.

**Type**: String[]

[^top](#inprocessconfiguration)



## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "FactoryClassName": {
      "type": "string",
      "description": "Fully qualified name of the factory class"
    },
    "JarFiles": {
      "type": "array",
      "items": {
        "type": "string",
        "description": "Path to JAR file or directory"
      },
      "description": "List of JAR files to be loaded or directories containing the JAR files"
    }
  },
  "required": [
    "FactoryClassName",
    "JarFiles"
  ]
}

```



## Examples

JAR Directory configuration

```json
{
  "FactoryClassName": "com.amazonaws.sfc.AdapterNameClassFacory",
  "JarFiles": [
    "./adapters/myadapter-jars-directory",
  ]
}
```



Basic configuration, single jar file

```json
{
  "FactoryClassName": "com.amazonaws.sfc.AdapterNameClassFacory",
  "JarFiles": [
     "./adapters/adapter-jar.jar",
  ]
}
```



Multiple JARs configuration:

```json
{
  "FactoryClassName": "com.amazonaws.sfc.AdapterNameClassFacory",
  "JarFiles": [
    "./adapters/adapter-jar1.jar",
    "./adapters/adapter-jar2.jar",
  ]
}
```



[^top](#inprocessconfiguration)
