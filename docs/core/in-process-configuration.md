## InProcessConfiguration

[SFC Configuration](./sfc-configuration) > [ProtocolAdapterTypes](./sfc-configuration#ProtocolAdapterTypes)

[SFC Configuration](./sfc-configuration) > [TargetTypes](./sfc-configuration#TargetTypes) 

[SFC Configuration](./sfc-configuration) > [Metrics](./sfc-configuration#Metrics) > [Writer](./metrics-writer-configuration#MetricsWriter)

[SFC Configuration](./sfc-configuration) > [LogWriter](./sfc-configuration#LogWriter)

[SFC Configuration](./sfc-configuration) > [ConfigProvider](./sfc-configuration#ConfigProvider)

- [Schema](#Schema)
- [Examples](#Examples)

**Properties:**

- [FactoryClassName](#FactoryClassName)
- [JarFiles](#JarFiles)

---
### FactoryClassName
Name of the factory class used to create instances of a source protocol adapter of a target.
For target instances, this class must have a static method named "newInstance".
The signature of the method for protocol adapters have 3 parameters:

- ConfigReader, the reader that can be used by the newly created instance to read its configuration data.
- Schedule name for protocol adapters instances or  the target identifier for the target instances.

- Logger, logger for output of the newly created instance

The newInstance method for targets has an additional parameter: 

- TargetResultHandler, an instance of an object that implements this interface can be passed to let the target return the result of delivering the target data to their destinations. It can be used ACK, NACK and ERROR the serial numbers or full message by using the interface handleResult method. The method returnedData can be called to query what data the result handler expects to be returned. (serial or full message for each of these types).


**Type**: String

---
### JarFiles
List of path names to JarFiles, that implement a target type, that needs to be loaded by the SFC core.
These entries can either be path names to the jar files of to the directory in which these reside. If the entry is a directory it will expand to a list of all jar files in that directory,

**Type**: String[]

[^top](#InProcessConfiguration)



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
     "./adapters/adapter-jar.jar,
  ]
}
```



Multiple JARs configuration:

```json
{
  "FactoryClassName": "com.amazonaws.sfc.AdapterNameClassFacory",
  "JarFiles": [
    "./adapters/adapter-jar1.jar,
    "./adapters/adapter-jar2.jar,
  ]
}
```



[^top](#InProcessConfiguration)
