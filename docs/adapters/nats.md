# NATS Adapter Configuration

The SFC Nats protocol adapter enables seamless integration between SFC and NATS messaging systems. It provides bidirectional message translation and routing between SFC's internal message format and NATS publish/subscribe patterns.

The adapter supports both core NATS and NATS JetStream, allowing for both real-time messaging and persistent message streaming scenarios. It handles automatic reconnection, message quality of service, and maintains message delivery guarantees according to the configured settings.

This protocol adapter is particularly useful in microservices architectures where NATS serves as the messaging backbone, enabling SFC to participate in existing NATS-based ecosystems while maintaining its core functionality and message processing capabilities.

In order to use this adapter as in [in-process](../sfc-running-adapters.md#running-protocol-adapters-in-process) type adapter the type must be added to the [AdapterTypes](../core/sfc-configuration.md#adaptertypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"AdapterTypes" :{
  "NATS" : {
    "JarFiles" : ["<location of deployment>/nats/lib"]
  },
  "FactoryClassName" : "com.amazonaws.sfc.nats.NatsAdapter"
}
```

**Configuration:**

- [NatsSourceConfiguration](#natssourceconfiguration)
- [NatsChannelConfiguration](#natschannelconfiguration)
- [SubjectNameMappingConfiguration](#subjectnamemappingconfiguration-type)
- [NatsAdapterConfiguration](#natsadapterconfiguration)
- [NatsServerConfiguration](#natsserverconfiguration)

---

## NatsSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) >  [Source](../core/source-configuration.md) 

The NatsSourceConfiguration class defines the configuration parameters for receiving messages from a NATS messaging system. 

Source configuration for the NATS protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type. 

- [Schema](#natssourceconfiguration-schema)
- [Examples](#natssourceconfiguration-examples)

**Properties:**
- [AdapterServer](#adapterserver)
- [Channels](#channels)

---
### AdapterServer
Server Identifier for the NATS server to read from. This referenced server must be present in the Servers section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a server in the [Servers](#servers) section of the NATS server used by the source.

---
### Channels
The channels configuration for an NATS source holds configuration data to read values from subjects on the source NATS server. The element is a map indexed by the channel identifier. Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

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

Configuration class for the NATS protocol adapter that defines settings for connecting to a NATS messaging server and configuring message channels. It includes server connection parameters (URL, timeouts, reconnection policies) and channel configurations that specify subject subscriptions and optional subject name mappings for receiving data updates

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
Configuration for processing JSON formatted messages received from NATS subjects. If enabled, the adapter will parse the message content as JSON.

**Type**: Boolean

Default is true

---
### Selector
Optional JMESPath expression that selects or transforms specific values from structured message data, allowing filtering and reshaping of the data before processing

**Type**: Datatype: String

Parameter: JMESPath expression, see https://jmespath.org/

---
### SubjectNameMappingConfiguration
Optional configuration that maps received subject names to alternative value names, providing consistent naming when receiving data from multiple or wildcard subjects

**Type**: [SubjectNameMappingConfiguration](#subjectnamemappingconfiguration-type)

---
### Subjects
List of NATS subjects to subscribe to. Subject names support wildcards: * for matching any string within a single level, and > for matching all remaining levels when used at the end of a pattern.

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

Configuration class that defines mappings to transform NATS subject names into alternative value names. It enables standardization of value names when receiving data from multiple or wildcard subjects through a dictionary of regular expression patterns and their corresponding replacement templates.

- [Schema](#subjectnamemappingconfiguration-schema)
- [Examples](#subjectnamemappingconfiguration-examples)

**Properties:**

- [IncludeUnmappedSubjects](#includeunmappedsubjects)
- [Mappings](#mappings)

---
### IncludeUnmappedSubjects
Controls whether to include messages from subjects that don't match any mapping pattern. When true, unmapped subjects use their original name; when false, messages from unmapped subjects are dropped

```json
{
    "IncludeUnmappedSubjects": true,
    "Mapping": {
        "sensors.*.temperature": "temp"
    }
}
```

In this example, a message from "sensors.room1.temperature" would be mapped to "temp", while a message from "sensors.room1.humidity" would keep its original subject name since it's unmapped and IncludeUnmappedSubjects is true. If IncludeUnmappedSubjects is set to false the message would be dropped.



**Type**: Boolean

Default is false


---
### Mappings
Mapping table that transforms received subject names into data value names using regular expressions. This is particularly useful when a channel subscribes to multiple subjects or uses wildcards, allowing standardized naming for data from different subject hierarchies.

- Uses regular expressions as keys to match incoming subject names
- Replacement strings can include captured groups from the regex using substitution parameters ($1, $2, etc.)
- At least one subject must be defined in the channel's subject list
- Provides consistent naming convention for data received from different subject hierarchies

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

**Type**: Map[String,String]

The must be at least one subject in the list of subjects.

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
    "factory\\.line-(\\w+)": "production/line-{1}"
  }
}
```

