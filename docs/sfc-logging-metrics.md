# SFC Logging and metrics collection

- [Logging](#logging)

- [Metrics Collection](#metrics-collection)
  
  - [Running Metrics writers as an IPC service](#running-metrics-writers-as-an-ipc-service)
  - [Running metric writers in-process](#running-metric-writers-in-process)
  
  

# Logging

By default, log information is written to the console.

There are 4 trace levels, Error (stderr), Warning(stdout), Info(stdout) and Trace(stdout) which can be specified when
starting the SCF core or a protocol adapter or target writer service.

Logging output will contain the system date and time, the logging level, source of the event and a message. The logging
infrastructure will intercept and blank the values of secrets configured in the SFC configuration.

Instead of writing to the console custom log writer can be implemented and [configured](./core/sfc-configuration.md#logwriter). Details on how to implement a
custom log writer can be found in section [Custom Logging](#logging).



## Metrics collection

Metrics collection is enabled by adding a Metrics configuration section in the top level of the SFC configuration. This section specifies the writer for metrics data, which can be either an in-process metrics writer or a MetricsServer. For an in-process metrics writer, specify the jar files that implement it and provide a factory class name to create an instance. For a MetricsServer, specify the address and port number of the service.

Metrics can be disabled by setting the "Enabled" property to false in the Metrics section. This disables metrics collection from all sources. A property "Namespace" can be set for use by the writer implementation, with a default value of "SFC".

The metrics collector automatically gathers warning and error messages from SFC logging. The default collection interval is 60 seconds, which can be modified by setting the "Interval" property to the desired time in seconds.

This structure provides a clear overview of how to configure metrics collection in the SFC system, including options for enabling/disabling, setting the namespace, and adjusting the collection interval.

For each metrics data point the following information is collected:

- name
- value
- units
- timestamp
- dimensions

By default, the dimensions are:

- source: name of the component that generated the datapoint. For protocol adapters this is the identifier of the
  adapter or the adapter and the source (separated by a ":" ) from the configuration. For targets the source is the
  identifier of the target from the configuration. For the code it is "SfcCore".
- category: can be "Target", "Adapter" or "Core"
- type: the actual type of the connector (e.g., "OpcuaAdapter"), target (e.g., "AwsSqsTargetWriter") or "SfcCore"

Additional dimensions can be added by adding a "CommonDimensions" property in the metrics section which is a map with
name-value pairs.

Additionally, each adapter or target can have a Metrics section with an Enabled property to enable or disable the
collection of metrics for that component, and a map of CommonDimensions which will be added to every data point
collected for that component.

Additional settings can be set for the actual configured writer. For the AWS CloudWatch Metrics writer, a section
named "CloudWatch" can be added with the following properties:

- CredentialProviderClient: name of a configured client in the [AwsIotCredentialProviderClients](./core/sfc-configuration.md#awsiotcredentialproviderclients) section of the SFC
  configuration to use to obtain credentials to put metrics data. (The role used for the client must give permission for
  calls to the PutMetricData API call for the AWS CloudWatch service). If no client is configured
  the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain).
- Region: Region used for the AWS CloudWatch Service
- Interval: Interval in seconds to write to AWS CloudWatch. Metrics are written at least once with this interval or
  earlier if the maximum of 1000 data points or the configured buffer size is reached.
- BatchSize: Size of the buffer used to store data points before these are written to CloudWatch, or earlier if the
  interval period is reached.

The following metric values are collected:

| **Metric name**         | **Description**                                               | **Collected by**                     |
|-------------------------|---------------------------------------------------------------|--------------------------------------|
| BytesReceived           | Bytes read by the adapter                                     | ModbusTCP, PCCC, ADS, SLMP connector |
| BytesSend               | Bytes send by the adapter                                     | ModbusTCP, PCCC, ADS, SLMP connector |
| BytesWritten            | Bytes written by target                                       | Selected adapters                    |
| Connection              | Number of connections                                         | All connectors                       |
| ConnectionErrors        | Number of failed connections                                  | All connectors                       |
| Errors                  | Number of logged errors                                       | Core and all connectors and targets  |
| Memory                  | Used memory by process in MB                                  | Core and all connectors and targets  |
| MessageBufferedSize     | Size of buffered messages in bytes                            | StoreForwardTarget                   |
| Messages                | Number of messages processed                                  | All targets                          |
| MessagesBufferedCount   | Number of buffered messages                                   | StoreForwardTarget                   |
| MessagesBufferedDeleted | Number of messaged deleted                                    | StoreForwardTarget                   |
| ReadDuration            | Time in milliseconds used by adapter to read data from source | All adapters                         |
| ReadErrors              | Number of read errors                                         | All adapters                         |
| Reads                   | Number of reads                                               | All adapters                         |
| ReadSuccess             | Number of succeeded reads                                     | All adapters                         |
| Values read             | Number of values read                                         | All adapters                         |
| Warnings                | Number of logged warnings                                     | Core and all connectors and targets  |
| WriteDuration           | Time in milliseconds used by target to write data             | All targets                          |
| WriteErrors             | Number of failed writes                                       | All Targets                          |
| Writes                  | Writes by targets                                             | All targets                          |



## Running Metrics writers as an IPC service.

The writers have a service wrapper that enables these targets can be executed as an IPC Service process. For each
writer, a tar file is generated by the build process that includes the application script file to start the service, as
well as all required library files. The application tar file contains script files (`bin/<targettype>`
*and* `bin/<targettype>.bat`) to launch the applications, and all required libraries (/lib/*.jar)

| **Writer**             | **Application name**   | **Main class**                                                 |
|------------------------|------------------------|----------------------------------------------------------------|
| AWS CloudWatch Metrics | Aws-cloudwatch-metrics | com.amazonaws.sfc.cloudwatch.AwsCloudWatchMetricsWriterService |

The writers do have all the following command line parameters in common.

| Parameter     | Description                                                                                                                                                                                                                                                                                                                                                                                            |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| -connection   | Security level used to secure traffic between SFC core and metrics service. PlainText : No encryption ServerSideTLS : Data is encrypted, requires -cert and -key parameters MutualTLS : Data is encrypted, required -cert, ca and -key parameters The connection type must match the connection type, as set to the ConnectionType attribute for the client, to communicates with the metrics service. |
| -cert         | Server certificate file to secure IPC (gRPC) traffic for connection types ServerSideTLS and MutualTLS                                                                                                                                                                                                                                                                                                  |
| -key          | Server private file to secure IPC (gRPC) traffic for connection types ServerSideTLS and MutualTLS                                                                                                                                                                                                                                                                                                      |
| -ca           | CA certificate file to secure IPC (gRPC) traffic for connection type MutualTLS                                                                                                                                                                                                                                                                                                                         |
| -envport      | The name of the environment variable that contains the port number for the service to listen on for requests.                                                                                                                                                                                                                                                                                          |
| -error        | Set log output level to error level. (Error message only)                                                                                                                                                                                                                                                                                                                                              |
| -h, -help     | Shows command line parameter help.                                                                                                                                                                                                                                                                                                                                                                     |
| -info         | Set log output level to info level. (Info, warning and error messages)                                                                                                                                                                                                                                                                                                                                 |
| -key          | Key file to secure IPC (gRPC) traffic using SSL (optional).                                                                                                                                                                                                                                                                                                                                            |
| -port         | port number for the service to listen on for requests.                                                                                                                                                                                                                                                                                                                                                 |
| -trace        | Set log output level to most detailed trace level (Info, warning, error, and detailed trace messages)                                                                                                                                                                                                                                                                                                  |
| -warning      | Set log output level to warning level. (Error and warning messages)                                                                                                                                                                                                                                                                                                                                    |

The port number, used by the service, can be specified using different methods which are applied in the following order

- The value of the `-port` command line parameter
- The value of the environment variable specified by the `-envport` parameter

After the service is started is it waiting for an initialization call on the specified port. The core is using an IPC
client to send the configuration data, which has common but also writer type specific elements, to the service that will
use it to initialize the actual writer. The client will use a client streaming method call to stream the metrics data to
the writer.

## Running metric writers in-process

To run metric writers in the same process as the SFC core, they need to be implemented for the same JDK as used for the
core. To make it possible to use a custom writer without making changes to the SFC code, there are no links in the core
to the libraries that implement the target. In the configuration of an in-process metric writer type, the pathnames of
the jar files that contain the classes that implement the writer need to be explicitly configured. When the SFC core
creates an instance of the writer, it loads the configured jar files and uses a static factory method to create the
actual instance. The name of the factory class, which could be the actual writer class itself, needs to be configured as
well.

The jar files are part of the target deployment and can be found in the lib directory of the deployment package. To
specify the path to the jar files it is recommended to use a placeholder, instead of hard-coding, the directory where
the adapter, and targets, are deployed and set an environment variable for this directory.

