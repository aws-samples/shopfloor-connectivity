# AWS SiteWise Edge Target
The `AWS-SITEWISEEDGE-TARGET` is a specific type of target configuration in SFC that allows you to connect and send data to an MQTT topic consumed by the AWS IoT SiteWise Edge service. The `Targets` configuration element can contain entries of this type, and the `TargetType` of these entries must be set to `"AWS-SITEWISEEDGE-TARGET"`.

This target adapter follows the Time Quality Value (TQV) schema for ingesting data into SiteWise Edge. For a better understanding of the TQV schema, please refer to the [Ingest data using the AWS IoT SiteWise API](https://docs.aws.amazon.com/iot-sitewise/latest/userguide/ingest-api.html) documentation.

[Targets](./README.md)

## SiteWiseEdgeTargetConfiguration

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 17%" />
<col style="width: 13%" />
<col style="width: 55%" />
</colgroup>

<tbody>
<tr>
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>
<tr>
<td>TopicName</td>
<td>Name of the MQTT topic to which SiteWise Edge will subscribe for ingesting data. You may use a combination of %source%, %target%, and %channel% variables.</td>
<td>String</td>
<td>Default: %channel%</td>
</tr>

<tr>
<td>EndPoint</td>  
<td>SiteWise Edge MQTT broker endpoint address</td>  
<td>String</td>  
<td>Optionally with a port number (see Port)

If no scheme is specified in the address, then it will be added based on the Connection type.
("tcp://" for PlainText or "ssl://" for ServerSideTLS or MutualTLS)
<p>To get the ATS endpoint for an account use the AWS CLI command<br />
aws iot describe-endpoint --endpoint-type iot:Data-ATS</p>
<p><a href="https://awscli.amazonaws.com/v2/documentation/api/latest/reference/iot/describe-endpoint.html">https://awscli.amazonaws.com/v2/documentation/api/latest/reference/iot/describe-endpoint.html</a></p>
</td>
</tr>  

<tr>  
<td>Port</td>  
<td>SiteWise Edge MQTT broker port</td>  
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

<tr>  
<td>Connection</td>  
<td>Connection type</td>  
<td>String</td>  
<td>

- "PlainText" (Default)
- "ServerSideTLS"
- "MutualTLS"

</td>  
</tr>

<tr>  
<td>SslServerCertificate</td>  
<td>Path to server certificate file to verify the identity of the broker.</td>  
<td>String</td>  
<td>If no certificate file is specified it is obtained from the server.
<p>Used for connections of type ServerSideTLS and MutualTLS</p></td>  
</tr>  

<tr>  
<td>PrivateKey</td>  
<td>Path to client private key file</td>  
<td>String</td>  
<td></td>  
</tr>  

<tr>  
<td>RootCA</td>  
<td>Path to root certificate file. The Root CA file in an MQTT client is used for server certificate verification when establishing a secure connection with the broker (using TLS/SSL)</td>  
<td>String</td>  
<td></td>  
</tr>  

<tr>  
<td>Certificate</td>  
<td>Path to client certificate file. Used if broker used certificate authentication</td>  
<td>String</td>  
<td></td>  
</tr>  

<tr>  
<td>ClientName</td>  
<td>Client name to provide when connecting to the SiteWise Edge MQTT broker. When running on Greengrass core, this should be the name of the IoT Thing which is providing the certificates.</td>  
<td>String</td>  
<td>Length Constraints: Minimum length of 1. Maximum length of 128.

Pattern: [a-zA-Z0-9:_-]+</td>  
</tr>  

<tr>  
<td>VerifyHostname</td>  
<td>Verify the server hostname from the provided certificates. Set this to `false` when connecting to SiteWise Edge running on Greengrass. IoT self-signed certificates do not provide the hostname.</td>
<td>Boolean</td>  
<td></td>  
</tr>  


<tr>  
<td>Username</td>  
<td>Username if broker is using username and password authentication</td>  
<td>String</td>  
<td>Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.</td>
<tr>  
<td>Password</td>  
<td>Password if broker is using username and password authentication</td>  
<td>String</td>  
<td>Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.</td>  
</tr>  

<tr>  
<td>ConnectionTimeout</td>  
<td>Timeout for connecting to the broker in seconds</td>  
<td>Int</td>  
<td>Default is 10 seconds</td>
<tr>  
<td>WaitAfterConnectError</td>  
<td>Period in seconds to wait before trying to connect after a connection failure</td>  
<td>Int</td>  
<td>Default is 60 seconds</td>
</tr>  

<tr>  
<td>ConnectRetries</td>  
<td>Number of retries to connect to MQTT broker</td>  
<td>Int</td>  
<td>Default is 10</td>
</tr>  

<tr>
<td>PublishTimeout</td>
<td>Timeout in seconds for publishing</td>
<td>Long</td>
<td>Default is 10 seconds</td>
</tr>

<tr>  
<td>BatchCount</td>  
<td>Number of TQV messages to buffer per channel before sending data as a batch to the SiteWise Edge MQTT broker.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.</td>
</tr>

<tr>  
<td>BatchSize</td>  
<td>Channel TQV Payload size in KB of messages to batch before sending data as a batch to the SiteWise Edge MQTT broker.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.
The size is calculated on the uncompressed payload of the messages.</td>
</tr>

<tr>  
<td>BatchInterval</td>  
<td>Interval in milliseconds after which all messages are sent to the SiteWise Edge MQTT Broker, even when the BatchSize or BatchCount limit is not reached.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.
</td>
</tr>




</tbody></table>

[^top](#aws-sitewiseedge-target)