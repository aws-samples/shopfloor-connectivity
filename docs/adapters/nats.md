# NATS Adapter Configuration

This section describes the configuration types for the NATS adapter and contains the extensions and specific
configuration types.

- [NatsSourceConfiguration](#natssourceconfiguration)
- [NatsChannelConfiguration](#natschannelconfiguration)
- [SubjectNameMapping](#subjectnamemapping)
- [NatsAdapterConfiguration](#natsadapterconfiguration)
- [NatsServerConfiguration](#natsserverconfiguration)
- [NatsTlsConfiguration](#natstlsconfiguration)


[Protocol Adapters](./README.md)

## NatsSourceConfiguration

The NatsSourceConfiguration extends the common <a href = "../core/source-configuration.md" >Source configuration</a> with 
NATS specific source configuration data.The AdapterType for the source must be set to **NATS**.

<table>
<colgroup>
<col/><col/><col/><col/>
</colgroup>


<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Channels</td>
<td><p>The channels configuration for an NATS source holds configuration data to read values from subjects on the source NATS server.</p>
<p>The element is a map indexed by the channel identifier.</p>
<p>Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.</p></td>
<td>Map[String,<a href="#NatsChannelConfiguration">NatsChannelConfiguration</a>]</td>
<td>At least 1 channel must be configured.</td>
</tr>

<tr class="odd">
<td>AdapterServer</td>
<td>Server Identifier for the NATS server to read from. This referenced server must be present in the Servers section of the adapter 
referred to by the ProtocolAdapter attribute of the source.</td>
<td>String</td>
<td>Must be an identifier of a server in the Servers section of the NATS server used by the source.</td>
</tr>

</tbody>
</table>

## NatsChannelConfiguration

The NatsChannelConfiguration extends the common Channel configuration with NATS specific channel configuration data.

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Subjects</td>
<td>A string array containing the subjects for this channel to subscribe to. The subjects names may contain single-level (*) and multi-level (>) wildcards</td>
<td>String[]</td>
<td>The must be **at least one subject** in the list of subjects.</td>
</tr>

<tr class="odd">
<td>Json</td>
<td>Set to true if data received from subjects is in JSON format</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>

<tr class="even">
<td>SubjectNameMapping</td>
<td>Mapping from subject names to alternative names. As a channel can have multiple subjects, that also can include wildcards, 
this mapping can be used to build consistent and expected value names.</td>
<td><a href="#SubjectNameMapping">SubjectNameMapping</a></td>
<td></td>
</tr>

<tr class="odd">
<td>Selector</td>
<td><p>Evaluate a <a href="https://jmespath.org/">JMESpath</a> query against the value of a structured data type and returns the result.</p>
<p>The selector can be used to restructure or select values from structured data types.</p></td>
<td>Datatype: String</td>
<td>Parameter: <a href="https://jmespath.org/">JMESPath</a> expression, see https://jmespath.org/</td>
</tr>

</tbody>
</table>

[^top](#natsadapterconfiguration)

## SubjectNameMapping

The mapping of subject names of received data updates to data value names

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Mappings</td>
<td><p>Mapping table for mapping the subject names of received subject data updates to data value names. As a channel can 
subscribe to multiple subjects, that can also include wildcards, updates from different subjects can be received.</p>
<p>This element is a map that uses regular expression strings as indexes. The entries in the map are strings that will be 
used as replacement strings if the regular expression of the entry matches the name of the subject for an update.</p>
<p>The replacement string can include substitution parameters for capturing groups in the regular expression.</p></td>
<td>Map[String,String]</td>
<td>The must be at least one subject in the list of subjects.</td>
</tr>

<tr class="odd">
<td>Json</td>
<td>Set to true if data received from subjects is in JSON format</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>

<tr class="even">
<td>SubjectNameMapping</td>
<td>Mapping from subject names to alternative names. As a channel can have multiple subjects, that also can include wildcards, this mapping can be used to build consistent and expected value names.</td>
<td><a href="#SubjectNameMapping">SubjectNameMapping</a></td>
<td><p>Example:</p>
<p>Channel subscription is:</p>
<p>"Subjects" :[ "test"/>"]</p>
<p>The mapping is:</p>
<p>"Mappings": {<br />
"test\.(\\w+)": "test-$1"<br />
}</p>
<p>The mapping above matches updates for sub-levels of the test subject, it will use the name of the sub-level to create a name for the received data.</p>
<p>If an update is received for data in subject "test.a" then the name of the data value will be "test-a"</p></td>
</tr>

<tr class="odd">
<td>IncludeUnmappedSubjects</td>
<td>If set to false, updates for values from subjects that do not match any of the expressions in the mapping's element will be dropped. If set to true then the name of the value will be the name of the subject the update was received for.</td>
<td>Boolean</td>
<td>Default is false</td>
</tr>

<tr class="even">
<td>Selector</td>
<td><p>Evaluate a <a href="https://jmespath.org/">JMESpath</a> query against the value of a structured data type and returns the result.</p>
<p>The selector can be used to restructure or select values from structured data types.</p></td>
<td>String</td>
<td>Parameter: <a href="https://jmespath.org/">JMESPath</a> expression, see https://jmespath.org/</td>
</tr>

</tbody>
</table>

[^top](#natsadapterconfiguration)

## NatsAdapterConfiguration

The NatsAdapterConfiguration extends the common adapter configuration with NATS specific adapter configuration settings. The AdapterType to use for this adapter is "NATS"

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>ReadMode</td>
<td>Read mode of the adapter. Set to "KeepAll" to collect all messages on subscribed subjects during a read interval.
Set to "KeepLast", which is the default, to keep only the last received message.</td>
<td>String</td>
<td>

- "KeepLast" to collect last message received in read interval (Default)
- "KeepAll" to collect all messages received in read interval
</td>
</tr>


<tr class="odd">
<td>Servers</td>
<td>Servers configured for this adapter. The nats source using the adapter must refer to one of these servers with the 
AdapterServer attribute.</td>
<td>Map[String,<a href="#natsserverconfiguration">NatsServerConfiguration</a>]</td>
<td></td>
</tr>


<tr class="even">
<td>ReceivedDataChannelSize</td>
<td>Size of internal buffer to receive data for subject subscriptions</td>
<td>Int</td>
<td>Default is 1000</td>
</tr>


</tbody>
</table>

[^top](#natsadapterconfiguration)

## NatsServerConfiguration

NATS Server configuration

<table>  
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>
 
<tbody>  
<tr class="odd">  
<td><strong>Name</strong></td>  
<td><strong>Description</strong></td>  
<td><strong>Type</strong></td>  
<td><strong>Comments</strong></td>  
</tr>
  
<tr class="even">  
<td>Url</td>  
<td>Server url</td>  
<td>String</td>  
<td>

The schema for the url can be "nats://", "tls://"  or "tls://". If the scheme is "tls:" then
the "Tls" property for the serer must be set to specify the required key and certificates.

Multiple urls can be configured for known all known servers as a comma separated list.

</td>
</tr>

<tr class="odd">  
<td>ConnectRetries</td>  
<td>Maximum number of retries connecting to the server.</td>  
<td>Integer</td>  
<td>
Default = 3
</td> 
</tr>

<tr class="even">  
<td>WaitAfterConnectError</td>  
<td>Number of seconds to wait after connecting to the sever failed.</td>  
<td>Integer</td>  
<td>
Default = 10
</td> 
</tr>

<tr class="odd">  
<td>Token</td>  
<td>Random token authentication works like passwords for simple setups, but 
larger systems should use more secure authentication methods since tokens rely on solely 
on secrecy.
In  case a token is used not to configured as clear text in the configuration, instead use a 
placeholder for a secret stored in and retrieved from the 
<a href="../core/cloud-secret-configuration.md">AWS Secrets Manager service</a>.

</td>  
<td>String</td>  
<td>
<a href="https://docs.nats.io/using-nats/developer/connecting/token">https://docs.nats.io/using-nats/developer/connecting/token</a>
</td> 
</tr>

<tr class="even">  
<td>Username</td>  
<td>Username to authenticate with the server.

It is strongly recommended to configure the username as clear text in the configuration, instead use a 
placeholder for a secret stored in and retrieved from the 
<a href="../core/cloud-secret-configuration.md">AWS Secrets Manager service</a>.

</td>  
<td>String</td>  
<td>
<a href="https://docs.nats.io/using-nats/developer/connecting/userpass">https://docs.nats.io/using-nats/developer/connecting/userpass</a>

If a Username is configured then the Password must be configured as well.
</td> 
</tr>

<tr class="odd">  
<td>Password</td>  
<td>Password to authenticate with the server.

It is strongly recommended to configure the password is used not to configured as clear text in the configuration, but instead use a 
placeholder for a secret stored in and retrieved from the 
<a href="../core/cloud-secret-configuration.md">AWS Secrets Manager service</a>.

</td>  
<td>String</td>  
<td>
<a href="https://docs.nats.io/using-nats/developer/connecting/userpass">https://docs.nats.io/using-nats/developer/connecting/userpass</a>

If a Password is configured then the Username must be configured as well.
</td> 
</tr>

<tr class="even">  
<td>NKeyFile</td>  
<td>Pathname of a file containing the NKEY.

NATS NKeys are a public-key signature system based on Ed25519 that provides strong authentication 
and identity management. They allow secure authentication between NATS clients and servers using 
public/private key pairs.
</td>  
<td>String</td>  
<td>
<a href="https://docs.nats.io/using-nats/developer/connecting/nkey">https://docs.nats.io/using-nats/developer/connecting/nkey</a>
</td> 
</tr>

<tr class="odd">  
<td>CredentialsFile</td>  
<td>Pathname of a file containing credentials.

NATS credentials files contain a user JWT token and an NKey private seed, 
used together for secure client authentication and authorization.
</td>  
<td>String</td>  
<td>
<a href="https://docs.nats.io/using-nats/developer/connecting/creds">https://docs.nats.io/using-nats/developer/connecting/creds</a>
</td> 
</tr>

  
<tr class="odd">  
<td>Tls</td>  
<td>While authentication limits which clients can connect, TLS can be used to encrypt 
traffic between client/server and check the server’s identity. Additionally - in the most 
secure version of TLS with NATS - the server can be configured to verify the client's identity, 
thus authenticating it. When started in TLS mode, a nats-server will require all clients to 
connect with TLS. 
Moreover, if configured to connect with TLS, client libraries will fail to connect to a 
server without TLS.</td>  
<td>TlsConfiguration</td>  
<td>
<a href="https://docs.nats.io/using-nats/developer/connecting/tls">https://docs.nats.io/using-nats/developer/connecting/tls</a>
</td>  
</tr>
</tbody>
</table>

[^top](#natsadapterconfiguration)
 
## NatsTlsConfiguration

<table>  
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>  
<tr class="odd">  
<td><strong>Name</strong></td>  
<td><strong>Description</strong></td>  
<td><strong>Type</strong></td>  
<td><strong>Comments</strong></td>  
</tr>

<tr class="even">  
<td>RootCA</td>  
<td>Path to root certificate file.</td>  
<td>String</td>  
<td></td>  
</tr>
  
<tr class="odd">  
<td>Certificate</td>  
<td>Path to client certificate file.</td>  
<td>String</td>  
<td></td>  
</tr>

<tr class="even">  
<td>RootCA</td>  
<td>Path to root private  file.</td>  
<td>String</td>  
<td></td>  
</tr>

</tbody>  
</table>

[^top](#natsadapterconfiguration)