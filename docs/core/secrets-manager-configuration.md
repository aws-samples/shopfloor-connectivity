## SecretsManagerConfiguration

- [Schema](#Schema)
- [Examples](#Examples)


**Properties:**
- [CertificatesAndKeysByFileReference](#CertificatesAndKeysByFileReference)
- [CreatePrivateKeyIfNotExists](#CreatePrivateKeyIfNotExists)
- [CredentialProviderClient](#CredentialProviderClient)
- [GreenGrassDeploymentPath](#GreenGrassDeploymentPath)
- [PrivateKeyFile](#PrivateKeyFile)
- [Region](#Region)
- [Secrets](#Secrets)
- [StoredSecretsDir](#StoredSecretsDir)
- [StoredSecretsFile](#StoredSecretsFile)

---
### CertificatesAndKeysByFileReference
Can be set to true to transmit private key by filename to external IPC services. The file name must exist and be accessible in the environment running the service

**Type**: Boolean

Default is false

---
### CreatePrivateKeyIfNotExists
If set the file containing a secret key that will be used to encrypt locally stored secrets will be created if it does not exist.

**Type**: Boolean

Default is true

---
### CredentialProviderClient
Name of configured credentials client that will be used to read secrets stored in the AWS Secrets Manager service. The name must refer to a client defined in the  "AwsIotCredentialProviderClients" section at the top level SFC configuration file (https://aws.amazon.com/blogs/security/how-to-eliminate-the-need-for-hardcoded-aws-credentials-in-devices-by-using-the-aws-iot-credentials-provider/)

**Type**: String

If not set the AWS SDK credential provider chain is used. (https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html)

---
### GreenGrassDeploymentPath
Path to an existing and accessible GreenGrass V2 deployment. If set then the private key used in that deployment is used to encrypt local secrets.

**Type**: String

Optional

The typical root directory for Greengrass 2 deployment is /greengrass/v2. The process running the core or target must have access to the file effectiveConfig.yaml in subdirectory config. Note that these directories and files have restricted access.

---
### PrivateKeyFile
Name of file containing the private key used to encrypt locally stores secrets

**Type**: String

Default is "sfc-secrets-manager-private-key.pem"

---
### Region
Region of the used AWS Secrets Manager Service

**Type**: String

---
### Secrets
Configured secrets obtained by this secrets manager configuration

**Type**: [CloudSecretConfiguration](./cloud-secret-configuration.md)

---
### StoredSecretsDir
Name of the directory where stored secrets file and optionally also the private key file are created,

**Type**: String

Default is home directory of user running the process

---
### StoredSecretsFile
Name of the file used to store secrets.

**Type**: String

Default is " sfc-secrets-manager-secrets"

[^top](#SecretsManagerConfiguration)



## Schema



```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "CertificatesAndKeysByFileReference": {
      "type": "boolean",
      "default": false,
      "description": "Flag indicating if certificates and keys are referenced by file"
    },
    "CreatePrivateKeyIfNotExists": {
      "type": "boolean",
      "default": true,
      "description": "Flag indicating if private key should be created if it doesn't exist"
    },
    "CredentialProviderClient": {
      "$ref": "#/definitions/CredentialProviderConfiguration",
      "description": "Configuration for the credential provider client defined in top level section"
    },
    "GreenGrassDeploymentPath": {
      "type": "string",
      "pattern": "^(/[^/]+)+$|^/$",
      "description": "Path to Greengrass deployment"
    },
    "PrivateKeyFile": {
      "type": "string",
      "pattern": "^([A-Za-z]:)?[\\/\\\\](?:[^\\/\\\\\\n\\r\\t\\f\\v]+[\\/\\\\])*[^\\/\\\\\\n\\r\\t\\f\\v]*$",
      "description": "Path to private key file"
    },
    "Region": {
      "type": "string",
      "pattern": "^[a-z]{2}(-[a-z]+)+-\\d{1}$",
      "description": "AWS region for Secrets Manager"
    },
    "Secrets": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/CloudSecretConfiguration"
      },
      "minItems" : 1,
      "description": "List of cloud secret configurations"
    },
    "StoredSecretsDir": {
      "type": "string",
      "pattern": "^([A-Za-z]:)?[\\/\\\\](?:[^\\/\\\\\\n\\r\\t\\f\\v]+[\\/\\\\])*[^\\/\\\\\\n\\r\\t\\f\\v]*$",
      "description": "Directory path for stored secrets"
    },
    "StoredSecretsFile": {
      "type": "string",
      "pattern": "^([A-Za-z]:)?[\\/\\\\](?:[^\\/\\\\\\n\\r\\t\\f\\v]+[\\/\\\\])*[^\\/\\\\\\n\\r\\t\\f\\v]*$",
      "description": "File path for stored secrets"
    }
  },
  "allOf": [
    {
      "if": {
        "properties": {
          "GreenGrassDeploymentPath": { "type": "string" }
        },
        "required": ["GreenGrassDeploymentPath"]
      },
      "then": {
        "properties": {
          "PrivateKeyFile": { "type": "string" },
          "StoredSecretsDir": { "type": "string" },
          "Region": { "type": "string" }
        }
      },
      "else": {
        "required": ["PrivateKeyFile", "StoredSecretsDir", "Region"]
      }
    }
  ]
}
```



## Examples



Basic configuration:

```json
{
  "Region": "us-west-2",
  "CredentialProviderClient" : "AwsIotClient",
  "Secrets": [
     {
        "SecretId": "arn:aws:secretsmanager:us-west-2:123456789012:secret:database-credentials",
        "Alias": "db-creds"
    },
    {
        "SecretId": "proxy-credentials",
        "Alias": "proxy-creds"
     }
  ]
}
```



Using settings from Greengrass configuration (works also when nou using as GreenGrass component)

```json
{
  "Region": "eu-central-1",
  "GreenGrassDeploymentPath": "/greengrass/v2",
  "Secrets": [
     {
        "SecretId": "arn:aws:secretsmanager:us-west-2:123456789012:secret:database-credentials",
        "Alias": "db-creds"
    },
    {
        "SecretId": "proxy-credentials",
        "Alias": "proxy-creds"
     }
  ],
}
```



Explicit settings for storing secrets:

```json
{
  "Region": "us-east-1",
  "CredentialProviderClient" : "AwsIotClient",
  "PrivateKeyFile": "./secrets/private.key",
  "Secrets": [
    {
      "Alias": "AppSec1",
      "SecretARN": "arn:aws:secretsmanager:us-east-1:123456789012:secret:app-secret-1"
    },
    {
      "Alias": "AppSec2",
      "SecretARN": "arn:aws:secretsmanager:us-east-1:123456789012:secret:app-secret-2"
    }
  ],
  "StoredSecretsDir": "./secrets",
  "StoredSecretsFile": "stored-secrets.json"
}
```
