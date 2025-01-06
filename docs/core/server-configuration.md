## ServerConfiguration


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
- [ServerResultsChannelSize](#ServerResultsChannelSize)

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
### ServerResultsChannelSize
Timeout in milliseconds to send data to internal results buffer

**Type**: Int

Default is  10000

[^top](#ServerConfiguration)

