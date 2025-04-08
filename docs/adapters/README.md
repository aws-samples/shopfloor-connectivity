# SFC Protocol Adapters

Protocol adapters in SFC (Shop Floor Connectivity) are interfaces that abstract and translate data from various industrial protocols and devices into a common format, allowing seamless data collection from different sources like PLCs, databases, and industrial equipment to AWS services. They act as standardized connectors that handle the protocol-specific communication details.

- [**ADS (Beckhoff)**](./ads.md)

  ADS (Automation Device Specification) protocol adapter enables reading data from Beckhoff PLCs and controllers using their native TwinCAT communication protocol.

- [**J1939**](./j1939.md)


  J1939 protocol adapter enables reading data from heavy-duty vehicle networks and equipment using the SAE J1939 standard over CAN bus

- [**MQTT**](./mqtt.md)

  MQTT protocol adapter enables reading data from MQTT brokers using a publish/subscribe messaging pattern for lightweight IoT communications

- **[Modbus TCP](./modbus.md)**

  Modbus TCP protocol adapter enables reading data from industrial devices and PLCs using the Modbus TCP/IP protocol, a widely used industrial communication standard.

- **[NATS](./nats.md)**

  NATS protocol adapter enables reading data from NATS messaging systems using a publish/subscribe architecture for cloud-native and distributed systems communication.

- **[OPCUA ](./opcua.md)**

  OPC UA protocol adapter enables reading data from industrial devices and systems using the platform-independent OPC Unified Architecture protocol for secure, reliable industrial communications

- **[OPCDA](./opcda.md)**

  OPC DA protocol adapter enables reading data from legacy industrial automation systems using the traditional OPC Data Access specification for Windows-based systems

- **[PCCC (Allen Bradley/Rockwell)](./pccc.md)**

  PCCC (Programmable Controller Communication Commands) protocol adapter enables reading data from Allen-Bradley/Rockwell PLCs

- **[REST](./rest.md)**

  REST protocol adapter enables reading data from web services and APIs using HTTP GET method to retrieve data from RESTful endpoints.

- **[S7 (Siemens)](./s7.md)**

  S7 protocol adapter enables reading data from Siemens S7 family of PLCs (S7-300, S7-400, S7-1200, S7-1500) using the native S7 communication protocol.

- **[Simulator](./simulator.md)**

  The Simulator Adapter generates synthetic data using configurable value and composite simulations instead of reading from physical devices.

- **[SLMP (Mitsubishi/Melsec)](slmp.md)**

  SLMP (Seamless Message Protocol) protocol adapter enables reading data from Mitsubishi/Melsec PLCs using their native communication protocol

- **[SNMP](./snmp.md)**

  SNMP (Simple Network Management Protocol) protocol adapter enables reading data from network devices, equipment, and systems by polling their management information base (MIB) values

- **[SQL](./sql.md)**

  SQL protocol adapter enables reading data from relational databases (like MySQL, PostgreSQL, Oracle, SQL Server) through JDBC connections using SQL queries.

