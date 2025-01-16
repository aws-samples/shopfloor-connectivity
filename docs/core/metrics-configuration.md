[SFC Configuration](./sfc-configuration) > [Metrics](./sfc-configuration#Metrics)

[SFC Configuration](./sfc-configuration) > [ProtocolAdapters](./sfc-configuration#ProtocolAdapters) > [ProtocolAdapter](./protocol-adapter-configuration.md) > [Metrics](./protocol-adapter-configuration.md#Metrics) 

[SFC Configuration](./sfc-configuration) > [Targets](./sfc-configuration#Targets) > [TargetAdapter](./target-configuration.md) > [Metrics](./target-configuration.md#Metrics) 

## MetricsConfiguration

- [Schema](#Schema)
- [Examples](#Schema)

**Properties:**

- [CollectCoreMetrics](#CollectCoreMetrics)
- [CommonDimensions](#CommonDimensions)
- [Enabled](#Enabled)
- [Interval](#Interval)
- [Namespace](#Namespace)
- [Writer](#Writer)

---
### CollectCoreMetrics
Collection of core detailed metrics enabled or disabled

**Type**: Boolean

Default is true

---
### CommonDimensions
Set of extra dimensions added to every datapoint

**Type**: Map(String,String)

Optional

---
### Enabled
Collection enabled or disabled

**Type**: Boolean

Default is true

---
### Interval
Interval in seconds for reading metrics from adapters, targets and core

**Type**: Integer

Default is 10

---
### Namespace
Namespace for collected metrics

**Type**: String

Default is "SFC"

---
### Writer
Writer for writing collect metrics data

**Type**: [MetricsWriterConfiguration](./metrics-writer-configuration)

[^top](#MetricsConfiguration)

## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "CollectCoreMetrics": {
      "type": "boolean",
      "description": "Flag to enable/disable collection of core metrics",
      "default": true
    },
    "CommonDimensions": {
      "type": "object",
      "description": "Map of dimension names to values, both strings",
      "patternProperties": {
        "^.*$": {
          "type": "string"
        }
      },
      "additionalProperties": false
    },
    "Enabled": {
      "type": "boolean",
      "description": "Flag to enable/disable metrics collection",
      "default": true
    },
    "Interval": {
      "type": "integer",
      "description": "Interval in milliseconds for metrics collection",
      "minimum": 10
    },
    "Namespace": {
      "type": "string",
      "description": "Namespace for the metrics",
      "default": "SFC"
    },
    "Writer": {
      "$ref": "#/definitions/MetricsWriterConfiguration",
      "description": "The metrics writer implementation configuration"
    }
  },
  "required": [
    "Writer"
  ]
}
```



## Examples

IPC metrics writer

```json
{
  "Writer": {
    "CommonDimensions": {
      "Environment": "Production",
      "Plant": "us-west"
    },
    "Interval": 60,
    "MetricsServer": {
      "Address": "localhost",
      "Port": 50000
    }
  }
}
```



In-process writer configuration:

```json
{
  "Enabled": true,
  "Interval": 60,
  "CommonDimensions": {
    "Environment": "Production",
    "Plant": "us-west"
  },
  "Writer": {
    "MetricsWriter": {
      "FactoryClassName": "com.amazonaws.sfc.metrics.CloudWatchMetricsWriter",
      "JarFiles": [
        "./aws-cloudwatch-metrics/libs
      ]
    }
  }
}
```

[^top](#MetricsConfiguration)

## 
