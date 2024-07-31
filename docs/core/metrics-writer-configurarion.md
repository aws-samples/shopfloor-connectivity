 
## MetricsWriterConfiguration

| Writer used to write collected metrics data. This writer can be configured as an in-process instance or a client to and IPC service. If both options are configured the IPC service is used. |                                             |                                                   |              |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|---------------------------------------------------|--------------|
| **Name**                                                                                                                                                                                     | **Description**                             | **Type**                                          | **Comments** |
| MetricsWriter                                                                                                                                                                                | Jar files implementing the writer           | [InProcessConfigurarion](#inprocessconfiguration) |              |
| MetricsServer                                                                                                                                                                                | Server providing the metrics writer service | [ServerConfigurartion](#serverconfiguration)      |              |

[SfcTopLevelConfiguration](sfc-top-level-config.md)

[^top](../../README.md#toc)