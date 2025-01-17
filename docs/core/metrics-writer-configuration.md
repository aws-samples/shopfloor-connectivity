## MetricsWriterConfiguration

[SFC Configuration](./sfc-configuration.md) > [Metrics](./sfc-configuration.md#metrics) > [Writer](./metrics-writer-configuration.md#metricswriter)

[SFC Configuration](./sfc-configuration.md) > [ProtocolAdapters](./sfc-configuration.md#protocoladapters) > [ProtocolAdapter](./protocol-adapter-configuration.md) > [Metrics](./protocol-adapter-configuration.md#metrics) > [Writer](./metrics-writer-configuration.md#metricswriter)

[SFC Configuration](./sfc-configuration.md) > [TargetAdapters](./sfc-configuration.md#targets) > [Target](./target-configuration.md) > [Metrics](./target-configuration.md#metrics) > [Writer](./metrics-writer-configuration.md#metricswriter)

- [Schema](#schema)
- [Examples](#examples)

**Properties:**

- [MetricsServer](#metricsserver)
- [MetricsWriter](#metricswriter)

---
### MetricsServer
Server providing the metrics writer service

**Type**: [ServerConfiguration](./server-configuration.md )

---
### MetricsWriter
Jar files implementing the writer

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

