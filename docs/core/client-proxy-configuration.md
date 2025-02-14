

## ClientProxyConfiguration

[SFC Configuration](./sfc-configuration.md) > [AwsIotCredentialProviderClients](./sfc-configuration.md#awsiotcredentialproviderclients) > [Proxy](./aws-iot-credential-provider-configuration.md#proxy)

The ClientProxyConfiguration class defines settings for client-level proxy connections, including the proxy server URL, optional authentication credentials (username/password), and addresses that can bypass the proxy (NoProxyAddresses). It provides a structured way to configure how client connections are routed through a proxy server in the SFC system.

- [Schema](#schema)
- [Examples](#examples)

**Properties:**

- [NoProxyAddresses](#noproxyaddresses)
- [ProxyPassword](#proxypassword)
- [ProxyUrl](#proxyurl)
- [ProxyUsername](#proxyusername)

  

---
### NoProxyAddresses
The NoProxyAddresses property accepts a comma-separated list of addresses that should bypass the proxy server. These addresses will be accessed directly without going through the configured proxy. This optional string property allows you to specify exceptions to proxy routing, such as local or internal network addresses.

**Type**: String

---
### ProxyPassword
The ProxyPassword property specifies the password for proxy server authentication. This optional string property should be used in conjunction with ProxyUsername when the proxy server requires authentication credentials.

**Type**: String

Optional

---
### ProxyUrl
The ProxyUrl property specifies the URL address of the proxy server that will handle client connections. This required string property defines the endpoint where proxy requests should be directed.

**Type**: String

---
### ProxyUsername
The ProxyUsername property specifies the username for proxy server authentication. This optional string property should be used together with ProxyPassword when the proxy server requires authentication credentials.

**Type**: String

[^top](#clientproxyconfiguration)



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

[^top](#clientproxyconfiguration)
