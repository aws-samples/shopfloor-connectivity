# SFC Example in process configuration for OPCUA to AWS MSK

The file `in-process-opcua-msk.json` contains an example template for
reading data from an OPCUA server and sending the data to an AWS MSK topic. Both the adapter 
and targets are configured to run in the sfc-main process. 

The AWS MSK target is a target adapter optimized to write data to AWS Managed Kafka, using
AWS_MSK_IAM for authorization and authentication, for which it can use the SFC functionality to
use X505 certificated to obtain the credentials to access the service.

In order to use the configuration, make the changes described below, and
use it as the value of the --config parameter when starting sfc-main.

A debug target is included in the example to optionally write the output
to the console.
&nbsp;  
&nbsp;  

## Deployment directory

A Placeholder ${SFC_DEPLOYMENT_DIR} is used in the configuration. SFC
dynamically replaces these placeholders with the value of the
environment variable from the placeholder. In this example it should
have the value of the pathname of the directory where scf-main, the used
adapters and targets are deployed with the following directory
structure. (This structure can be changed by setting the pathnames in
the AdapterTypes and TargetTypes sections)

${SFC_DEPLOYMENT_DIR}  
&nbsp;&nbsp;&nbsp;|-sfc-main  
&nbsp;&nbsp;&nbsp;|-debug-target    
&nbsp;&nbsp;&nbsp;|-aws-msk-target  
&nbsp;&nbsp;&nbsp;|-opcua  
&nbsp;  

## Target section
```json
"Targets": [
  "#DebugTarget",
  "MskTarget"
]
```

In order to write the data to both the MSK topic and the console
uncomment the DebugTarget by deleting the'#'.  
&nbsp;
&nbsp;  


## MSK target section

```json

"MskTarget": {
    "CredentialProviderClient": "AwsIotClient",
    "TargetType": "AWS-MSK",
    "BootstrapServers": [
        "< HOSTNAME-1 >:9198",
        "< HOSTNAME-2 >:9198",
        "< HOSTNAME-3 >:9198"
    ],
    "TopicName": "< TOPIC >",
    "Key": "< KEY >",
    "Compression": "gzip",
    "Serialization": "json",
    "Acknowledgements": "all"
}


```
&nbsp;
-   < HOSTNAME-1.,HOST-NAME-3 >, host names for the MSK brokers

-   < TOPIC NAME >, name of the MSK topic. Note that the role that is used by the referred CredentialProviderClient, or the credentials provided by the default credentials chain, must allow the required permission to write data to this topic

-   < KEY >, optional key for the written records

    &nbsp;
-   `Compression` is set to gzip
-   `Serialization` is set to JSON (other option is protobuf)
-   `Acks` is set to "all" (other options are "leader" and "none")
-  `CredentialProviderClient` specifies the credentials provider which is
  used to give access to the used AWS service. For more information see
  section AwsIotCredentialProviderClients below. If this element is not set then
  the default credentials chain is used. (https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html)
  Note that the used role/credentials must allow writing to the configured topic.

See the SFC documentation for all available settings and values of the AWS MSK adapter.
&nbsp;
## Sources section

```json
"Sources": {
    "OPCUA-SOURCE": {
      "Name": "OPCUA-SOURCE",
      "ProtocolAdapter": "OPC-UA",
      "AdapterOpcuaServer": "OPCUA-SERVER",
      "Description": "OPCUA local server",
      "SourceReadingMode": "Subscription",
      "Channels": {
        "LevelAlarm": {
          "Name": "LevelAlarm",
          "NodeId": "ns=6;s=MyLevel.Alarm",
          "EventType": "ExclusiveLevelAlarmType"
        },
        "ServerStatus": {
          "Name": "ServerStatus",
          "NodeId": "ns=0;i=2256"
        },
        "SimulationCounter": {
          "Name": "Counter",
          "NodeId": "ns=3;i=1001"
        },
```

The sources section configures an OPCUA source. It is set up to use the OPCUA adapter (`"OPCUA"`) to read in 
subscription mode from the server `"OPCUA-SERVER"` defined in that adapter. The nodes/events from which to read 
data from are defined in the channels for this source. These channels contain the NodeId and an optional name to explicitly set the name of the value in the output data.

## ProtocolAdapters section

```json
"ProtocolAdapters": {
    "OPC-UA": {
    "AdapterType": "OPCUA",
    "OpcuaServers": {
        "OPCUA-SERVER": {
        "Address": "opc.tcp://localhost",
        "Path": "OPCUA/SimulationServer",
        "Port": 53530
    }
  }
},

```

This section contains a single OPCUA adapter from which the data is read. It is set up to read from a local OPCUA 
simulation server. The type of the source ("OPC-UA") and the actual server ("OPCUA-SERVER") are referred by the OPCUA source ("OPCUA-SOURCE").
&nbsp;
## TargetTypes section

```json
 "TargetTypes": {
    "DEBUG-TARGET": {
      "JarFiles": [
        "${SFC_DEPLOYMENT_DIR}/debug-target/lib"
      ],
      "FactoryClassName": "com.amazonaws.sfc.debugtarget.DebugTargetWriter"
    },
    "AWS-MSK": {
      "JarFiles": [
        "${SFC_DEPLOYMENT_DIR}/aws-msk-target/lib"
      ],
      "FactoryClassName": "com.amazonaws.sfc.awsmsk.AwsMskTargetWriter"
    }
  },
```


This section configured the target types loaded by the SFC main process. The `JarFiles` setting includes the location where the 
jar files which implement the target, are located. The `FactoryClassName` is used by SFC to create instances of the target.
The names `DEBUG-TARGET` and `MSK-TARGET` are used in the `TargetType` setting of the adapter configuration.

```json
"AdapterTypes": {
    "OPCUA": {
      "JarFiles": [
        "${SFC_DEPLOYMENT_DIR}/opcua/lib"
      ],
      "FactoryClassName": "com.amazonaws.sfc.opcua.OpcuaAdapter"
    }
  }
```

This section configured the adapters types loaded by the SFC main process. The `JarFiles` setting includes the location where the
jar files which implement the adapter, are located. The `FactoryClassName` is used by SFC to create instances of the adapters.
The name `OPCUA` are used in the `ProtocolAdapter` setting of the configuration of the source.

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
    "GreenGrassDeploymentPath": "< GREENGRASS DEPLOYMENT DIR >/v2"
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
