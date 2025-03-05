# Release Notes:

## Version 1.8.6, 5 march February 2025

- Optimization of structured output data for OPCUA writer target

---

## Version 1.8.5, 4 march February 2025

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