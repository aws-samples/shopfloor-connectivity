# OPCUA Writer target adapter

The OPC UA Writer target adapter is designed to write source data to an OPC UA server. It achieves this by configuring nodes that establish mappings between data values and nodes on the server.

An alternative OPC UA target adapter  to this adapter is the  [OPCUA Target](./opcua.md) adapter which hosts the OPC UA server within the target, unlike this adapter which writes values to an external server.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "OPCUA-WRITER-TARGET": {
      "JarFiles" : ["<location of deployment>/opcua-writer-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.opcuawritetarget.OpcuaTargetWriter"
   }
}
```



**Configuration:**

- [OpcuaWriterTargetConfiguration](#opcuawritertargetconfiguration)
- [OpcuaNodeConfiguration](#opcuanodeconfiguration)
- [OpcuaCertificateValidationConfiguration](#opcuacertificatevalidationconfiguration)
- [OpcuaCertificateValidationOptions](#opcuacertificatevalidationoptions-type)

## OpcuaWriterTargetConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

OpcuaWriterTargetConfiguration extends the type [TargetConfiguration](../core/target-configuration.md) with specific configuration data for publishing the data to and OPCUA server. The Targets configuration element can contain entries of this type; the TargetType of these entries must be set to **"OPCUA-WRITER-TARGET"**.

- [Schema](#opcuawritertargetconfiguration-schema)
- [Example](#opcuawritertargetconfiguration-example)

**Properties:**

- [Address](#address)
- [BatchSize](#batchsize)
- [Certificate](#certificate)
- [CertificateValidation](#certificatevalidation)
- [ConnectTimeout](#connecttimeout)
- [MaxChunkCount](#maxchunkcount)
- [MaxChunkSize](#maxchunksize)
- [MaxMessageSize](#maxmessagesize)
- [Nodes](#nodes)
- [Path](#path)
- [Port](#port)
- [SecurityPolicy](#securitypolicy)
- [WaitAfterConnectError](#waitafterconnecterror)
- [WriteTimeout](#writetimeout)



### Address

The Address property specifies the network address or endpoint URL of the OPC UA server. This is used to establish the connection to the server the adapter will write to.

**Type**: String

---

### BatchSize

The BatchSize property specifies the maximum number of nodes that will be written in a single batch write. If this property is not set, then all values from a receiver target data message will be written in a single batch. This property can be used when the amount of data is larger than can be written in a single batch write to the server.

Type: Integer

---

### Certificate

The Certificate property specifies the client certificate configuration used to establish a secure connection with the OPC UA server. Client certificates are an essential part of OPC UA security that:

1. Authenticate the client to the server
2. Enable secure communication through message signing and encryption
3. Establish trust between the client and server

When connecting to an OPC UA server that requires security, the client must present a valid certificate. The server validates this certificate to:

- Verify the client's identity
- Ensure the client is authorized to connect
- Enable encrypted communication channels

If the server doesn't initially trust the client's certificate, it will typically place it in a "rejected" certificates folder. An administrator can then review and move it to a "trusted" folder to allow future connections from that client.

**Type**: [CertificateConfiguration](../core/certificate-configuration.md)

---

### CertificateValidation

The CertificateValidation property specifies how server certificates should be validated when establishing a connection. This configuration determines how the client handles and validates certificates presented by the OPC UA server, including: 

- Whether to accept untrusted certificates
- Certificate validation rules and requirements
- How to handle certificate validation failures
- Trust chain validation settings

This is an important security configuration that helps ensure connections are only established with legitimate and trusted servers.

**Type**: [CertificateValidationConfiguration](#opcuacertificatevalidationconfiguration)

---

### ConnectTimeout

The ConnectTimeout property specifies the maximum time, in milliseconds, to wait when attempting to establish a connection with the OPC UA server. If a connection cannot be established within this time period, the connection attempt will fail. 

**Type** : Integer

The default value is 10000 milliseconds (10 seconds), and the minimum allowed value is 1000 milliseconds (1 second).

---

### MaxChunkCount

The MaxChunkCount property specifies the maximum number of chunks that a message can be divided into when being transmitted between the client and server.

**Type** : Integer

By default, this value is calculated as [MaxMessageSize](#maxmessagesize) / [MaxChunkSize](#maxchunksize) * 2. The multiplication by 2 accounts for overhead in chunk construction, as not all the chunk size is used for actual message data.

---

### MaxChunkSize

The MaxChunkSize property defines the maximum size in bytes for a single chunk of a message. 

**Type** : Integer

Default value is 65535 (64KB)
Minimum allowed value is 8196 (8KB)
Maximum allowed value is 2,147,483,639 (MaxInt-8 is approximately 2048GB)

---

### MaxMessageSize

The MaxChunkSize property defines the maximum message size in bytes.

**Type** : Integer

Default value is 2,097,152 (2MB)
Minimum allowed value is 8196 (8KB)
Maximum allowed value is 2,147,483,639 (MaxInt-8 is approximately 2048GB)

---

### Nodes

List of OPC UA node configurations that define the mappings between target data values and their corresponding nodes on the OPC UA server. Each node configuration specifies how incoming target data values should be written to specific nodes in the OPC UA server address space. At least one node must be configured.

**Type:** [[OpcuaNodeConfiguration](#opcuanodeconfiguration)

---

### Path

The Path property specifies the server path or name.

**Type**: String

The connection address that will be constructed is `<Address>:<Port>[/<Path>]`

Optional property

---

### Port

The Port property specifies the OPCUA server port.

**Type** : Integer

Default value is 53530

---

### SecurityPolicy

The SecurityPolicy property specifies the security policy to be used.

**Type** : String

Valid values are:

- None: No security (no encryption or signing)
- Basic128Rsa15: RSA15 key wrap algorithm with 128-bit encryption
- Basic256: RSA PKCS#1 v1.5 with 256-bit encryption
- Basic256Sha256: RSA with SHA256 and 256-bit encryption
- Aes128ShaRsaOaep: AES-128 with RSA-OAEP encryption and SHA signing

Note: When using any value other than "None", a client certificate must be configured.

**Type**: String

Default is None

---

### WaitAfterConnectError

The WaitAfterConnectError property specifies the time in milliseconds to wait before attempting to reconnect after a connection error. 

**Type**: Integer

Default is 10000, the minimum value is 1000

---

### WriteTimeout

The WriteTimeout property specifies the timeout in milliseconds when writing to the server. 

**Type** : Integer

Default value is 10000



## OpcuaWriterTargetConfiguration Schema

[SFC-Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

```json
{
  "OpcuaWriterTargetConfiguration": {
    "type": "object",
    "description": "Configuration for an OPC UA writer target, extends TargetConfiguration",
    "allOf": [
      {
        "$ref": "#/definitions/TargetConfiguration"
      },
      {
        "type": "object",
        "required": ["nodes"],
        "properties": {
          "Address": {
            "type": "string",
            "description": "Address of the OPC UA server"
          },
          "BatchSize": {
            "type": "integer",
            "description": "Size of batches for writing values"
          },
          "Certificate": {
            "type": "string",
            "description": "Certificate for OPC UA security"
          },
          "CertificateValidation": {
              "$ref": "#/definitions/CertificateValidationConfiguration",
              "description": "Configuration for certificate validation"
            },
          "ConnectTimeout": {
            "type": "integer",
            "description": "Timeout for connection attempts in milliseconds"
          },
          "MaxChunkCount": {
            "type": "integer",
            "description": "Maximum number of chunks in a message"
          },
          "MaxChunkSize": {
            "type": "integer",
            "description": "Maximum size of a chunk in bytes"
          },
          "MaxMessageSize": {
            "type": "integer",
            "description": "Maximum size of a message in bytes"
          },
          "Nodes": {
            "type": "array",
            "description": "List of OPC UA node configurations",
            "items": {
              "$ref": "#/definitions/OpcuaNodeConfiguration"
            }
          },
          "Path": {
            "type": "string",
            "description": "Path component of the OPC UA server URL"
          },
          "Port": {
            "type": "integer",
            "description": "Port number of the OPC UA server"
          },
          "SecurityPolicy": {
            "type": "string",
            "description": "Security policy for OPC UA communication"
          },
          "WaitAfterConnectError": {
            "type": "integer",
            "description": "Time to wait after a connection error in milliseconds"
          },
          "WriteTimeout": {
            "type": "integer",
            "description": "Timeout for write operations in milliseconds"
          }
        }
      }
    ]
  }
}


