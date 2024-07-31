# MQTT Protocol Configuration

This section describes the configuration types for the MQTT protocol adapter and contains the extensions and specific
configuration types

- [MqttSourceConfiguration](#mqttsourceconfiguration)
- [MqttChannelConfiguration](#mqttchannelconfiguration)
- [TopicNameMapping](#topicnamemapping)
- [MqttAdapterConfiguration](#mqttadapterconfiguration)
- [MqttBrokerConfiguration](#mqttbrokerconfiguration)


  [Protocol Adapters](protocol-adapters.md)

## MqttSourceConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 0%" />
<col style="width: 27%" />
<col style="width: 0%" />
<col style="width: 28%" />
<col style="width: 0%" />
<col style="width: 24%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="7"><p><strong>Extends Source configuration</strong></p>
<p>The MqttSourceConfiguration extends the common Source configuration with MQTT specific source configuration data</p></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td colspan="2"><strong>Name</strong></td>
<td colspan="2"><strong>Description</strong></td>
<td colspan="2"><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>
<tr class="even">
<td colspan="2">Channels</td>
<td colspan="2"><p>The channels configuration for an MQTT source holds configuration data to read values from topics on the source MQTT broker.</p>
<p>The element is a map indexed by the channel identifier.</p>
<p>Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.</p></td>
<td colspan="2">Map[String,<a href="#mqttchannelconfiguration">MqttChannelConfiguration</a>]</td>
<td>At least 1 channel must be configured.</td>
</tr>
<tr class="odd">
<td>AdapterBroker</td>
<td colspan="2">Broker Identifier for the MQTT server to read from. This referenced server must be present in the Brokers section of the adapter referred to by the ProtocolAdapter attribute of the source.</td>
<td colspan="2">String</td>
<td colspan="2">Must be an identifier of a broker in the Brokers section of the MQTT adapter used by the source.</td>
</tr>
</tbody>
</table>

## MqttChannelConfiguration

<table>
<colgroup>
<col style="width: 17%" />
<col style="width: 29%" />
<col style="width: 27%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p><strong>Extends Channel configuration</strong></p>
<p>The MqttChannelConfiguration extends the common Channel configuration with MQTT specific channel configuration data</p></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>
<tr class="even">
<td>Topics</td>
<td>A string containing the topics to subscribe to. The topic names may contain single-level (+) and multi-level (#) wildcards</td>
<td>String[]</td>
<td>The must be at least one topic in the list of topics.</td>
</tr>
<tr class="odd">
<td>Json</td>
<td>Set to true if data received from topics is in JSON format</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>
<tr class="even">
<td>TopicNameMapping</td>
<td>Mapping from topic names to alternative names. As a channel can have multiple topics, that also can include wildcards, this mapping can be used to build consistent and expected value names.</td>
<td><a href="#topicnamemapping">TopicNameMapping</a></td>
<td></td>
</tr>
<tr class="odd">
<td>Selector</td>
<td><p>Evaluate a <a href="https://jmespath.org/">JMESpath</a> query against the value of a structured data type and returns the result.</p>
<p>The selector can be used to restructure or select values from structured data types.</p></td>
<td>Datatype: Structure or array</td>
<td>Parameter: <a href="https://jmespath.org/">JMESPath</a> expression, see https://jmespath.org/</td>
</tr>
</tbody>
</table>

[^top](#mqtt-protocol-configuration)

## TopicNameMapping

<table>
<colgroup>
<col style="width: 17%" />
<col style="width: 29%" />
<col style="width: 27%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4">The mapping of topic names of received data updates to data value names</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>
<tr class="even">
<td>Mappings</td>
<td><p>Mapping table for mapping the topic names of received topic data updates to data value names. As a channel can subscribe to multiple topics, that can also include wildcards, updates from different topics can be received.</p>
<p>This element is a map that uses regular expression strings as indexes. The entries in the map are strings that will be used as replacement strings if the regular expression of the entry matches the name of the topic for an update.</p>
<p>The replacement string can include substitution parameters for capturing groups in the regular expression.</p></td>
<td>Map[String,String]</td>
<td>The must be at least one topic in the list of topics.</td>
</tr>
<tr class="odd">
<td>Json</td>
<td>Set to true if data received from topics is in JSON format</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>
<tr class="even">
<td>TopicNameMapping</td>
<td>Mapping from topic names to alternative names. As a channel can have multiple topics, that also can include wildcards, this mapping can be used to build consistent and expected value names.</td>
<td><a href="#topicnamemapping">TopicNameMapping</a></td>
<td><p>Example:</p>
<p>Channel subscription is:</p>
<p>"Topics" :[ "test"/#"]</p>
<p>The mapping is:</p>
<p>"Mappings": {<br />
"test/(\\w+)": "test-$1"<br />
}</p>
<p>The mapping above matches updates for sub-levels of the test topic, it will use the name of the sub-level to create a name for the received data.</p>
<p>If an update is received for data in topic "test/a" then the name of the data value will be "test-a"</p></td>
</tr>
<tr class="odd">
<td>IncludeUnmappedTopics</td>
<td>If set to false, updates for values from topics that do not match any of the expressions in the mapping's element will be dropped. If set to true then the name of the value will be the name of the topic the update was received for.</td>
<td>Boolean</td>
<td>Default is false</td>
</tr>
<tr class="even">
<td>Selector</td>
<td><p>Evaluate a <a href="https://jmespath.org/">JMESpath</a> query against the value of a structured data type and returns the result.</p>
<p>The selector can be used to restructure or select values from structured data types.</p></td>
<td>Datatype: Structure or array</td>
<td>Parameter: <a href="https://jmespath.org/">JMESPath</a> expression, see https://jmespath.org/</td>
</tr>
</tbody>
</table>

[^top](#mqtt-protocol-configuration)

## MqttAdapterConfiguration

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 19%" />
<col style="width: 25%" />
<col style="width: 40%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p><strong>Extends ProtocolAdapterConfiguration</strong></p>
<p>The MqttAdapterConfiguration extends the common adapter configuration with MQTT specific adapter configuration settings. The AdapterType to use for this adapter is "MQTT".</p></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Brokers</td>
<td>Brokers configured for this adapter. The mqtt source using the adapter must refer to one of these servers with the AdapterBroker attribute.</td>
<td>Map[String,<a href="#mqttbrokerconfiguration">MqttBrokerConfiguration</a>]</td>
<td></td>
</tr>

<tr class="odd">
<td>ReceivedDataChannelSize</td>
<td>Size of internal buffer to receive data for topic subscriptions</td>
<td>Int</td>
<td>Default is 1000</td>
</tr>

<tr class="odd">
<td>ReceivedDataChannelTimeout</td>
<td>Timeout in milliseconds to send data to internal buffer for received data for topic subscriptions</td>
<td>Int</td>
<td>Default is 1000</td>
</tr>

</tbody>
</table>

[^top](#mqtt-protocol-configuration)

## MqttBrokerConfiguration

<table>  
<colgroup>  
<col style="width: 17%" />  
<col style="width: 29%" />  
<col style="width: 20%" />  
<col style="width: 32%" />  
</colgroup>   
<tbody>  
<tr class="odd">  
<td><strong>Name</strong></td>  
<td><strong>Description</strong></td>  
<td><strong>Type</strong></td>  
<td><strong>Comments</strong></td>  
</tr>  
<tr class="even">  
<td>EndPoint</td>  
<td>Broker endpoint address</td>  
<td>String</td>  
<td>Optionally with training port number (see Port)


If no scheme is specified in the address, then it will be added based on the Connection type.
("tcp://" for PlainText or "ssl://" for ServerSideTLS or MutualTLS)</td>
</tr>  
<tr class="odd">  
<td>Port</td>  
<td>Port on MQTT broker</td>  
<td>Integer</td>  
<td>

Commonly port numbers are

- 1883 for PlaintText
- 8883 for ServerSideTLS
- 8884 for MutualTLS.
- 443 for AWS IoT Core endpoints

In no port number is specified then the EndPoint address is searched for a training port number.

</td> 
</tr>  
<tr class="even">  
<td>Connection</td>  
<td>Connection type</td>  
<td>String</td>  
<td>

- "PlainText" (Default)
- "ServerSideTLS"
- "MutualTLS"

</td>  
</tr>  
<tr class="even">  
<td>SslServerCertificate</td>  
<td>Path to server certificate file to verify the identity of the broker.</td>  
<td>String</td>  
<td>If no certificate file is specified it is obtained from the server.
<p>Used for connections of type ServerSideTLS and MutualTLS</p></td>  
</tr>  
<tr class="odd">  
<td>PrivateKey</td>  
<td>Path to client private key file</td>  
<td>String</td>  
<td></td>  
</tr>  
<tr class="even">  
<td>RootCA</td>  
<td>Path to root certificate file. The Root CA file in an MQTT client is used for server certificate verification when establishing a secure connection with the broker (using TLS/SSL)</td>  
<td>String</td>  
<td></td>  
</tr>  
<tr class="odd">  
<td>Certificate</td>  
<td>Path to client certificate file. Used if broker used certificate authentication</td>  
<td>String</td>  
<td></td>  
</tr>  
<tr class="even">  
<td>Username</td>  
<td>Username if broker is using username and password authentication</td>  
<td>String</td>  
<td>Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.</td>
<tr class="odd">  
<td>Password</td>  
<td>Password if broker is using username and password authentication</td>  
<td>String</td>  
<td>Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.</td>  
</tr>  
<tr class="even">  
<td>ConnectionTimeout</td>  
<td>Timeout for connecting to the broker in seconds</td>  
<td>Int</td>  
<td>Default is 10 seconds</td>
<tr class="odd">  
<td>WaitAfterConnectError</td>  
<td>Period in seconds to wait before trying to connect after a connection failure</td>  
<td>Int</td>  
<td>Default is 60 seconds</td>

</tr>  
</tbody>  
</table>

[^top](#mqtt-protocol-configuration)