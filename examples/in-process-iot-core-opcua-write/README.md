# SFC Example in process configuration for AWS IoT Core to OPC UA Write

The file [`iot-core-opcua-write.json`](iot-core-opcua-write.json) contains an example template for reading data from a 
AWS IoT Core topic and writing the received value to a tag of an OPCUA Server. This sample could be used for integrating cloud-side generated set points to control systems at a plant level.
&nbsp;

In order to use the configuration, make the changes described below and
use it as the value of the --config parameter when starting sfc-main.

A debug target is included in the example to optionally write the output
to the console.
&nbsp;  
&nbsp;  


## Deployment directory

A Placeholder ${SFC_DEPLOYMENT_DIR} is used in the configuration. SFC
dynamically replaces these placeholders with the value of the
environment variable from the placeholder. In this example it should
have the value of the path name of the directory where sfc-main, the used
adapters and targets are deployed with the following directory
structure. (This structure can be changed by setting the path name in
the AdapterTypes and TargetTypes sections)

${SFC_DEPLOYMENT_DIR}  
&nbsp;&nbsp;&nbsp;|-sfc-main  
&nbsp;&nbsp;&nbsp;|-debug-target    
&nbsp;&nbsp;&nbsp;|-opcua-writer-target   
&nbsp;&nbsp;&nbsp;|-mqtt  
&nbsp;  

## Update AWS IoT Core endpoint configuration
Replace the placeholders in the EndPoint configuration with your AWS account specific endpoint information.
You can retrieve your endpoint by running the aws cli command:  `aws iot describe-endpoint --endpoint-type iot:Data-ATS`.

```json
      "Brokers": {
        "default-broker": {
          "EndPoint": "ssl://XXX-ats.iot.XXX.amazonaws.com",
          ...
        }
      }
```

## Configure IoT Credentials
Follow the instructions for creating a new thing in the AWS IoT Core [`Documentation`](https://docs.aws.amazon.com/iot/latest/developerguide/iot-quick-start.html). You will create a new IoT Thing and download its connection kit, which will include files that you will need in the next step.
Finally, download the [`Amazon Root CA`](https://www.amazontrust.com/repository/AmazonRootCA1.pem) and move the CA file and the things cert and key to the local folder that is referenced in the broker configuration.

```json
      "Brokers": {
        "default-broker": {
          "Certificate": "${SFC_DEPLOYMENT_DIR}/certs/sfc.cert.pem",
          "PrivateKey": "${SFC_DEPLOYMENT_DIR}/certs/sfc.private.key",
          "RootCA": "${SFC_DEPLOYMENT_DIR}/certs/AmazonRootCA1.pem",
          ...
        }
      }
```

## Target section
```json
"Targets": [
  "OPCUAWrite",
  "#DebugTarget"
]
```

In order to write the data to both the OPCUA server and the console
uncomment the DebugTarget by deleting the '#'.  
&nbsp;

## Run SFC from deployment directory
```console
sfc-main/bin/sfc-main -config ./iot-core-opcua-write.json
```

[Examples](../../docs/examples/README.md)
