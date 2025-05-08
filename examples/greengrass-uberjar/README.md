SFC `uberjar` setup in Greengrass V2
=======================================

# Introduction

This setup will demonstrate how to integrate SFC into Greengrass V2 with a single artifact jar.
In this way the component can run on windows and linux, does not require any container, unpack zips or so.
It uses the uberjar created by SFC that contains the SFC core, all adapters, targets and the dependencies.
As this is an executable jar the sfc config can be read through the environment configuration which allows to run the same command on all platforms.

# Preconditions

We assume you have a running AWS IoT Greengrass core. If you don't have one running yet please look here:
[Getting started with AWS IoT Greengrass V2](https://docs.aws.amazon.com/greengrass/v2/developerguide/getting-started.html) .

The setup here will just explain how to build the SFC component that then can be deployed to the running Greengrass core.

# Setup

## Step 1: Create an uberjar for SFC

Checkout SFC to your local directory. Then execute the following command to build a uberjar:

```shell
cd core/sfc-uberjar
bash ../../gradlew build
```

This will produce an executable uberjar in `build/libs` like `build/libs/sfc-uberjar-1.9.3.jar`.

## Step 2: Create an Amazon S3 bucket if it doesn't exist

AWS IoT Greengrass can only deploy artifacts from an Amazon S3 bucket in the same AWS region
as the greengrass component is created.
If you have an S3 bucket you want to use you can go to Step 3 if not please create an Amazon S3 bucket
in the same region where you want to build the component. (Greengrass will not allow you to create a component 
with an artefact link to a different region) 

## Step 3: Upload the jar to the Amazon S3 bucket

Upload the uberjar e.g. `core/uberjar/build/libs/sfc-uberjar-1.9.3.jar` to the S3 bucket. Feel free to put it into a folder if you want. 
Please copy the S3 URL e.g. `s3://my-s3-bucket/sfc-uberjar-1.9.3.jar` which you will need in the next step.

## Step 4: Create a Greengrass Component

* Open the AWS Console with AWS IoT Greengrass on the same region where the S3 bucket is located and the Greengrass core is connected to.
* Click on `Greengrass devices`, select `Components` and then click on `Create Component` on the top right.
* replace the sample recipe with the following recipe. **Ensure to replace the artifact URI with the S3 path from Step 3** 

```json
{
  "RecipeFormatVersion": "2020-01-25",
  "ComponentName": "com.amazonaws.sfc",
  "ComponentVersion": "1.0.0",
  "ComponentType": "aws.greengrass.generic",
  "ComponentDescription": "SFC all in one",
  "ComponentPublisher": "Me",
  "ComponentConfiguration": {
    "DefaultConfiguration": {
      "SFC_CONFIG": {
        "AWSVersion": "2022-04-02",
        "Name": "MQTT writing to Debug",
        "Version": 1,
        "LogLevel": "Debug",
        "Schedules": [
          {
            "Name": "DCSChangeSetpoint",
            "Interval": 1000,
            "Active": true,
            "TimestampLevel": "Both",
            "Sources": {
              "MQTT": [
                "*"
              ]
            },
            "Targets": [
              "DebugTarget"
            ]
          }
        ],
        "Sources": {
          "MQTT": {
            "Name": "MQTT",
            "ProtocolAdapter": "MQTT",
            "AdapterBroker": "local-mqtt-broker",
            "Channels": {
              "setpoint_x1": {
                "Topics": [
                  "setpoint/x1"
                ]
              }
            }
          }
        },
        "Targets": {
          "DebugTarget": {
            "TargetType": "DEBUG-TARGET"
          }
        },
        "TargetTypes": {
          "DEBUG-TARGET": {
            "FactoryClassName": "com.amazonaws.sfc.debugtarget.DebugTargetWriter"
          }
        },
        "AdapterTypes": {
          "MQTT": {
            "FactoryClassName": "com.amazonaws.sfc.mqtt.MqttAdapter"
          }
        },
        "ProtocolAdapters": {
          "MQTT": {
            "AdapterType": "MQTT",
            "Brokers": {
              "local-mqtt-broker": {
                "EndPoint": "tcp://127.0.0.1",
                "Port": 1883
              }
            }
          }
        }
      }
    }
  },
  "Manifests": [
    {
      "Platform": {
        "os": "*",
        "runtime": "*"
      },
      "Lifecycle": {
        "Run": {
          "RequiresPrivilege": true,
          "Script": "java -jar {artifacts:path}/sfc-uberjar-1.9.3.jar -trace",
          "Setenv": {
            "SFC_CONFIG": "{configuration:/SFC_CONFIG}"
          }
        }
      },
      "Artifacts": [
        {
          "Uri": "s3://[REPLACE WITH YOUR S3 BUCKET]/sfc-uberjar-1.9.3.jar"
        }
      ]
    }
  ]
}

```
* Click `Create component` to finish creating the component

## Step 5: Deploy the component

* select the Greengrass core ddevice taht you have already running in the AWS Console.
* Revise the deployment and add the SFC component.
* Deploy the revised deployment to the Greengrass core.
* Check the logs in `\greengrass]\v2\logs` there should be a file called `com.amazonaws.sfc.log`
