Example: OPC UA to AWS IoT Core using filters
=============================================

## What does the example do?
This example reads data from a public, open OPC UA server (uademo.prosysopc.com) and sends it to AWS IoT Core, demonstrating the 
usage of *[Metadata](../../docs/README.md#metadata)*, *[Transformations](../../docs/sfc-data-processing-filtering.md#transformations)* as well as the following filter types: *[Change Filter](../../docs/sfc-data-processing-filtering.md#data-change-filters)*, *[Condition Filter](../../docs/sfc-data-processing-filtering.md#condition-filters)*, *[Value Filter](../../docs/sfc-data-processing-filtering.md#data-change-filters)*.  

The public OPC UA server exposes simulation tags which we will use. We use two tags:  
1. A counter tag that is incremented by 1 from 0 to 30 in an infinite loop.  
2. A tag producing random values in the range -2 to 2.  
  
The counter tag is used as a trigger. Whenever its value changes by at least 50%, we forward the data.  
To do so, we use a *Change Filter*.  
Here's an example of how it works:  
- previous value 1, new value 2: Change 50% -> tag is read, channel value exists.  
- previous value 3, new value 4: Change only 33% -> tag is not read, channel value does not exist.  

Three flavors of the random value tag are connected to the trigger tag through a *Condition Filter*.  
That data is only forwarded if the trigger channel has a value.   

Besides, the example demonstrates a *Value Filter* only forwarding data greater than zero and also a *Transformation* rounding values to two digits.  
Further, the application of *Metadata* is demonstrated.

A real-world use case for this setup is to create a snapshot of certain OPC-UA tags when the trigger tag is changed.  
This can be used in scenarios where the tags hold final processing data like torque moments to be captured when a part's processing is finished on a machine.

## How to set up and run the example?
The setup of the scenario is similar to the steps in the [Quickstart example](../../README.md#quickstart-example) of this repo with small modifications.  
Please note that sending data to AWS IoT Core might incur a cost when exceeding the free tier limit.

>**Requirements**: Java runtime, aws cli [Credentials Configuration](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-configure.html#configure-precedence). 
Make sure you have AWS permissions as described in [AWS IoT Core Target](../../docs/targets/aws-iot-core.md)

### Installation

First, we have to clone that repo so we can access the current version.  

Then we download and extract the SFC bundles. These are precompiled executables to get started quickly:

```shell
# Define sfc version and directory
export VERSION=$(git describe --tags --abbrev=0)
export SFC_DEPLOYMENT_DIR="./sfc"
```

```shell
# Download and extract bundles into folder ./sfc
mkdir $SFC_DEPLOYMENT_DIR && cd $SFC_DEPLOYMENT_DIR
wget https://github.com/aws-samples/shopfloor-connectivity/releases/download/$VERSION/\
{aws-iot-core-target,debug-target,opcua,sfc-main}.tar.gz

for file in *.tar.gz; do
  tar -xf "$file"
  rm "$file"
done
cd -
```

### Configure

Next we will define the AWS region we want to send the data to:

```shell
export AWS_REGION="us-east-1"
```

Now we will have to configure the SFC, therefore create the config file as follows.

```shell
cat << EOF > $SFC_DEPLOYMENT_DIR/example.json
  {
    "AWSVersion": "2022-04-02",
    "Name": "OPCUA to AWS IoT core denoing filter features",
    "Version": 1,
    "LogLevel": "Info",
    "ElementNames": {
      "Value": "value",
      "Timestamp": "timestamp",
      "Metadata": "metadata"
    },
    "Schedules": [
      {
        "Name": "OpcuaToIotCore",
        "Interval": 150,
        "Description": "Read data from OPC UA tags and send it IoT Core to demo filters",
        "Active": true,
        "TimestampLevel": "Both",
        "Sources": {
          "OPCUA-SOURCE": [
            "*"
          ]
        },
        "Targets": [
          "IoTCoreTarget"
        ]
      }
    ],
    "ChangeFilters": {
      "ChangedBy50Percent": {
        "Type": "Percent",
        "Value": 50,
        "AtLeast": 60000
      }
    },
    "ValueFilters": {
      "OnlyWhenGreaterThan0": {
        "Operator": "gt",
        "Value": 0
      }
    },
    "ConditionFilters": {
      "TriggerFired": {
        "Operator": "present",
        "Value": ["TriggerTag"]
      }
    },
    "Transformations": {
      "TwoDigits": [
        {"Operator": "TruncAt", "Operand": 2}
      ]
    },
    "Sources": {
      "OPCUA-SOURCE": {
        "Name": "OPCUA-SOURCE",
        "ProtocolAdapter": "OPC-UA",
        "AdapterOpcuaServer": "OPCUA-SERVER-1",
        "Description": "Remote OPCUA test server",
        "SourceReadingMode": "Subscription",
        "SubscribePublishingInterval": 100,
        "Metadata": {
          "Some": "...arbitrary data",
          "Attached": "...to every message"
        },
        "Channels": {
          "TriggerTag": {
            "Name": "Trigger",
            "NodeId": "ns=3;i=1001",
            "ChangeFilter": "ChangedBy50Percent"
          },
          "DataTagToReadWhenTriggerFired": {
            "Name": "SomeRandomValue",
            "NodeId": "ns=3;i=1002",
            "ConditionFilter": "TriggerFired",
            "Metadata": {
              "More": "Metadata",
              "Attached": "...to this channel"
            }
          },
          "Rounded2Digits": {
            "Name": "RandomValueRounded",
            "NodeId": "ns=3;i=1002",
            "ConditionFilter": "TriggerFired",
            "Transformation": "TwoDigits",
            "Metadata": {
              "Digits": 2
            }
          },
          "Rounded2DigitsAndGreater0": {
            "Name": "RandomValueRoundedAndGreaterThan0",
            "NodeId": "ns=3;i=1002",
            "ConditionFilter": "TriggerFired",
            "Transformation": "TwoDigits",
            "ValueFilter": "OnlyWhenGreaterThan0",
            "Metadata": {
              "Digits": 2,
              "Cuttoff": 0
            }
          }          
        }
      }
    },
    "Targets": {
      "DebugTarget": {
        "Active": true,
        "TargetType": "DEBUG-TARGET"
      },
      "IoTCoreTarget": {
        "Active": true,
        "TargetType": "AWS-IOT-CORE",
        "Region": "us-east-1",
        "TopicName": "some/iot/topic"
      }
    },
    "TargetTypes": {
      "DEBUG-TARGET": {
        "JarFiles": [
          "./sfc/debug-target/lib"
        ],
        "FactoryClassName": "com.amazonaws.sfc.debugtarget.DebugTargetWriter"
      },
      "AWS-IOT-CORE": {
        "JarFiles": [
          "./sfc/aws-iot-core-target/lib"
        ],
        "FactoryClassName": "com.amazonaws.sfc.awsiotcore.AwsIotCoreTargetWriter"
      }      
    },
    "AdapterTypes": {
      "OPCUA": {
        "JarFiles": [
          "./sfc/opcua/lib"
        ],
        "FactoryClassName": "com.amazonaws.sfc.opcua.OpcuaAdapter"
      }
    },
    "ProtocolAdapters": {
      "OPC-UA": {
        "AdapterType": "OPCUA",
        "OpcuaServers": {
          "OPCUA-SERVER-1": {
            "Address": "opc.tcp://uademo.prosysopc.com",
            "Path": "OPCUA/SimulationServer",
            "Port": 53530,
            "ConnectTimeout": "10000",
            "ReadBatchSize": 500
          }
        }
      }
    }
  }

EOF
```

<br/>
With the file being created everything is set up so you can run the process

```shell
# run sfc
sfc/sfc-main/bin/sfc-main -config sfc/example.json
```

### Observe result

Once you start the process as given in the quickstart, the data will become visible in AWS IoT Core.  

To observe the data in AWS IoT Core, log on to your AWS account, and use the built-in MQTT client  
of the AWS IoT Core console to subscribe to the topic named in the *IoTCoreTarget* definition.  
Also, make sure you are using the region as mentioned in the definition.

