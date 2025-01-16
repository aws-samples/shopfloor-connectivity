
# Service Health Endpoints

- Health Probe Configuration for SFC Processes

  

  Each component of an SFC process (sfc-main service, protocol adapters, target adapters, and metric writers) can be configured with a health probe endpoint. This endpoint can be polled by the platform used to control service instances (e.g., Docker Compose, Kubernetes). Servers will respond with a configurable response (default is "OK") if the service is in a non-faulty state, as determined by the service implementation's logic.

  Key points:

- Health probe endpoints for adapter, target, and metric services become active after initialization by the sfc-main process.

- The health probe can optionally use a different network adapter/network than the data streams between the core process and the service.

- After initialization, the health probe listens for HTTP GET and HEAD requests on the configured port and network interface (`http://address:port/`). An optional path can be appended (`http://address:port/path`).

- A configurable period can be set after which, if repeated health probe requests fail, the process will be stopped.

  Protection measures:

- A configurable rate limiter limits the number of calls per second (default: 10 requests/second).
- The service status is cached and retained for a configurable period before re-evaluation (default: 1000 milliseconds).
- IP filters can be configured to restrict the IP addresses from which requests can be made.
- The handler is restricted to using a single thread for handling probe requests.

These measures protect the service from excessive load and unwanted requests.

Health probe endpoints for SFC service are configured by adding a HealthProbe configuration sections at the following
locations:

| Service               | HealthProbe Configuration                                                                | Checks                                                                                                |
|-----------------------|------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| SFC Core main process | At top level of configuration                                                            | Active status of all data read, write and aggregation(*) workers and metrics processor(*) (*) if used |
| Protocol Adapters     | In the server configuration used by an adapter in the AdapterServers section             | Status of listening ports for the hosted gRPC service                                                 |
| Target Adapters       | In the server configuration used by a target in the TargetServers section                | Status of listening ports for the hosted gRPC service                                                 |
| Metrics writer        | In the MetricsServer section for the writer configured in the top level Metrics section. | Status of listening ports for the hosted gRPC service                                                 |

For details on the HealthProbe configuration see [HealthProbeConfiguration](./core/health-probe-configuration.md)



