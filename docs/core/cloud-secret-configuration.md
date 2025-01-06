
## CloudSecretConfiguration

Configuration for a secret obtained from AWS Secrets manager

**Properties:**

- [Alias](#Alias)
- [Labels](#Labels)
- [Name](#Name)
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

