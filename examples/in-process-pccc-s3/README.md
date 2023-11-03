# SFC Example in process configuration for Rockwell PCCC to Amazon

The file in-process-pccc-s3.json file contains an example template for
reading data from a Rockwell controller using PCCC over EthernetIP and
sending the data to an S3 bucket.

In order to use the configuration, make the changes describer below, and
use it as the value of the --config parameter when starting sfc-main.

A debug target is included in the example to optionally write the output
to the console.

## Deployment directory

A Placeholder \${SFC_DEPLOYMENT_DIR} is used in the configuration. SFC
dynamically replaces these placeholders with the value of the
environment variable from the placeholder. It in this example it should
have the value of the pathname of the directory where scf-main, the used
adapters and targets are deployed with the following directory
structure. (This structure can be changed by setting the pathnames in
the AdapterTypes and TargetTypes sections)

${SFC_DEPLOYMENT_DIR}\
&nbsp;&nbsp;&nbsp;|-sfc-main\
&nbsp;&nbsp;&nbsp;|-debug-target\
&nbsp;&nbsp;&nbsp;|-aws-s3-target\
&nbsp;&nbsp;&nbsp;|-pccc


## Target section
```
"Targets": [
  "#DebugTarget",
  "S3Target"
]
```

In order to write the data to both the S3 bucket as well as the console
uncomment the DebugTarget by deleting the'#'.

## S3Target section

```
"S3Target": {
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

-   \<YOUR-REGION\>, your region e.g., eu-west-1

-   \<YOUR-BUCKET-NAME\>, bucket name to store data

-   \<OPTIONAL PREFIX TO USE IN BUCKET\>\", Optional prefix for data in
    the bucket

The S3Target is setup to write data to the specified bucket once every
minute or when the data volume is 1MB in size. Zip Compression is
enabled to reduce the size of the data which is send to and stored in
the S3 bucket, remove the "Compression" line or set to "None" to disable
compression.

CredentialProviderClient specifies the credentials provider which is
used to give access to the used AWS service. For more information see
section AwsIotCredentialProviderClients below.

## Sources Section

In this section, the values are defined as channels, which are read from
the controller. In this template there is an example for every
address/type supported by the adapter. In order to change the name of
the value as it is included in the data which is send to the targets
include a setting "Name" for the channel.

## ProtocolAdapters section

```
"ProtocolAdapters": {
  "PCCC": {
    "AdapterType": "PCCC",
    "Controllers": {
      "MicroLogix1400": {
        "Address": "<CONTROLLER IP ADDRESS>",
   "OptimizeReads": true,

      }
    }
  }
}

```

-   \<CONTROLLER IP ADDRESS\>, IP address of the controller

This section configures the controller from which the data is read. The
default port 44818 is used which can be changed by Including a Port
setting specifying that value.

OptimizeReads is set to true to allow the adapter to combine reads from
the controller.

## AwsIotCredentialProviderClients

This section configures one or more clients which can be referred to by
targets which need access to AWS services.

A credential provider will make use of the AWS IoT Credentials service
to obtain temporary credentials. This process is describer at
<https://aws.amazon.com/blogs/security/how-to-eliminate-the-need-for-hardcoded-aws-credentials-in-devices-by-using-the-aws-iot-credentials-provider/>

The resources used in the configuration can easily be setup by creating
a Thing in the AWS IoT service. The role the RoleAlias point to must
give access to the services used by the target which uses the client.

```
"AwsIotCredentialProviderClients": {
  "AwsIotClient": {
    "IotCredentialEndpoint": "<ID>.credentials.iot.<YOUR REGION>.amazonaws.com",
    "RoleAlias": "<ROLE EXCHANGE ALIAS>”,
    "ThingName": "<THING NAME> ",
    "Certificate": "<PATH TO DEVICE CERTIFICATE .crt FILE>",
    "PrivateKey": "<PATH TO PRIVATE KEY .key FILE>",
    "RootCa": "<PATH TO ROOt CERTIFICATE .pen FILE>",
  }
}
```


If there is a GreenGrass V2 deployment on the same machine, instead off
all settings a setting named GreenGrassDeploymentPath can be used to
point to that deployment. SFC will use the GreenGrass V2 configurations
setting. Specific setting can be overridden by setting a value for that
setting, which will replace the value from the GreenGrass V2
Configuration. Note that although SFC can be deployed as a GreenGrass
component, it can also run as a standalone lone process or in a docker
container and still use a GreenGrass configuration.

```
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
