# NATS Adapter Configuration

NATS Protocol adapter configuration

---
- [NatsSourceConfiguration](#natssourceconfiguration)
- [NatsChannelConfiguration](#natschannelconfiguration)
- [SubjectNameMappingConfiguration](#subjectnamemappingconfiguration-type)
- [NatsAdapterConfiguration](#natsadapterconfiguration)
- [NatsServerConfiguration](#natsserverconfiguration)

---

## NatsSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) >  [Source](../core/source-configuration.md) 



Source configuration for the NATS protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type. 

- [Schema](#natssourceconfiguration-schema)
- [Examples](#natssourceconfiguration-examples)

**Properties:**
- [AdapterServer](#adapterserver)
- [Channels](#channels)

---
### AdapterServer
Server Identifier for the NATS server to read from. This referenced server must be present in the Servers section of the adapter 
referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a server in the [Servers](#servers) section of the NATS server used by the source.

---
### Channels
The channels configuration for an NATS source holds configuration data to read values from subjects on the source NATS server.
The element is a map indexed by the channel identifier.
Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,[NatsChannelConfiguration](#natschannelconfiguration)]

At least 1 channel must be configured.

### NatsSourceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for NATS source",
  "allOf": [
    {
      "$ref": "#/definitions/BaseSourceConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AdapterServer": {
          "type": "string",
          "description": "Reference to the NATS server configuration to be used by this adapter"
        },
        "Channels": {
          "type": "object",
          "description": "Map of NATS channel configurations",
          "additionalProperties": {
            "$ref": "#/definitions/NatsChannelConfiguration"
          },
          "minProperties": 1
        }
      },
      "required": [
        "AdapterServer",
        "Channels"
      ]
    }
  ]
}
```

### NatsSourceConfiguration Examples

```json
{
  "ProtocolAdapter" : "NatsAdapter",
  "AdapterServer": "main-nats",
  "Channels": {
    "temperature": {
      "Subjects": [
        "sensors.temperature"
      ],
      "Json": true
    }
  }
}
```

[^top](#natsadapterconfiguration)

## NatsChannelConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#channels) > [Channel](../core/channel-configuration.md)



The NatsChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the NATS protocol adapter.

- [Schema](#natschannelconfiguration-schema)
- [Examples](#natschannelconfiguration-examples)

  

**Properties:**

- [Json](#json)
- [Selector](#selector)
- [SubjectNameMappingConfiguration](#subjectnamemappingconfiguration)
- [Subjects](#subjects)

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
### SubjectNameMappingConfiguration
Mapping from subject names to alternative names. As a channel can have multiple subjects, that also can include wildcards, 
this mapping can be used to build consistent and expected value names.

**Type**: [SubjectNameMappingConfiguration](#subjectnamemappingconfiguration-type)

---
### Subjects
A string array containing the subjects for this channel to subscribe to. The subjects names may contain single-level (*) and multi-level (>) wildcards

**Type**: String[]

The must be **at least one subject** in the list of subjects.

[^top](#natsadapterconfiguration)

### NatsChannelConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for NATS channel",
  "allOf": [
    {
      "$ref": "#/definitions/ChannelConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Json": {
          "type": "boolean",
          "description": "Indicates if the payload is in JSON format"
        },
        "Selector": {
          "type": "string",
          "description": "Selector for filtering messages"
        },
        "SubjectNameMappingConfiguration": {
          "$ref": "#/definitions/SubjectNameMappingConfiguration",
          "description": "Configuration for mapping subject names"
        },
        "Subjects": {
          "type": "array",
          "description": "List of NATS subjects to subscribe to",
          "items": {
            "type": "string"
          },
          "minItems": 1
        }
      },
      "required": [
        "Subjects"
      ]
    }
  ]
}
```

### NatsChannelConfiguration Examples

```json
{
  "Subjects": [
    "sensors.temperature"
  ]
}
```



```json
{
  "Subjects": [
    "devices.*.readings",
    "devices.*.status"
  ],
  "SubjectNameMappingConfiguration": {
    "Mappings": {
      "devices\\.(\\w+)\\.(\\w+)": "devices-{2}-{1}"
    }
  }
}
```





## SubjectNameMappingConfiguration type

[NatsChannel](#natschannelconfiguration) > [SubjectNameMappingConfiguration](#subjectnamemappingconfiguration)



- [Schema](#subjectnamemappingconfiguration-schema)
- [Examples](#subjectnamemappingconfiguration-examples)

**Properties:**

- [IncludeUnmappedSubjects](#includeunmappedsubjects)
- [Mappings](#mappings)

---
### IncludeUnmappedSubjects
If set to false, updates for values from subjects that do not match any of the expressions in the mapping's element will be dropped. If set to true then the name of the value will be the name of the subject the update was received for.

**Type**: Boolean

Default is false


---
### Mappings
Mapping table for mapping the subject names of received subject data updates to data value names. As a channel can 
subscribe to multiple subjects, that can also include wildcards, updates from different subjects can be received.
This element is a map that uses regular expression strings as indexes. The entries in the map are strings that will be 
used as replacement strings if the regular expression of the entry matches the name of the subject for an update.
The replacement string can include substitution parameters for capturing groups in the regular expression.


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

**Type**: Map[String,String]

The must be at least one subject in the list of subjects.

The mapping above matches updates for sub-levels of the test subject, it will use the name of the sub-level to create a name for the received data.
If an update is received for data in subject "test.a" then the name of the data value will be "test-a"

### SubjectNameMappingConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "IncludeUnmappedSubjects": {
      "type": "boolean",
      "description": "Flag to include subjects that don't match any mapping",
      "default": false
    },
    "Mappings": {
      "type": "object",
      "additionalProperties": {
        "type": "string"
      },
      "minProperties": 1
    }
  },
  "required": [
    "Mappings"
  ]
}
```

### SubjectNameMappingConfiguration Examples



```json
{
  "Mappings": {
    "source\\.subject": "target.topic"
  }
}
```



Basic mapping with matching pattern for wildcards

```json
{
  "Mappings": {
    "device\\.(\\w+)\\.temperature": "sensors-temperature-{1}"
  }
}
```



Multiple mappings with unmapped topics included:

```json
{
  "IncludeUnmappedSubjects": true,
  "Mappings": {
    "device\\.(\\w+)\\.temperature": "sensors/temp/{1}",
    "device\\.(\\w+)\\.humidity": "sensors/humid/{1}",
    "factory\\.line-(\w+)": "production/line-{1}"
  }
}
```

^top](#natsadapterconfiguration)



## NatsAdapterConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [ProtocolAdapters](../core/sfc-configuration.md#protocoladapters) > [Adapter](../core/protocol-adapter-configuration.md) 



NatsAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the NATS Protocol adapter.

- [Schema](#natsadapterconfiguration-schema)
- [Examples](#natsadapterconfiguration-examples)

**Properties:**



- [MaxRetainSize](#maxretainsize)
- [MaxRetainPeriod](#maxretainperiod)

- [ReadMode](#readmode)
- [ReceivedDataChannelSize](#receiveddatachannelsize)
- [ReceivedDataChannelTimeout](#receiveddatachanneltimeout)
- [Servers](#servers)



---

### MaxRetainPeriod

When [ReadMode](#readmode) is `KeepAll` this parameter can be used to restrict the maximum number of stored values.

**Type**: Integer

The default value is 3600000 (1 hour). If set to 0 there is no maximum period.

---

### MaxRetainSize

When [ReadMode](#readmode) is `KeepAll` this parameter can be used to restrict the period in milliseconds for which values are stored.

**Type**: Integer

The default value is 10000 If set to 0 there is no maximum number of values.

---

### MaxRetainPeriod

When [ReadMode](#readmode) is `KeepAll` this parameter can be used to restrict the maximum number of stored values.

**Type**: Integer

The default value is 3600000 (1 hour). If set to 0 there is no maximum period.

---
### ReadMode
Read mode of the adapter. Set to "KeepAll" to collect all messages on subscribed subjects during a read interval.
Set to "KeepLast", which is the default, to keep only the last received message.

**Type**: String

- "KeepLast" to collect last message received in read interval (Default)
- "KeepAll" to collect all messages received in read interval

- 


---

### ReceivedDataChannelSize

Size of internal buffer to receive data for subject subscriptions

**Type**: Int

Default is 1000


---

### ReceivedDataChannelTimeout

Timeout in milliseconds to send data to internal buffer for received data for subject subscriptions

**Type**: Int

Default is 1000

**Type**: Int

Default is 1000

---
### Servers
Servers configured for this adapter. The nats source using the adapter must refer to one of these servers with the 
AdapterServer attribute.

**Type**: Map[String,[NatsServerConfiguration](#natsserverconfiguration)]

[^top](#natsadapterconfiguration)

### NatsAdapterConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for NATS adapter",
  "allOf": [
    {
      "$ref": "#/definitions/AdapterConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Servers": {
          "type": "object",
          "description": "Map of NATS server configurations",
          "additionalProperties": {
            "$ref": "#/definitions/NatsServerConfiguration"
          },
          "minProperties": 1
        },
        "MaxRetainPeriod" :{
          "type" : "integer",
          "description": "Max value retain period",
          "default" : 0
        },
        "MaxRetainSize" :{
          "type" : "integer",
          "description": "Max value retain size",
          "default" : 0
        },
        "ReadMode": {
          "type": "string",
          "description": "Mode for reading MQTT messages",
          "enum": [
            "KeepLast",
            "KeepAll"
          ],
          "default": "KeepLast"
        },
      },
      "required": [
        "Servers"
      ]
    }
  ]
}
```

### NatsAdapterConfiguration Examples

```json
{
  "AdapterType" : "NATS",
  "Servers": {
    "main": {
      "Url": "nats://secure.nats.com:4222",
      "CredentialsFile": "/path/to/nats.creds",
      "Tls": {
        "Certificate": "/path/to/client-cert.pem",
        "PrivateKey": "/path/to/private-key.pem",
        "RootCA": "/path/to/root-ca.pem"
      }
    }
  }
}

```

## NatsServerConfiguration

[NatsAdapter](#natsadapterconfiguration) > [Servers](#servers)



- [Schema](#natsserverconfiguration-schema)
- [Examples](#natsserverconfiguration-examples)

**Properties:**

- [ConnectRetries](#connectretries)
- [CredentialsFile](#credentialsfile)
- [NKeyFile](#nkeyfile)
- [Password](#password)
- [Tls](#tls)
- [Token](#token)
- [Url](#url)
- [Username](#username)
- [WaitAfterConnectError](#waitafterconnecterror)

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

**Type**: [CertificateConfiguration](../core/certificate-configuration)


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


Default is 10

### NatsServerConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for NATS server connection",
  "properties": {
    "ConnectRetries": {
      "type": "integer",
      "description": "Number of connection retry attempts",
      "default": 3
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
      "$ref": "#/definitions/CertificateConfiguration",
      "description": "TLS configuration for secure connection"
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
      "description": "Wait time in seconds after connection error",
      "default": 10
    }
  },
  "required": [
    "Url"
  ]
}
```

### NatsServerConfiguration Examples

Basic configuration:

```
{
  "Url": "nats://localhost:4222"
}
```



Basic configuration using 2 servers in a cluster

```
{
  "Url": "nats://server1:4222,nats://server2:4222"
}
```



Username/Password authentication using configuration placeholders

```json
{
  "Url": "nats://nats.example.com:4222",
  "Username": "${nats_user}",
  "Password": "${nats_password}",
  "ConnectRetries": 5,
  "WaitAfterConnectError": 15
}
```



TLS with credentials file:

```json
{
  "Url": "nats://secure.nats.com:4222",
  "CredentialsFile": "/path/to/nats.creds",
  "Tls": {
    "Certificate": "/path/to/client-cert.pem",
    "PrivateKey": "/path/to/private-key.pem",
    "RootCA": "/path/to/root-ca.pem"
  }
}
```



NKey authentication:

```json
{
  "Url": "nats://nats.example.com:4222",
  "NKeyFile": "/path/to/user.nkey",
}
```



Token authentication with TLS using configuration placeholder for the token.

```json
{
  "Url": "nats://nats.example.com:4222",
  "Token": "${secret-token}",
  "ConnectRetries": 3,
  "WaitAfterConnectError": 10
}
```



[^top](#natsadapterconfiguration)

[

