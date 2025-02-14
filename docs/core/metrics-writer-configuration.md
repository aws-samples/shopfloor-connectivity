## MetricsWriterConfiguration

[SFC Configuration](./sfc-configuration.md) > [Metrics](./sfc-configuration.md#metrics) > [Writer](./metrics-writer-configuration.md#metricswriter)

[SFC Configuration](./sfc-configuration.md) > [ProtocolAdapters](./sfc-configuration.md#protocoladapters) > [ProtocolAdapter](./protocol-adapter-configuration.md) > [Metrics](./protocol-adapter-configuration.md#metrics) > [Writer](./metrics-writer-configuration.md#metricswriter)

[SFC Configuration](./sfc-configuration.md) > [TargetAdapters](./sfc-configuration.md#targets) > [Target](./target-configuration.md) > [Metrics](./target-configuration.md#metrics) > [Writer](./metrics-writer-configuration.md#metricswriter)

Defines how metrics data is written and transmitted, supporting two modes: in-process writer for direct metrics handling within the application, or IPC-based server configuration for writing metrics through a separate process. This configuration determines the mechanism used to output collected metrics data.

- [Schema](#schema)
- [Examples](#examples)

**Properties:**

- [MetricsServer](#metricsserver)
- [MetricsWriter](#metricswriter)

---
### MetricsServer
Specifies the configuration for an external metrics server that handles metrics data through IPC (Inter-Process Communication). This property defines the connection details (like address and port) for the remote metrics writing service.

**Type**: [ServerConfiguration](./server-configuration.md )

---
### MetricsWriter
Defines the in-process metrics writer implementation configuration, specifying the factory class and JAR files containing the metrics writer code. This configuration enables direct metrics handling within the same process as the application.

**Type**: [InProcessConfiguration](./in-process-configuration.md)

[^top](#metricswriterconfiguration)



## Schema

```'json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "oneOf": [
    {
      "type": "object",
      "properties": {
        "MetricsWriter": {
          "$ref": "#/definitions/InProcessConfiguration",
          "description": "In-process metrics writer configuration"
        }
      }
    },
    {
      "type": "object",
      "properties": {
        "MetricsServer": {
          "$ref": "#/definitions/ServerConfiguration",
          "description": "Metrics server configuration"
        }
      }
    }
  ]
}
```



## Examples

Using in-process MetricsWriter:

```json
{
  "MetricsWriter": {
    "FactoryClassName": "com.amazonaws.sfc.metrics.CloudWatchMetricsWriter",
    "JarFiles": [
      "./cloudwatch-metrics/libs"
    ]
  }
}
```



Using IPC MetricsServer:

```json
{
  "MetricsServer": {
    "Address": "localhost",
    "Port": 50000
  }
}
```

[^top](#metricswriterconfiguration)

