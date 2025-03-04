
# AWS SiteWise Edge Target

The AWS IoT [SiteWise Edge](https://aws.amazon.com/iot-sitewise/sitewise-edge/) target adapter for Shop Floor Connectivity (SFC) enables data transfer from industrial equipment to AWS IoT SiteWise Edge gateways running on-premises.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWS-SITEWISEEDGE-TARGET": {
      "JarFiles" : ["<location of deployment>/aws-sitewiseedge-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.awssitewiseedge.SiteWiseEdgeTargetWriter"
   }
}
```



## SiteWiseEdgeTargetConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

The `AWS-SITEWISEEDGE-TARGET` is a specific type of target configuration in SFC that allows you to connect and send data to an MQTT topic consumed by the AWS IoT SiteWise Edge service. The `Targets` configuration element can contain entries of this type, and the `TargetType` of these entries must be set to `"AWS-SITEWISEEDGE-TARGET"`.
This type extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for this adapter.
This target adapter follows the Time Quality Value (TQV) schema for ingesting data into SiteWise Edge. For a better understanding of the TQV schema, please refer to the [Ingest data using the AWS IoT SiteWise API](https://docs.aws.amazon.com/iot-sitewise/latest/userguide/ingest-api.html) documentation.

- [Schema](#sitewiseedgetargetconfiguration-schema)
- [Examples](#sitewiseedgetargetconfiguration-examples)


**Properties:**
- [BatchCount](#batchcount)
- [BatchInterval](#batchinterval)
- [BatchSize](#batchsize)
- [Certificate](#certificate)
- [ClientName](#clientname)
- [ConnectRetries](#connectretries)
- [Connection](#connection)
- [ConnectionTimeout](#connectiontimeout)
- [EndPoint](#endpoint)
- [Password](#password)
- [Port](#port)
- [PrivateKey](#privatekey)
- [PublishTimeout](#publishtimeout)
- [RootCA](#rootca)
- [SslServerCertificate](#sslservercertificate)
- [TopicName](#topicname)
- [Username](#username)
- [VerifyHostname](#verifyhostname)
- [WaitAfterConnectError](#waitafterconnecterror)

---
### BatchCount
Number of TQV messages to buffer per channel before sending data as a batch to the SiteWise Edge MQTT broker.

**Type**: Int

Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.

---
### BatchInterval
Interval in milliseconds after which all messages are sent to the SiteWise Edge MQTT Broker, even when the BatchSize or BatchCount limit is not reached.

**Type**: Int

Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.


---
### BatchSize
Channel TQV Payload size in KB of messages to batch before sending data as a batch to the SiteWise Edge MQTT broker.

**Type**: Int

Batching is enabled by setting a value for one or more of BatchSize, BatchCount and BatchInterval.
Whenever the number of messages, total message size or an interval is reached the buffered data is sent as an array of messages to the topic.
The size is calculated on the uncompressed payload of the messages.

---
### Certificate
Path to client certificate file. Used if broker used certificate authentication

**Type**: String

---
### ClientName
Client name to provide when connecting to the SiteWise Edge MQTT broker. When running on Greengrass core, this should be the name of the IoT Thing which is providing the certificates.

**Type**: String

Length Constraints: Minimum length of 1. Maximum length of 128.

Pattern: [a-zA-Z0-9:_-]+

---
### ConnectRetries
Number of retries to connect to MQTT broker

**Type**: Int

Default is 10

---
### Connection
Connection type

**Type**: String

- "PlainText" (Default)
- "ServerSideTLS"
- "MutualTLS"

---
### ConnectionTimeout
Timeout for connecting to the broker in seconds

**Type**: Int

Default is 10 seconds

---
### EndPoint
SiteWise Edge MQTT broker endpoint address

**Type**: String

Optionally with a port number (see Port)

If no scheme is specified in the address, then it will be added based on the Connection type.
("tcp://" for PlainText or "ssl://" for ServerSideTLS or MutualTLS)

To get the ATS endpoint for an account use the AWS CLI command

```console 
iot describe-endpoint --endpoint-type iot:Data-ATS
```

https://awscli.amazonaws.com/v2/documentation/api/latest/reference/iot/describe-endpoint.html


---
### Password
Password if broker is using username and password authentication

**Type**: String

Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the [AWS secrets manager](../core/secrets-manager-configuration.md).

---
### Port
SiteWise Edge MQTT broker port

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
Name of the MQTT topic to which SiteWise Edge will subscribe for ingesting data. You may use a combination of %source%, %target%, and %channel% variables.

**Type**: String

Default: %channel%

---
### Username
Username if broker is using username and password authentication

**Type**: String

Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the [AWS secrets manager](../core/secrets-manager-configuration.md).

---
### VerifyHostname
Verify the server hostname from the provided certificates. Set this to `false` when connecting to SiteWise Edge running on Greengrass. IoT self-signed certificates do not provide the hostname.

**Type**: Boolean

---
### WaitAfterConnectError
Period in seconds to wait before trying to connect after a connection failure

**Type**: Int

Default is 60 seconds

### SiteWiseEdgeTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "SiteWiseEdgeTargetConfiguration",
  "type": "object",
  "allOf": [
    {
      "$ref": "#/definitions/TargetConfiguration"
    },
    {
      "$ref": "#/definitions/AwsServiceConfig"
    },
    {
      "type": "object",
      "properties": {
        "BatchCount": {
          "type": "integer",
          "description": "Number of messages to batch"
        },
        "BatchInterval": {
          "type": "integer",
          "description": "Interval between batch processing in milliseconds"
        },
        "BatchSize": {
          "type": "integer",
          "description": "Size of the batch in bytes"
        },
        "Certificate": {
          "type": "string",
          "description": "Client certificate for authentication"
        },
        "ClientName": {
          "type": "string",
          "description": "Name of the client"
        },
        "ConnectRetries": {
          "type": "integer",
          "description": "Number of connection retry attempts"
        },
        "Connection": {
          "type": "string",
          "description": "Connection string"
        },
        "ConnectionTimeout": {
          "type": "integer",
          "description": "Connection timeout in milliseconds"
        },
        "EndPoint": {
          "type": "string",
          "description": "Endpoint URL"
        },
        "Password": {
          "type": "string",
          "description": "Password for authentication"
        },
        "Port": {
          "type": "integer",
          "description": "Port number"
        },
        "PrivateKey": {
          "type": "string",
          "description": "Private key for authentication"
        },
        "PublishTimeout": {
          "type": "integer",
          "description": "Timeout for publish operations in milliseconds"
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
          "description": "Name of the topic"
        },
        "Username": {
          "type": "string",
          "description": "Username for authentication"
        },
        "VerifyHostname": {
          "type": "boolean",
          "description": "Whether to verify hostname in SSL certificate"
        },
        "WaitAfterConnectError": {
          "type": "integer",
          "description": "Wait time after connection error in milliseconds"
        }
      }
    }
  ]
}

```

### SiteWiseEdgeTargetConfiguration Examples

```json
{
  "Active": true,
  "TargetType": "AWS-SITEWISEEDGE-TARGET",
  "TopicName": "%channel%",
  "ClientName": "${CLIENT_ID}",
  "EndPoint": "ssl://${GATEWAY_HOSTNAME}",
  "Port": 8883,
  "Connection": "ServerSideTLS",
  "RootCA": "${GATEWAY_CA_FILE}",
  "Certificate": "${CLIENT_CERTIFICATE_FILE}",
  "PrivateKey": "${CLIENT_KEY_FILE}",
  "VerifyHostname": false,
  "BatchSize": 1000,
  "BatchInterval": 5000,
  "BatchCount": 10
}
```

[^top](#aws-sitewise-edge-target)

