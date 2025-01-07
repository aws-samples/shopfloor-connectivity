
# OPCDA Protocol Configuration


---
- [OpcdaSourceConfiguration](#OpcdaSourceConfiguration)
- [OpcdaChannelConfiguration](#OpcdaChannelConfiguration)
- [OpcdaAdapterConfiguration](#OpcdaAdapterConfiguration)
- [OpcdaServerConfiguration](#OpcdaServerConfiguration)

---

## OpcdaSourceConfiguration


**Properties:**
- [AdapterOpcdaServer](#AdapterOpcdaServer)
- [Channels](#Channels)
- [SourceReadingMode](#SourceReadingMode)

---
### AdapterOpcdaServer
Server Identifier for the OPCDA server to read from. This referenced server must be present in the dServers section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a server in the OpcdaServers section of the OPCDA adapter used by the source.

---
### Channels
The channels configuration for an OPCDA source holds configuration data to read values from items on the source OPCDA server.
The element is a map indexed by the channel identifier.
Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,[OpcdaChannelConfiguration](#OpcdaChannelConfiguration)]

At least 1 channel must be configured.

---
### SourceReadingMode
Mode for reading values from OPCDA server.

- "Subscription": connector will create a subscription and will monitor the items configured in the channels for the source. When reading from the adapter in this mode, only items that have been changed in the schedule interval period will be returned, except for the initial read that will return all monitored items.
- "Polling", the connector will batch-read all items configured in the channels for the source with the interval defined in the schedule.


**Type**: String 
Possible values are “Subscription" or "Polling".

Default is "Subscription".

[^top](#opcda-protocol-configuration)




## OpcdaChannelConfiguration


**Properties:**
- [Item](#Item)

---
### Item
A string containing the name of the item to read the value from or to monitor.

**Type**: String

[^top](#opcda-protocol-configuration)




## OpcdaAdapterConfiguration

**Properties:**

- [OpcdaServers](#OpcdaServers)

---
### OpcdaServers
Opcda servers configured for this adapter. The Opcda source using the adapter must refer to one of these servers with the AdapterOpcdaServer attribute.

**Type**: Map[String,[OpcdaServerConfiguration](#OpcdaServerConfiguration)]

[^top](#opcda-protocol-configuration)




## OpcdaServerConfiguration


**Properties:**
- [ConnectTimeout](#ConnectTimeout)
- [ReadBatchSize](#ReadBatchSize)
- [ReadTimeout](#ReadTimeout)
- [SamplingRate](#SamplingRate)
- [Url](#Url)
- [WaitAfterConnectError](#WaitAfterConnectError)

---
### ConnectTimeout
Timeout in milliseconds connecting to the server/td>
Integer

**Type**: Integer

Default is 10000, the minimum value is 1000



---
### ReadBatchSize
Max number of items to read in a single batch read from the server

**Type**: Integer

If not specified all configured items are read in a single read

---
### ReadTimeout
Timeout in milliseconds reading from the server

**Type**: Integer

Default is 10000

---
### SamplingRate
Time in milliseconds for sampling items in subscription mode.

**Type**: Integer

If not specified then the shorted interval will be used from all active schedules that have a source using this server.

---
### Url
Url	Address of the OPCDA server

**Type**: String

e.g., "opcda://192.168.1.145/Simulation"

---
### WaitAfterConnectError
Time in milliseconds to wait to reconnect after a connection error

**Type**: Integer

Default is 10000, the minimum value is 1000

[^top](#opcda-protocol-configuration)

