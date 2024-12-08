# NATS Target 
<br>
<p>NatsTargetConfiguration extends the type <a href="../core/target-configuration.md" >TargetConfiguration</a> with 
specific configuration data for connecting to and sending to a NATS subject. The Targets configuration element can 
contain entries of this type, the TargetType of these entries must be set to <strong>"NATS-TARGET"</strong></p>
<br>

[NatsTargetConfiguration](#natstargetconfiguration)
[NatsTlsConfiguration](#natstlsconfiguration)


[Targets](./README.md)

## NatsTargetConfiguration

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


<tr class="odd">  
<td>Url</td>  
<td>Server url</td>  
<td>String</td>  
<td>

The schema for the url can be "nats://", "tls://"  or "tls://". If the scheme is "tls:" then
the "Tls" property for the serer must be set to specify the required key and certificates.

</td>
</tr>
<tr class="even">
<td>SubjectName</td>
<td>Name or name template of the subject</td>
<td>String</td>
<td>
A template can be used for the subjectName to render the actual subject name using placeholders. In this template, 
besides placeholders for environment variables (${name}) the following placeholders are available:

- %schedule%
- %target%
- %source%
- %channel%

To use the values of **metadata** at the top, source or channel level of the target data, the name af the metadata value can 
be used with a '%' prefix and postfix.

Value placeholders can be used to add additional subject levels or grouping values to a specific subject.

Template examples:

- plant1-**%source%** : Values from each source will be published to a subject for that source
- plant1-**%line%**   : Values from all sources will be grouped by the value of the %line% metadata and published to a subject for that value

In case a placeholder is not resolved, when a value for a used placeholder is part of the data,
then an alternative subject name can be configured by setting the name of that subject to the **"AlternateTopiName"** setting.

Note that the use of placeholders to send data to specific subjects will result in additional publish calls to the server.

</td>
</tr>

<tr class="odd">
<td>AlternateSubjectName</td>
<td>Name or name template of the subject values are published in case there are unmapped template placeholders in the SubjectName</td>
<td>Boolean</td>
<td>
</td>
</tr>

<tr class="odd">
<td>WarnAlternateSubjectName</td>
<td>Generate warning if data is published to AlternateSubjectName</td>
<td>Boolean</td>
<td>
Default is tue
</td>
</tr>

<tr class="even">  
<td>ConnectRetries</td>  
<td>Maximum number of retries connecting to the server.</td>  
<td>Integer</td>  
<td>
Default = 3
</td> 
</tr>

<tr class="odd">  
<td>WaitAfterConnectError</td>  
<td>Number of seconds to wait after connecting to the sever failed.</td>  
<td>Integer</td>  
<td>
Default = 10
</td> 
</tr>

<tr class="even">
<td>PublishTimeout</td>
<td>Timeout in seconds for publishing</td>
<td>Long</td>
<td>Default is 10 seconds</td>
</tr>



<tr class="odd">  
<td>BatchCount</td>  
<td>Number of messages to buffer before sending data as a batch to a subject.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the subject.</td>
</tr>

<tr class="even">  
<td>BatchSize</td>  
<td>Payload size in KB of messages to batch before sending data as a batch to a subject.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the subject.
The size is calculated on the uncompressed payload of the messages.</td>
</tr>

<tr class="odd">  
<td>BatchInterval</td>  
<td>Interval in milliseconds after which a batch of messages is sent to the subject, even when the BatchSize or BatchCount limit is not reached.</td>  
<td>Int</td>  
<td>Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the subject.
</td>
</tr>

<tr class="even">
<td>MaxPayloadSize</td>
<td>Max payload size in KB for messages.</td>
<td>Int</td>
<td>Note if compression is enabled the payload size of a single target data message, or a batch of messages can be larger, than this value. When batching of messages is enabled, without compression a batch
of messages will be sent to the subject when this size is reached.</td>
</tr>

<tr class="odd">  
<td>Compression</td>  
<td>Compression method for NATS message payloads.</td>  
<td>String

- "None"
- "Zip"
- "GZip"

</td>  
<td>Default is "None"</td>
</tr> 


<tr class="even">  
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

<tr class="odd">  
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

<tr class="even">  
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

<tr class="odd">  
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

<tr class="even">  
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

</tbody></table>

[^top](#natstargetconfiguration)


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

[^top](#natstargetconfiguration)