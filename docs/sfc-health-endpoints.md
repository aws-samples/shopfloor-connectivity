
# Service Health Endpoints

In order to check the state of an SFC process (sfc-main service and protocol adapters, target adapters and metric
writer, running as a service on the local or a remote server) each of these can be configured to have a health probe
endpoint. This endpoint can be polled by the platform used to control the service instances (e.g., Docker Compose,
Kubernetes). Servers will respond with a configurable response (default is "OK") if the service is in non-faulty state,
which is determined by the logic of that service implementation.

The health probe endpoints of the adapter, target and metric services, become active after they have been initialized by
the initialization call made by the sfc-main process, as the request for that call contains the required information to
start the health probe.

Optionally the health probe can be configured use a different network adapter/network as used by the data streams
between the core process and the service.

After receiving the initialization data, the health probe will listen for HTTP GET and HEAD requests on the configured
port on the default or explicit configured network interface (`http://address:port/`) . Optionally a path can be
configured to be appended to the endpoint address (`http://address:port/path`)

Optionally a period can be configured after which repeated health probe requests did not return a positive result the
process will be stopped. This option can be used if the environment which is controlling the instances does not try to
stop the unhealthy service instances itself before a new instance is started.

In order to protect the service from extensive load and unwanted the request the handler for this service:

- A configurable rate limiter is used to limit the number of calls per second (default is 10 request/second)
- The status of the service is cached by the probe handler and retained for a configurable period before being
  re-evaluated (default is 1000 milliseconds)
- A list of IP filters can be configured to restrict the IP addresses from which requests can be made
- The handler is restricted to only use a single thread for handling probe requests

Health probe endpoints for SFC service are configured by adding a HealthProbe configuration sections at the following
locations:

| Service               | HealthProbe Configuration                                                                | Checks                                                                                                |
|-----------------------|------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| SFC Core main process | At top level of configuration                                                            | Active status of all data read, write and aggregation(*) workers and metrics processor(*) (*) if used |
| Protocol Adapters     | In the server configuration used by an adapter in the AdapterServers section             | Status of listening ports for the hosted gRPC service                                                 |
| Target Adapters       | In the server configuration used by a target in the TargetServers section                | Status of listening ports for the hosted gRPC service                                                 |
| Metrics writer        | In the MetricsServer section for the writer configured in the top level Metrics section. | Status of listening ports for the hosted gRPC service                                                 |

For details on the HealthProbe configuration see HealthProbeConfiguration



