## SecretsManagerConfiguration

[SFC Configuration](./sfc-configuration.md#metrics) > [SecretsManager](./sfc-configuration.md#secretsmanager) 

Manages secure storage and access of secrets using AWS Secrets Manager and local encryption. Supports AWS credentials from IoT credentials provider or SDK credential chain, with options for local secret storage using Greengrass V2 deployment keys or custom encryption keys.

- [Schema](#schema)
- [Examples](#examples)


**Properties:**
- [CertificatesAndKeysByFileReference](#certificatesandkeysbyfilereference)
- [CreatePrivateKeyIfNotExists](#createprivatekeyifnotexists)
- [CredentialProviderClient](#credentialproviderclient)
- [GreenGrassDeploymentPath](#greengrassdeploymentpath)
- [PrivateKeyFile](#privatekeyfile)
- [Region](#region)
- [Secrets](#secrets)
- [StoredSecretsDir](#storedsecretsdir)
- [StoredSecretsFile](#storedsecretsfile)

---
### CertificatesAndKeysByFileReference
Controls whether private keys are passed by filename reference to external IPC services. When true, ensures the referenced key files exist and are accessible in the service's runtime environment. Defaults to false

**Type**: Boolean

Default is false

---
### CreatePrivateKeyIfNotExists
Determines whether to automatically generate a new private key file for encrypting local secrets if one doesn't exist. When true (default), creates the key file automatically; when false, requires manual key file creation.

**Type**: Boolean

Default is true

---
### CredentialProviderClient

The CredentialProviderClient property specifies which AWS credential provider client to use for authentication. It references a client defined in the SFC's top-level configuration under [AwsIotCredentialProviderClients](../core/sfc-configuration.md#awsiotcredentialproviderclients) section. This client uses X.509 certificates to obtain temporary AWS credentials through the  [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---
### GreenGrassDeploymentPath
Specifies the path to a Greengrass V2 deployment whose private key will be used for local secret encryption. Typically, /greengrass/v2, requires access to effectiveConfig.yaml in the config subdirectory. The running process must have proper permissions to access these restricted files.

**Type**: String

---
### PrivateKeyFile
Specifies the filename for the private key used to encrypt locally stored secrets. If not specified, defaults to "sfc-secrets-manager-private-key.pem".

**Type**: String

---
### Region
Specifies the AWS region where the Secrets Manager service is located (e.g., "us-east-1", "eu-west-1"). 

**Type**: String

---
### Secrets
Defines the secrets to be retrieved from AWS Secrets Manager using this configuration. 

**Type**: [CloudSecretConfiguration](./cloud-secret-configuration.md)

---
### StoredSecretsDir
Specifies the directory path where secret files and private key files are stored. If not set, defaults to the home directory of the user running the process.

**Type**: String

---
### StoredSecretsFile
Specifies the filename for storing encrypted secrets. If not set, defaults to "sfc-secrets-manager-secrets".

**Type**: String

Default is " sfc-secrets-manager-secrets"

[^top](#secretsmanagerconfiguration)



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
