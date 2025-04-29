# AWS Lambda Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

The AWS [Lambda](https://aws.amazon.com/lambda/) target adapter for Shop Floor Connectivity  enables direct integration with AWS Lambda functions from industrial data sources. This adapter receives collected data from the SFC Core component and invokes specified Lambda functions, allowing for serverless processing of industrial device data. The adapter supports batching, compression and data transformations using Apache Velocity templates to format the payload before invoking the Lambda functions.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWSLAMBDA": {
      "JarFiles" : ["<location of deployment>/aws-lambda-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.awslambda.AwsLambdaTargetWriter"
   }
}
```




## AwsLambdaTargetConfiguration

AwsLambdaFunctionConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for calling an AWS lambda function. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-LAMBDA"**


Requires IAM permission `lambda:InvokeFunction` for the lambda function that is called.

- [Schema](#awslambdatargetconfiguration-schema)
- [Examples](#awslambdatargetconfiguration-examples)

**Properties:**

- [BatchSize](#batchsize)
- [Compression](#compression)
- [CredentialProviderClient](#credentialproviderclient)
- [Endpoint](#endpoint)
- [Formatter](#formatter)
- [FunctionName](#functionname)
- [Interval](#interval)
- [Qualifier](#qualifier)
- [Template](#template)
- [Region](#region)

---
### BatchSize
This configuration property allows control over message batching when invoking Lambda functions. When BatchSize is set greater than 1, multiple messages are combined into a single array before invoking the Lambda function, which can improve efficiency by reducing the number of function invocations. The batching process will trigger an invocation either when the batch size is reached or when adding another message would exceed Lambda's payload size limits.

**Type**: Integer

Default is 10

---
### Compression
This configuration property controls payload compression for Lambda function invocations. When compression is enabled, the payload is compressed using the specified algorithm, encoded in base64, and wrapped in a JSON structure containing both the compression type and the encoded payload. It's important to note that while compression can reduce data transfer size for large payloads, the base64 encoding adds approximately 33% overhead to the compressed data size, so compression should be used selectively based on payload characteristics.

As this payload needs to be valid JSON.
The data is wrapped in structure with the following fields:

- "compression" : Used compression
- "payload": Compressed data as a base64 encoded string.
When using compression for the lambda payload verify if actual compression out weights the overhead of the base64 encoded of the compressed data.

**Type**: String

**Values:** 

- "None"  (Default)

- "GZip"
- "Zip"

---

### CredentialProviderClient

The CredentialProviderClient property specifies which AWS credential provider client to use for authentication. It references a client defined in the SFC's top-level configuration under [AwsIotCredentialProviderClients](../core/sfc-configuration.md#awsiotcredentialproviderclients) section. This client uses X.509 certificates to obtain temporary AWS credentials through the  [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String



---

### Endpoint

The EndPoint property specifies the VPC endpoint URL used to access AWS services privately through AWS PrivateLink without requiring an internet gateway or NAT device. When not specified, the service's default public endpoint for the configured region will be used.

https://docs.aws.amazon.com/vpc/latest/privatelink/aws-services-privatelink-support.html

**Type:** String

---

### Formatter

Configuration allows for custom formatting of data written by a target. A [custom formatter](../sfc-extending.md#custom-formatters), implemented as a JVM class, converts a sequence of target data messages into a specific format and returns the formatted data as an array of bytes.

When a formatter is used, a [template](#template) configured for that target is ignored.

**Type:** [InProcessConfiguration](../core/in-process-configuration.md)

---
### FunctionName
Name of the Lambda function

The function name must comply to the following constraints:

- Minimum length: 1 character

- Maximum length: 64 characters

- Allowed characters:

  - Letters (a-z, A-Z)

  - Numbers (0-9)

  - Hyphens (-)

  - Underscores (_)

- Must start with a letter or number
- Cannot end with a hyphen

**Type**: String

---
### Interval
Interval in milliseconds after which data is sent to stream even if the buffer is not full

**Type** : Integer

Optional, if not set only [BatchSize](#batchsize) is used, minimum value is 10

---
### Qualifier
Version or alias of the Lambda function to invoke

**Type** : String

Default is "latest"

---
### Template

Specifies the file path to an [Apache velocity](https://velocity.apache.org/)  template used for  [transforming the output data](../sfc-target-templates.md) target output data. This optional setting enables custom formatting of data before it is sent to the target. Available context variables include:

- $schedule
- $sources
- $metadata
- $serial
- $timestamp
- names specified in ElementNames configuration
- $tab (for inserting tab characters)

Pathname to file containing an [Apache velocity](https://velocity.apache.org/) template that can be applied to [transform the output data](../sfc-target-templates.md) of the target.

The following [Velocity tools](https://velocity.apache.org/tools/3.1/tools-summary.html) can be used in the transformation template:

- $datetool
- $collection
- $context
- $math
- $number

Additional epoch timestamp values can be added to the data used for the transformation by setting the [TemplateEpochTimestamp](../core/target-configuration.md#templateepochtimestamp) property to true,

For targets where the data does not require specific output format, the data is serialized as [JSON data](../sfc-data-format.md#sfc-output-data-schemas).

When a custom [formatter](#formatter) is configured for a target then this property is ignored.

**Type**: String

---

### Region

AWS Region for Lambda service

**Type** : String

### AwsLambdaTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AwsLambdaTargetConfiguration",
  "type": "object",
  "allOf": [
    {
      "$ref": "#/definitions/TargetConfiguration"
    },
    {
      "$ref": "#/definitions/AwsServiceConfig"
    },
    {
      "type": "object",
      "properties": {
        "BatchSize": {
          "type": "integer",
          "description": "Size of the batch for Lambda invocations"
        },
        "Compression": {
          "type": "string",
          "description": "Compression type for payload",
          "enum": ["None", "Zip", "GZip"],
          "default": "None"
        },
        "CredentialProviderClient": {
          "type": "string",
          "description": "The credential provider client name"
        },
        "FunctionName": {
          "type": "string",
          "description": "Name or ARN of the Lambda function"
        },
        "Interval": {
          "type": "integer",
          "description": "Interval in milliseconds between batch invocations"
        },
        "Qualifier": {
          "type": "string",
          "description": "Version or alias of the Lambda function"
        },
        "Region": {
          "type": "string",
          "description": "AWS region for Lambda"
        }
      },
      "required": ["FunctionName"]
    }
  ]
}

```

### AwsLambdaTargetConfiguration Examples



Configuration using CredentialProviderClient,

```json
{
  "TargetType" : "AWS-LAMBDA",  
  "FunctionName": "process-data-function",
  "Region": "us-east-1",
  "BatchSize": 50,
  "Interval": 1000,
  "CredentialProviderClient": "aws-credentials-provider"
}

```



```json
{
  "TargetType" : "AWS-LAMBDA",    
  "FunctionName": "process-data-function",
  "Region": "us-east-1",
  "BatchSize": 50,
  "Interval": 1000
}

```



[^top](#aws-lambda-target)

