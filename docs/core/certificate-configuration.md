# CertificateConfiguration

The CertificateConfiguration class manages SSL/TLS certificate settings and validation. It handles certificate paths, private keys, and certificate formats (PEM or PKCS12). The class supports both standard certificates and self-signed certificates, with configuration options for certificate aliases, passwords, and expiration warning periods. It includes validation logic to ensure proper certificate format and required key files are specified. 

- [Schema](#certificateconfiguration-schema)

- [Examples](#certificateconfiguration-examples)

  **Properties:**

- [Alias](#alias)

- [CertificateFile](#certificatefile)

- [ExpirationWarningPeriod](#expirationwarningperiod)

- [Format](#format)

- [Password](#password)

- [PrivateKeyFile](#privatekeyfile)

- [SelfSignedCertificate](#selfsignedcertificate)

---
### Alias
The Alias property specifies the name used to identify the certificate entry within a PKCS12 format certificate file. It defaults to "alias" if not explicitly set. This alias is used when accessing the certificate and private key entries stored in the PKCS12 keystore.

**Type**: String

Default is "alias"

---
### CertificateFile
The CertificateFile property specifies the file system path to the certificate file, which can be in either PEM or PKCS12 format. This path indicates where the certificate is stored on the system and will be used to load the certificate for SSL/TLS operations.

**Type**: String

---
### ExpirationWarningPeriod
The ExpirationWarningPeriod property defines the number of days before a certificate's expiration when the adapter should start generating daily warnings and metrics. By default, it's set to 30 days, giving administrators time to take action before the certificate expires. Setting this value to 0 will disable the expiration warnings. This helps prevent unexpected certificate expiration issues by providing advance notification.

**Type**: Integer

Default is 30, set to 0 to disable.

---
### Format
The Format property specifies the encoding format of the certificate file, accepting either "Pem" or "Pkcs12" as valid values. If this property is not explicitly set, the adapter will try to automatically determine the format based on the certificate file's extension. PEM files typically use extensions like .pem, while PKCS12 files commonly use extension  .pfx.

**Type**: String

If not specified the adapter will attempt to determine the type from the filename of the key file.

---
### Password
The Password property specifies the password required to access and decrypt a PKCS12 format certificate file. This password is used to protect the private key and certificate information stored within the PKCS12 keystore.

**Type**: String

---
### PrivateKeyFile
The PrivateKeyFile property specifies the file system path to the private key file. For PEM format certificates, this property is required as the private key is stored in a separate file. For PKCS12 format certificates, this property is optional since the private key is typically stored within the PKCS12 file itself along with the certificate.

**Type**: String

---
### SelfSignedCertificate
The SelfSignedCertificate property contains configuration settings for generating a self-signed certificate. If this property is configured and the specified certificate file doesn't exist, the adapter will automatically create a new self-signed certificate using these settings. This property accepts a SelfSignedCertificateConfiguration object that defines parameters like the certificate's subject, validity period, and other attributes needed for certificate generation.

**Type**: [SelfSignedCertificateConfiguration](./self-signed-certificate-configuration.md)

[^top](#certificateconfiguration)


### CertificateConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "Alias": {
      "type": "string",
      "description": "Alias name for the certificate"
    },
    "CertificateFile": {
      "type": "string",
      "description": "Path to the certificate file"
    },
    "ExpirationWarningPeriod": {
      "type": "integer",
      "description": "Number of days before certificate expiration to start warning",
      "default": 30
    },
    "Format": {
      "type": "string",
      "description": "Format of the certificate",
      "enum": ["pem", "pfx"]
    },
    "Password": {
      "type": "string",
      "description": "Password for the certificate private key"
    },
    "PrivateKeyFile": {
      "type": "string",
      "description": "Path to the private key file"
    },
    "SelfSignedCertificate": {
      "$ref": "#/definitions/SelfSignedCertificateConfiguration",
      "description": "Configuration for self-signed certificate generation"
    }
  },
  "oneOf": [
    {
      "required": ["CertificateFile"]
    },
    {
      "required": ["SelfSignedCertificate"]
    }
  ]
}

```

### CertificateConfiguration Examples

Basic configuration with existing certificate:

```json
{
  "CertificateFile": "/certs/server.crt",
  "PrivateKeyFile": "/certs/server.key",
  "Format": "pem,",
  "ExpirationWarningPeriod": 30
}
```

Configuration with password-protected private key:

```json
{
  "CertificateFile": "C:\\Certificates\\client.pfx",
  "Password": "${CERT_PASSWORD}",
  "Format": "pfx",
  "Alias": "client-cert",
  "ExpirationWarningPeriod": 14
}
```



Example 3 - Self-signed certificate configuration:

```json
{
  "CertificateFile": "/certs/server.crt",
  "PrivateKeyFile": "/certs/server.key",
  "SelfSignedCertificate": {
    "CommonName": "example.com",
    "Organization": "Example Corp",
    "ValidityPeriod": 365
  },
  "ExpirationWarningPeriod": 60
}
```


[^top](#certificateconfiguration)





