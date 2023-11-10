# SFC Example IPC configuration for Rockwell PCCC to Amazon S3

The file in-process-pccc-s3.json file contains an example template for reading data from a Rockwell controller using PCCC over EthernetIP and sending the data to an S3 bucket.

This configuration uses a deployment where each module runs as a service in an individual process and communicate using a stream over a TCP/IP connection. These processes can run on the same system or on different systems. Use cases for this type of deployment are:

-   Distributing the load of multiple adapters or targets over multiple systems

-   Run just the adapters on edge devices with limited capacity

-   Distribute components in different networks, e.g., adapters in the OT network, sfc-core in in network and targets which need internet connectivity in the IT network or DMZ.

In order to use the configuration, make the changes describer below, and use it as the value of the –config parameter when starting sfc-main.

A debug target is included in the example to optionally write the output to the console.

## Deployment and starting the service modules

Deploy the sfc-main, PCCC adapter, S3 target and optionally the debug target to individual directories.

Each module has a subdirectory called bin in which there are two files, one for Linux and one for Windows systems, to start the module as a service.

It’s recommended to first start the PCCC protocol adapter and the S3 target and optionally the Debug target and specify the port number used by the module using the -port parameter.

Then start the sfc-main module and use the -config parameter to specify the name of the used config file. The port numbers in this configuration file for the adapter and target services should match with the port numbers used to start these services.

When the adapter and target services are started the services will listen on the specified port for the configuration for that service. Atter the sfc-main process is started, it will send the specific configuration data for each service to the configured address and port for that service. When this configuration data is received by the protocol or target service it will initialize adapters will start reading data and streaming it to the sfc-main process, and targets will receive the data from sfc-main and sending it to their destinations. When updates are made to the configuration file used by sfc-main, it will automatically load the new configuration and distribute the new configuration to the adapter.

Startup commands for Linux deployments. When running from the console use terminal session for every service or run the servers as Docker containers.

-   <path to PCCC adapter deployment>/bin/pccc -port 50001

-   <path to S3 target deployment>/bin/aws-s3-target -port 50002

-   <path to debug target deployment>/bin/debug-target -port 50003

-   <path to sfc-main deployment>/bin/sfc-main -config <path to config file>

&nbsp;  


**Starting the PCCC adapter service**

```
$ pccc/bin/pccc -port 50001

2023-11-10 17:03:17.814 INFO - Created instance of service IpcAdapterService
2023-11-10 17:03:17.815 INFO - Running service instance
2023-11-10 17:03:18.264 INFO - IPC protocol service started, listening on 192.168.1.65:50001, connection type is PlainText
```
&nbsp;
**Starting the S3 Target service**
```
$aws-s3-target/bin/aws-s3-target -port 50002
2023-11-10 17:05:40.811 INFO - Created instance of service IpcTargetServer
2023-11-10 17:05:40.812 INFO - Running service instance
2023-11-10 17:05:41.417 INFO - Target IPC service started, listening on 192.168.1.65:50002, connection type is PlainText
```
&nbsp;
**Starting the (optional) Debug target service**

```
$ debug-target/bin/debug-target -port 500032023-11-10 17:08:00.866 INFO - Created instance of service IpcTargetServer
2023-11-10 17:08:00.867 INFO - Running service instance
2023-11-10 17:08:01.307 INFO - Target IPC service started, listening on 192.168.1.65:50003, connection type is PlainText
```
&nbsp;
**Starting the sfc-main service**

```
$ sfc-main/bin/sfc-main -config ipc-pccc-s3/pccc.json
2023-11-10 17:22:48.230 INFO - Creating configuration provider of type ConfigProvider
2023-11-10 17:22:48.246 INFO - Waiting for configuration
2023-11-10 17:22:48.251 INFO - Sending initial configuration from file "pccc.json"
2023-11-10 17:22:48.816 INFO - Received configuration data from config provider
2023-11-10 17:22:48.819 INFO - Waiting for configuration
2023-11-10 17:22:48.819 INFO - Creating and starting new service instance
2023-11-10 17:22:49.03 INFO - Created instance of service MainControllerService
2023-11-10 17:22:49.03 INFO - Running service instance
2023-11-10 17:22:49.08 INFO - Creating an IPC process writer for target "DebugTarget", for server "DebugTargetServer" on server DebugTargetServer
2023-11-10 17:22:49.12 INFO - Creating an IPC process writer for target "S3Target", for server "S3TargetServer" on server S3TargetServer
2023-11-10 17:22:49.13 INFO - Creating client to connect to IPC service localhost:50002 using connection type PlainText
2023-11-10 17:22:49.13 INFO - Creating client to connect to IPC service localhost:50003 using connection type PlainText
2023-11-10 17:22:49.18 INFO - No adapter or target metrics are collected
2023-11-10 17:22:49.19 INFO - Initializing IPC source adapter service on localhost:50001
2023-11-10 17:22:49.19 INFO - Creating client to connect to IPC service localhost:50001 using connection type PlainText
2023-11-10 17:22:49.150 INFO - Initializing IPC target service for "DebugTarget" on server localhost:50003
2023-11-10 17:22:49.151 INFO - Initializing IPC target service for "S3Target" on server localhost:50002
2023-11-10 17:22:49.154 INFO - Sending configuration “{ EDITED }" to target "DebugTarget"
2023-11-10 17:22:49.169 INFO - Sending configuration “{ EDITED }" to target "S3Target"
2023-11-10 17:22:49.226 INFO - IPC server for target "S3Target" initialized
2023-11-10 17:22:49.226 INFO - IPC server for target "DebugTarget" initialized
2023-11-10 17:22:49.440 INFO - IPC source service adapter for server localhost:50001 initialized
```

