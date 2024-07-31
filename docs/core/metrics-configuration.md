## MetricsConfiguration

| Configuration for metrics configuration. In order to collect and write metrics this section must include a metrics writers |                                                                         |                                                          |                  |
|----------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|----------------------------------------------------------|------------------|
| **Name**                                                                                                                   | **Description**                                                         | **Type**                                                 | **Comments**     |
| Enabled                                                                                                                    | Set to false to disable metrics collection                              | Boolean                                                  | Default= true    |
| CommonDimensions                                                                                                           | Set of extra dimensions added to every datapoint                        | Map(String,String)                                       | Optional         |
| CollectCoreMetrics                                                                                                         | Set to false to disable collection from core metrics data               | Boolean                                                  | Default= true    |
| Interval                                                                                                                   | Interval in seconds for reading metrics from adapters, targets and core | Integer                                                  | 10               |
| Writer                                                                                                                     | Used writer for metrics data                                            | [MetricWriterConfiguration](./metrics-writer-configurarion.md) |                  |
| Namespace                                                                                                                  | Namespace for collected metrics                                         | String                                                   | Default is "SFC" |


[SfcTopLevelConfiguration](sfc-top-level-config.md)

[^top](../../README.md#toc)