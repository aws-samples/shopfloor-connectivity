# AwsServiceConfiguration

The AwsServiceConfig class contains common properties and settings used across different AWS service configurations. It serves as a base configuration for AWS service integrations.

- [Schema](#awsserviceconfig-schema)

- [Example](#awsserviceconfig-examples)

**Properties:**

- [CredentialProviderClient](#credentialproviderclient)
- [Endpoint](#endpoint)
- [Region](#region)

---

### CredentialProviderClient

The CredentialProviderClient property specifies which AWS credential provider client to use for authentication. It references a client defined in the SFC's top-level configuration under [AwsIotCredentialProviderClients](../core/sfc-configuration.md#awsiotcredentialproviderclients) section. This client uses X.509 certificates to obtain temporary AWS credentials through the  [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---

### Endpoint

The EndPoint property specifies the VPC endpoint URL used to access AWS services privately through AWS PrivateLink without requiring an internet gateway or NAT device. When not specified, the service's default public endpoint for the configured region will be used.

https://docs.aws.amazon.com/vpc/latest/privatelink/aws-services-privatelink-support.html

**Type:** String

---

### Region

The Region property specifies the name of a valid AWS service region where the service will be accessed (e.g., us-east-1, eu-west-1). This determines the geographical AWS region endpoint that will be used for service requests.

**Type :** String

[^top](#awsserviceconfiguration)

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

[^top](#awsserviceconfiguration)