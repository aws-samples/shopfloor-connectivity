# Shop Floor Connectivity and SiteWise Edge

## Introduction

Shop Floor Connectivity (SFC) is a data ingestion technology that can deliver data to multiple AWS Services. SFC extends and unifies data collection capabilities additionally to our existing Industrial Internet of Things (IIoT) data collection services, allowing customers to provide data in a consistent way to a wide range of AWS Services. It allows customers to collect data from their industrial equipment and deliver it to the AWS services that work best for their requirements. Customers get the cost and functional benefits of specific AWS services and save costs on licenses for additional connectivity products.

[AWS IoT SiteWise Edge](https://aws.amazon.com/iot-sitewise/sitewise-edge/) software makes it easy to collect, organize, process, and monitor equipment data on-premises. It enables factory operators to get visibility into their equipment data and make decisions that help improve equipment uptime, product quality, and process efficiency. SiteWise Edge is installed on local hardware such as third-party industrial gateways and computers, or on AWS Outposts and AWS Snow Family compute devices. Since SiteWise Edge runs on-premises, local applications that use data from SiteWise Edge will continue to work even during intermittent cloud connectivity.

*This workshop demonstrates data ingestion from an on premises OPC-UA server to AWS IoT SiteWise Edge using the SFC in-process deployment. So, we omit the installation of the gateway software to ingest the data.*

## Create and Setup Gateway


**Prerequisites:**

Ensure your AWS account and region have the associated [Greengrass service role](https://docs.aws.amazon.com/greengrass/v2/developerguide/greengrass-service-role.html) enabled, which is required for external connections to the Greengrass MQTT broker.

1. Sign in to the [AWS Management Console](https://console.aws.amazon.com/)
2. Navigate to the [IoT SiteWise console](https://console.aws.amazon.com/iotsitewise/home#/gateway)
3. Create a SiteWise Edge Gateway by following the instructions in the [AWS IoT SiteWise User Guide](https://docs.aws.amazon.com/iot-sitewise/latest/userguide/create-gateway-ggv2.html).
4. Install the SiteWise Edge Gateway software on a local device by following the instructions in the [AWS IoT SiteWise User Guide](https://docs.aws.amazon.com/iot-sitewise/latest/userguide/install-gateway-software-on-local-device.html).


**Note:** Throughout these instructions, the terms "SiteWise Edge Gateway" and "Greengrass core device" are used interchangeably to refer to the same device.


## Enable Secure MQTT Connectivity on SiteWise Edge Gateway

**Note:** To connect to the MQTT broker, you may need to modify your firewall rules to make port 8883 accessible from the connecting device.

1. Navigate to the [AWS IoT SiteWise Edge Gateways](https://console.aws.amazon.com/iotsitewise/home#/gateway) console
2. Select the SiteWise Edge Gateway you created in the previous section
3. Under the **Gateway configuration** panel, click the link under the **Greengrass core device** heading
4. Click on the **Deployments** tab, and then click the link to the existing deployment
5. Under the **Actions** menu button, click **Revise**
6. In the "Revise deployment" dialog box, click **Revise deployment**
7. Click **Next**
8. Next to the search box, uncheck the **Show only selected components** option
9. Search for and add the following components:
   1. `aws.greengrass.clientdevices.mqtt.EMQX`
   2. `aws.greengrass.clientdevices.Auth`
   3. `aws.greengrass.clientdevices.IPDetector`
10. Click **Next**
11. Select the `aws.greengrass.clientdevices.Auth` component and click **Configure component**
12. Paste the following configuration into the **Configuration to merge** section:

```json
{
  "deviceGroups": {
    "formatVersion": "2021-03-05",
    "definitions": {
      "DemoDeviceGroup": {
        "selectionRule": "thingName: DemoClientThing*",
        "policyName": "DemoClientThingPolicy"
      }
    },
    "policies": {
      "DemoClientThingPolicy": {
        "AllowAll": {
          "statementDescription": "Allow client devices.",
          "operations": [
            "mqtt:connect",
            "mqtt:publish",
            "mqtt:subscribe"
          ],
          "resources": [
            "*"
          ]
        }
      }
    }
  }
}
```

13. Click **Confirm**
14. Select the `aws.greengrass.clientdevices.mqtt.EMQX` component and click **Configure component**
15. Paste the following configuration into the **Configuration to merge** section:

```json
{
    "emqxConfig": {
        "authorization": {
            "no_match": "allow"
        },
        "listeners": {
            "tcp": {
                "default": {
                    "enabled": true,
                    "enable_authn": false
                }
            },
            "ssl": {
                "default": {
                    "enabled": true,
                    "enable_authn": true,
                    "ssl_options": {
                        "keyfile": "{work:path}\\data\\key.pem",
                        "certfile": "{work:path}\\data\\cert.pem",
                        "cacertfile": "{work:path}\\data\\ca.pem",
                        "verify": "verify_peer",
                        "versions": [
                            "tlsv1.3",
                            "tlsv1.2"
                        ],
                        "fail_if_no_peer_cert": true
                    }
                }
            }
        }
    },
    "authMode": "bypass_on_failure",
    "dockerOptions": "-p 8883:8883 -p 127.0.0.1:1883:1883",
    "requiresPrivilege": "true"
}
```

16. Click **Confirm**
17. Click **Skip to Review**
18. Click **Deploy**
19. Wait for the **Deployment status** to change to **completed**


After completing these steps, the EMQX MQTT broker component should be deployed and configured on your SiteWise Edge Gateway.

## Obtain MQTT Client Certificates (X.509)

**Note:** You will need to disable hostname validation in your MQTT client to use the X.509 certificates created in this section.

1. [Configure the AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-configure.html) with your [AWS account credentials](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-authentication.html) to access your AWS account
2. Create an IoT policy that allows connections to the Greengrass MQTT broker:

```shell
aws iot create-policy \
   --policy-name DemoClientThingPolicy \
   --policy-document '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "iot:Connect",
        "iot:Publish",
        "iot:Subscribe",
        "iot:Receive",
        "greengrass:Discover"
      ],
      "Resource": "*"
    }
  ]
}'
```

3. Create an IoT Thing named `DemoClientThing` and save its X.509 certificates:

```shell
mkdir -p ~/gateway-client-certs
cd ~/gateway-client-certs
THING_NAME=DemoClientThing
aws iot create-thing --thing-name $THING_NAME
CERTIFICATE_ARN=$(aws iot create-keys-and-certificate --private-key-out $THING_NAME.key --certificate-pem-out $THING_NAME.crt --query "certificateArn" --out text --set-as-active)
aws iot attach-policy --policy-name DemoClientThingPolicy --target $CERTIFICATE_ARN
aws iot attach-thing-principal --thing-name $THING_NAME --principal $CERTIFICATE_ARN
```

4. Associate the `DemoClientThing` with the Greengrass core. You can retrieve the name of the Greengrass Core by going to the [AWS IoT SiteWise Edge Gateways](https://console.aws.amazon.com/iotsitewise/home#/gateway) console and selecting the gateway.

```shell
aws greengrassv2 batch-associate-client-device-with-core-device \
   --core-device-thing-name <REPLACE WITH CORE NAME> \
   --entries thingName=$THING_NAME
```

5. Retrieve the Greengrass Core CA certificate:

```shell
curl -s --cert ${THING_NAME}.crt \
    --key ${THING_NAME}.key \
    https://greengrass-ats.iot.${AWS_REGION}.amazonaws.com:8443/greengrass/discover/thing/${THING_NAME} | \
    jq -r '.GGGroups[0].CAs[0]' > ${THING_NAME}CA.crt
```

After completing these steps, you should have the following files in the `~/gateway-client-certs` directory:


* Certificate Authority (CA): `DemoClientThingCA.crt`
* Client Certificate: `DemoClientThing.crt`
* Client Key: `DemoClientThing.key`


You can test the usage of these certificates with the [mosquitto](https://mosquitto.org/download/) MQTT client:

```shell
mosquitto_sub -h <gateway_address> -p 8883 -q 0 -t '#' \
    --cafile DemoClientThingCA.crt \
    --cert DemoClientThing.crt \
    --key DemoClientThing.key \
    -i DemoClientThing \
    --insecure
```



## Create SiteWise Model and Asset

To associate incoming data with assets in the cloud, deploy the [sitewise_resources.yaml](./resources/cf-templates/sitewise_resources.yaml) CloudFormation template:

```shell
aws cloudformation deploy \
  --template-file sitewise_resources.yaml  \
  --stack-name SFCSiteWiseEdgeDemo
```

**Note:** Make sure to replace `sitewise_resources.yaml` with the actual file path or URL of the CloudFormation template file.

After the deployment is complete, you can navigate to the [AWS IoT SiteWise console](https://console.aws.amazon.com/iotsitewise/home) to view the created model and asset.

## Shop Floor Connectivity Setup

### Installation

Follow these instructions to run the Shop Floor Connectivity (SFC) application on a separate device located on the same network as the SiteWise Edge Gateway. First, download the required SFC bundles:

```shell
# Define sfc version and directory
export VERSION=$(curl -s "https://api.github.com/repos/aws-samples/shopfloor-connectivity/tags" | jq -r '.[0].name')
export SFC_DEPLOYMENT_DIR="./sfc"
```

```shell
# Download and extract bundles into folder ./sfc
mkdir $SFC_DEPLOYMENT_DIR && cd $SFC_DEPLOYMENT_DIR
wget https://github.com/aws-samples/shopfloor-connectivity/releases/download/$VERSION/\
{aws-sitewiseedge-target,debug-target,opcua,sfc-main}.tar.gz

for file in *.tar.gz; do
  tar -xf "$file"
  rm "$file"
done
cd -
```

### Configuration

>**Note:** Please expand the section below, to see the json config...


<details>
<summary>Expand to view the JSON configuration</summary>

```shell
cat << 'EOF' > $SFC_DEPLOYMENT_DIR/example.json
{
    "AWSVersion": "2022-04-02",
    "Name": "OPCUA to MQTT, using in process source and targets",
    "Version": 1,
    "LogLevel": "Info",
    "ElementNames": {
        "Value": "value",
        "Timestamp": "timestamp",
        "Metadata": "metadata"
    },
    "Schedules": [
        {
            "Name": "OpcuaToMqtt",
            "Interval": 200,
            "Description": "Read OPCUA and send to MQTT",
            "Active": true,
            "TimestampLevel": "Both",
            "Sources": {
                "OPCUA-SOURCE": [
                    "*"
                ]
            },
            "Targets": [
                "SiteWiseEdgeTarget",
                "#DebugTarget"
            ]
        }
    ],
    "Sources": {
        "OPCUA-SOURCE": {
            "Name": "OPCUA-SOURCE",
            "ProtocolAdapter": "OPC-UA",
            "AdapterOpcuaServer": "OPCUA-SERVER-1",
            "Description": "OPCUA local test server",
            "SourceReadingMode": "Polling",
            "SubscribePublishingInterval": 100,
            "Channels": {
                "ServerStatus": {
                    "Name": "ServerStatus",
                    "NodeId": "ns=0;i=2256"
                },
                "ServerTime": {
                    "Name": "ServerTime",
                    "NodeId": "ns=0;i=2256",
                    "Selector": "@.currentTime"
                },
                "State": {
                    "Name": "State",
                    "NodeId": "ns=0;i=2259"
                },
                "Machine1AbsoluteErrorTime": {
                    "Name": "AbsoluteErrorTime",
                    "NodeId": "ns=21;i=59048"
                },
                "Machine1AbsoluteLength": {
                    "Name": "AbsoluteLength",
                    "NodeId": "ns=21;i=59066"
                },
                "Machine1AbsoluteMachineOffTime": {
                    "Name": "AbsoluteMachineOffTime",
                    "NodeId": "ns=21;i=59041"
                },
                "Machine1AbsoluteMachineOnTime": {
                    "Name": "AbsoluteMachineOnTime",
                    "NodeId": "ns=21;i=59050"
                },
                "Machine1AbsolutePiecesIn": {
                    "Name": "AbsolutePiecesIn",
                    "NodeId": "ns=21;i=59068"
                },
                "Machine1FeedSpeed": {
                    "Name": "FeedSpeed",
                    "NodeId": "ns=21;i=59039"
                }
            }
        }
    },
    "Targets": {
        "DebugTarget": {
            "Active": true,
            "TargetType": "DEBUG-TARGET"
        },
        "SiteWiseEdgeTarget": {
            "Active": true,
            "TargetType": "AWS-SITEWISEEDGE-TARGET",
            "TopicName": "%channel%",
            "ClientName": "${CLIENT_ID}",
            "EndPoint": "ssl://${GATEWAY_HOSTNAME}",
            "Port": 8883,
            "Connection": "ServerSideTLS",
            "RootCA": "${GATEWAY_CA_FILE}",
            "Certificate": "${CLIENT_CERTIFICATE_FILE}",
            "PrivateKey": "${CLIENT_KEY_FILE}",
            "VerifyHostname": false,
            "BatchSize": 1000,
            "BatchInterval": 5000,
            "BatchCount": 10
        }
    },
    "TargetTypes": {
        "DEBUG-TARGET": {
            "JarFiles": [
                "${SFC_DEPLOYMENT_DIR}/debug-target/lib"
            ],
            "FactoryClassName": "com.amazonaws.sfc.debugtarget.DebugTargetWriter"
        },
        "AWS-SITEWISEEDGE-TARGET": {
            "JarFiles": [
                "${SFC_DEPLOYMENT_DIR}/aws-sitewiseedge-target/lib"
            ],
            "FactoryClassName": "com.amazonaws.sfc.awssitewiseedge.SiteWiseEdgeTargetWriter"
        }
    },
    "AdapterTypes": {
        "OPCUA": {
            "JarFiles": [
                "${SFC_DEPLOYMENT_DIR}/opcua/lib"
            ],
            "FactoryClassName": "com.amazonaws.sfc.opcua.OpcuaAdapter"
        }
    },
    "ProtocolAdapters": {
        "OPC-UA": {
            "AdapterType": "OPCUA",
            "OpcuaServers": {
                "OPCUA-SERVER-1": {
                    "Address": "opc.tcp://localhost",
                    "Path": "/",
                    "Port": 4840,
                    "ConnectTimeout": "10000",
                    "ReadBatchSize": 500
                }
            }
        }
    }
}
EOF
```

</details>

Copy the MQTT client certificates to the SFC device into the `./sfc/swe-certs/` directory and set the required environment variables:

```shell
# define configuration values
export GATEWAY_HOSTNAME=<REPLACE WITH SITEWISE EDGE HOSTNAME>
export CLIENT_ID="DemoClientThing"
export GATEWAY_CA_FILE="./sfc/swe-certs/DemoClientThingCA.crt"
export CLIENT_CERTIFICATE_FILE="./sfc/swe-certs/DemoClientThing.crt"
export CLIENT_KEY_FILE="./sfc/swe-certs/DemoClientThing.key"
```

### Run

With everything set up, you can start the OPC-UA server and the SFC application:

```shell
# start umati opc-ua sample server
sudo docker run -d -p 4840:4840 ghcr.io/umati/sample-server:main

# run sfc
sfc/sfc-main/bin/sfc-main -config sfc/example.json
```

After completing these steps, the SFC application should be running and ingesting data from the OPC-UA server to the SiteWise Edge Gateway using MQTT.

## Query SiteWise

To view the data ingested from the OPC-UA server, you can go to the [AWS IoT SiteWise console](https://console.aws.amazon.com/iotsitewise/home#/assets) and navigate to the `DemoAsset` created earlier. Alternatively, you can query the data using the AWS CLI:

```shell
aws iotsitewise get-asset-property-value-history \
    --property-alias AbsoluteMachineOnTime
```

This command will retrieve the historical values for the `AbsoluteMachineOnTime` property associated with the `DemoAsset`.

You can replace `AbsoluteMachineOnTime` with any other property alias defined in the SiteWise model to query different data points.

[Examples](../../docs/examples/README.md)