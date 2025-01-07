[SFC Top Level](./sfc-top-level-config.md)>[AwsIotCredentialProviderClientConfiguration]( ./sfc-top-level-config.md#AwsIotCredentialProviderClients )
## AwsIotCredentialProviderClientConfiguration

An AWS IoT Credentials Provider Client configuration is used  to obtain temporary credentials used when AWS service API calls using X509 certificates. When used by AWS service targets the name of the configuration is specified as the value for the CredentialProviderClient in the configuration for that target.

For more info see [Session credentials for targets accessing AWS Service](../sfc-aws-service-credentials.md)


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

**Default,Constraints,Examples**: Optional

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

**Default,Constraints,Examples**: Default = false

---
### ThingName
AWS IoT thing name using the device certificate

**Type**: String

[^top](#AwsIotCredentialProviderClientConfiguration)

