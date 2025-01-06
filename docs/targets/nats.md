# NATS Target

- [NatsTargetConfiguration](#NatsTargetConfiguration)
- [NatsServerConfiguration](#NatsServerConfiguration)
- [NatsTlsConfiguration](#NatsTlsConfiguration)

---

## NatsTargetConfiguration

NatsTargetConfiguration extends the type <a href="../core/target-configuration.md" >TargetConfiguration</a> with specific configuration data for connecting to and sending to a NATS subject. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"NATS-TARGET"</strong>


**Properties:**
- [AlternateSubjectName](#AlternateSubjectName)
- [BatchCount](#BatchCount)
- [BatchInterval](#BatchInterval)
- [BatchSize](#BatchSize)
- [Compression](#Compression)
- [MaxPayloadSize](#MaxPayloadSize)

- [NatsServer](#NatsServer)
- [PublishTimeout](#PublishTimeout)
- [SubjectName](#SubjectName)

---
### AlternateSubjectName
Name or name template of the subject values are published in case there are unmapped template placeholders in the SubjectName

**Type**: Boolean




---
### BatchCount
Number of messages to buffer before sending data as a batch to a subject.

**Type**: Int

Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the subject.

---
### BatchInterval
Interval in milliseconds after which a batch of messages is sent to the subject, even when the BatchSize or BatchCount limit is not reached.

**Type**: Int

Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the subject.


---
### BatchSize
Payload size in KB of messages to batch before sending data as a batch to a subject.

**Type**: Int

Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the subject.
The size is calculated on the uncompressed payload of the messages.

---
### Compression
Compression method for NATS message payloads.

**Type**: String

- "None"
- "Zip"
- "GZip"



Default is "None"

---
### MaxPayloadSize
Max payload size in KB for messages.

**Type**: Int

Note if compression is enabled the payload size of a single target data message, or a batch of messages can be larger, than this value. When batching of messages is enabled, without compression a batch
of messages will be sent to the subject when this size is reached.


---
### NatsServer
Nats target server for publishing data

**Type**: NatsServerConfiguration

Comments

---
### PublishTimeout
Timeout in seconds for publishing

**Type**: Long

Default is 10 seconds

---
### SubjectName
Name or name template of the subject

**Type**: String


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



[^top](#NATS Target)


## NatsServerConfiguration


**Properties:**
- [ConnectRetries](#ConnectRetries)
- [CredentialsFile](#CredentialsFile)
- [NKeyFile](#NKeyFile)

- [Password](#Password)
- [Tls](#Tls)
- [Token](#Token)
- [Url](#Url)
- [Username](#Username)
- [WaitAfterConnectError](#WaitAfterConnectError)

---
### ConnectRetries
Maximum number of retries connecting to the server.

**Type**: Integer


Default = 3


---
### CredentialsFile
Pathname of a file containing credentials.

NATS credentials files contain a user JWT token and an NKey private seed,
used together for secure client authentication and authorization.


**Type**: String


https://docs.nats.io/using-nats/developer/connecting/creds


---
### NKeyFile
Pathname of a file containing the NKEY.

NATS NKeys are a public-key signature system based on Ed25519 that provides strong authentication
and identity management. They allow secure authentication between NATS clients and servers using
public/private key pairs.


**Type**: String


https://docs.nats.io/using-nats/developer/connecting/nkey



---
### Password
Password to authenticate with the server.

It is strongly recommended to configure the password is used not to configured as clear text in the configuration, but instead use a
placeholder for a secret stored in and retrieved from the
AWS Secrets Manager service.



**Type**: String


https://docs.nats.io/using-nats/developer/connecting/userpass

If a Password is configured then the Username must be configured as well.


---
### Tls
While authentication limits which clients can connect, TLS can be used to encrypt 
traffic between client/server and check the server’s identity. Additionally - in the most 
secure version of TLS with NATS - the server can be configured to verify the client's identity, 
thus authenticating it. When started in TLS mode, a nats-server will require all clients to 
connect with TLS. 
Moreover, if configured to connect with TLS, client libraries will fail to connect to a 
server without TLS.

**Type**: TlsConfiguration


https://docs.nats.io/using-nats/developer/connecting/tls


---
### Token
Random token authentication works like passwords for simple setups, but 
larger systems should use more secure authentication methods since tokens rely on solely 
on secrecy.
In  case a token is used not to configured as clear text in the configuration, instead use a 
placeholder for a secret stored in and retrieved from the 
AWS Secrets Manager service.



**Type**: String


https://docs.nats.io/using-nats/developer/connecting/token


---
### Url
Server url

**Type**: String



The schema for the url can be "nats://", "tls://"  or "tls://". If the scheme is "tls:" then
the "Tls" property for the serer must be set to specify the required key and certificates.

Multiple urls can be configured for known all known servers as a comma separated list.



---
### Username
Username to authenticate with the server.

It is strongly recommended to configure the username as clear text in the configuration, instead use a
placeholder for a secret stored in and retrieved from the
AWS Secrets Manager service.



**Type**: String


https://docs.nats.io/using-nats/developer/connecting/userpass

If a Username is configured then the Password must be configured as well.


---
### WaitAfterConnectError
Number of seconds to wait after connecting to the sever failed.

**Type**: Integer


Default = 10


[^top](#NATS Target)


## NatsTlsConfiguration


**Properties:**
- [Certificate](#Certificate)

- [RootCA](#RootCA)
- [RootCA](#RootCA)

---
### Certificate
Path to client certificate file.

**Type**: String


---
### RootCA
Path to root certificate file.

**Type**: String

---
### RootCA
Path to root private  file.

**Type**: String

[^top](#NATS Target)

