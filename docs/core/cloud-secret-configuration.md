## CloudSecretConfiguration

[SFC Configuration](./sfc-configuration.md) > [SecretsManager](./sfc-configuration.md#secretsmanager) > [Secrets](./secrets-manager-configuration.md#secrets)

The CloudSecretConfiguration class defines how to retrieve and reference secrets from AWS Secrets Manager. It specifies the secret's identifier (name or ARN), an optional alias for local reference, and version labels for accessing specific secret values. This configuration enables secure access to sensitive information stored in AWS Secrets Manager.

- [Schema](#schema)
- [Examples](#examples)

**Properties:**

- [Alias](#alias)
- [Labels](#labels)
- [SecretId](#secretid)

  

---
### Alias
The Alias property provides an alternative local name for referencing the secret within configuration placeholders. This optional string property allows you to use a simpler or more context-appropriate name when referring to the secret instead of using its actual SecretId or ARN.

**Type**: String

---
### Labels
The Labels property specifies which version of the secret to retrieve using AWS Secrets Manager staging labels. This string property defaults to "AWSCURRENT" to fetch the current version of the secret, but can be set to other staging labels to access different versions.

**Type**: String

---
### SecretId
The SecretId property identifies the AWS Secrets Manager secret using either its name or Amazon Resource Name (ARN). This required string property allows referencing the secret in configuration placeholders using either format, providing flexibility in how the secret is identified and accessed.

 If the ARN of a secret is used, both the ARN or the name of the read secret can be used as a reference in the placeholder.

**Type**: String

[^top](#cloudsecretconfiguration)



## Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "SecretId": {
      "type": "string",
      "description": "The ID or ARN of the secret"
    },
    "Alias": {
      "type": "string",
      "description": "Alias name for the secret"
    },
    "Labels": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "description": "List of labels/staging labels associated with the secret"
    }
  }
}
```



### Examples



Basic configuration with just SecretId:

```json
{
  "SecretId": "myApplicationSecret"
}
```

Using AWS Secrets Manager ARN:

```json
{
  "SecretId": "arn:aws:secretsmanager:us-east-1:123456789012:secret:production/database/credentials"
}
```



With SecretId and Alias:

```json
{
  "SecretId": "database-credentials",
  "Alias": "prod-db-creds"
}
```

With SecretId and staging Labels:

```json
{
  "SecretId": "app-secrets",
  "Labels": ["AWSCURRENT", "AWSPENDING"]
}
```



Secret by ARN with alias

```json
{
  "SecretId": "arn:aws:secretsmanager:us-west-2:123456789012:secret:api/keys",
  "Alias": "api-credentials"
}
```

[^top](#cloudsecretconfiguration)
