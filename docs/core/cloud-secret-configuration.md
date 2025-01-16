## CloudSecretConfiguration

[SFC Configuration](./sfc-configuration) > [SecretsManager](./sfc-configuration#SecretsManager) > [Secrets](./secrets-manager-configuration.md#Secrets)

Configuration for a secret obtained from AWS Secrets manager

- [Schema](#Schema)
- [Examples](#Examples)

**Properties:**

- [Alias](#Alias)

- [Labels](#Labels)

- [SecretId](#SecretId)

  

---
### Alias
Alias for the secret

**Type**: String

Optional. Alternative local name to reference a secret from a placeholder in the configuration.

---
### Labels
 Labels to specify specific value to read for the secret. 

**Type**: String

Default is AWSCURRENT which is the current value of a secret

---
### SecretId
Name or ARN of the secret

**Type**: String

 If the ARN of a secret is used, both the ARN or the name of the read secret can be used as a reference in the placeholder.

[^top](#CloudSecretConfiguration)



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

[^top](#CloudSecretConfiguration)
