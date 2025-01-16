## AwsIotCredentialProviderClientConfiguration

[SFC Configuration](./sfc-configuration) > [AwsIotCredentialProviderClientConfiguration](./sfc-configuration#AwsIotCredentialProviderClients)

An AWS IoT Credentials Provider Client configuration is used  to obtain temporary credentials used when AWS service API calls using X509 certificates. When used by AWS service targets the name of the configuration is specified as the value for the CredentialProviderClient in the configuration for that target.

For more info see [Session credentials for targets accessing AWS Service](../sfc-aws-service-credentials.md)

- [Schema](#Schema)
- [Examples](#Examples)


**Properties:**
- [CertificateFile](#CertificateFile)
- [CertificatesByFileReference](#CertificatesByFileReference)
- [ExpiryClockSkewSeconds](#ExpiryClockSkewSeconds)
- [GreenGrassDeploymentPath](#GreenGrassDeploymentPath)
- [IotCredentialEndpoint](#IotCredentialEndpoint)
- [PrivateKeyFile](#PrivateKeyFile)
- [Proxy](#Proxy)
- [RoleAlias](#RoleAlias)
- [RootCA](#RootCA)
- [SkipCredentialsExpiryCheck](#SkipCredentialsExpiryCheck)
- [ThingName](#ThingName)

---
### CertificateFile
The pathname of the device certificate file

**Type**: String

---
### CertificatesByFileReference
Option to pass credential provider-client certificate and key file by filename (true) or by content (false).

**Type**: Boolean

Default is false

---
### ExpiryClockSkewSeconds
Seconds that will be added to the system time when checking the expiration of the credentials. New credentials will be retrieved when the clock time of the system plus these number of seconds is beyond the credentials expiration time. This value can be set to avoid using expired credentials when the system clock runs slightly behinds

**Type**: Int

Default = 300

---
### GreenGrassDeploymentPath
Pathname for the root of Greengrass deployment. If set then the ClientProxyConfiguration IotCredentialEndpoint, RoleAlias, ThingName, Certificate, PrivateKey, RootCA, and Proxy will be read from the GreenGrass configuration file. If any of these is specified then it will override the setting read from the configuration file.

**Type**: String

The typical root directory for Greengrass 2 deployment is /greengrass/v2. The process running the core or target must have access to the file effectiveConfig.yaml in subdirectory config. Note that these directories and files have restricted access.

---
### IotCredentialEndpoint
Endpoint for credential provider service

**Type**: String

Can be obtained by CLI command

```console
aws iot describe-endpoint --endpoint-type iot:CredentialProvider
```
Format is <your_aws_account_specific_prefix>.credentials.<region>.amazonaws.com

---
### PrivateKeyFile
Path to the private key file for the device

**Type**: String

---
### Proxy
Proxy configuration if the client is using a proxy server to access the internet.

**Type**: 

Optional

---
### RoleAlias
Alias pointing to an IAM role. The credentials provider request must include a role alias name to indicate which IAM role to assume for obtaining a security token from AWS

**Type**: String

---
### RootCA
The pathname of the root CA certificate file

**Type**: String

---
### SkipCredentialsExpiryCheck
For systems that don't have a reliable clock time, this setting can be set to true to skip the expiry date of the credentials. This may result in failing API Service calls due to expired session credentials, which must be handled in the target implementation.

**Type**: Boolean

Default = false

---
### ThingName
AWS IoT thing name using the device certificate

**Type**: String

[^top](#AwsIotCredentialProviderClientConfiguration)

## Schema



```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "ThingName": {
      "type": "string",
      "minLength": 1
    },
    "RoleAlias": {
      "type": "string",
      "minLength": 1
    },
    "CertificateFile": {
      "type": "string",
      "minLength": 1,
      "pattern": "^([A-Za-z]:)?[\\/\\\\](?:[^\\/\\\\\\n\\r\\t\\f\\v]+[\\/\\\\])*[^\\/\\\\\\n\\r\\t\\f\\v]*$",
      "description": "Path to certificate file. Can be either Windows style (C:\\path\\to\\cert.pem) or Unix style (/path/to/cert.pem)"
    },
    "PrivateKeyFile": {
      "type": "string",
      "minLength": 1,
      "pattern": "^([A-Za-z]:)?[\\/\\\\](?:[^\\/\\\\\\n\\r\\t\\f\\v]+[\\/\\\\])*[^\\/\\\\\\n\\r\\t\\f\\v]*$",
      "description": "Path to private key file. Can be either Windows style (C:\\path\\to\\key.pem) or Unix style (/path/to/key.pem)"
    },
    "RootCaFile": {
      "type": "string",
      "pattern": "^([A-Za-z]:)?[\\/\\\\](?:[^\\/\\\\\\n\\r\\t\\f\\v]+[\\/\\\\])*[^\\/\\\\\\n\\r\\t\\f\\v]*$",
      "description": "Optional path to root CA file. If specified, must be either Windows style (C:\\path\\to\\root-ca.pem) or Unix style (/path/to/root-ca.pem)"
    },
    "IotCredentialEndpoint": {
      "type": "string",
      "minLength": 1,
      "pattern": "^[a-z0-9]+\\.credentials\\.iot\\.[a-z]{2}-[a-z]+-\\d{1}\\.amazonaws\\.com$",
      "description": "AWS IoT endpoint"
    },
    "Region": {
      "$ref": "#/definitions/AwsRegion"
    },
    "SkipCredentialsExpiryCheck": {
      "type": "boolean",
      "default": false
    },
    "ExpiryClockSkewSeconds": {
      "type": "integer",
      "minimum": 0,
      "default": 300
    },
    "GreenGrassDeploymentPath": {
      "type": "string",
      "pattern": "^(/[^/]+)+$|^/$",
      "description": "Optional GreenGrass deployment path, must be a valid Unix-style path"
    },
    "Proxy": {
      "$ref": "#/definitions/ClientProxy",
      "description": "Optional proxy configuration"
    }
  },
  "allOf": [
    {
      "if": {
        "properties": {
          "GreenGrassDeploymentPath": {
            "not": {
              "type": "string"
            }
          }
        }
      },
      "then": {
        "required": [
          "ThingName",
          "RoleAlias",
          "CertificateFile",
          "PrivateKeyFile",
          "RootCaFile",
          "Endpoint"
        ]
      }
    }
  ]
}
```



## Examples



Configuration specifying all required properties:

```json
{
  "ThingName": "MyIoTThing",
  "RoleAlias": "GreengrassV2TokenExchangeRole",
  "CertificateFile": "/greengrass/v2/device.pem.crt",
  "PrivateKeyFile": "/greengrass/v2/private.pem.key",
  "RootCaFile": "/greengrass/v2/AmazonRootCA1.pem",
  "IotCredentialEndpoint": "c1alcfbzvfkjpi.credentials.iot.eu-west-1.amazonaws.com",
  "Region": "eu-west-1"
}
```

Configuration referring to a GreenGrass deployment configuration:

```json
"AwsIotCredentialProviderClient": {
  "GreenGrassDeploymentPath": "/greengrass/v2",
  "Region": "eu-west-1"
}
```



Configuration using a proxy for internet access:

```json
{
  "ThingName": "MyIoTThing",
  "RoleAlias": "GreengrassV2TokenExchangeRole",
  "CertificateFile": "C:\\greengrass\\v2\\device.pem.crt",
  "PrivateKeyFile": "C:\\greengrass\\v2\\private.pem.key",
  "RootCaFile": "C:\\greengrass\\v2\\AmazonRootCA1.pem",
  "IotCredentialEndpoint": "c1alcfbzvfkjpi.credentials.iot.eu-west-1.amazonaws.com",
  "Region": "eu-west-1",
  "Proxy": {
    "ProxyHost": "proxy.example.com",
    "ProxyPort": 8080,
    "Username": "proxyuser",
    "Password": "proxypass",
    "NonProxyAddresses": "localhost,127.0.0.1,internal.example.com"
  }

}
```

[^top](#AwsIotCredentialProviderClientConfiguration)
