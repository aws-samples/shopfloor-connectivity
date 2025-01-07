# AWS Lambda Target



## AwsLambdaTargetConfiguration

AwsLambdaFunctionConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for calling an AWS lambda function. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"AWS-LAMBDA"</strong>

<p>Requires IAM permission lambda:InvokeFunction for the lambda function that is called.</p>


**Properties:**
- [BatchSize](#BatchSize)
- [Compression](#Compression)
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

**Type**: "None" | "GZip" | "Zip"

Default is "None"

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

[^top](#aws-lambda-target)