```



## OpcuaWriterTargetConfiguration Example



```json
{
    "TargetType": "OPCUA-WRITER-TARGET",
    "Address": "opc.tcp://server-prod",
    "Path": "factorydata",
    "Nodes": [
      {
        "NodeId": "ns=3;i=1011",
        "DataPath": "@.sources.Conveyor.values.Running.value"
      },
      {
        "NodeId": "ns=3;i=1012",
        "DataPath": "@.sources.Conveyor.values.FlowRate.value"
      }
    ]
  }
```



## OpcuaNodeConfiguration

Defines the mapping between the values in data received by the target adapter and the nodes on the server these are written to. Optionally it can specify a transformation to apply before the data is written.

- [Schema](#opcuanodeconfiguration-schema)
- [Examples](#opcuanodeconfiguration-examples)

**Properties:**

- [ArrayDimensions](#arraydimensions)

- [DataPath](#datapath)

- [DataType](#datatype)

- [NodeId](#nodeid)

- [TimestampPath](#timestamppath)

- [Transformation](#transformation)

- [WarnIfNotPresent](#warnifnotpresent)

  

### ArrayDimensions
Specifies the dimensions and size of an array value. Specifying the dimensions is essential for array values, and the DataType property is being specified in order to cast the values to the specified type.  The maximum number of dimensions is 3.

The array contains the sizes for the dimensions of an array variable;
e.g.

[3] : Value array of 3 elements
[3,2] : Value array of 3 by 2 elements



**Type**: [Int]

---



### DataPath

[JMES](https://jmespath.org/) path expression that selects the value to write to the OPC UA server node from the received data structure.

A path typically has the format 

`"sources.< source name >.values< value name>.value"` 

Important notes:

- Special characters (like '-') must be enclosed in quotes
- Path must follow JMESPath syntax rules
- Must resolve to a single value in the data structure
- Case-sensitive

**Type**: String

---

### DataType

Specifies the OPC UA data type for the property value. If no type is specified, the type of the value is used to determine the data type used for the value that is written to the node. The DataType is used to specify a specific data type that matches the data type of the node.

Possible values:

- `BOOLEAN`
- `BYTE`
- `BYTE_STRING`
- `DATE_TIME`
- `DOUBLE`
- `EXPANDED_NODE_ID`
- `FLOAT`
- `INT16`
- `INT32`
- `INT64`
- `NODE_ID`
- `SBYTE`
- `STRING`
- `STRUCT`
- `XML_ELEMENT`

The '_' character in the type names can be omitted, so type `BYTE_STRING` equals to `BYTESTRING`

**Type**: String

---

### NodeId

The NodeId property specifies a string containing the identifier of the node to write a value to.

The id must have the format:
`ns=<namespaceIndex>;<identifiertype>=<identifier>`

with the fields:

`<namespace index>`: The namespace index formatted as a number.
`<identifier type>`: A flag that specifies the identifier type. The flag has the following values:

- I: Integer
- S: String
- G: Guid
- B: ByteString

`<identifier>`: The identifier encoded as string.

**Type**: String

---

### TimestampPath
Defines the path to extract timestamp information using JMESPath syntax.

A path typically has the format 

`"sources.< source name >.values< value name >.timestamp"`

If not specified, the adapter will look for a timestamp in the order: value level, source level, root level.
Note that JMESPath syntax treats characters like '-' as special characters, and therefore the element in the path must be in quotes.

---
### Transformation
Specifies a transformation ID to process the value before writing it to the variable node.

References a transformation defined in the  [Transformations](../core/sfc-configuration.md#transformations) section of the SFC configuration, applied after [DataPath](#datapath) query retrieval but before writing to the OPC UA node.

**Type**: String

---
### WarnIfNotPresent
Controls whether a warning is generated when the specified data path doesn't return a value. This could happen if the syntax is correct but doesn't select a value, or when the value is not present.

**Type**: Boolean

Default is true

---




### OpcuaNodeConfiguration Schema

```json
{
  "OpcuaNodeConfiguration": {
    "type": "object",
    "description": "Defines the mapping between the values in data received by the target adapter and the nodes on the server these are written to.",
    "required": ["NodeId", "DataPath"],
    "properties": {
      "NodeId": {
        "type": "string",
        "description": "The node identifier on the OPC UA server"
      },
      "DataPath": {
        "type": "string",
        "description": "JMESPath expression to select data from the input"
      },
      "DataType": {
        "type": "string",
         "enum": [
          "BOOLEAN",
          "SBYTE",
          "BYTE", 
          "INT16",
          "UINT16",
          "INT32",
          "UINT32",
          "INT64",
          "UINT64",
          "FLOAT",
          "DOUBLE",
          "STRING",
          "DATE_TIME",
          "GUID",
          "BYTE_STRING",
          "XML_ELEMENT",
          "NODE_ID",
          "EXPANDED_NODE_ID",
          "QUALIFIED_NAME",
          "LOCALIZED_TEXT"
        ],
        "description": "Data type of the node value"
      },
      "Dimensions": {
        "type": "array",
        "items": {
          "type": "integer"
        },
        "description": "Array dimensions for array data types",
        "maxItems" : 3
      },
      "TimestampPath": {
        "type": "string",
        "description": "Optional JMESPath expression to select timestamp from the input"
      },
      "WarnIfNotPresent": {
        "type": "boolean",
        "description": "Flag to control warning if data is not present in input",
        "default": false
      }
    }
  }
}

