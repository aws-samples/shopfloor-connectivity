

## ClientProxyConfiguration

[SFC Configuration](./sfc-configuration) > [AwsIotCredentialProviderClients](./sfc-configuration#AwsIotCredentialProviderClients) > [Proxy](./aws-iot-credential-provider-configuration.md#Proxy)

Configuration for a client level proxy

- [Schema](#Schema)

- [Examples](#Examples)

**Properties:**

- [NoProxyAddresses](#NoProxyAddresses)

- [ProxyPassword](#ProxyPassword)

- [ProxyUrl](#ProxyUrl)

- [ProxyUsername](#ProxyUsername)

  

---
### NoProxyAddresses
Comma-separated list of addresses for which can be accessed without using the proxy

**Type**: String

Optional

---
### ProxyPassword
Proxy server password

**Type**: String

Optional

---
### ProxyUrl
Url of the proxy server to use 

**Type**: String

---
### ProxyUsername
Proxy server username

**Type**: String

[^top](#ClientProxyConfiguration)



## Schema:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "ProxyHost": {
      "type": "string",
      "minLength": 1,
      "description": "Proxy server hostname"
    },
    "ProxyPort": {
      "type": "integer",
      "minimum": 1,
      "maximum": 65535,
      "description": "Proxy server port number"
    },
    "Username": {
      "type": "string",
      "description": "Optional proxy authentication username"
    },
    "Password": {
      "type": "string",
      "description": "Optional proxy authentication password"
    },
    "NonProxyAddresses": {
      "type": "string",
      "description": "Optional comma-separated list of hosts that should bypass the proxy",
      "examples": [
        "localhost,127.0.0.1",
        "internal.example.com,*.local,10.0.0.*"
      ]
    }
  },
  "required": [
    "ProxyHost",
    "ProxyPort"
  ],
  "additionalProperties": false,
  "allOf": [
    {
      "if": {
        "required": [
          "Username"
        ]
      },
      "then": {
        "required": [
          "Password"
        ]
      }
    },
    {
      "if": {
        "required": [
          "Password"
        ]
      },
      "then": {
        "required": [
          "Username"
        ]
      }
    }
  ]
}
```

## Examples

Basic configuration (only required fields):

```json
{
  "ProxyHost": "proxy.example.com",
  "ProxyPort": 8080

}
```



With authentication:

```json
{
  "ProxyHost": "proxy.example.com",
  "ProxyPort": 8080,
  "Username": "${proxyuser}",
  "Password": "${proxypass}"
}
```

With non-proxy addresses:

```json
{
  "ProxyHost": "proxy.example.com",
  "ProxyPort": 8080,
  "NonProxyAddresses": "localhost,127.0.0.1,*.internal.example.com"
}
```



Complete configuration, all fields:

```json
{
  "ProxyHost": "proxy.example.com",
  "ProxyPort": 8080,
  "Username": "${proxyuser}",
  "Password": "${proxypass}"
  "NonProxyAddresses": "localhost,127.0.0.1,*.internal.example.com,10.0.0.*"
}

```

[^top](#ClientProxyConfiguration)
