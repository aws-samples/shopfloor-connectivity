## ProtocolAdapterConfiguration


**Properties:**
- [AdapterServer](#AdapterServer)
- [AdapterType](#AdapterType)

---
### AdapterServer
If the adapter runs as a service in a separate process, then this attribute must refer to an entry for that server in the [ProtocolAdapterServers](./sfc-top-level-config.md#AdapterServers) section.

**Type**: String

 If this attribute is not set then the SFC core will load and execute the protocol adapter in the SFC core process. If set then and IPC client will be used to communicate with the service that runs the protocol adapter. If an adapter server is specified, then the [AdapterType](#AdapterType) setting is not used.

---
### AdapterType
Type of the adapter. These types are predefined for each adapter type (e.g., OPCUA, MQTT,MODBUS-TCP, SNMP, S7, ADS ).
If the adapter is running in the same process as the SFC core module, then it must refer to an entry in the [ProtocolAdapterTypes](./sfc-top-level-config.md#ProtocolAdapterTypes) section. 

**Type**: String

[^top](#ProtocolAdapterConfiguration)