```



### OpcuaNodeConfiguration Examples

Minimal configuration. 

```json
 {
     "NodeId": "ns=3;i=1015",
     "DataPath": "@.sources.Conveyor.values.Power.value",
  }
```



Configuration with explicit definition of TimestampPath to use the timestamp at source level instead of value level and an explicit data type definition

```json
 {
      "NodeId": "ns=3;i=1015",
      "DataPath": "@.sources.FluidConveyor.values.Power.value",
      "TimestampPath": "@.sources.Conveyor.timestamp",
   	  "DataType" : "FLOAT"
 }
```



Configuration with explicit definition of TimestampPath to use the timestamp at source level instead of value level

```json
 {
      "NodeId": "ns=3;i=1016",
      "DataPath": "@.sources.Conveyor.values.Temperature.value",
      "Transformation" : "ToCelsius"
 }
```



The `ToCelsius` transformation, which converts the temperature reading to Celsius and truncates it at 2 digits,  is defined in the [transformations](../core/sfc-configuration.md#transformations) section as

```json
  "Transformations" :{
    "ToCelsius" : [
         { "Operator" : "Celsius"},
         { "Operator" : "TruncAt", "Operand" : 2}
    ]
  }
```



## OpcuaCertificateValidationConfiguration

[OpcuaWriterTargetConfiguration](#opcuawritertargetconfiguration) > [CertificateValidation](#certificatevalidation)

The OpcuaCertificateValidationConfiguration class defines the configuration settings for OPC UA certificate validation.

- [Schema](#opcuacertificatevalidationconfiguration-schema)
- [Examples](#opcuacertificatevalidationconfiguration-example)

**Properties:**

- [Active](#active)
- [Directory](#directory)
- [ValidationOptions](#validationoptions)

------

### Active

The Active property is a flag that enables or disables the validation of server certificates.

**Type** : Boolean

When set to true, server certificate validation is enabled.
When set to false, server certificate validation is disabled

------

### Directory

The Directory property specifies the pathname to the base directory where certificates and certificate revocation lists (CRLs) are stored.

**Type** : String

Important notes:

- This directory must exist prior to use
- The adapter will automatically create any required subdirectories if they don't exist

Directory structure

```
[Configured directory name]
   |----- issuers
   |        |---- certs
   |        |---- crl
   |      trusted
   |        |---- certs
   |        |---- crl
   |----- rejected
