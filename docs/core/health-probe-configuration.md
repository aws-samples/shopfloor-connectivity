## HealthProbeConfiguration


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

