## SecretsManagerConfiguration


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
Name of configured credentials client that will be used to read secrets stored in the AWS Secrets Manager service.

**Type**: String

If not set the AWS SDK credential provider chain is used.

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

**Type**: [CloudSecretConfiguration](./cloud-secret-coniguration.md)

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