```

This structure shows:

- A root directory (specified by the Directory property)

- Three main subdirectories:

  - issuers/ - Contains two subdirectories:

    - certs/ - For issuer certificates
    - crl/ - For issuer certificate revocation lists

  - trusted/ - Contains two subdirectories:

    - certs/ - For trusted certificates
    - crl/ - For trusted certificate revocation lists

  - rejected/ - For rejected certificates

------

### ValidationOptions

The ValidationOptions property configures optional certificate validation checks.

 If this property is not set, all validation options are enabled by default.

**Type**: [OpcuaCertificateValidationOptions](#opcuacertificatevalidationoptions-type)



### OpcuaCertificateValidationConfiguration Schema

```json
 {
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for certificate validation",
  "properties": {
    "Active": {
      "type": "boolean",
      "description": "Enable or disable certificate validation",
      "default": true
    },
    "Directory": {
      "type": "string",
      "description": "Directory path for certificate storage and validation"
    },
    "ValidationOptions": {
      "$ref": "#/definitions/ValidationOptions",
      "description": "Options for certificate validation"
    }
  }
}
```

### OpcuaCertificateValidationConfiguration Example

Basic configuration:

```json
{
  "Directory": "./certificates",
  "Active": true
}
```

With validation options:

```json
{
  "Directory": "./certificates",
  "Active": true,
  "ValidationOptions": {
    "ApplicationUri": false,
    "ExtKeyUsageEndEntity": false,
    "HostOrIp": false,
    "KeyUsageEndEntity": false,
    "KeyUsageIssuer": true,
    "Revocation": true,
    "Validity": true
  }
}
```



## OpcuaCertificateValidationOptions type

[OpcuaWriterTargetConfiguration](#opcuawritertargetconfiguration) > [CertificateValidation](#certificatevalidation) > [ValidationOptions](#validationoptions)

The OpcuaCertificateValidationOptions class defines configuration options for certificate validation checks during OPC UA connections.

This class allows you to specify which validation checks should be performed when validating certificates. When a certificate validation option is disabled, that specific check will be skipped during the validation process.

- [Schema](#opcuacertificatevalidationoptions-type-schema)
- [Examples](#opcuacertificatevalidationoptions-type-example)

**Properties:**

- [ApplicationUri](#applicationuri)
- [ExtKeyUsageEndEntity](#extkeyusageendentity)
- [HostOrIp](#hostorip)
- [KeyUsageEndEntity](#keyusageendentity)
- [KeyUsageIssuer](#keyusageissuer)
- [Revocation](#revocation)
- [Validity](#validity)

---

### ApplicationUri

The ApplicationUri property determines whether to validate the Application URI against the Subject Alternative Names in the certificate.

When enabled (true):

- The Application URI from the server's application description will be checked against the ApplicationUri specified in the Subject Alternative Names field of the certificate 
- This validation helps ensure the certificate belongs to the expected application

When disabled (false):

- This specific validation check will be skipped

**Type** : Boolean

**Default** : true

---

### ExtKeyUsageEndEntity

The ExtKeyUsageEndEntity property determines whether to check the Extended Key Usage (EKU) extension in end-entity certificates.

When enabled (true):

- The Extended Key Usage extension must be present in end-entity certificates
- The extension will be validated to ensure proper usage constraints
- This helps ensure the certificate is being used for its intended purpose

When disabled (false):

- The Extended Key Usage extension check will be skipped for end-entity certificates

**Type** : Boolean

**Default** : true

---

### HostOrIp

The HostOrIp property controls whether the host name or IP address must be present and validated in the Subject Alternative Names (SAN) field of the certificate.

When enabled (true):

- Requires the host name or IP address to be present in the certificate's Subject Alternative Names
- Validates that the connection endpoint matches the host name or IP address specified in the SAN
- Helps prevent connection to unauthorized endpoints

When disabled (false):

- Skips the validation of host name or IP address in the Subject Alternative Names

**Type** : Boolean

**Default** : true

---

### KeyUsageEndEntity

The KeyUsageEndEntity property controls the validation of the Key Usage extension for end-entity certificates.

When enabled (true):

- Requires the Key Usage extension to be present in end-entity certificates
- Validates the extension content to ensure proper key usage constraints
- Verifies that the certificate's key is being used for its intended purposes (such as digital signatures, key encipherment, etc.)

When disabled (false):

- Skips the validation check for the Key Usage extension in end-entity certificates

**Type** : Boolean

**Default** : true

---

### KeyUsageIssuer

The KeyUsageIssuer property controls the validation of the Key Usage extension for Certificate Authority (CA) certificates. 

When enabled (true):

- Requires the Key Usage extension to be present in CA certificates
- Validates that the CA certificate has appropriate key usage flags set
- Ensures the CA certificate has proper permissions for signing other certificates
- Verifies the CA certificate is being used within its intended constraints

When disabled (false):

- Skips the validation check for Key Usage extension in CA certificates

**Type** : Boolean

**Default** : true

---

### Revocation

The Revocation property controls whether certificate revocation checking is performed during the validation process.

When enabled (true):

- Checks if certificates have been revoked
- Verifies certificate status using Certificate Revocation Lists (CRLs) 
- Helps ensure that invalid or compromised certificates are not accepted
- Provides an additional security layer by detecting and rejecting certificates that have been explicitly invalidated

When disabled (false):

- Skips all certificate revocation checks
- Will not verify if certificates have been revoked by the issuing authority

**Type** : Boolean

**Default** : true

---

### Validity

The Validity property controls whether to check the certificate's validity period during validation. 

When enabled (true):

- Verifies that the current date/time falls within the certificate's validity period
- Checks both the "Not Before" and "Not After" dates of the certificate
- Ensures that expired certificates or certificates that are not yet valid are rejected
- Helps maintain security by preventing the use of certificates outside their intended timeframe

When disabled (false):

- Skips the certificate validity period check
- Will accept certificates regardless of their expiration status or future validity dates

Note: It's generally recommended to keep this enabled as using expired certificates can pose security risks.

**Type** : Boolean
**Default** : true





### OpcuaCertificateValidationOptions Type Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration options for certificate validation",
  "properties": {
    "ApplicationUri": {
      "type": "boolean",
      "description": "Enable validation of application URI",
      "default": true
    },
    "ExtKeyUsageEndEntity": {
      "type": "boolean",
      "description": "Enable validation of extended key usage for end entity certificates",
      "default": true
    },
    "HostOrIp": {
      "type": "boolean",
      "description": "Enable validation of host name or IP address",
      "default": true
    },
    "KeyUsageEndEntity": {
      "type": "boolean",
      "description": "Enable validation of key usage for end entity certificates",
      "default": true
    },
    "KeyUsageIssuer": {
      "type": "boolean",
      "description": "Enable validation of key usage for issuer certificates",
      "default": true
    },
    "Revocation": {
      "type": "boolean",
      "description": "Enable certificate revocation checking",
      "default": true
    },
    "Validity": {
      "type": "boolean",
      "description": "Enable validation of certificate validity period",
      "default": true
    }
  }
}

```

### OpcuaCertificateValidationOptions Type Example

```json
{
  "ApplicationUri": false,
  "ExtKeyUsageEndEntity": false,
  "HostOrIp": false,
  "KeyUsageEndEntity": false,
  "KeyUsageIssuer": true,
  "Revocation": true,
  "Validity": true
}

```

