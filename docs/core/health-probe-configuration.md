# HealthProbeConfiguration

[SFC Configuration](./sfc-configuration.md) > [HealthProbe](./sfc-configuration.md#healthprobe)

[SFC Configuration](./sfc-configuration.md) > [TargetServers](./sfc-configuration.md#targetservers) > [TargetServer](./server-configuration.md) > [HealthProbe](./server-configuration.md#healthprobe)

[SFC Configuration](./sfc-configuration.md) > [AdapterServers](./sfc-configuration.md#adapterservers) > [AdapterServer](./server-configuration.md) > [HealthProbe](./server-configuration.md#healthprobe)

The HealthProbeConfiguration class defines settings for a health monitoring endpoint that allows external systems to check the operational status of a service. It specifies network settings (port, interface, allowed IPs), response behavior, and automatic shutdown conditions when a service remains unhealthy for a specified period.

- [Schema](#schema)
- [Examples](#examples)

**Properties:**

- [AllowedIpAddresses](#allowedipaddresses)
- [Interface](#interface)
- [Path](#path)
- [Port](#port)
- [RateLimit](#ratelimit)
- [Response](#response)
- [RetainStatePeriod](#retainstateperiod)
- [StopAfterUnhealthyPeriod](#stopafterunhealthyperiod)

  

---
### AllowedIpAddresses
The AllowedIpAddresses property defines a list of IP addresses permitted to access the health probe endpoint. This optional string array supports wildcard patterns (e.g., 10.10.10*) and defaults to an empty list allowing access from any IP. Requests from unauthorized IPs receive a 403 HTTP error response.

**Type**: [String]

---
### Interface
The Interface property specifies which network interface the health probe endpoint should use (e.g., en0). This optional string property allows using an alternative interface than the one used for device communication. If not specified, the default IPv4 network interface is used.

**Type**: Sting

---
### Path
The Path property defines the URL path where the health probe endpoint will be accessible. This string property specifies the route that will be used for health check requests.

**Type**: String

---
### Port
The Port property specifies the network port number for the health probe endpoint. This required integer value must be explicitly configured and must be unique - it cannot conflict with ports used by other endpoints on the same system or network interface.

**Type**: Int

---
### RateLimit
The RateLimit property defines the maximum number of health probe requests allowed to the endpoint. This integer property defaults to 10 requests. When the rate limit is exceeded, the endpoint returns an HTTP 503 error response.

**Type**: Int

---
### Response
The Response property defines the string value returned by the health probe endpoint when the service is healthy (defaults to "OK"), while no response is provided when the service is unhealthy

**Type**: String


---
### RetainStatePeriod
The RetainStatePeriod property specifies how long (in milliseconds) the service should cache and reuse the last evaluated health status before performing a new health check. This integer property defaults to 1000 milliseconds.

**Type**: Int

---
### StopAfterUnhealthyPeriod
The StopAfterUnhealthyPeriod property defines the duration (in seconds) after which the service will stop itself if it consistently fails health checks. This integer property must be explicitly set to enable auto-shutdown behavior. It's particularly useful in environments like AWS Greengrass where the service manager only monitors process status but does not handle unhealthy service termination.

**Type**: Int

[^top](#healthprobeconfiguration)



## Schema

```json{
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "AllowedIpAddresses": {
      "type": "array",
      "items": {
        "type": "string",
        "format": "ipv4"
      },
      "description": "List of IP addresses allowed to access the health probe"
    },
    "Active": {
      "type": "boolean",
      "default": true
    },
    "Interface": {
      "type": "string",
      "description": "Network interface to bind the health probe"
    },
    "Path": {
      "type": "string",
      "description": "URL path for the health probe endpoint"
    },
    "Port": {
      "type": "integer",
      "minimum": 1,
      "maximum": 65535,
      "description": "Port number for the health probe"
    },
    "RateLimit": {
      "type": "integer",
      "minimum": 1,
      "description": "Maximum number of requests allowed per second",
      "default": 10
    },
    "Response": {
      "type": "string",
      "default": "OK",
      "description": "Response message for the health probe when service is healthy"
    },
    "RetainStatePeriod": {
      "type": "integer",
      "default": 1000
    },
    "StopAfterUnhealthyPeriod": {
      "type": "integer"
    }
  },
  "required": [
    "Port"
  ]
}
```



## Examples

Minimal configuration:

```json
{
  "Port": 8080
}
```



Basic configuration 

```json
{
  "Port": 8080,
  "Active": true,
  "Path": "/health",
  "Response": "OK",
  "RateLimit": 10,
  "RetainStatePeriod": 1000
}
```



Full configuration:

```json
{
  "Port": 8080,
  "Active": true,
  "Path": "/healthcheck",
  "Interface": "eth0",
  "AllowedIpAddresses": [
    "192.168.1.100",
    "10.0.0.50",
    "172.16.0.1"
  ],
  "RateLimit": 15,
  "Response": "Service Healthy",
  "RetainStatePeriod": 1000,
  "StopAfterUnhealthyPeriod": 600
}
```



Custom path and response:

```json
{
  "Port": 3000,
  "Path": "/status",
  "Response": "System operational",
  "Active": true,
  "RateLimit": 20,
  "RetainStatePeriod": 1500
}
```



Disabled probe configuration:

```json
{
  "Port": 8080,
  "Active": false,
  "RetainStatePeriod": 1000,
  "StopAfterUnhealthyPeriod": 3000
}
```

[^top](#healthprobeconfiguration)
