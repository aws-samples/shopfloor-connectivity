# SFC Example IPC configuration for Misubishi/MELSEC SLMP to Amazon S3

The file ipc-slmp-s3.json file contains an example template for reading data from a Mitsubishi/Melsec controller using the SLMP protocol and sending the data to an S3 bucket. The main.tmc program file is included to declare the variables which are read from the device.

This configuration uses a deployment where each module runs as a service in an individual process and communicate using a stream over a TCP/IP connection. These processes can run on the same system or on different systems. Use cases for this type of deployment are:

-   Distributing the load of multiple adapters or targets over multiple systems

-   Run just the adapters on edge devices with limited capacity

-   Distribute components in different networks, e.g., adapters in the OT network, sfc-core in network and targets which need internet connectivity in the IT network or DMZ.

In order to use the configuration, make the changes describer below, and use it as the value of the –config parameter when starting sfc-main.

A debug target is included in the example to optionally write the output to the console.

## Deployment and starting the service modules

Deploy the sfc-main, SLMP adapter, S3 target and optionally the debug target to individual directories.

Each module has a subdirectory called bin in which there are two files, one for Linux and one for Windows systems, to start the module as a service.

It’s recommended to first start the SLMP protocol adapter and the S3 target and optionally the Debug target and specify the port number used by the module using the -port parameter.

Then start the sfc-main module and use the -config parameter to specify the name of the used config file. The port numbers in this configuration file for the adapter and target services should match with the port numbers used to start these services.

When the adapter and target services are started the services will listen on the specified port for the configuration for that service. After the sfc-main process is started, it will send the specific configuration data for each service to the configured address and port for that service. When this configuration data is received by the protocol or target service it will initialize adapters will start reading data and streaming it to the sfc-main process, and targets will receive the data from sfc-main and sending it to their destinations. When updates are made to the configuration file used by sfc-main, it will automatically load the new configuration and distribute the new configuration to the adapter.

Startup commands for Linux deployments. When running from the console use terminal session for every service or run the servers as Docker containers.

-   <path to SLMP adapter deployment>/bin/slmp -port 50001

-   <path to S3 target deployment>/bin/aws-s3-target -port 50003

-   <path to debug target deployment>/bin/debug-target -port 50002 (optional)

-   <path to sfc-main deployment>/bin/sfc-main -config <path to config file>

&nbsp;  



**Starting the SLMP adapter service**

```bash
$ $ slmp/bin/slmp -port 50001
2024-07-22 19:26:31.965 INFO  - Created instance of service IpcAdapterService
2024-07-22 19:26:31.965 INFO  - Running service instance
2024-07-22 19:26:32.385 INFO  - IPC protocol serv
```
&nbsp;
**Starting the S3 Target service**
```bash
$ aws-s3-target/bin/aws-s3-target -port 50003 
2024-07-22 19:27:53.886 INFO  - Created instance of service IpcTargetServer
2024-07-22 19:27:53.886 INFO  - Running service instance
2024-07-22 19:27:54.156 INFO  - Target IPC service started, listening on  192.168.1.65:50003, connection type is PlainText
```
&nbsp;
**Starting the (optional) Debug target service**

```bash
$ debug-target/bin/debug-target -port 50002
2024-07-22 19:28:21.78  INFO  - Created instance of service IpcTargetServer
2024-07-22 19:28:21.79  INFO  - Running service instance
2024-07-22 19:28:21.337 INFO  - Target IPC service started, listening on  192.168.1.65:50002, connection type is PlainText
```
&nbsp;
**Starting the sfc-main service**

```bash
$ sfc-main/bin/sfc-main -config ipc-slmp-s3.json 
Picked up JAVA_TOOL_OPTIONS: -Dlog4j2.formatMsgNoLookups=true
2024-07-22 19:31:56.895 INFO  - Creating configuration provider of type ConfigProvider
2024-07-22 19:31:56.902 INFO  - Waiting for configuration
2024-07-22 19:31:56.904 INFO  - Sending initial configuration from file "ipc-slmp-s3.json"
2024-07-22 19:31:57.199 INFO  - Received configuration data from config provider
2024-07-22 19:31:57.200 INFO  - Waiting for configuration
2024-07-22 19:31:57.200 INFO  - Creating and starting new service instance
2024-07-22 19:31:57.288 INFO  - Created instance of service MainControllerService
2024-07-22 19:31:57.289 INFO  - Running service instance
2024-07-22 19:31:57.292 INFO  - Creating an IPC process writer for target "DebugTarget", for server "DebugTargetServer" on server DebugTargetServer
2024-07-22 19:31:57.294 INFO  - Creating an IPC process writer for target "S3Target", for server "S3TargetServer" on server S3TargetServer
2024-07-22 19:31:57.295 INFO  - Creating client to connect to IPC service localhost:50003 using connection type PlainText
2024-07-22 19:31:57.295 INFO  - Creating client to connect to IPC service localhost:50002 using connection type PlainText
2024-07-22 19:31:57.298 INFO  - No adapter or target metrics are collected
2024-07-22 19:31:57.299 INFO  - Initializing IPC source adapter service on localhost:50001
2024-07-22 19:31:57.299 INFO  - Creating client to connect to IPC service localhost:50001 using connection type PlainText
2024-07-22 19:31:57.349 INFO  - Initializing IPC target service  for  "S3Target" on server localhost:50003
2024-07-22 19:31:57.349 INFO  - Initializing IPC target service  for  "DebugTarget" on server localhost:50002
2024-07-22 19:31:57.351 INFO  - Sending configuration "{ EDITED }" to target "DebugTarget"
2024-07-22 19:31:57.357 INFO  - Sending configuration "{ EDITED }" to target "S3Target"
2024-07-22 19:31:57.387 INFO  - IPC server for target "S3Target" initialized
2024-07-22 19:31:57.387 INFO  - IPC server for target "DebugTarget" initialized
2024-07-22 19:31:57.502 INFO  - IPC source service adapter for server localhost:50001 initialized
```

