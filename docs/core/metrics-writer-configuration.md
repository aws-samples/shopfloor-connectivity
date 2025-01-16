[SFC Configuration](./sfc-top-level-config.md#Metrics) > [Metrics](./sfc-top-level-config.md#Metrics) > [Writer](./metrics-writer-configuration#MetricsWriter)

[SFC Configuration](./sfc-top-level-config.md) > [ProtocolAdapters](./sfc-top-level-config.md#ProtocolAdapters) > [ProtocolAdapter](./protocol-adapter-configuration.md) > [Metrics](./protocol-adapter-configuration.md#Metrics) > [Writer](./metrics-writer-configuration#MetricsWriter)

[SFC Configuration](./sfc-top-level-config.md) > [TargetAdapters](./sfc-top-level-config.md#Targets) > [Target](./target-configuration.md) > [Metrics](./target-configuration.md#Metrics) > [Writer](./metrics-writer-configuration#MetricsWriter)

## MetricsWriterConfiguration

- [Schema](#Schema)
- [Examples](#Examples)

**Properties:**

- [MetricsServer](#MetricsServer)
- [MetricsWriter](#MetricsWriter)

---
### MetricsServer
Server providing the metrics writer service

**Type**: [ServerConfiguration](./server-configuration.md )

---
### MetricsWriter
Jar files implementing the writer

**Type**: [InProcessConfiguration](./in-process-configuration.md)

[^top](#MetricsWriterConfiguration)



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

[^top](#MetricsWriterConfiguration)

