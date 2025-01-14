# SelfSignedCertificateConfig

Elements used for generating a self-signed certificate

- [Schema](#schema)
- [Examples](#Examples)

**Properties:**

- [ApplicationUri](#applicationuri)
- [CommonName](#commonname)
- [CountryCode](#countrycode)
- [DnsNames](#dnsnames)
- [IpAddresses](#ipaddresses)
- [LocalityName](#localityname)
- [Organization](#organization)
- [OrganizationUnit](#organizationunit)
- [StateName](#statename)
- [ValidityPeriodDays](#validityperioddays)

---

### ApplicationUri

Application URI for the certificate

**Type**: [String]

---
### CommonName

Common name for the certificate

**Type:** string

---
### CountryCode

Two-letter country code

**Type**:   String

---
### DnsNames
List of DNS names for the certificate

**Type**:  [String]

---
### IpAddresses

List of IP addresses for the certificate

**Type**: [String]

---
### LocalityName

Locality (city) for the certificate

**Type:**   String

---
### Organization

Organization name for the certificate

**Type:**   String

---
### **OrganizationUnit**

Organizational unit for the certificate      

**Type**: String

---
### StateName

State/province for the certificate

**Type:**   String

---
### ValidityPeriodDays
Number of days the certificate will be valid

**Type**: Integer

Default is 1000

---

[^Top](#SelfSignedCertificateConfig)



### Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "CommonName": {
      "type": String
      "description": "Common name for the certificate"
    },
    "Organization": {
   Type:   String
      "description": "Organization name for the certificate"
    },
    "OrganizationalUnit": {
   Type:   String
      "description": "Organizational unit for the certificate"
    },
    "LocalityName": {
   Type:   String
      "description": "Locality (city) for the certificate"
    },
    "StateName": {
   Type:   String
      "description": "State/province for the certificate"
    },
    "CountryCode": {
   Type:   String
      "description": "Two-letter country code",
      "minLength": 2,
      "maxLength": 2
    },
    "DnsNames": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "description": "List of DNS names for the certificate"
    },
    "IpAddress": {
      "type": "array",
      "items": {
        "type": String
        "format": "ipv4"
      },
      "description": "List of IP addresses for the certificate"
    },
    "ApplicationUri": {
   Type:   String
      "description": "Application URI for the certificate"
    },
    "ValidityPeriodDays": {
      "type": "integer",
      "description": "Number of days the certificate will be valid",
      "minimum": 1
    }
  },
  "required": [
    "commonName"
  ]
}

```



## Examples

Minimal configuration

```json
{
  "CommonName": "example.com"
}
```



Complete configuration

```json
{
  "CommonName": "example.com",
  "Organization": "Example Corporation",
  "OrganizationalUnit": "IT Department",
  "LocalityName": "Seattle",
  "StateName": "Washington",
  "CountryCode": "US",
  "DnsNames": [
    "example.com",
    "*.example.com",
    "api.example.com",
    "web.example.com"
  ],
  "IpAddress": [
    "192.168.1.1",
    "10.0.0.1",
    "172.16.0.1"
  ],
  "ApplicationUri": "urn:example:application:cert",
  "ValidityPeriodDays": 365
}
```



Partial configuration

```json
{
  "CommonName": "api.company.com",
  "Organization": "Company Ltd",
  "CountryCode": "GB",
  "DnsNames": [
    "api.company.com",
    "*.api.company.com"
  ],
  "ValidityPeriodDays": 730
}
```
