## InProcessConfiguration


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
List of pathnames to JarFiles, that implement a target type, that needs to be loaded by the SFC core.
These entries can either be pathnames to the jar files of to the directory in which these reside. If the entry is a directory it will expand to a list of all jar files in that directory,

**Type**: String[]

[^top](#InProcessConfiguration)