## Configuring the Protocol Adapter as a service

To communicate with the protocol adapter as a service add the “AdapterServer” item to the configuration for the adapter. The value must be set to a server in the “AdapterServers” section of the configuration.
```json
"ProtocolAdapters": {  
    "PCCC": {  
        "AdapterServer": "PcccAdapterServer",
```

In the AdapterServers section the address (localhost or address of other system) and port number of the server are specified. The sfc-core will use these to communicate with the adapter service.

**IMPORTANT: The port number specified in the configuration must match with the port number specified with the -port parameter used to start the adapter service.**

```json
 "AdapterServers": {  
     "PcccAdapterServer": {  
         "Address": <IP ADDRESS OF SERVICE>  
         "Port": <PORT FOR SERVICE>  
     }  
 },
```

## Configuring the targets as a service

To communicate with the targets as a service add the “TargetServer” item to the configuration for the target. The value must be set to a server in the “TargetServers” section of the configuration.

```json
"S3Target": {
    "TargetServer": "S3TargetServer",
```

In the TargetServers section the address (localhost or address of other system) and port number of the server are specified. The sfc-core will use these to communicate with the target service.

IMPORTANT: The port numbers specified in the configuration must match with the port numbers specified with the -port parameters used to start the target services.

```json
"TargetServers": {  
    "DebugTargetServer": {  
        "Address": " <IP ADDRESS OF DEBUG TARGET SERVICE>  
        "Port": <PORT FOR DEBUG TARGET SERVCE>  
    },  
    "S3TargetServer": {  
        "Address": <IP ADDRESS OF S3 TARGET SERVICE>  
        "Port": <PORT FOR S3 TARGET SERVICE>  
    }  
},
```

In order to write the data to both the S3 bucket as well as the console uncomment the DebugTarget by deleting the’#’ an ensure the DebugServer service is started.

## S3Target section

```json
"S3Target": {
    "TargetServer": "S3TargetServer",  
    "Active": true,  
    "TargetType": "AWS-S3",  
    "Region": "<YOUR-REGION>",  
    "BucketName": "<YOUR-BUCKET-NAME>",  
    "Interval": 60,  
    "BufferSize": 1,  
    "Prefix": "<OPTIONAL PREFIX TO USE IN BUCKET>",  
    "CredentialProviderClient": "AwsIotClient",  
    "Compression": "Zip"  
}
```

-   <YOUR-REGION>, your region e.g., eu-west-1
-   <YOUR-BUCKET-NAME>, bucket name to store data
-    < OPTIONAL PREFIX TO USE IN BUCKET>, Optional prefix for data in
    the bucket

The `S3Target` is set up to write data to the specified bucket once every
minute or when the data volume is 1MB in size. Zip Compression is
enabled to reduce the size of the data which is send to and stored in
the S3 bucket, remove the "Compression" line or set to "None" to disable
compression.

`CredentialProviderClient` specifies the credentials provider which is
used to give access to the used AWS service. For more information see
section AwsIotCredentialProviderClients below.
&nbsp;  
&nbsp;


## Sources Section

In this section, the values are defined as channels, which are read from
the controller. In this template there is an example for every
address/type supported by the adapter. In order to change the name of
the value as it is included in the data which is sent to the targets,
include a setting "Name" for the channel.
&nbsp;  
&nbsp;

## ProtocolAdapters section

```json
"ProtocolAdapters": {
  "PCCC": {
    "AdapterServer": "PcccAdapterServer"
    "AdapterType": "PCCC",
    "Controllers": {
      "MicroLogix1400": {
        "Address": "<CONTROLLER IP ADDRESS>"
      }
    }
  }
}

```

-   <CONTROLLER IP ADDRESS>, IP address of the controller

This section configures the controller from which the data is read. The
default port 44818 is used which can be changed by Including a Port
setting specifying that value.

OptimizeReads is set to true to allow the adapter to combine reads from
the controller.
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

```json
"AwsIotCredentialProviderClients" : {
  "AwsIotClient": {
    "IotCredentialEndpoint": "<ID>.credentials.iot.<YOUR REGION>.amazonaws.com",
    "RoleAlias": "< ROLE EXCHANGE ALIAS >”,
    "ThingName": "< THING NAME > ",
    "Certificate": "< PATH TO DEVICE CERTIFICATE .crt FILE >",
    "PrivateKey": "< PATH TO PRIVATE KEY .key FILE >",
    "RootCa": "< PATH TO ROOt CERTIFICATE .pen FILE >",
  }
}
```


If there is a GreenGrass V2 deployment on the same machine, instead of
all settings a setting named GreenGrassDeploymentPath can be used to
point to that deployment. SFC will use the GreenGrass V2 configurations
setting. Specific setting can be overridden by setting a value for that
setting, which will replace the value from the GreenGrass V2
Configuration. Note that although SFC can be deployed as a GreenGrass
component, it can also run as a standalone lone process or in a docker
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
