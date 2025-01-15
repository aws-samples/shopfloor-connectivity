## CertificateValidationOptions

- [Schema](#CertificateValidationOptions-Schema)
- [Examples](#CertificateValidationOptions-Examples)

**Properties:**

- [ApplicationUri](#ApplicationUri)
- [ExtKeyUsageEndEntity](#ExtKeyUsageEndEntity)
- [HostOrIp](#HostOrIp)
- [KeyUsageEndEntity](#KeyUsageEndEntity)
- [KeyUsageIssuer](#KeyUsageIssuer)
- [Revocation](#Revocation)
- [Validity](#Validity)

---

### ApplicationUri

Check Application description against the ApplicationUri from Subject Alternative Names

**Type**: Boolean

Default is true

---

### ExtKeyUsageEndEntity

Extended key usage extension must be present and will be validated for end-entity certificates

**Type**: Boolean

Default is true

---

### HostOrIp

Host or IP address must be present in Alternate Subject Names and will be checked

**Type**: Boolean

Default is true

---

### KeyUsageEndEntity

Key usage extension must be present and will be validated for end-entity certificates

**Type**: Boolean

Default is true

---

### KeyUsageIssuer

Key usage must be present and will be checked for CA certificates

**Type**: Boolean

Default is true



---

### Revocation

Revocation checking

**Type**: Boolean

Default is true

---

### Validity

Check certificate expiry

**Type**: Boolean

Default is true

### CertificateValidationOptions Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration options for certificate validation",
  "properties": {
    "ApplicationUri": {
      "type": "boolean",
      "description": "Enable validation of application URI",
      "default": true
    },
    "ExtKeyUsageEndEntity": {
      "type": "boolean",
      "description": "Enable validation of extended key usage for end entity certificates",
      "default": true
    },
    "HostOrIp": {
      "type": "boolean",
      "description": "Enable validation of host name or IP address",
      "default": true
    },
    "KeyUsageEndEntity": {
      "type": "boolean",
      "description": "Enable validation of key usage for end entity certificates",
      "default": true
    },
    "KeyUsageIssuer": {
      "type": "boolean",
      "description": "Enable validation of key usage for issuer certificates",
      "default": true
    },
    "Revocation": {
      "type": "boolean",
      "description": "Enable certificate revocation checking",
      "default": true
    },
    "Validity": {
      "type": "boolean",
      "description": "Enable validation of certificate validity period",
      "default": true
    }
  }
}

```

### CertificateValidationOptions Examples

```json
{
  "ApplicationUri": false,
  "ExtKeyUsageEndEntity": false,
  "HostOrIp": false,
  "KeyUsageEndEntity": false,
  "KeyUsageIssuer": true,
  "Revocation": true,
  "Validity": true
}

```

[^top](#CertificateValidationOptions)