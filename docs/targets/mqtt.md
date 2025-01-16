# MQTT Target

[SFC Configuration](../core/sfc-top-level-config.md) > [Targets](../core/sfc-top-level-config.md#Targets) >  [Target](../core/target-configuration.md) 



## MqttTargetConfiguration

MqttTargetConfiguration extends the type TargetConfiguration with specific configuration data for connecting to and sending to MQTT topic. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to "MQTT-TARGET"

- [Schema](#MqttTargetConfiguration-Schema)
- [Examples](#MqttTargetConfiguration-Examples)

**Properties:**
- [AlternateTopicName](#AlternateTopicName)
- [BatchCount](#BatchCount)
- [BatchInterval](#BatchInterval)
- [BatchSize](#BatchSize)
- [Certificate](#Certificate)
- [Compression](#Compression)
- [ConnectRetries](#ConnectRetries)
- [Connection](#Connection)
- [ConnectionTimeout](#ConnectionTimeout)
- [EndPoint](#EndPoint)
- [MaxPayloadSize](#MaxPayloadSize)
- [Password](#Password)
- [Port](#Port)
- [PrivateKey](#PrivateKey)
- [PublishTimeout](#PublishTimeout)
- [QoS](#QoS)
- [Retain](#Retain)
- [RootCA](#RootCA)
- [SslServerCertificate](#SslServerCertificate)
- [TopicName](#TopicName)
- [Username](#Username)
- [WaitAfterConnectError](#WaitAfterConnectError)
- [WarnAlternateTopicName](#WarnAlternateTopicName)

---
### AlternateTopicName
Name or name template of the topic values are published in case there are unmapped template placeholders in the TopicName

**Type**: String

---
### BatchCount
Number of messages to buffer before sending data as a batch to a topic.

**Type**: Int

Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.

---
### BatchInterval
Interval in milliseconds after which a batch of messages is sent to the topic, even when the BatchSize or BatchCount limit is not reached.

**Type**: Int

Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.

---
### BatchSize
Payload size in KB of messages to batch before sending data as a batch to a topic.

**Type**: Int

Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.
The size is calculated on the uncompressed payload of the messages.

---
### Certificate
Path to client certificate file. Used if broker used certificate authentication

**Type**: String

---
### Compression
Compression method for MQTT message payloads.

**Values:**
- "None" (Default)
- "Zip"
- "GZip"

**Type**: String

---
### ConnectRetries
Number of retries to connect to MQTT broker

**Type**: Int

Default is 10

---
### Connection
Connection type

**Values:**
- "PlainText" (Default)
- "ServerSideTLS"
- "MutualTLS"

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
To get the ATS endpoint for an account use the AWS CLI command
aws iot describe-endpoint --endpoint-type iot:Data-ATS
https://awscli.amazonaws.com/v2/documentation/api/latest/reference/iot/describe-endpoint.html


---
### MaxPayloadSize
Max payload size in KB for MQTT messages.

**Type**: Int

Note if compression is enabled the payload size of a single target data message, or a batch of messages can be larger, than this value. When batching of messages is enabled, without compression a batch
of messages will be sent to the topic when this size is reached.


---
### Password
Password if broker is using username and password authentication

**Type**: String

Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.

---
### Port
Port on MQTT broker

**Type**: Integer

Commonly port numbers are

- 1883 for PlainText
- 8883 for ServerSideTLS
- 8884 for MutualTLS.
- 443 for AWS IoT Core endpoints

In no port number is specified then the EndPoint address is searched for a training port number.

---
### PrivateKey
Path to client private key file

**Type**: String

---
### PublishTimeout
Timeout in seconds for publishing

**Type**: Long

Default is 10 seconds

---
### QoS
Quality of service

**Type**: Integer

Default is 0

0 = At most once
1 = At least once
2 = Exactly once

---
### Retain
Set to true to store a single message per a given MQTT topic for delivery to any current and future topic subscribers.

**Type**: Boolean

Default is false

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
### TopicName
Name or name template of the topic

**Type**: String


A template can be used for the topicName to render the actual topic name using placeholders. In this template, 
besides placeholders for environment variables (${name}) the following placeholders are available:

- %schedule%
- %target%
- %source%
- %channel%

To use the values of **metadata** at the top, source or channel level of the target data, the name af the metadata value can 
be used with a '%' prefix and postfix.

Value placeholders can be used to add additional topic levels or grouping values to a specific topic.

Template examples:

- plant1-**%source%** : Values from each source will be published to a topic for that source
- plant1-**%line%**   : Values from all sources will be grouped by the value of the %line% metadata and published to a topic for that value

In case a placeholder is not resolved, when a value for a used placeholder is part of the data,
then an alternative topic name can be configured by setting the name of that topic to the **"AlternateTopiName"** setting.

Note that the use of placeholders to send data to specific topics will result in additional publish calls to the broker.



---
### Username
Username if broker is using username and password authentication

**Type**: String

Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.

---
### WaitAfterConnectError
Period in seconds to wait before trying to connect after a connection failure

**Type**: Int

Default is 60 seconds

---
### WarnAlternateTopicName
Generate warning if data is published to AlternateTopicName

**Type**: Boolean


Default is tue

### MqttTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "MqttTargetConfiguration",
  "type": "object",
  "allOf": [
    {
      "$ref": "#/definitions/TargetConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AlternateTopicName": {
          "type": "string",
          "description": "Alternate topic name when dynamic propery has unresolved placeholders
          "
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
        "Certificate": {
          "type": "string",
          "description": "Client certificate for TLS authentication"
        },
        "Compression": {
          "type": "string",
          "description": "Type of message compression",
          "enum": ["None", "Zip", "GZip"],
          "default": "None"
        },
        "ConnectRetries": {
          "type": "integer",
          "description": "Number of connection retry attempts"
        },
        "Connection": {
          "type": "string",
          "description": "Type of connection security",
          "enum": ["PlainText", "ServerSideTLS", "MutualTLS"],
          "default": "PlainText"
        },
        "ConnectionTimeout": {
          "type": "integer",
          "description": "Connection timeout in milliseconds"
        },
        "EndPoint": {
          "type": "string",
          "description": "MQTT broker endpoint"
        },
        "MaxPayloadSize": {
          "type": "integer",
          "description": "Maximum size of message payload in bytes"
        },
        "Password": {
          "type": "string",
          "description": "Password for authentication"
        },
        "Port": {
          "type": "integer",
          "description": "Port number for MQTT connection"
        },
        "PrivateKey": {
          "type": "string",
          "description": "Private key for TLS authentication"
        },
        "PublishTimeout": {
          "type": "integer",
          "description": "Timeout for publish operations in milliseconds"
        },
        "QoS": {
          "type": "integer",
          "description": "Quality of Service level",
          "enum": [0, 1, 2],
          "default": 0
        },
        "Retain": {
          "type": "boolean",
          "description": "Whether messages should be retained by broker"
        },
        "RootCA": {
          "type": "string",
          "description": "Root CA certificate"
        },
        "SslServerCertificate": {
          "type": "string",
          "description": "SSL server certificate"
        },
        "TopicName": {
          "type": "string",
          "description": "Primary topic name"
        },
        "Username": {
          "type": "string",
          "description": "Username for authentication"
        },
        "WaitAfterConnectError": {
          "type": "integer",
          "description": "Wait time in milliseconds after connection error"
        },
        "WarnAlternateTopicName": {
          "type": "boolean",
          "description": "Whether to warn when using alternate topic"
        }
      },
      "required": ["EndPoint", "TopicName"]
    }
  ]
}

```

### MqttTargetConfiguration Examples

MQTT Configuration using (secret) placeholders for username and password

```json
{
  "TargetType" : "MQTT-TARGET",
  "EndPoint": "mqtt.example.com",
  "Port": 1883,
  "TopicName": "sensors/data",
  "QoS": 1,
  "Username": "${username}",
  "Password": "${password}",
  "ConnectionTimeout": 30000
}
```

Secure MQTT with TLS:

```json
{
  "TargetType" : "MQTT-TARGET"
  "EndPoint": "secure-mqtt.example.com",
  "Port": 8883,
  "TopicName": "production/metrics",
  "Connection": "MutualTLS",
  "Certificate": "/path/to/client-cert.pem",
  "PrivateKey": "/path/to/private-key.pem",
  "RootCA": "/path/to/root-ca.pem",
  "QoS": 2,
  "Compression": "GZip",
  "BatchSize": 1024,
  "BatchInterval": 5000
}
```

Configuration  with dynamic topic names from target- and metadata values

```json
{
  "TargetType" : "MQTT-TARGET"
  "EndPoint": "mqtt.internal.com",
  "Port": 1883,
  "TopicName": "data/%location%/%line%/%source%",
  "AlternateTopicName": "data/sensors",
  "WarnAlternateTopicName": true
}
```



[^top](#mqtt-target)

