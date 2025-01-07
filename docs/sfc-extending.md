# Extending the SFC Framework

- [Implementing a protocol adapter](#implementing-a-protocol-adapter)
  - [Creating an in-process protocol adapter instance](#creating-an-in-process-protocol-adapter-instance)
  - [IPC adapter services](#ipc-adapter-services)
  - [Using JVM protocol adapter classes as IPC services](#using-jvm-protocol-adapter-classes-as-ipc-services)
- [Implementing a target adapter](#implementing-a-target-adapter)
  - [Creating in-process adapter instances](#creating-in-process-adapter-instances)
  - [IPC target services](#ipc-target-services)
  - [Using JVM target classes as IPC services](#using-jvm-target-classes-as-ipc-services)

- [Custom Configuration Handlers](#custom-configuration-handlers)
- [Custom Logging](#custom-logging)
- [Custom Metric Writers](#custom-metric-writers)
  - [Metrics Collection](#metrics-collection)




This section describes how additional protocol adapters and targets can be implemented.

Both protocol adapters and targets can be implemented in languages like Java, Kotlin, or any other JVM language. These
adapters and targets have the option to run in the same process as the SFC core module or as an external IPC service.
The SFC framework, which is a JVM-based application, provides a set of classes that implements most of the
infrastructure for adapter and target servers, logging, and configuration, so developers can focus on implementing the
actual protocol.

For protocols or targets that require libraries or languages that cannot be executed in a JVM environment, an IPC server
implementation can be used. The requirement is that the language and runtime must support the gRPC protocol.


# Implementing a protocol adapter

The main activity of a protocol adapter is to read data from industrial devices using a specific protocol. The SFC core
instructs the protocol adapter which data to read. The core itself is not aware of the actual protocol used by the
adapter and the instructions are generic, so they can be used for any type of adapter. The implementation of an adapter
will use these instructions, and its specific configuration data, to translate to execute protocol-specific API or
service calls to read the data. The data is returned to the SFC core in a format that is not specific to the used
protocol.

For JVM implementations the SFC core defines the following interface:

```kotlin
interface ProtocolAdapter {
  // Read channel values from a source
  suspend fun read(sourceID: String, channels: List<String>?): SourceReadResult

  // Stop the adapter
  suspend fun stop(timeout: Duration)
}
```

Protocol implementations **need to implement** this interface.

## Read function

This method takes the source ID which refers to a protocol-specific source configuration. The schedules running in the
SFC core can request data from multiple sources that use the same protocol, so the adapter may receive requests for
different sources. The channels parameter is a list of value names, which are a part of the source configuration. Note
that all of these values are simple protocol agnostic string identifiers. The adapter implementation will need to map
these identifiers, using its specific configuration for the source and values to the required API or service calls.

Examples of sources and channels for protocols are:

- OPCUA, sources are OPCUA server, channels are OPCUA nodes
- MODBUS, sources are MODBUS devices, channels are (ranges of) registers or discrete input or outputs
- MQTT, sources are brokers, channels are topic names

The returned SourceReadResult can be an instance of either a SourceReadSuccess if the values were read successfully from
the source, or a SourceReadError if the reading of the values failed.

A SourceReadSuccess contains a map of ChannelReadValues, indexed by their abstract channel name. Each ChannelReadValue
holds the actual value that was read, which could be of any type, and optionally a timestamp for that value. The besides
this per value timestamp, the SourceReadSuccess also contains a timestamp at the source level. If the timestamp is the
same for each read value are the same then this source level timestamp can be used to reduce the volume of data. The SFC
core will automatically use the source level timestamp if a value does not have a per value timestamp.

If no timestamps are set by the adapter the SFC core will use the local date and time as the moment of reading.

The SourceReadError, which is returned if reading from a source failed, contains a description of the error and a
timestamp. The SFC core will automatically log these errors.

When the SFC core is stopped it will create the adapter stop method to let the adapter cleanup resources or close any
sessions.



## Creating an in-process protocol adapter instance

The SFC core is responsible for creating and closing down instances of adapters that run in the same process. As the SFC
core is not aware of the actual protocol it depends solely on the InProcess configuration for the protocol source. This
configuration contains which jar files that implement the adapter will need to be explicitly loaded by the SCF core
process and the name of a factory class. After loading the jar files the core will create an instance of the factory
class and call it the static "newInstance" method.

Each adapter implementation must implement a factory class that implements this method with the following signature:

```kotlin
fun newInstance(vararg createParameters: Any?): SourceValuesReader?
```

4 values are passed through createParameters by the core when creating an in-process instance of the adapter.

These values are:

- *configReader*: ConfigReader,
- *scheduleName*: String,
- *adapterID*: String
- *logger*: Logger


The *configReader* is an abstraction of the SFC configuration, as each protocol implementation has, besides the common
SFC core values, its specific configuration and overridden configuration types. The adapter implementation can simply
obtain an instance of its configuration by calling the getConfig method, specifying configuration type to return parsed
from the JSON data held by the configReader.

The *scheduleName* is the name of the schedule which is using the protocol adapter.

The *adapterID* is the ID of the adapter in the configuration

The *logger* is an abstraction for logging error, information, warning, and trace level messages.

The newInstance method uses the configuration to create an instance of adapter class that implements the
SourceValuesReader. If creating the instance fails due to configuration or other issues the reason can be logged using
the provided logger and the method returns null.

## IPC adapter services

To run the adapter in a different process, or on a different device, as the SFC core, for reasons of scaling,
runtime/JVM requirements, etc., it needs to implement a gRPC IPC service. The service needs to be started explicitly by
a system service, as a GreenGrass component or a Docker container.

The gRPC service, which can be implemented in any language or runtime supporting gRPC, needs to implement the
ProtocolAdapterService:

```kotlin
service ProtocolAdapterService {
  // Reads values, server-side streaming
  rpc ReadValues (ReadValuesRequest) returns (stream ReadValuesReply) {}
  rpc InitializeAdapter (InitializeAdapterRequest) returns (InitializeAdapterResponse){}
}
```

The InitializeAdapter message is sent by the core to the service, providing it with the subset of the configuration
information that is relevant for the adapter instance. This allows the service to bootstrap with a minimum of
configuration, just enough to bootstrap and listen for the InitializeAdapter request. When the SFC core starts, it will
send a specific InitializeAdapterRequest to the adapter service. The service uses the configuration information in the
request to (re-)configure the protocol adapter. The service returns a response containing an indication of whether the
configuration of the adapter was successful, and if this is not the case additional error information. When the request
fails, a timeout occurs or the service is not reachable, then the SFC core will periodically retry by re-sending the
request. The configuration, as JSON format in the adapterConfiguration field, contains all relevant configuration data
selected by the SFC core for that adapter. The adapter can use an instance of the SFC ConfigReader class, to read the
configuration data as an instance of the configuration type class for the adapter.

The ReadValues method is a streaming server request, meaning that after receiving the ReadValues request from the SCF
core it can stream values it read with the specified interval back to the client, that resides in the SFC core until the
SFC core closes the connection. The SourceReadValueRequest contains the identifier of the source and a list of channels
to read for that source, similar to the ProtocolAdapters interface read method parameters, used for JVM implementations
of an adapter. This makes it possible to provide a JVM implementation of an adapter that can run in the SFC core
process, as well as an IPC service, with little effort as the SFC implementation contains generic Service helper classes
to wrap the adapter classes.

The data returned by the service as stream to the core contains the ID of the source, a map indexed by the channel names
containing the values, a timestamp, and in case of an error description. The structure of the returned data is the same
as returned by the ProtocolAdapters read method. A major difference is that, to provide type-fidelity between the data
read by the adapter and received by the SFC core, that the message for returning the ChannelValues has a specific one-of
field in the message for every datatype supported by the SFC core. The SFC framework has helpers that abstract storing
the value in the distinctive field for the type of the data by the adapter. The SFC core has internal helpers to extract
the data in the original format. Additional wrappers for other than JVM implementation will be part of future adapter
implementations.

## Using JVM protocol adapter classes as IPC services

An adapter class that implements the ProtocolAdapter interface can simply be wrapped in a ServiceMain class to execute
it as a gRPC IPC service.

The code below shows the implementation of the OPCUA service that uses the ServiceMain class to wrap an instance of the
MqttAdapter class as a standalone service application.

```kotlin
class OpcuaServiceMain(logger: Logger) : ServiceMain(logger) {

  override fun createServiceInstance(args: Array<String>, logger: Logger): Service {
    return ProtocolAdapterService.createProtocolAdapterService(
      args,
      logger
    ) { _configReader: ConfigReader, _logger: Logger ->
      OpcuaAdapter.createMqttAdapter(_configReader, _logger)

    }
  }

  companion object {
    @JvmStatic
    @JvmName("main")
    fun main(args: Array<String>) = runBlocking {
      OpcuaServiceMain(logger = Logger.defaultLogger()).run(args)
    }
  }
}
```

The OpcuaServiceMain class simply inherits from the Service main class. It overrides the createServiceInstance method so
that it creates an instance of the OpcuaAdapter class returned by the ProtocolAdapterService.createProtocolAdapterService
helper method. The OpcuaAdapter.createOpcuaAdapter is a static method of the MqttAdapter that hides the actual creation of
the instance.

```kotlin
fun createMqttAdapter(configReader: ConfigReader, logger: Logger): ProtocolAdapter {

  // obtain mqtt configuration
  val config: MqttConfiguration = try {
    configReader.getConfig()
  } catch (e: Exception) {
    throw Exception("Error loading configuration: ${e.message}")
  }
  // create instance of adapter    
  return MqttAdapter(config, logger)
}
```

The main method of the server application can simply create an instance of the class, passing the command line
parameters which are parsed consistently for all servers and call the run method to run the service.



# Implementing a target adapter

The main activity of a target adapter is to write the data to a service or local target. The SFC core
sends the data to be written to the configured target adapters. The core itself is not aware of the actual target adapter.The implementation of a target adapter will receive the data and its specific configuration data, to translate to execute target-specific API or
service calls to write the data.

For JVM implementations the SFC core defines the following interface:

```kotlin
interface TargetWriter {
  
    suspend fun writeTargetData(targetData: TargetData)
  
    suspend fun close()
  
    val isInitialized: Boolean
        get() = true

    val metricsProvider: MetricsProvider?
}
```

Target implementations **need to implement** this interface.

## writeTargetData function

The writeTargetData method is responsible for writing the data, received from the SFC core process, to the target specific destination.

When the SFC core is stopped it will create the adapter stop method to let the adapter cleanup resources or close any
sessions.

[^top](#extending-the-sfc-framework)

## Creating in-process adapter instances

The SFC core is responsible for creating and closing down instances of targets that run in the same process. As the SFC
core is not aware of the actual target it depends solely on the InProcess configuration for the target. This
configuration contains which jar files that implement the target will need to be explicitly loaded by the SCF core
process and the name of a factory class. After loading the jar files the core will create an instance of the factory
class and call it the static "newInstance" method.

Each adapter implementation must implement a factory class that implements this method with the following signature:

```kotlin
fun newInstance(vararg createParameters: Any?): TargetWriter?
```

4 values are passed through createParameters by the core when creating an in-process instance of the adapter.

These values are:

- *configReader*: ConfigReader,
- *targetID*: String,
- *logger*: Logger
- *resultHandler*: TargetResultHandler

The *configReader* is an abstraction of the SFC configuration, as each target implementation has, besides the common
SFC core values, its specific configuration and overridden configuration types. The adapter implementation can simply
obtain an instance of its configuration by calling the getConfig method, specifying configuration type to return parsed
from the JSON data held by the configReader.

The *targetID* is the targetID for the adapter in the SFC configuration.

The *logger* is an abstraction for logging error, information, warning, and trace level messages.

The *resultHandler* is an interface that can be passed by a previous intermediate target if the target is part of a
chain of targets. The interface is used to return the results from delivering the data to the destination, e.g. a cloud
service, of the target. The interface has two methods:

- *returnedData*: which returns an instance of ResultHandlerData that contains information that the calling target
  expects to receive for acknowledged (ack), not acknowledged (nack) or error messages (error). This can either be the
  complete message, or just the serial number of the message or nothing.

- *handleResult*: is called by the target to result of delivering the message to the previous target in a chain. An
  instance of the TargetResult class is passed a parameter that includes the ID of the target and a list of serials
  numbers , or complete messages (see returnedData above) for delivered messages (ack), messages that could not be
  delivered due to loss of network connection or the destination service not available (nack) or messages that could not
  be processed by the target (error).

The TargetResultHelper and TargetBufferedResultHelper classes can be used to simplify reporting the result data by the
target.

The newInstance method uses the configuration to create an instance of target class that implements the
TargetWriter interface. If creating the instance fails due to configuration or other issues the reason can be logged using
the provided logger and the method returns null.

## IPC target services

To run the target in a different process, or on a different device, as the SFC core, for reasons of scaling,
runtime/JVM requirements, etc., it needs to implement a gRPC IPC service. The service needs to be started explicitly by
a system service, as a GreenGrass component or a Docker container.

The gRPC service, which can be implemented in any language or runtime supporting gRPC, needs to implement the
TargetAdapterService:

```kotlin
service TargetAdapterService{
  // Client side streaming of values to target service
  rpc WriteValues(stream WriteValuesRequest) returns (stream TargetResultResponse) {}
  rpc ReadMetrics(ReadMetricsRequest) returns(stream MetricsDataMessage){}
  rpc InitializeTarget(InitializeTargetRequest) returns (InitializeTargetResponse){}
}
```

The InitializeTargetMessage is sent by the core to the service, providing it with the subset of the configuration
information that is relevant for the target instance. This allows the service to bootstrap with a minimum of
configuration, just enough to bootstrap and listen for the InitializeAdapter request. When the SFC core starts, it will
send a specific InitializeTarget to the adapter service. The service uses the configuration information in the
request to (re-)configure the target. The service returns a response containing an indication of whether the
configuration of the target was successful, and if this is not the case additional error information. When the request
fails, a timeout occurs or the service is not reachable, then the SFC core will periodically retry by re-sending the
request. The configuration, as JSON format in the InitializeTargetRequest field, contains all relevant configuration data
selected by the SFC core for that target. The adapter can use an instance of the SFC ConfigReader class, to read the
configuration data as an instance of the configuration type class for the target.

The WriteValues method is a streaming client request, meaning that after making the WriteValues request from the SCF
core it will stream values to the target until the SFC core closes the connection.

The WriteValuesRequest contains data similar to the parameters of the TargetWriter interface writeTargetData method parameters,

used for JVM implementations of a target. This makes it possible to provide a JVM implementation of a target that can run in the SFC core
process, as well as an IPC service, with little effort as the SFC implementation contains generic Service helper classes
to wrap the target classes.

## Using JVM target classes as IPC services

A target class that implements the TargetWriter interface can simply be wrapped in a ServiceMain class to execute
it as a gRPC IPC service.

The code below shows the implementation of the AWS IoT Core target service that uses the ServiceMain class to wrap an instance of the
AwsIotCoreTargetWriter class as a standalone service application.

```kotlin
class AwsIotCoreTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service? {
        return createIpcTargetServer(args, configuration, AWS_IOT_CORE_TARGET, logger) { _configReader, _targetID, _logger, _resultHandler ->
            AwsIotCoreTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsIotCoreTargetService().run(args)
        }
    }
}
```

The main method of the server application can simply create an instance of the class, passing the command line
parameters which are parsed consistently for all servers and call the run method to run the service.

[^top](#extending-the-sfc-framework)

# Custom Configuration Handlers

Custom configuration handlers can be configured for custom handling, modifying or creating SFC configurations. The jar
files that implement the handler and the factory class to create instances of the handler can be configured in the
configuration file.

A custom handler is a class that implements the ConfigProviderInterface

```kotlin
interface ConfigProvider {
  val configuration: Channel<String>?
}
```

This interface has a single property, which is a channel to which versions of the configuration file are written. Each
time a new version of the configuration data, which must be a valid SFC configuration file, it is read by the SFC core
which will apply the changed configuration. The handler can use the date from the configuration file, which may contain
specific sections for this the type of handler, which is passed as a configuration string when the instance is created.

Each handler implementation must implement a factory class that implements a method with the following signature:

```kotlin
fun newInstance(vararg createParameters: Any?): ConfigProvider?
```

2 values are passed through createParameters by the core when creating an in-process instance of the adapter.

These values are:

- configString : String containing the input data from the configuration file
- configVerificationKey: PublicKey? Used to verify the content of the configuration
- logger: Logger, Logger log results of handler



# Custom Logging

Custom logging writers can be configured for writing log data, which is by default written to the console. The jar files
that implement the writer and the factory class to create instances of the writer can be configured in the configuration
file.

A custom writer is a class that implements the ConfigWriter interface

```kotlin
interface LogWriter {
  fun write(logLevel: LogLevel, timestamp: Long, source: String?, message: String)
  fun close()
}
```

Each writer implementation must implement a factory class that implements a method with the following signature:

```kotlin
fun newInstance(vararg createParameters: Any?): ConfigWriter?
```

A single value is through createParameters by the core when creating an in-process instance of the writer which is the
configuration that may contain specific section for the type of the writer.

[^top](#extending-the-sfc-framework)

# Custom Metric Writers

Metrics Writers are used to write metrics data points collected by SFC to a metrics storage or processing destination (
e.g., AWS CloudWatchMetrics)

A metrics Writer is a class that implements the MetricWriter Interface

```kotlin
interface MetricsWriter {
  suspend fun writeMetricsData(metricsData: MetricsData)
  suspend fun close()
}
```

Each metrics writer implementation must implement a factory class that implements a static method with the following
signature:

```kotlin
fun newInstance(configReader: ConfigReader): MetricsWriter?
```

The core passes a configuration reader to the method that the implementation can use to load a (writer specific)
configuration from the SFC configuration.

An implementation of a metrics writer can be exposed as an IPC service. This IPC service is defined in metrics.proto as
MetricsWriterService

```kotlin
Service MetricsWriterService {
  rpc WriteMetrics (stream MetricsDataMessage) returns (google.protobuf.Empty)
  rpc InitializeMetricsWriter (InitializeMetricsWriterRequest) returns (InitializeMetricsWriterResponse){}
}
```

For the service base class provided by the SFC framework can be used. The Service class for the writer needs to override
the abstract method createServiceInstance of that class by a method that creates an instance of the actual writer.

```kotlin
class AwsCloudWatchMetricsWriterService : ServiceMain() {

  override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service? {
    return createIpcMetricsServer(args, configuration, logger) { _configReader, _logger ->
      AwsCloudWatchMetricsWriter.newInstance(_configReader, _logger)
    }
  }

  companion object {
    @JvmStatic
    @JvmName("main")
    fun main(args: Array<String>): Unit = runBlocking {
      AwsCloudWatchMetricsWriterService().run(args)
    }
  }
}
```

The method creating the instance of the writer is provided as function parameter of the createIpcMetricsServer method.
This method is receiving a config reader and a logger instance.

## Metrics Collection

Protocol adapters and targets which do support the collection of metrics must return a non-null instance of an
implementation of the MetricsProvider interface as defined in the ProtocolAdapter or TargetWriter interface. The
component using the adapter or the target will use the interface to read the collected metrics.

If the adapter or target is hosted in an IPC service process, then the base classes for these services will provide the
metrics provider as part of the exposed service that will provide metrics as a server-side streaming methods. The IPC
client classes for adapters and targets,do implement a MetricsProvider implementation that will invoke and read the data
from the method that will stream the data to the client.
