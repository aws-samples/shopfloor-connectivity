# AWSServiceConfig

The BaseSourceConfiguration class contains the common properties for protocol adapter sources. Protocol adapters extend this class with specific properties for that adapter.

-[Schema](#AWSServiceConfig-Schema)

-[Example](#AWSServiceConfig-Examples)

**Properties:**

- [CredentialProviderClient](#CredentialProviderClient)
- [Region](#Region)

---

### CredentialProviderClient

Name of the AWS credential provider client defined in the SFC top level configuration section [AwsIotCredentialProviderClients]
(../core/sfc-top-level-config.md#AwsIotCredentialProviderClients) obtaining credentials using X509 certificates from the [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured
the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)


**Type:** String

---

### Region

Name of a valid AWS service region

**Type :** String

### AWSServiceConfig Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsServiceConfig",
  "type": "object",
  "properties": {
    "CredentialProviderClient": {
      "type": "string",
      "description": "The credential provider client name"
    },
    "Region": {
      "type": "string",
      "description": "AWS region"
    }
  }
}

```

### AWSServiceConfig Examples

```json
{
  "Region": "us-east-1",
  "CredentialProviderClient": "aws-credentials-provider"
}

```