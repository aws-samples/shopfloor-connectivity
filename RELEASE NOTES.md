# Release Notes:

## Version 1.5.5 25 November 2024

- Added message retain option to IoT Core and MQTT target adapters

---

## Version 1.5.4 15 November 2024

- Added message retain option to IoT Core and MQTT target adapters

---

## Version 1.5.3 14 November 2024

- MQTT Protocol adapter channel metadata not included for unmapped channels fixed

---


## Version 1.5.2 12 November 2024

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

- [Channel](docs/core/channel-configuration.md) option "Decompose" to decompose a structred value into new individual values for every element in the structure.

- [Source](docs/core/source-configuration.md) option "Compose" to compose new structured values from selected channel values.

- Documentation updates