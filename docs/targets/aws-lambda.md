# AWS Lambda Target

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration#Targets) >  [Target](../core/target-configuration.md) 




## AwsLambdaTargetConfiguration

AwsLambdaFunctionConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for calling an AWS lambda function. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"AWS-LAMBDA"**


Requires IAM permission `lambda:InvokeFunction` for the lambda function that is called.

- [Schema](#AwsLambdaTargetConfiguration-Schema)
- [Examples](#AwsLambdaTargetConfiguration-Examples)

**Properties:**

- [BatchSize](#BatchSize)
- [Compression](#Compression)
- [CredentialProviderClient](#CredentialProviderClient)
- [FunctionName](#FunctionName)
- [Interval](#Interval)
- [Qualifier](#Qualifier)
- [Region](#Region)

---
### BatchSize
Number of output messages to combine in a single invoke request for the lambda function. If BatchSize is greater than 1, then the output records are combined in a JSON array. The function will be called before the batch size is reached if the maximum payload size will be exceeded.

**Type**: Integer

Default is 10

---
### Compression
Compression used to compress invocation payload.
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

Name of the AWS credential provider client defined in the SFC top level configuration section [AwsIotCredentialProviderClients]
(../core/sfc-top-level-config.md#AwsIotCredentialProviderClients) obtaining credentials using X509 certificates from the [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---
### FunctionName
Name of the Lambda function

**Type**: String

---
### Interval
Interval in milliseconds after which data is sent to stream even if the buffer is not full

**Type**: Integer

Optional, if not set only BatchSize is used, minimum value is 10

---
### Qualifier
AWS Region for Lambda service

**Type**: String

Default is latest

---
### Region
AWS Region for Lambda service

**Type**: String

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
  "Region": "us-east-1,
  "BatchSize": 50,
  "Interval": 1000
}

```



[^top](#aws-lambda-target)

