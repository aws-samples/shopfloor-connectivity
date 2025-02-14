## AwsIotCredentialProviderClientConfiguration

[SFC Configuration](./sfc-configuration.md) > [AwsIotCredentialProviderClientConfiguration](./sfc-configuration.md#awsiotcredentialproviderclients)

An AWS IoT Credentials Provider Client configuration is used  to obtain temporary credentials used when AWS service API calls using X.509 certificates. When used by AWS service targets, the name of the configuration is specified as the value for the CredentialProviderClient in the configuration for that target. If  AWS IoT  Greengrass  is deployed on the system,  alternatively this [configuration](https://docs.aws.amazon.com/greengrass/v2/developerguide/device-auth.html) can be referenced even when SFC is not deployed as a Greengrass component.

For more info see [Session credentials for targets accessing AWS Service](../sfc-aws-service-credentials.md)

- [Schema](#schema)
- [Examples](#examples)


**Properties:**
- [CertificateFile](#certificatefile)
- [CertificatesByFileReference](#certificatesbyfilereference)
- [ExpiryClockSkewSeconds](#expiryclockskewseconds)
- [GreenGrassDeploymentPath](#greengrassdeploymentpath)
- [IotCredentialEndpoint](#iotcredentialendpoint)
- [PrivateKeyFile](#privatekeyfile)
- [Proxy](#proxy)
- [RoleAlias](#rolealias)
- [RootCA](#rootca)
- [SkipCredentialsExpiryCheck](#skipcredentialsexpirycheck)
- [ThingName](#thingname)

---
### CertificateFile
The CertificateFile property specifies the file system path to the X.509 device certificate file. This certificate is used to authenticate the device with AWS IoT Core services. The certificate must be registered with AWS IoT Core and associated with appropriate policies for authentication and authorization.

**Type**: String

---
### CertificatesByFileReference
The CertificatesByFileReference property controls how certificates and keys are passed to SFC components running as IPC (Inter-Process Communication) services. When true, files are referenced by their filenames, allowing certificates to exist on a different system than where the service runs. When false, the actual certificate and key contents are passed directly. This enables flexible credential management across distributed systems.

**Type**: Boolean

Default is false

---
### ExpiryClockSkewSeconds
The ExpiryClockSkewSeconds property defines a buffer time (in seconds) added to the system clock when checking credential expiration. This helps prevent using expired credentials when system clocks are slightly out of sync. The credential provider will proactively fetch new credentials when the current time plus this skew value exceeds the credential expiration time. For example, with the default value of 300 seconds (5 minutes), new credentials will be requested 5 minutes before actual expiration.

**Type**: Int

Default = 300 seconds

---
### GreenGrassDeploymentPath
The GreengrassDeploymentPath property specifies the root directory path of an AWS IoT Greengrass V2 deployment. When set, the credential provider will read configuration settings ( [IotCredentialEndpoint](#iotcredentialendpoint), [RoleAlias](#rolealias), [ThingName](#thingname), [CertificateFile](#certificatefile), [PrivateKeyFile](#privatekeyfile), [RootCA](#rootca), and [Proxy](#proxy))from the Greengrass configuration file. Individual settings specified elsewhere will override those from the Greengrass configuration.

The typical root directory for Greengrass 2 deployment is /greengrass/v2. The process running the core or target must have access to the file effectiveConfig.yaml in subdirectory config. Note that these directories and files have restricted access.

Note: This configuration can be used even when SFC is not deployed as a Greengrass component.

**Type**: String

---
### IotCredentialEndpoint
Endpoint for credential provider service

**Type**: String

Can be obtained by CLI command

```console
aws iot describe-endpoint --endpoint-type iot:CredentialProvider
```
Format is `your_aws_account_specific_prefix`.credentials.`region`.amazonaws.com

---
### PrivateKeyFile
The PrivateKeyFile property specifies the file system path to the private key file associated with the device certificate. This private key is used in conjunction with the device certificate for authentication with AWS IoT Core services and must be kept secure. The private key file must correspond to the public key in the device certificate.

**Type**: String

---
### Proxy
Proxy configuration if the client is using a proxy server to access the internet.

**Type**: [ClientProxyConfiguration](./client-proxy-configuration.md)

Optional

---
### RoleAlias
The RoleAlias property specifies an alias that points to an IAM role. When requesting temporary credentials, this alias must be included to indicate which IAM role should be assumed. The AWS IoT credentials provider uses this role alias to obtain temporary security tokens from AWS Security Token Service (STS) that grant the permissions defined in the referenced IAM role.

**Type**: String

---
### RootCA
The RootCA property specifies the file system path to the root Certificate Authority (CA) certificate file. This certificate is used to verify the authenticity of the AWS IoT Core endpoint during TLS handshake. The root CA certificate establishes the chain of trust for secure communications with AWS IoT services

**Type**: String

---
### SkipCredentialsExpiryCheck
The SkipCredentialsExpiryCheck property allows bypassing the credential expiration verification on systems with unreliable clock time. When set to true, the credential provider will not validate the expiration time of credentials. This can be useful in environments where system time may be incorrect, but it comes with risks - API service calls may fail if the credentials have actually expired. The target implementation must handle such failures appropriately.

**Type**: Boolean

Default = false

---
### ThingName
The ThingName property specifies the AWS IoT thing name associated with the device certificate. This is the unique identifier for the device in AWS IoT Core that corresponds to the device certificate being used for authentication. The thing name is used to identify the device when requesting credentials from the AWS IoT credentials provider service.

**Type**: String

[^top](#awsiotcredentialproviderclientconfiguration)

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

[^top](#awsiotcredentialproviderclientconfiguration)