## Configuring the Protocol Adapter as a service

To communicate with the protocol adapter as a service add the “AdapterServer” item to the configuration for the adapter. The value must be set to a server in the “AdapterServers” section of the configuration.
```json
"ProtocolAdapters": {  
    "SLMP": {  
        "AdapterServer": "SlmpAdapterServer",
```

In the AdapterServers section the address (localhost or address of other system) and port number of the server are specified. The sfc-core will use these to communicate with the adapter service.

**IMPORTANT: The port number specified in the configuration must match with the port number specified with the -port parameter used to start the adapter service.**

In this example the adapter server are defined in the file "servers.json" and are loaded from the "Adapters" sections in that file.


```json
  "AdapterServers": "@file:servers.json@Adapters",
```
The Adapters section of this file contains an entry for the server used by the SLMP protocol adapter.

```json
 
     "Adapters" : {
        "SlmpAdapterServer": {
           "Address": <IP ADDRESS OF SERVICE>
           "Port": <PORT FOR SERVICE>
        }
     }
```

## Configuring the targets as a service

To communicate with the targets as a service add the “TargetServer” item to the configuration for the target. The value must be set to a server in the “TargetServers” section of the configuration.

```json
"S3Target": {
    "TargetServer": "S3TargetServer",
```

In the TargetServers section the address (localhost or address of other system) and port number of the server are specified. The sfc-core will use these to communicate with the target service.

IMPORTANT: The port numbers specified in the configuration must match with the port numbers specified with the -port parameters used to start the target services.

In this example the target servers are defined in the file "servers.json" and are loaded from the "Targets" sections in that file.


```json
  "TargetServers": "@file:servers.json@Targets",
```

Both servers are defined in the Targets section of the file.

```json
      "Targets": {
         "S3TargetServer": {
            "Address": "< IP ADDRESS OF SERVICE >",
            "Port": 50003
         },
         "DebugTargetServer": {
             "Address": "< IP ADDRESS OF SERVICE >",
             "Port": 50002
         }
     }
```

In order to write the data to both the S3 bucket and the console uncomment the DebugTarget by deleting the ’#’ an ensure the DebugServer service is started.


## Target section
```json
"Targets": [
  "#DebugTarget",
  "S3Target"
]
```

In order to write the data to both the S3 bucket and the console uncomment the DebugTarget by deleting the '#'.  
&nbsp;
&nbsp;  



## AwsIotCredentialProviderClients

This section configures one or more clients which can be referred to by
targets which need access to AWS services.

A credential provider will make use of the AWS IoT Credentials service
to obtain temporary credentials. This process is described at
<https://aws.amazon.com/blogs/security/how-to-eliminate-the-need-for-hardcoded-aws-credentials-in-devices-by-using-the-aws-iot-credentials-provider/>

The resources used in the configuration can easily be setup by creating
a Thing in the AWS IoT service. The role that `RoleAlias` points to, must
give access to the services used by the target which uses the client.

The credential providers for this example are loaded from the file credential-providers.json.

```json
"AwsIotCredentialProviderClients" : {
  "AwsIotClient": {
    "IotCredentialEndpoint": "<ID>.credentials.iot.<YOUR REGION>.amazonaws.com",
    "RoleAlias": "< ROLE EXCHANGE ALIAS >”,
    "ThingName": "< THING NAME > ",
    "Certificate": "< PATH TO DEVICE CERTIFICATE .crt FILE >",
    "PrivateKey": "< PATH TO PRIVATE KEY .key FILE >",
    "RootCa": "< PATH TO ROOT CERTIFICATE .pem FILE >",
  }
}
```


If there is a GreenGrass V2 deployment on the same machine, instead of
all settings a setting named GreenGrassDeploymentPath can be used to
point to that deployment. SFC will use the GreenGrass V2 configurations
setting. Specific setting can be overridden by setting a value for that
setting, which will replace the value from the GreenGrass V2
Configuration. Note that although SFC can be deployed as a GreenGrass
component, it can also run as a standalone process or in a docker
container and still use a GreenGrass configuration.
&nbsp;  
&nbsp;  


```json
"AwsIotCredentialProviderClients": {
  "AwsIotClient": {
    "GreenGrassDeploymentPath": "<GREENGRASS DEPLOYMENT DIR>/v2"
  }
}
```

When the AWS service credentials are provided using one of the options
in the AWS SDK credentials provider chain
(<https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html>)
AwsIotCredentialProviderClients and any references in the targets can be
deleted. Using the temporary credentials provided through a configured
AwsIotCredentialProviderClient for production environment is strongly
recommended.

[Examples](../../docs/examples/README.md)