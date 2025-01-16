[SFC Configuration](./sfc-configuration) > [HealthProbe](./sfc-configuration#healthprobe)

[SFC Configuration](./sfc-configuration) > [TargetServers](./sfc-configuration#TargetServers) > [TargetServer](./server-configuration.md) > [HealthProbe](./server-configuration.md#healthprobe)

[SFC Configuration](./sfc-configuration) > [ProtocolAdapterServers](./sfc-configuration#ProtocolAdapterServers) > [AdapterServer](./server-configuration.md) > [HealthProbe](./server-configuration.md#healthprobe)

# HealthProbeConfiguration

- [Schema](#Schema)
- [Examples](#Examples)

**Properties:**

- [AllowedIpAddresses](#AllowedIpAddresses)

- [Interface](#Interface)

- [Path](#Path)

- [Port](#Port)

- [RateLimit](#RateLimit)

- [Response](#Response)

- [RetainStatePeriod](#RetainStatePeriod)

- [StopAfterUnhealthyPeriod](#StopAfterUnhealthyPeriod)

  

---
### AllowedIpAddresses
List of IP addresses that are allowed to make calls to the endpoint

**Type**: [String]

Default is an empty list, meaning requests can be made from any address.

IP addresses may contain wildcard sections, e.g., 10.10.10*
HTTP error 403 is returned if the ip address from where the request us made is not in this list.

---
### Interface
Name of the network interface used for the endpoint (e.g., en0) which could be an alternative port as used for communicating with the source devices or other SFC components.

**Type**: Sting

Default is empty, default IP4 network interface is used

---
### Path
Path for endpoint URL

**Type**: String

---
### Port
Port used for the endpoint

**Type**: Int

Must be explicitly set and may not be the same as the port use for other endpoints on the same system/network interface

---
### RateLimit
Maximum number of requests that can be made to the endpoint.

**Type**: Int

Default is 10
If this number is exceeded an HTTP 503 error is returned

---
### Response
Response used to as a response to a service probe request if the service is healthy

**Type**: String

Default is OK
Nothing is returned if the service is not healthy.

---
### RetainStatePeriod
Time in milliseconds to retain last evaluated service status

**Type**: Int

Default is 1000

---
### StopAfterUnhealthyPeriod
Period in seconds after which repeated health probe requests did not return a positive result the process will be stopped. This option can be used if the environment which is controlling the instances does not try to stop the unhealthy service instances itself before a new instance is started.

**Type**: Int

Must be explicitly set in order to stop the service after the period of not returning a healthy response to health probes. The use case for this element is when the mechanism used to manage the instances of the services only checks if the process of a services is running and does stop processes. An example of this is AWS Greengrass. 

[^top](#HealthProbeConfiguration)



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



1. Disabled probe configuration:

```json
{
  "Port": 8080,
  "Active": false,
  "RetainStatePeriod": 1000,
  "StopAfterUnhealthyPeriod": 3000
}
```

[^top](#HealthProbeConfiguration)
