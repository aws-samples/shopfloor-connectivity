# CertificateConfiguration

- [Schema](#CertificateConfiguration-Schema)

- [Examples](#CertificateConfiguration-Examples)

- 

  **Properties:**

- [Alias](#Alias)

- [CertificateFile](#CertificateFile)

- [ExpirationWarningPeriod](#ExpirationWarningPeriod)

- [Format](#Format)

- [Password](#Password)

- [PrivateKeyFile](#PrivateKeyFile)

- [SelfSignedCertificate](#SelfSignedCertificate)

---
### Alias
Alias to use for pkcs12 certificate files

**Type**: String

Default is "alias"

---
### CertificateFile
Pathname to pem or pkcs12 certificate file

**Type**: String

---
### ExpirationWarningPeriod
Period in days in which the adapter will generate a daily warning and metrics value before the client certificate expires.

**Type**: Integer

Default is 30, set to 0 to disable.

---
### Format
Format of the certificate file, can either be "Pem" or "Pkcs12".

**Type**: String

If not specified the adapter will attempt to determine the type from the filename of the key file.

---
### Password
Password for pkcs12 certificate files

**Type**: String

---
### PrivateKeyFile
Path name to pem private key file (optional for pkcs12, required for pem)

**Type**: String

---
### SelfSignedCertificate
Self-signed certificate configuration used to generate a self-signed certificate. If  this property is set, and the certificate file does not exist a self-signed certificate will be created.

**Type**: [SelfSignedCertificateConfiguration](./self-signed-certificate-configuration.md)

[^top](#CertificateConfiguration)


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


[^top](#CertificateConfiguration)





