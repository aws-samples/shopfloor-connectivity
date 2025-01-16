[SFC Configuration](./sfc-top-level-config.md) > [TargetServers](./sfc-top-level-config.md#TargetServers) > [TargetServer](./server-configuration.md) 

[SFC Configuration](./sfc-top-level-config.md) > [AdapterServers](./sfc-top-level-config.md#ProtocolAdapterServers) > [AdapterServer](./server-configuration.md) 

[SFC Configuration](./sfc-top-level-config.md#Metrics) > [Metrics](./sfc-top-level-config.md#Metrics) > [Writer](./metrics-writer-configuration#MetricsWriter) > [MetricsServer](./metrics-writer-configuration.md#MetricsServer)

## ServerConfiguration

- [Schema](#Schema)
- [Examples](#Examples)


**Properties:**
- [Address](#Address)
- [CaCertificate](#CaCertificate)
- [ClientCertificate](#ClientCertificate)
- [ClientPrivateKey](#ClientPrivateKey)
- [Compression](#Compression)
- [ConnectionType](#ConnectionType)
- [ExpirationWarningPeriod](#ExpirationWarningPeriod)
- [HealthProbe](#HealthProbe)
- [Port](#Port)
- [ServerResultsChannelSize](#ServerResultsChannelSize)
- [ServerResultsChannelTimeout](#ServerResultsChannelTimeout)

---
### Address
IP address or host name

**Type**: String

The default address is "localhost". If this address is used the IP4 address is resolved to use the local IP address.

---
### CaCertificate
The pathname of the file containing the CA certificate used by the client to encrypt network traffic. This parameter only needs to be set when the communication type is MutualTLS.

**Type**: String

Only required when MutualTLS is used to secure network traffic between SFC core and protocols adapter or target services

---
### ClientCertificate
The pathname of the file containing the certificate used by the client to encrypt network traffic. This parameter only needs to be set when the communication type is MutualTLS.

**Type**: String

Only required when MutualTLS is used to secure network traffic between SFC core and protocols adapter or target services

---
### ClientPrivateKey
The pathname of the file containing the private key used by the client to encrypt network traffic. This parameter only needs to be set when the communication type is MutualTLS.

**Type**: String

Only required when MutualTLS is used to secure network traffic between SFC core and protocols adapter or target services

---
### Compression
Enable or disable compression of data exchanged between services. Use this option to reduce the volume of the data exchanged between the services at the cost of CPU load to compress and decompress the data.

**Type**: Boolean

Default is false

---
### ConnectionType
Connection (security) type

- PlainText : No encryption of network traffic between SFC core and protocol adapter or target server
- ServerSideTLS: Encryption of network traffic between SFC core and protocol adapter or target server. Server provides its certificate to client. Requires servers to be started with parameters
  `-connection` set to ServerSideTLS and `-key` and `-cert` parameters set to the server's private key and certificate files.
- MutualTLS : Encryption of network traffic between SFC core and protocol adapter or target server. Server and client provide certificate to each other.
  Requires servers to be started with parameters `-connection` set to MutualTLS, `-key` and `-cert` parameters set to the server's private key and certificate files and the `-ca` parameter set to the ca certificate file. For MutualTLS the client must configure the ClientCertificate, ClientPrivateKey and CaCertificate which are used for the connection with the server.

**Type**: String

Default is "PlainText"

---
### ExpirationWarningPeriod
Period in days in which SFC will generate a daily warning and metrics value before a used certificate expires.

**Type**: Integer

Default is 30, set to 0 to disable.

---
### HealthProbe
Configures the health probe endpoint when the address is used for an SFC service process.

**Type**: [HealthProbeConfiguration](./health-probe-configuration.md)



---
### Port
Port number

**Type**: Integer

---
### ServerResultsChannelSize
Size of internal buffer used by IPC servers to send results to the SFC core

**Type**: Int

Default is 1000

---
### ServerResultsChannelTimeout
Timeout in milliseconds to send data to internal results buffer

**Type**: Int

Default is  10000

[^top](#ServerConfiguration)



## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "Address": {
      "type": "string",
      "pattern": "^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(\\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*$|^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
      "description": "Server hostname or IP address"
    },
    "CaCertificate": {
      "type": "string",
      "description": "CA certificate for mutual TLS"
    },
    "ClientCertificate": {
      "type": "string",
      "description": "Client certificate for mutual TLS"
    },
    "ClientPrivateKey": {
      "type": "string",
      "description": "Client private key for TLS connections"
    },
    "Compression": {
      "type": "boolean",
      "default": false,
      "description": "Enable/disable compression"
    },
    "ConnectionType": {
      "type": "string",
      "enum": ["PlainText", "ServerSideTLS", "MutualTLS"],
      "default": "PlainText",
      "description": "Type of connection security"
    },
    "ExpirationWarningPeriod": {
      "type": "integer",
      "minimum": 0,
      "default": 30,
      "description": "Certificate expiration warning period in seconds (0 to disable)"
    },
    "HealthProbe": {
      "$ref": "#/definitions/HealthProbeConfiguration",
      "description": "Health probe configuration"
    },
    "Port": {
      "type": "integer",
      "minimum": 1,
      "maximum": 65535,
      "description": "Server port number"
    },
    "ServerResultsChannelSize": {
      "type": "integer",
      "minimum": 1,
      "default": 1000,
      "description": "Size of the server results channel"
    },
    "ServerResultsChannelTimeout": {
      "type": "integer",
      "minimum": 1,
      "default": 1000,
      "description": "Timeout for server results channel in milliseconds"
    }
  },
  "required": ["Address", "Port"],
  "allOf": [
    {
      "if": {
        "properties": {
          "ConnectionType": { "const": "MutualTLS" }
        }
      },
      "then": {
        "required": ["CaCertificate", "ClientCertificate"]
      }
    }
  ],
  "additionalProperties": false
}
```



## Examples

Basic PlainText configuration with compression enabled:

```json
{
  "Address": "localhost",
  "Port": 8080,
   "Compression": true,
}
```



Server with TLS:

```json
{
  "Address": "server.example.com",
  "Port": 443,
  "ConnectionType": "ServerSideTLS",
  "Compression": true,
  "ServerResultsChannelTimeout": 2000
}
```



MutualTLS configuration:

```json
{
  "Address": "192.168.1.100",
  "Port": 8443,
  "ConnectionType": "MutualTLS",
  "CaCertificate": "/path/to/ca.crt",
  "ClientCertificate": "/path/to/client.crt",
  "ClientPrivateKey": "/path/to/client.key",
  "ExpirationWarningPeriod": 30,
  "Compression": true
}
```



Configuration with health probe:

```json
{
  "Address":  "192.168.1.100",
  "Port": 9000,
  "ConnectionType": "PlainText",
  "HealthProbe": {
      "Port": 8080,
      "Path": "/health",
    }
}
```

