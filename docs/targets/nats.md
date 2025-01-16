# NATS Target

- [NatsTargetConfiguration](#NatsTargetConfiguration)

- [NatsServerConfiguration](#NatsServerConfiguration)

  

## NatsTargetConfiguration

[SFC Configuration](../core/sfc-top-level-config.md) > [Targets](../core/sfc-top-level-config.md#Targets) >  [Target](../core/target-configuration.md) 



NatsTargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for connecting to and sending to a NATS subject. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"NATS-TARGET"**

- [Schema](#NatsTargetConfiguration-Schema)
- [Examples](#NatsTargetConfiguration-Examples)

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

**Values:**
- "None" (Default)
- "Zip"
- "GZip"

**Type**: String

---
### MaxPayloadSize
Max payload size in KB for messages.

**Type**: Int

Note if compression is enabled the payload size of a single target data message, or a batch of messages can be larger, than this value. When batching of messages is enabled, without compression a batch
of messages will be sent to the subject when this size is reached.


---
### NatsServer
Nats target server for publishing data

**Type**: [NatsServerConfiguration](#natsserverconfiguration)

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

### NatsTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "NatsTargetConfiguration",
  "type": "object",
  "allOf": [
    {
      "$ref": "#/definitions/TargetConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AlternateSubjectName": {
          "type": "string",
          "description": "Alternate subject name when primary subject is unavailable"
        },
        "BatchCount": {
          "type": "integer",
          "description": "Number of messages to batch before publishing"
        },
        "BatchInterval": {
          "type": "integer",
          "description": "Interval in milliseconds between batch publishes"
        },
        "BatchSize": {
          "type": "integer",
          "description": "Maximum size of batched messages in bytes"
        },
        "Compression": {
          "type": "string",
          "description": "Type of message compression",
          "enum": ["None", "Zip", "GZip"],
          "default": "None"
        },
        "MaxPayloadSize": {
          "type": "integer",
          "description": "Maximum size of message payload in bytes"
        },
        "NatsServer": {
          "$ref": "#/definitions/NatsServerConfiguration",
          "description": "NATS server configuration"
        },
        "PublishTimeout": {
          "type": "integer",
          "description": "Timeout for publish operations in milliseconds"
        },
        "SubjectName": {
          "type": "string",
          "description": "Primary subject name for publishing"
        }
      },
      "required": ["NatsServer", "SubjectName"]
    }
  ]
}

```

### NatsTargetConfiguration Examples

```json
  "TargetType" : "NATS-TARGET"
```

Example 1 - Basic Configuration:

```json
{
  "TargetType" : "NATS-TARGET",
  "SubjectName": "sensors.data",
  "NatsServer": {
    "Url": "nats://nats.example.com:4222",
    "ConnectRetries": 5,
    "WaitAfterConnectError": 10000
  },
  "Compression": "None"
}
```

Batched Publishing Configuration:

```json
{
  "SubjectName": "metrics.production",
  "BatchCount": 100,
  "BatchSize": 128,
  "BatchInterval": 1000,
  "NatsServer": {
    "Url": "nats://nats.example.com:4222"
  }
}
```



Dynamic subject names based on target- and metadata values

```json
{
  "SubjectName": "data/%location%/%line%/%source%",
  "AlternateSubjectName": "data/sensors",
  "WarnAlternateSubjectName": true,
  "BatchCount": 100,
  "BatchSize": 128,
  "BatchInterval": 1000,
  "NatsServer": {
    "Url": "nats://nats.example.com:4222"
  }
}
```



[^top](#nats-target)

## NatsServerConfiguration

[NatsTarget](#NatsTargetConfiguration) > [NatsServer](#NatsServer])



- [Schema](#NatsServerConfiguration-Schema)
- [Examples](#NatsServerConfiguration-Examples)

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

**Type**: [TlsConfiguration](../core/transformation-operator-configuration.md)


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

### NatsServerConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "NatsServerConfiguration",
  "type": "object",
  "properties": {
    "ConnectRetries": {
      "type": "integer",
      "description": "Number of connection retry attempts"
    },
    "CredentialsFile": {
      "type": "string", 
      "description": "Path to NATS credentials file"
    },
    "NKeyFile": {
      "type": "string",
      "description": "Path to NATS NKey file"
    },
    "Password": {
      "type": "string",
      "description": "Password for authentication"
    },
    "Tls": {
      "$ref": "#/definitions/TlsConfiguration",
      "description": "TLS configuration settings"
    },
    "Token": {
      "type": "string",
      "description": "Authentication token"
    },
    "Url": {
      "type": "string",
      "description": "NATS server URL"
    },
    "Username": {
      "type": "string",
      "description": "Username for authentication"
    },
    "WaitAfterConnectError": {
      "type": "integer",
      "description": "Wait time in milliseconds after connection error"
    }
  },
  "required": ["Url"]
}

```

### NatsServerConfiguration Examples

```json
"TargetType" : "NATS-TARGET"
```

Basic Configuration:

```json
{
  "TargetType" : "NATS-TARGET",
  "Url": "nats://localhost:4222",
  "Username": "${user}",
  "Password": "${password}",
  "ConnectRetries": 3,
  "WaitAfterConnectError": 5000
}
```



Secure Configuration with TLS:

```json
{
  "Url": "nats://nats.example.com:4222",
  "Tls": {
    "CertificateFile": "/path/to/client-cert.pem",
    "PrivateKeyFile": "/path/to/private-key.pem"
  },
  "ConnectRetries": 5,
  "WaitAfterConnectError": 10000
}
```



Token-Based Authentication:

```json
{
  "Url": "nats://nats-server:4222",
  "Token": "${token}",
  "ConnectRetries": 3,
  "WaitAfterConnectError": 3000
}
```



Credentials File Configuration:

```json
{
  "Url": "nats://prod.nats.com:4222",
  "CredentialsFile": "/path/to/credentials.creds",
  "NKeyFile": "/path/to/user.nkey"
}
```



[^top](#nats-target)

