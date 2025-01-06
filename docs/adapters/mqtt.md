
# MQTT Protocol Configuration


---
- [MqttSourceConfiguration](#MqttSourceConfiguration)
- [MqttChannelConfiguration](#MqttChannelConfiguration)
- [TopicNameMapping](#TopicNameMapping)
- [MqttAdapterConfiguration](#MqttAdapterConfiguration)
- [MqttBrokerConfiguration](#MqttBrokerConfiguration)

---

## MqttSourceConfiguration


**Properties:**
- [AdapterBroker](#AdapterBroker)
- [Channels](#Channels)

---
### AdapterBroker
Broker Identifier for the MQTT server to read from. This referenced server must be present in the Brokers section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a broker in the Brokers section of the MQTT adapter used by the source.

---
### Channels
The channels configuration for an MQTT source holds configuration data to read values from topics on the source MQTT broker.
The element is a map indexed by the channel identifier.
Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,MqttChannelConfiguration]

At least 1 channel must be configured.

[^top](#MQTT Protocol Configuration)




## MqttChannelConfiguration


**Properties:**
- [Json](#Json)

- [Selector](#Selector)
- [TopicNameMapping](#TopicNameMapping)
- [Topics](#Topics)

---
### Json
Set to true if data received from topics is in JSON format

**Type**: Boolean

Default is true

---
### Selector
Evaluate a JMESpath query against the value of a structured data type and returns the result.
The selector can be used to restructure or select values from structured data types.

**Type**: Datatype: String

**Parameter**: JMESPath expression, see https://jmespath.org/

---
### TopicNameMapping
Mapping from topic names to alternative names. As a channel can have multiple topics, that also can include wildcards, this mapping can be used to build consistent and expected value names.

**Type**: [TopicNameMapping](#TopicNameMapping)

---
### Topics
A string array  containing the topics to subscribe to. The topic names may contain single-level (+) and multi-level (#) wildcards

**Type**: String[]

The must be at least one topic in the list of topics.

[^top](#MQTT Protocol Configuration)




## TopicNameMapping


**Properties:**
- [IncludeUnmappedTopics](#IncludeUnmappedTopics)
- [Json](#Json)
- [Mappings](#Mappings)

- [Selector](#Selector)
- [TopicNameMapping](#TopicNameMapping)

---
### IncludeUnmappedTopics
If set to false, updates for values from topics that do not match any of the expressions in the mapping's element will be dropped. If set to true then the name of the value will be the name of the topic the update was received for.

**Type**: Boolean

Default is false

---
### Json
Set to true if data received from topics is in JSON format

**Type**: Boolean

Default is true

---
### Mappings
Mapping table for mapping the topic names of received topic data updates to data value names. As a channel can subscribe to multiple topics, that can also include wildcards, updates from different topics can be received.

This element is a map that uses regular expression strings as indexes. The entries in the map are strings that will be used as replacement strings if the regular expression of the entry matches the name of the topic for an update.

The replacement string can include substitution parameters for capturing groups in the regular expression.

**Type**: Map[String,String]

The must be at least one topic in the list of topics.

---
### Selector
Evaluate a JMESpath query against the value of a structured data type and returns the result.
The selector can be used to restructure or select values from structured data types.

**Type**: String

Parameter: JMESPath expression, see https://jmespath.org/

---
### TopicNameMapping
Mapping from topic names to alternative names. As a channel can have multiple topics, that also can include wildcards, this mapping can be used to build consistent and expected value names.

**Type**: TopicNameMapping

Example:

Channel subscription is:

```json
	"Topics" :[ "test"/#"]
```



The mapping is:

```json
	"Mappings": {
		"test/(\\w+)": "test-$1"
	}
```

The mapping above matches updates for sub-levels of the test topic, it will use the name of the sub-level to create a name for the received data.

If an update is received for data in topic "test/a" then the name of the data value will be "test-a"

[^top](#MQTT Protocol Configuration)




## MqttAdapterConfiguration


**Properties:**
- [Brokers](#Brokers)
- [ReadMode](#ReadMode)
- [ReceivedDataChannelSize](#ReceivedDataChannelSize)
- [ReceivedDataChannelTimeout](#ReceivedDataChannelTimeout)

---
### Brokers
Brokers configured for this adapter. The mqtt source using the adapter must refer to one of these servers with the AdapterBroker attribute.

**Type**: Map[String,[MqttBrokerConfiguration](#MqttBrokerConfiguration)]

---
### ReadMode
Read mode of the adapter. Set to "KeepAll" to collect all messages on subscribed topics during a read interval.
Set to "KeepLast", which is the default, to keep only the last received message.

**Type**: String


- "KeepLast" to collect last message received in read interval (Default)
- "KeepAll" to collect all messages received in read interval


---
### ReceivedDataChannelSize
Size of internal buffer to receive data for topic subscriptions

**Type**: Int

Default is 1000

---
### ReceivedDataChannelTimeout
Timeout in milliseconds to send data to internal buffer for received data for topic subscriptions

**Type**: Int

Default is 1000

[^top](#MQTT Protocol Configuration)




## MqttBrokerConfiguration


**Properties:**
- [Certificate](#Certificate)
- [Connection](#Connection)
- [ConnectionTimeout](#ConnectionTimeout)
- [EndPoint](#EndPoint)
- [Password](#Password)
- [Port](#Port)
- [PrivateKey](#PrivateKey)
- [RootCA](#RootCA)
- [SslServerCertificate](#SslServerCertificate)
- [Username](#Username)
- [WaitAfterConnectError](#WaitAfterConnectError)

---
### Certificate
Path to client certificate file. Used if broker used certificate authentication

**Type**: String

---
### Connection
Connection type

**Type**: String

- "PlainText" (Default)
- "ServerSideTLS"
- "MutualTLS"

---
### ConnectionTimeout
Timeout for connecting to the broker in seconds

**Type**: Int

Default is 10 seconds

---
### EndPoint
Broker endpoint address

**Type**: String

Optionally with training port number (see Port)

If no scheme is specified in the address, then it will be added based on the Connection type.

("tcp://" for PlainText or "ssl://" for ServerSideTLS or MutualTLS)

---
### Password
Password if broker is using username and password authentication

**Type**: String

**Username and password should not be included as clear text in the configuration.** It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.

---
### Port
Port on MQTT broker

**Type**: Integer

Commonly port numbers are

- 1883 for PlaintText
- 8883 for ServerSideTLS
- 8884 for MutualTLS.
- 443 for AWS IoT Core endpoints

In no port number is specified then the EndPoint address is searched for a training port number.

---
### PrivateKey
Path to client private key file

**Type**: String

---
### RootCA
Path to root certificate file. The Root CA file in an MQTT client is used for server certificate verification when establishing a secure connection with the broker (using TLS/SSL)

**Type**: String

---
### SslServerCertificate
Path to server certificate file to verify the identity of the broker.

**Type**: String

If no certificate file is specified it is obtained from the server.
Used for connections of type ServerSideTLS and MutualTLS

---
### Username
Username if broker is using username and password authentication

**Type**: String

Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.

---
### WaitAfterConnectError
Period in seconds to wait before trying to connect after a connection failure

**Type**: Int

Default is 60 seconds

[^top](#MQTT Protocol Configuration)

