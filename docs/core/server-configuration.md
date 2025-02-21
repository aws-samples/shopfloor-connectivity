## ServerConfiguration

[SFC Configuration](./sfc-configuration.md) > [TargetServers](./sfc-configuration.md#targetservers) > [TargetServer](./server-configuration.md) 

[SFC Configuration](./sfc-configuration.md) > [AdapterServers](./sfc-configuration.md#adapterservers) > [AdapterServer](./server-configuration.md) 

[SFC Configuration](./sfc-configuration.md) > [Metrics](./sfc-configuration.md#metrics) > [Writer](./metrics-writer-configuration.md#metricswriter) > [MetricsServer](./metrics-writer-configuration.md#metricsserver)

Defines network connection settings for SFC servers including target, adapter, and metrics servers. Specifies essential parameters like address, port, security options (PlainText/TLS), and performance settings. Supports flexible configuration of connection security through different TLS modes and certificate management. Includes health monitoring capabilities for service availability tracking.

- [Schema](#schema)
- [Examples](#examples)


**Properties:**
- [Address](#address)
- [CaCertificate](#cacertificate)
- [ClientCertificate](#clientcertificate)
- [ClientPrivateKey](#clientprivatekey)
- [Compression](#compression)
- [ConnectionType](#connectiontype)
- [ExpirationWarningPeriod](#expirationwarningperiod)
- [HealthProbe](#healthprobe)
- [Port](#port)
- [ServerResultsChannelSize](#serverresultschannelsize)
- [ServerResultsChannelTimeout](#serverresultschanneltimeout)

---
### Address
Specifies the network address where the server can be reached, either as an IP address (e.g., "192.168.1.100") or hostname (e.g., "server.example.com"). When set to the default value "localhost", the system automatically resolves to the machine's local IP address for proper network connectivity. 

**Type**: String

---
### CaCertificate
Specifies the file path to the Certificate Authority (CA) certificate that the client uses to verify the server's identity in MutualTLS connections. This certificate is essential for establishing trust in MutualTLS mode, where both client and server authenticate each other. Only required when ConnectionType is set to "MutualTLS".

**Type**: String

---
### ClientCertificate
Specifies the file path to the client's certificate used to identify itself to the server in MutualTLS connections. This certificate proves the client's identity to the server and is required only when ConnectionType is set to "MutualTLS". The certificate must be signed by the CA trusted by the server.

**Type**: String

---
### ClientPrivateKey
Specifies the file path to the client's private key that pairs with the ClientCertificate for MutualTLS authentication. This private key is used to establish secure connections and must be kept secure. Only required when ConnectionType is set to "MutualTLS" and must correspond to the provided ClientCertificate.

**Type**: String

---
### Compression
Controls data compression for network traffic between services. When enabled (true), it reduces data volume, potentially improving network performance, especially for bandwidth-constrained connections. However, it increases CPU usage for compression/decompression. Disabled by default (false) to prioritize CPU efficiency over bandwidth savings.

**Type**: Boolean

Default is false

---
### ConnectionType
Defines the security level for network communications between SFC components.

- PlainText : No encryption of network traffic between SFC core and protocol adapter or target server
- ServerSideTLS: Encryption of network traffic between SFC core and protocol adapter or target server. Server provides its certificate to client. Requires servers to be started with parameters
  `-connection` set to ServerSideTLS and `-key` and `-cert` parameters set to the server's private key and certificate files.
- MutualTLS : Encryption of network traffic between SFC core and protocol adapter or target server. Server and client provide certificate to each other.
  Requires servers to be started with parameters `-connection` set to MutualTLS, `-key` and `-cert` parameters set to the server's private key and certificate files and the `-ca` parameter set to the ca certificate file. For MutualTLS the client must configure the ClientCertificate, ClientPrivateKey and CaCertificate which are used for the connection with the server.

**Type**: String

Default is "PlainText"

---
### ExpirationWarningPeriod
Defines the advance notification period (in days) for certificate expiration warnings. By default, generates daily warnings and metrics starting 30 days before any certificate expires. Can be disabled by setting to 0. Helps prevent unexpected service disruptions due to expired certificates.

**Type**: Integer

Default is 30, set to 0 to disable.

---
### HealthProbe
Configures health monitoring settings for the server endpoint, allowing the system to track service availability and health status. Uses HealthProbeConfiguration to define how health checks are performed, including check frequency, thresholds, and response criteria for determining service health state.

**Type**: [HealthProbeConfiguration](./health-probe-configuration.md)



---
### Port
Specifies the network port number where the server listens for incoming connections. This integer value identifies the specific communication endpoint on the server, allowing clients to establish connections to the service. Must be within valid port range (0-65535).

**Type**: Integer

---
### ServerResultsChannelSize
Defines the size of the internal buffer used for Inter-Process Communication (IPC) when sending results from protocol adapters or target servers back to the SFC core. The default value of 1000 determines how many results can be queued before backpressure is applied. Increasing this value allows for more results to be buffered but consumes more memory.

**Type**: Int

Default is 1000

---
### ServerResultsChannelTimeout
Specifies the maximum time (in milliseconds) allowed for sending data to the internal results buffer. If the buffer cannot accept new data within this timeout period (default 10000ms or 10 seconds), the operation will fail. This timeout prevents indefinite blocking when the buffer is full and helps manage backpressure scenarios.

**Type**: Int

Default is  10000

[^top](#serverconfiguration)



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

