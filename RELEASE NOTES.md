# Release Notes:

## version 1.10.3, 12  Aug 2025

- Restored creation of the sfc deployment bundle

---

## version 1.10.2, 11  Aug 2025

- Documentation update

---

## version 1.10.1, 6 Aug 2025

- Updated the [maximum size limit for messages in SQS](https://aws.amazon.com/about-aws/whats-new/2025/08/amazon-sqs-max-payload-size-1mib/) target to 1MB,

---
## version 1.10.0, 9 July 2025

- New: [AWS S3 Tables target](./docs/targets/aws-s3-tables.md)
- New OPCUA source adapter [user certificate](./docs/adapters/opcua.md#usercertificate) authentication

---

## version 1.9.5, 4 June 2025

- Bug fix for InstanceFactory classloader

---

## version 1.9.4, 3 June 2025

- [Specify the config via environment variables](./docs/sfc-running-core-process.md#additional-functionality-to-specify-the-config-via-environment-variables)
- [Running the process from a single jar file](./docs/sfc-running-core-process.md#running-the-process-from-a-single-jar-file)

---
## Version 1.9.3, 6 May 2025

- Configuration reader bug fix
`
---

## Version 1.9.2, 30 April 2025

- New: [Custom formatters ](./docs/sfc-extending.md#custom-formatters) for targets adapters, enabling the implementation of custom formatting. These formatters are implemented as JVM classes and process target data written by the adapter.
- New: [@include](./docs/sfc-configuration.md#including-configuration-sections) statement to include data from external files into configuration file
- New: [ClientId](./docs/targets/mqtt.md#clientid) configuration property for [MQTT target](./docs/targets/mqtt.md).
- New: Support for reading multiple can sockets for [J13939 adapter](./docs/adapters/j1939.md#j1939adapterconfiguration)
- New: Collecting data as [raw](./docs/adapters/j1939.md#rawformat) values for [J1939 adapter channels](./docs/adapters/j1939.md#j1939channelconfiguration)
- New: [BufferCount](./docs/targets/file.md#buffercount) property for [File target](./docs/targets/file.md)

## Version 1.9.1, 10 April 2025
`
- Upgrade to AWS SDK 2.31.18
- Fixed version dependency for running S3 target as an in-process target

---

## Version 1.9.0, 8 April 2025

- New [Simulator Adapter](./docs/adapters/simulator.md), The Simulator Adapter is a special-purpose adapter that generates synthetic data using [configurable simulations](./docs/adapters/simulator.md#simulations) instead of reading data from physical industrial devices.
- [AWS S3 target](./docs/targets/aws-s3.md) new [ObjectKey](./docs/targets/aws-s3.md#objectkey) and [Extension](./docs/targets/aws-s3.md#extension) configuration properties
- [Endpoint](./docs/core/aws-service-configuration.md#endpoint) configuration of AWS service targets to override the default public service endpoint with a VPC private endpoint.
- Adapter configuration optimizations

---

## Version 1.8.9, 20 March 2025

- [OPC UA protocol adapter](./docs/adapters/opcua.md#opcuaserverconfiguration) server configuration new  [Username](./docs/adapters/opcua.md#username)/[Password](./docs/adapters/opcua.md#password) authentication

---

## Version 1.8.8, 20 March 2025

- Relaxed checking of TLS configuration for NATS adapter and target

---

## Version 1.8.7, 11 March 2025

- Extended datatype and conversions for OPCUA and OPCUA-Writer targets
- Documentation updates
- Fixed serialization of structured datatypes in MQTT Adapter

---

## Version 1.8.6, 10 March 2025

- Added OPCUA Writer example
- Documentation updates

---

## Version 1.8.6, 5 March 2025

- Optimization of structured output data for OPCUA writer target

---

## Version 1.8.5, 4 March 2025

- Documentation updates
- OPCUA Target monitor output fix
- Support for [templates](./docs/core/target-configuration.md#template) in File Target adapter 
- Modbus TCP configuration validation update
- OPCUA Target handling of unsigned datatypes
- Fix in quickstart documentation

## Version 1.8.4, 26 February 2025

- New [OPCUA Writer](./docs/targets/opcua-writer.md) Target

---
## Version 1.8.3, 25 February 2025

- Epoch [timestamp](./docs/core/target-configuration.md#templateepochtimestamp) data for target template transformations
- Documentation updates

  ---
## Version 1.8.2, 21 February 2025

- Fallback caching options for http calls executed to obtain [external configuration data](./docs/sfc-configuration.md#including-configuration-sections), [CacheUrlConfigResults](./docs/core/sfc-configuration.md#cacheurlconfigresults) and [CacheUrlConfigDirectory](./docs/core/sfc-configuration.md#cacheurlconfigdirectory) 

  ---

## Version 1.8.1, 14 February 2025

- Updated logic for validating S3 target adapter [bucket name](./docs/targets/aws-s3.md#bucketname)
- Updated logic for validating Lambda target adapter [function name](./docs/targets/aws-lambda.md#functionname)
- Documentation update

---

## Version 1.8.0, 11 February 2025

- New [J1939](./docs/adapters/j1939.md)  protocol adapter

- [MQTT](./docs/adapters/mqtt.md) adapter properties [MaxRetainSize](./docs/adapters/mqtt.md#maxretainsize) and [maxRetainPeriod](./docs/adapters/mqtt.md#maxretainperiod) to restrict the number of stored values when [ReadMode](./docs/adapters/mqtt.md#readmode) is KeepAll. 
- [NATS](./docs/adapters/nats.md) adapter properties [MaxRetainSize](./docs/adapters/nats.md#maxretainsize) and [maxRetainPeriod](./docs/adapters/nats.md#maxretainperiod) to restrict the number of stored values when [ReadMode](./docs/adapters/nats.md#readmode) is KeepAll.
- [OPCUA](./docs/adapters/opcua.md) adapter properties [MaxEventRetainSize](./docs/adapters/opcua.md#maxeventretainsize) and [maxEventRetainPeriod](./docs/adapters/opcua.md#maxeventretainperiod) to restrict the number of stored events during a reading interval.


## Version 1.7.6, 24 January 2025

- Improved connection management for MQTT protocol adapter

---


## Version 1.7.4 & 1.7.5  17 January 2025

- Documentation restructure.

---

## Version 1.7.3, 8 January 2025

Fix in parsing configuration placeholders containing special characters
Cleanup logging output

---

## Version 1.7.2, 18 December 2024

- S7 Protocol adapter Detecting

---

## Version 1.7.1, 17 December 2024

- Adding target data message serial number and timestamp to template transformation context

---

## Version 1.7.0, 12 December 2024

- New NATS protocol adapter
- New NATS target adapter with dynamic subject names
- Updated MQTT target dynamic topic names
- Updated AWS IoT Core target dynamic topic names
- New -nocolor parameter for service components to disable color output in console output

---


## Version 1.6.0, 27 November 2024

- REST protocol adapter

---

## Version 1.5.4, 15 November 2024

- Added message retain option to IoT Core and MQTT target adapters

---

## Version 1.5.3, 14 November 2024

- MQTT Protocol adapter channel metadata not included for unmapped channels fixed

---


## Version 1.5.2, 12 November 2024

- MQTT Protocol adapter "ReadMode" setting: "KeepLast" to collect only the last received message from topic, "KeepAll", collects all messages received in a read interval.
"KeepLast" is the default as this was the behaviour in previous versions.
---

## Version 1.5.0, 31 October 2024

- NEW OPC UA Target Adapter: Allows exposing the data collected by the SFC protocol adapter as an OPC UA model.
---


## Version 1.4.2, 5 September 2024

- [OPCUA autodiscovery example](https://github.com/aws-samples/shopfloor-connectivity/tree/mainline/examples/opcua-auto-discovery) validation of external ID's
- Decompose setting to control decomposition of structured values at [source](https://github.com/aws-samples/shopfloor-connectivity/blob/mainline/docs/core/source-configuration.md) level
- Support for decomposition of lists of structured values
- Control over output of numeric values for targets with JSON output [UnquoteNumericJsonValues](https://github.com/aws-samples/shopfloor-connectivity/blob/mainline/docs/core/target-configuration.md)
- Spread setting to control decomposition of list values at [source](https://github.com/aws-samples/shopfloor-connectivity/blob/mainline/docs/core/source-configuration.md) level
- Documentation fixes and updates

---


## Version 1.4.1, 22 August 2024

- [Transformation operators](docs/core/transformation-operator-configuration.md) : Chunked,NumbersToFloatBE, NumberToFloatLE, ReverseList, ToShort, ToSigned, ToUnsigned.

- [Channel configuration](docs/core/channel-configuration.md) option "Spread" to elements of source list values in into separate values.

- [Target configuration](docs/core/target-configuration.md) option "UnquoteNumericJsonValues" to strip double quotes in target JSON output.

- Documentation updates


---

## Version 1.4.0, 20 August 2024

- [Sitewise Edge target adapter](docs/targets/aws-sitewiseedge.md) in addition to Sitewise service adapter

- [Transformation operators](docs/core/transformation-operator-configuration.md): BytesToDoubleBE, BytesToDoubleLE, BytesToFloatBE, BytesToFloatLE

- [Channel](docs/core/channel-configuration.md) option "Decompose" to decompose a structured value into new individual values for every element in the structure.

- [Source](docs/core/source-configuration.md) option "Compose" to compose new structured values from selected channel values.

- Documentation updates