^top](#natsadapterconfiguration)



## NatsAdapterConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [ProtocolAdapters](../core/sfc-configuration.md#protocoladapters) > [Adapter](../core/protocol-adapter-configuration.md) 

Configuration class for the NATS adapter that defines connection settings, authentication, and channel configurations for interacting with a NATS message brokers.

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

When [ReadMode](#readmode) is `KeepAll`  this parameter can be used to restrict the period in milliseconds for which values are stored. This property helps manage memory usage by limiting how long retained NATS messages are kept in the system.

For example:

- If MaxRetainPeriod is set to 3600000 (1 hour), any retained messages older than one hour will be discarded
- This prevents unbounded growth of stored messages while still maintaining a useful history
- The time period is measured in milliseconds from when the message was received

This setting is particularly useful when dealing with high-frequency NATS messages or when system memory constraints need to be considered

**Type**: Integer

The default value is 3.600.00 (1 hour). If set to 0 there is no maximum period.

---

### MaxRetainSize

When [ReadMode](#readmode) is  `KeepAll` this parameter can be used to restrict the maximum number of stored values. This property helps control memory usage by limiting the total number of retained NATS messages that can be stored at any given time.

For example:

- If MaxRetainSize is set to 1000, only the most recent 1000 messages will be kept
- When the limit is reached, the oldest messages are discarded to make room for new ones
- This creates a rolling buffer of the most recent messages

This setting is particularly useful for preventing memory issues in systems that handle high volumes of NATS messages while still maintaining access to recent message history

**Type**: Integer

The default value is 10000.
If set to 0 there is no maximum number of values.

---

### ReadMode

- Read mode of the adapter. Set to KeepAll to collect all messages on subscribed topics during a read interval.
  Set to "KeepLast", which is the default, to keep only the last received message. 

**Type** : String


- KeepLast  to collect last values received in read interval each topic, discarding earlier messages (Default)

- KeepAll to collect values received in read interval up to the maximum specified by MaxRetainSize values of not older than specified by MaxRetainPeriod



---

### ReceivedDataChannelSize

The ReceivedDataChannelSize property defines the size of the channel buffer used for receiving NATS messages. This setting determines how many messages can be queued in memory before processing.

Key points:

- Controls the buffer capacity for incoming NATS messages
- Helps manage memory usage and message processing flow
- Larger values allow more messages to be queued but consume more memory

This setting is important for tuning the performance and reliability of the NATS adapter based on your specific message volume and processing requirements

**Type**: Int

Default is 1000

---

### ReceivedDataChannelTimeout

Timeout in milliseconds to send data to internal buffer for received data for topic subscriptions. This property specifies how long the system will wait when attempting to add received NATS messages to the internal processing buffer.

Key aspects:

- Defines the maximum time (in milliseconds) to wait when buffering received messages
- Helps prevent system blockage if the internal buffer becomes full
- If the timeout is reached, the system may drop messages to prevent blocking

This timeout setting is crucial for maintaining system responsiveness while handling high volumes of NATS messages, preventing deadlocks that could occur if the buffer becomes full.

**Type**: Int

Default is 1000

---
### Servers
List of NATS server configurations that can be referenced by NATS sources using the AdapterServer attribute

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
          "description": "Mode for reading NATS messages",
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

Configuration class that defines connection settings for a NATS server, including server URL, authentication credentials, and connection options. Each server configuration can be referenced by multiple NATS sources through its unique identifier.

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
Number of connection retry attempts to make when establishing a connection to the NATS server

**Type**: Integer


Default = 3


---
### CredentialsFile
Path to a NATS credentials file containing a user JWT token and NKey private seed used for secure client authentication.


**Type**: String


https://docs.nats.io/using-nats/developer/connecting/creds


---
### NKeyFile
Path to a file containing the NKEY (Ed25519-based public/private key pair) used for secure authentication with the NATS server.

NATS NKeys are a public-key signature system based on Ed25519 that provides strong authentication and identity management. They allow secure authentication between NATS clients and servers using  public/private key pairs.


**Type**: String


https://docs.nats.io/using-nats/developer/connecting/nkey

---
### Password

Password if broker is using username and password authentication

**Type** : String

**Username and password should not be included as clear text in the configuration.** It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.

Security considerations:

- Never store passwords in plain text

- Use [AWS Secrets Manager](../core/secrets-manager-configuration.md) to securely store credentials

- Use placeholders in configuration files

**Type**: String


https://docs.nats.io/using-nats/developer/connecting/userpass

If a Password is configured then the Username must be configured as well.


---
### Tls

From the [NATS-docs](
): *While authentication limits which clients can connect, TLS can be used to encrypt traffic between client/server and check the server's identity. Additionally - in the most secure version of TLS with NATS - the server can be configured to verify the client's identity, thus authenticating it. When started in TLS mode, a nats-server will require all clients to connect with TLS. Moreover, if configured to connect with TLS, client libraries will fail to connect to a server without TLS.*

**Type**: [TlsConfiguration](../core/certificate-configuration.md)


https://docs.nats.io/using-nats/developer/connecting/tls


---
### Token
Random token authentication, akin to passwords for rudimentary configurations, is suitable for such scenarios. However, for more extensive systems, employing more secure authentication methods is advisable, as tokens solely rely on secrecy.
In the event that a token is not configured to store its value in clear text in the configuration, utilize a placeholder for a secret stored and retrieved from the AWS Secrets Manager service.

**Type**: String


https://docs.nats.io/using-nats/developer/connecting/token


---
### Url
Server URL(s) for connecting to the NATS server.

**Type**: String

The schema for the url can be `nats://` or `tls://`. If the scheme is "tls:" then
the "Tls" property for the server **could** also be set to specify the required key and certificates - for establishing `mTLS` secured auth.

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

