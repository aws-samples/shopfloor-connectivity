# NATS Adapter Configuration


---
- [NatsSourceConfiguration](#NatsSourceConfiguration)
- [NatsChannelConfiguration](#NatsChannelConfiguration)
- [SubjectNameMapping](#SubjectNameMapping)
- [NatsAdapterConfiguration](#NatsAdapterConfiguration)
- [NatsServerConfiguration](#NatsServerConfiguration)
- [NatsTlsConfiguration](#NatsTlsConfiguration)

---

## NatsSourceConfiguration


**Properties:**
- [AdapterServer](#AdapterServer)
- [Channels](#Channels)

---
### AdapterServer
Server Identifier for the NATS server to read from. This referenced server must be present in the Servers section of the adapter 
referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a server in the [Servers](#Servers) section of the NATS server used by the source.

---
### Channels
The channels configuration for an NATS source holds configuration data to read values from subjects on the source NATS server.
The element is a map indexed by the channel identifier.
Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,[NatsChannelConfiguration](#NatsChannelConfiguration)]

At least 1 channel must be configured.



[^top](#NATS Adapter Configuration)


## NatsChannelConfiguration


**Properties:**
- [Json](#Json)
- [Selector](#Selector)
- [SubjectNameMapping](#SubjectNameMapping)
- [Subjects](#Subjects)

---
### Json
Set to true if data received from subjects is in JSON format

**Type**: Boolean

Default is true

---
### Selector
Evaluate a JMESpath query against the value of a structured data type and returns the result.
The selector can be used to restructure or select values from structured data types.

**Type**: Datatype: String

Parameter: JMESPath expression, see https://jmespath.org/

---
### SubjectNameMapping
Mapping from subject names to alternative names. As a channel can have multiple subjects, that also can include wildcards, 
this mapping can be used to build consistent and expected value names.

**Type**: [SubjectNameMapping](#SubjectNameMapping)

---
### Subjects
A string array containing the subjects for this channel to subscribe to. The subjects names may contain single-level (*) and multi-level (>) wildcards

**Type**: String[]

The must be **at least one subject** in the list of subjects.

[^top](#NATS Adapter Configuration)




## SubjectNameMapping


**Properties:**
- [IncludeUnmappedSubjects](#IncludeUnmappedSubjects)
- [Json](#Json)
- [Mappings](#Mappings)
- [Selector](#Selector)
- [SubjectNameMapping](#SubjectNameMapping)

---
### IncludeUnmappedSubjects
If set to false, updates for values from subjects that do not match any of the expressions in the mapping's element will be dropped. If set to true then the name of the value will be the name of the subject the update was received for.

**Type**: Boolean

Default is false

---
### Json
Set to true if data received from subjects is in JSON format

**Type**: Boolean

Default is true

---
### Mappings
Mapping table for mapping the subject names of received subject data updates to data value names. As a channel can 
subscribe to multiple subjects, that can also include wildcards, updates from different subjects can be received.
This element is a map that uses regular expression strings as indexes. The entries in the map are strings that will be 
used as replacement strings if the regular expression of the entry matches the name of the subject for an update.
The replacement string can include substitution parameters for capturing groups in the regular expression.

**Type**: Map[String,String]

The must be at least one subject in the list of subjects.

---
### Selector
Evaluate a JMESpath query against the value of a structured data type and returns the result.
The selector can be used to restructure or select values from structured data types.

**Type**: String

Parameter: JMESPath expression, see https://jmespath.org/

---
### SubjectNameMapping
Mapping from subject names to alternative names. As a channel can have multiple subjects, that also can include wildcards, this mapping can be used to build consistent and expected value names.

**Type**: SubjectNameMapping

Example:
Channel subscription is:

```json
	"Subjects" :[ "test"/>"]
```

The mapping is:

```json
	"Mappings": {
		"test\.(\\w+)": "test-$1"
	}
```



The mapping above matches updates for sub-levels of the test subject, it will use the name of the sub-level to create a name for the received data.
If an update is received for data in subject "test.a" then the name of the data value will be "test-a"

[^top](#NATS Adapter Configuration)




## NatsAdapterConfiguration

**Properties:**

- [ReadMode](#ReadMode)
- [ReceivedDataChannelSize](#ReceivedDataChannelSize)
- [Servers](#Servers)



---
### ReadMode
Read mode of the adapter. Set to "KeepAll" to collect all messages on subscribed subjects during a read interval.
Set to "KeepLast", which is the default, to keep only the last received message.

**Type**: String

- "KeepLast" to collect last message received in read interval (Default)
- "KeepAll" to collect all messages received in read interval


---
### ReceivedDataChannelSize
Size of internal buffer to receive data for subject subscriptions

**Type**: Int

Default is 1000

---
### Servers
Servers configured for this adapter. The nats source using the adapter must refer to one of these servers with the 
AdapterServer attribute.

**Type**: Map[String,[NatsServerConfiguration](#NatsServerConfiguration)]

[^top](#NATS Adapter Configuration)




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

[^top](#NATS Adapter Configuration)




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

[^top](#NATS Adapter Configuration)

