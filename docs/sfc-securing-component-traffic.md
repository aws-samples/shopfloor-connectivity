# Securing Network Traffic between SFC components

- [PlainText](#plaintext)
- [ServerSideTLS](#serversidetls)
- [MutualTLS](#mutualtls)
- [Generating keys and certificates](#generating-keys-and-certificates)

All network traffic between SFC components can be secured using encryption. The following options can be used

## PlainText

The network traffic between SFC components is not encrypted.

## ServerSideTLS

The network traffic is encrypted using the private key of the service, the service is providing its X.509 server
certificate to the client to decrypt the traffic. The service process needs to be started using the -key and -cert
parameters specifying the files containing servers private key and server certificate. The -connection type parameter
must be set to ServerSideTLS. In the SFC configuration the [ConnectionType](../docs/core/server-configuration.md#connectiontype) in the [ServiceConfiguration](../docs/core/server-configuration.md) for the server
must be set to ServerSideTLS.

The value used for the connection type parameter used for the service and the configured ConnectionType must match.

Note that the address which is configured to communicate with the service must be present as DNS name or IP address as
one of the Alternative Subject Names in the server certificate.

## MutualTLS

The network traffic is encrypted using the private key of the service and the private key of the client, the service and
service provide their X.509 certificates to each other to decrypt the traffic. The service process needs to be started
using the -key, -cert and -ca parameters specifying the files containing servers private key and server and CA
certificates. The -connection type parameter must be set to MutualTLS. In the SFC configuration the ConnectionType in
the ServiceConfiguration for the server must be set to MutualTLS. The ClientPrivateKey, ClientCertificate and
CaCertificate must be set to the files containing the clients private key, client certificate and CA certificate.

The value used for the connection type parameter used for the service and the configured ConnectionType must match.

The address which is configured to communicate with the service must be present as DNS name or IP address as one of the
Alternative Subject Names in the server certificate.

The address of the client must be present as DNS name or IP address as one of the Alternative Subject Names in the
client certificate.

## Generating keys and certificates

The script below can be used to create the required keys and certificates to which can be used for ServerSideTLS and
MutualTLS connections in test environments

*NOTE*:

- The script is provided to generate self-signed certificates for test purposed only and should not be used in
  production environments.
- For convenience the script includes the IP addresses of all available network interfaces as IP addresses, and the
  hostname (plus localhost) of the system on which the script is executed, in the as IP addresses of the sand DNS names
  as alternative subject names of the generated certificates. This assumes a test setup where both the SFC core and
  service are executed on the same system. When the SFC core and SFC services run on different systems the script must
  be executed on both of the systems and the relevant certificates must be used on that system as key and certificate
  parameters for the server, or configuration values used by the SFC core.
- In production environments the IP addresses and DNS names should be included in the certificate to the expected client
  and service addresses for that environment.

```sh
rrm *.pem
rm *.srl
rm *.cnf

C="NL"
ST="NH"
L="AMS"
O="MYORG"
OU="MYOU"

for i in $(ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p')
do
 IP_LIST+="IP:$i,"
done
IP_LIST+="IP:127.0.0.1,"
IP_LIST+="IP:0.0.0.0"

HOST=$(hostname -s)
DNS_NAMES="DNS:$HOST,DNS:localhost"
CN="/C=$C/ST=$ST/L=$L/O=$O/OU=$OU/CN=$HOST"

# CA
# Private key and self-signed certificate
openssl req -x509 -newkey rsa:4096 -days 365 -nodes -keyout ca-key.pem -out ca-cert.pem -subj "$CN"-CA""

echo "CA's self-signed certificate"
openssl x509 -in ca-cert.pem -noout -text



# SERVER
# Private key and certificate signing request
openssl req -newkey rsa:4096 -nodes -keyout server-key.pem -out server-req.pem -subj "$CN"-SERVER""

echo "subjectAltName=$DNS_NAMES,$IP_LIST" > server-ext.cnf

# Create certificate
openssl x509 -req -in server-req.pem -days 365 -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out server-cert.pem -extfile server-ext.cnf

echo "Server's signed certificate"
openssl x509 -in server-cert.pem -noout -text



# CLIENT
# Private key and certificate signing request
openssl req -newkey rsa:4096 -nodes -keyout client-key.pem -out client-req.pem -subj "$CN"-CLIENT""

echo "subjectAltName=$DNS_NAMES,$IP_LIST" > client-ext.cnf

# Create certificate
openssl x509 -req -in client-req.pem -days 365 -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out client-cert.pem -extfile client-ext.cnf

echo "Client's signed certificate"
openssl x509 -in client-cert.pem -noout -text

```