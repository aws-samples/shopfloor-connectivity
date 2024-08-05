# MQTT Target
<br>
<p>MqttTargetConfiguration extends the type TargetConfiguration with specific configuration data for connecting to and sending to MQTT topic. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"MQTT-TARGET"</strong></p>
<br>
[Targets](./README.md)

## MqttTargetConfiguration

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 17%" />
<col style="width: 13%" />
<col style="width: 55%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>
<tr class="even">
<td>TopicName</td>
<td>Name of the topic</td>
<td>String</td>
<td></td>
</tr>

<tr class="odd">
<td>EndPoint</td>  
<td>Broker endpoint address</td>  
<td>String</td>  
<td>Optionally with training port number (see Port)

If no scheme is specified in the address, then it will be added based on the Connection type.
("tcp://" for PlainText or "ssl://" for ServerSideTLS or MutualTLS)
<p>To get the ATS endpoint for an account use the AWS CLI command<br />
aws iot describe-endpoint --endpoint-type iot:Data-ATS</p>
<p><a href="https://awscli.amazonaws.com/v2/documentation/api/latest/reference/iot/describe-endpoint.html">https://awscli.amazonaws.com/v2/documentation/api/latest/reference/iot/describe-endpoint.html</a></p>
</td>
</tr>  
<tr class="even">  
<td>Port</td>  
<td>Port on MQTT broker</td>  
<td>Integer</td>  
<td>

Commonly port numbers are

- 1883 for PlainText
- 8883 for ServerSideTLS
- 8884 for MutualTLS.
- 443 for AWS IoT Core endpoints

In no port number is specified then the EndPoint address is searched for a training port number.

</td> 
</tr>  
<tr class="odd">  
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
<td>QoS</td>
<td>Quality of service</td>
<td>Integer</td>
<td>Default is 0

<p>0 = At most once</p>
<p>1 = At least once</p>
<p>2 = Exactly once</p></td>
</tr>
<tr class="odd">  
<td>Username</td>  
<td>Username if broker is using username and password authentication</td>  
<td>String</td>  
<td>Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.</td>
<tr class="even">  
<td>Password</td>  
<td>Password if broker is using username and password authentication</td>  
<td>String</td>  
<td>Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.</td>  
</tr>  
<tr class="odd">  
<td>ConnectionTimeout</td>  
<td>Timeout for connecting to the broker in seconds</td>  
<td>Int</td>  
<td>Default is 10 seconds</td>
<tr class="even">  
<td>WaitAfterConnectError</td>  
<td>Period in seconds to wait before trying to connect after a connection failure</td>  
<td>Int</td>  
<td>Default is 60 seconds</td>
</tr>  
<tr class="odd">  
<td>ConnectRetries</td>  
<td>Number of retries to connect to MQTT broker</td>  
<td>Int</td>  
<td>Default is 10</td>
</tr>  
<tr class="odd">
<td>PublishTimeout</td>
<td>Timeout in seconds for publishing</td>
<td>Long</td>
<td>Default is 10 seconds</td>
</tr>


<tr class="even">  
<td>BatchCount</td>  
<td>Number of messages to buffer before sending data as a batch  to topic.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.</td>
</tr>

<tr class="odd">  
<td>BatchSize</td>  
<td>Payload size in KB of messages to batch before sending data as a batch to topic.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.
The size is calculated on the uncompressed payload of the messages.</td>
</tr>

<tr class="even">  
<td>BatchInterval</td>  
<td>Interval in milliseconds after which a batch of messages is sent to the topic, even when the BatchSize or BatchCount limit is not reached.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.
</td>
</tr>

<tr class="odd">
<td>MaxPayloadSize</td>
<td>Max payload size in KB for MQTT messages.</td>
<td>Int</td>
<td>Note if compression is enabled the payload size of a single target data message, or a batch of messages can be larger, than this value. When batching of messages is enabled, without compression a batch
of messages will be sent to the topic when this size is reached.</td>
</tr>

<tr class="even">  
<td>Compression</td>  
<td>Compression method for MQTT message payloads.</td>  
<td>String

- "None"
- "Zip"
- "GZip"

</td>  
<td>Default is "None"</td>
</tr> 


</tbody></table>

[^top](#mqtt-target)