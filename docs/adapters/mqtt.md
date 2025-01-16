# MQTT Protocol Configuration

[SFC Configuration](../core/sfc-configuration) > [Sources](../core/sfc-configuration#Sources) >  [Source](../core/source-configuration.md) 



MQTT Protocol adapter configuration.


---
- [MqttSourceConfiguration](#MqttSourceConfiguration)
- [MqttChannelConfiguration](#MqttChannelConfiguration)
- [TopicNameMapping](#TopicNameMapping)
- [MqttAdapterConfiguration](#MqttAdapterConfiguration)
- [MqttBrokerConfiguration](#MqttBrokerConfiguration)
- [TopicNameMappingConfiguration](#TopicNameMappingConfiguration-type)

---

## MqttSourceConfiguration

Source configuration for the MQTT protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type. 

- [Schema](#MqttSourceConfiguration-Schema)
- [Examples](#MqttSourceConfiguration-Examples)

**Properties:**
- [AdapterBroker](#AdapterBroker)
- [Channels](#Channels)

---
### AdapterBroker
Broker Identifier for the MQTT server to read from. This referenced server must be present in the Brokers section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a broker in the Brokers section of the MQTT adapter used by the source.

---
### Channels
The channels configuration for an MQTT source holds configuration data to read values from topics on the source MQTT broker.
The element is a map indexed by the channel identifier.
Channels can be "commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,[MqttChannelConfiguration](#MqttChannelConfiguration)]

At least 1 channel must be configured.

[^top](#mqtt-protocol-configuration)

### MqttSourceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "definitions": {
    "MqttSourceConfiguration": {
      "type": "object",
      "description": "Configuration for MQTT source adapter",
      "allOf": [
        {
          "$ref": "#/definitions/BaseSourceConfiguration"
        },
        {
          "type": "object",
          "properties": {
            "AdapterBroker": {
              "type": "string",
              "description": "Reference to the broker configuration to be used by this adapter"
            },
            "Channels": {
              "type": "object",
              "description": "Map of MQTT channel configurations",
              "additionalProperties": {
                "$ref": "#/definitions/MqttChannelConfiguration"
              },
              "minProperties": 1
            }
          },
          "required": [
            "AdapterBroker",
            "Channels"
          ]
        }
      ]
    }
  }
}

```

### MqttSourceConfiguration Examples

```json
{
  "ProtocolAdapter" : "mqtt-adapter",
  "AdapterBroker": "mqtt-broker-1",
  "Channels": {
    "temperature_sensor": {
      "Topics": ["sensors/temperature"]
    }
  }
}
```

## MqttChannelConfiguration

[SFC Configuration](../core/sfc-configuration) > [Sources](../core/sfc-configuration#Sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#Channels) > [Channel](../core/channel-configuration.md)



The MqttChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the MQTT protocol adapter.

- [Schema](#MqttChannelConfiguration-Schema)
- [Examples](#MqttChannelConfiguration-Examples)

**Properties:**
- [Json](#Json)

- [Selector](#Selector)
- [TopicNameMappingConfiguration](#TopicNameMappingConfiguration)
- [Topics](#Topics)

---
### Json
Set to true if data received from topics is in JSON format

**Type**: Boolean

Default is true

---
### Selector
Evaluate a JMESpath query against the value of a structured data type and returns the result.
The selector can be used to restructure or select values from structured data types.

**Type**: Datatype: String

**Parameter**: JMESPath expression, see https://jmespath.org/

---
### TopicNameMappingConfiguration
Mapping from topic names to alternative names. As a channel can have multiple topics, that also can include wildcards, this mapping can be used to build consistent and expected value names.

**Type**: [TopicNameMappingConfiguration](#TopicNameMappingConfiguration-type)

---
### Topics
A string array  containing the topics to subscribe to. The topic names may contain single-level (+) and multi-level (#) wildcards

**Type**: String[]

The must be at least one topic in the list of topics.

[^top](#mqtt-protocol-configuration)

### MqttChannelConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for MQTT channel",
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
        "TopicNameMapping": {
          "$ref": "#/definitions/TopicNameMappingConfiguration",
          "description": "Configuration for mapping topic names"
        },
        "Topics": {
          "type": "array",
          "description": "List of MQTT topics to subscribe to",
          "items": {
            "type": "string"
          },
          "minItems": 1
        }
      },
      "required": [
        "Topics"
      ]
    }
  ]
}
```

### MqttChannelConfiguration Examples

```json
{
  "Topics": [
    "sensors/temperature/room1"
  ]
}
```



```json
{
  "Topics": [
    "Topics" :[ "sensors/temperature"/#"]
  ],
  "TopicNameMappingConfiguration":{
    "Mappings": {
      "test/(\\w+)": "temperature-$1"
    }
}
```



## TopicNameMappingConfiguration type

[MqttSource](#MqttSourceConfiguration) > [Channels](#Channels) > [Channel](#MqttChannelConfiguration) > [TopicNameMappingConfiguration](#TopicNameMappingConfiguration)



Mapping from topic names to alternative names. As a channel can have multiple topics, that also can include wildcards, this mapping can be used to build consistent and expected value names.

**Type**: TopicNameMapping

Example:

Channel subscription is:

```json
	"Topics" :[ "test"/#"]
```



The mapping is:

```json
	"Mappings": {
		"test/(\\w+)": "test-$1"
	}
```

The mapping above matches updates for sub-levels of the test topic, it will use the name of the sub-level to create a name for the received data.

If an update is received for data in topic "test/a" then the name of the data value will be "test-a"

- [Schema](#TopicNameMappingConfiguration-Schema)
- [Examples](#TopicNameMappingConfiguration-Examples)

**Properties:**
- [IncludeUnmappedTopics](#IncludeUnmappedTopics)

- [Mappings](#Mappings)

  

  

---
### IncludeUnmappedTopics
If set to false, updates for values from topics that do not match any of the expressions in the mapping's element will be dropped. If set to true then the name of the value will be the name of the topic the update was received for.

**Type**: Boolean

Default is false

---
### Mappings
Mapping table for mapping the topic names of received topic data updates to data value names. As a channel can subscribe to multiple topics, that can also include wildcards, updates from different topics can be received.

This element is a map that uses regular expression strings as indexes. The entries in the map are strings that will be used as replacement strings if the regular expression of the entry matches the name of the topic for an update.

The replacement string can include substitution parameters for capturing groups in the regular expression.

**Type**: Map[String,String]

The must be at least one topic in the list of topics.

---
### TopicNameMapping
Mapping from topic names to alternative names. As a channel can have multiple topics, that also can include wildcards, this mapping can be used to build consistent and expected value names.

**Type**: TopicNameMapping

Example:

Channel subscription is:

```json
	"Topics" :[ "test"/#"]
```



The mapping is:

```json
	"Mappings": {
		"test/(\\w+)": "test-$1"
	}
```

The mapping above matches updates for sub-levels of the test topic, it will use the name of the sub-level to create a name for the received data.

If an update is received for data in topic "test/a" then the name of the data value will be "test-a"

[^top](#mqtt-protocol-configuration)


### TopicNameMappingConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for MQTT topic name mapping",
  "properties": {
    "IncludeUnmappedTopics": {
      "type": "boolean",
      "description": "Flag to include topics that don't match any mapping",
      "default": false
    },
    "Mappings": {
      "type": "object",
      "description": "Map of source topic patterns to target topic patterns",
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

### TopicNameMappingConfiguration Examples



Minimal configuration:

```json
{
  "Mappings": {
    "source/topic": "target/topic"
  }
}
```



Basic mapping with matching pattern for wildcards

```json
{
  "IncludeUnmappedTopics": false,
  "Mappings": {
    "device/(\\w+)/temperature": "sensors/temp/{1}"
  }
}
```



Multiple mappings with unmapped topics included:

```json
{
  "IncludeUnmappedTopics": true,
  "Mappings": {
    "device/(\\w+)/temperature": "sensors/tempe/{1}",
    "device/(\\w+)/humidity": "sensors//humid/{1}",
    "factory/line-(\w+)": "production/line/{1}"
  }
}
```



## MqttAdapterConfiguration

[SFC Configuration](../core/sfc-configuration) > [ProtocolAdapters](../core/sfc-configuration#ProtocolAdapters) > [Adapter](../core/protocol-adapter-configuration.md) 



MqttAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the MQTT Protocol adapter.

AdsAdapterConfiguration 

The MqttAdapterConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the MQTT  protocol adapter.

- [Schema](#MqttAdapterConfiguration-Schema)
- [Examples](#MqttAdapterConfiguration-Examples)

**Properties:**
- [Brokers](#Brokers)
- [ReadMode](#ReadMode)
- [ReceivedDataChannelSize](#ReceivedDataChannelSize)
- [ReceivedDataChannelTimeout](#ReceivedDataChannelTimeout)

---
### Brokers
Brokers configured for this adapter. The mqtt source using the adapter must refer to one of these servers with the AdapterBroker attribute.

**Type**: Map[String,[MqttBrokerConfiguration](#MqttBrokerConfiguration)]

---
### ReadMode
Read mode of the adapter. Set to "KeepAll" to collect all messages on subscribed topics during a read interval.
Set to "KeepLast", which is the default, to keep only the last received message.

**Type**: String


- "KeepLast" to collect last message received in read interval (Default)
- "KeepAll" to collect all messages received in read interval


---
### ReceivedDataChannelSize
Size of internal buffer to receive data for topic subscriptions

**Type**: Int

Default is 1000

---
### ReceivedDataChannelTimeout
Timeout in milliseconds to send data to internal buffer for received data for topic subscriptions

**Type**: Int

Default is 1000

[^top](#mqtt-protocol-configuration)

### MqttAdapterConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for MQTT adapter",
  "allOf": [
    {
      "$ref": "#/definitions/AdapterConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Brokers": {
          "type": "object",
          "description": "Map of MQTT broker configurations",
          "additionalProperties": {
            "$ref": "#/definitions/MqttBrokerConfiguration"
          },
          "minProperties": 1
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
        "ReceivedDataChannelSize": {
          "type": "integer",
          "description": "Size of the channel for received data",
          "default": 1000
        },
        "ReceivedDataChannelTimeout": {
          "type": "integer",
          "description": "Timeout for the received data channel in milliseconds",
          "default": 1000
        }
      },
      "required": [
        "Brokers"
      ]
    }
  ]
}
```

### MqttAdapterConfiguration Examples

Minimal configuration:

```json
{
  "AdapterType" : "MQTT",
  "Brokers": {
    "default-broker": {
      // MqttBrokerConfiguration properties here
    }
  }
}
```

Multiple brokers with KeepAll mode:

```json
{
  "AdapterType" : "MQTT",
  "Brokers": {
    "primary-broker": {
      // MqttBrokerConfiguration properties here
    },
    "backup-broker": {
      // MqttBrokerConfiguration properties here
    }
  },
  "ReadMode": "KeepAll",
  "ReceivedDataChannelSize": 5000,
  "ReceivedDataChannelTimeout": 10000
}
```





## MqttBrokerConfiguration

[MqttProtocolAdapter](#MqttAdapterConfiguration) > [Brokers](#Brokers) 



- [Schema](#MqttBrokerConfiguration-Schema)
- [Examples](#MqttBrokerConfiguration-Examples)

**Properties:**
- [Certificate](#Certificate)
- [ConnectionTimeout](#ConnectionTimeout)
- [EndPoint](#EndPoint)
- [Password](#Password)
- [Port](#Port)
- [PrivateKey](#PrivateKey)
- [RootCA](#RootCA)
- [SslServerCertificate](#SslServerCertificate)
- [Username](#Username)
- [VerifyHostName](#VerifyHostName)
- [WaitAfterConnectError](#WaitAfterConnectError)

---
### Certificate
Path to client certificate file. Used if broker used certificate authentication

**Type**: String

---
### ConnectionTimeout
Timeout for connecting to the broker in seconds

**Type**: Int

Default is 10 seconds

---
### EndPoint
Broker endpoint address

**Type**: String

Optionally with training port number (see Port)

If no scheme is specified in the address, then it will be added based on the Connection type.

("tcp://" for PlainText or "ssl://" for ServerSideTLS or MutualTLS)

---
### Password
Password if broker is using username and password authentication

**Type**: String

**Username and password should not be included as clear text in the configuration.** It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.

---
### Port
Port on MQTT broker

**Type**: Integer

Commonly port numbers are

- 1883 for PlaintText
- 8883 for ServerSideTLS
- 8884 for MutualTLS.
- 443 for AWS IoT Core endpoints

In no port number is specified then the EndPoint address is searched for a training port number.

---
### PrivateKey
Path to client private key file

**Type**: String

---
### RootCA
Path to root certificate file. The Root CA file in an MQTT client is used for server certificate verification when establishing a secure connection with the broker (using TLS/SSL)

**Type**: String

---
### SslServerCertificate
Path to server certificate file to verify the identity of the broker.

**Type**: String

If no certificate file is specified it is obtained from the server.
Used for connections of type ServerSideTLS and MutualTLS

---
### Username
Username if broker is using username and password authentication

**Type**: String

Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.

---

### VerifyHostName

Flag to enable verification of hostname

**Type:** Boolean

Default is true

---
### WaitAfterConnectError
Period in seconds to wait before trying to connect after a connection failure

**Type**: Int

Default is 60 seconds

[^top](#mqtt-protocol-configuration)

### MqttBrokerConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for MQTT broker connection",
  "properties": {
    "Certificate": {
      "type": "string",
      "description": "Client certificate file path"
    },
    "ConnectionTimeout": {
      "type": "integer",
      "description": "Connection timeout in seconds".
      "default" : 10
    },
    "EndPoint": {
      "type": "string",
      "description": "MQTT broker endpoint address"
    },
    "Password": {
      "type": "string",
      "description": "Password for authentication"
    },
    "Port": {
      "type": "integer",
      "description": "MQTT broker port number"
    },
    "PrivateKey": {
      "type": "string",
      "description": "Client private key file path"
    },
    "RootCA": {
      "type": "string",
      "description": "Root CA certificate file path"
    },
    "SslServerCertificate": {
      "type": "string",
      "description": "SSL server certificate file path"
    },
    "Username": {
      "type": "string",
      "description": "Username for authentication"
    },
    "VerifyHostName":{
      "type" : "boolean",
      "default" : true
    },
    
    "WaitAfterConnectError": {
      "type": "integer",
      "description": "Wait time in seconds after connection error",
      "defaul1": 10
    }
  },
  "required": [
    "EndPoint",
    "Port"
  ]
}
```

### MqttBrokerConfiguration Examples

Basic configuration with required fields only:

```json
{
  "EndPoint": "localhost",
  "Port": 1883
}
```



SSL/TLS with certificate-based authentication:

```json
{
  "EndPoint": "mqtt.example.com",
  "Port": 8883,
  "Certificate": "/path/to/client-cert.pem",
  "PrivateKey": "/path/to/private-key.pem",
  "RootCA": "/path/to/root-ca.pem",
  "VerifyHostName": true
}
```



Example 5 - AWS IoT Core configuration:

```json
{
  "EndPoint": "xxxxxxxxxxxxxxx-ats.iot.region.amazonaws.com",
  "Port": 8883,
  "Certificate": "/certs/device-certificate.pem.crt",
  "PrivateKey": "/certs/private.pem.key",
  "RootCA": "/certs/AmazonRootCA1.pem",
  "ConnectionTimeout": 15
}
```

