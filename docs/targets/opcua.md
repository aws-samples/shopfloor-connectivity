# OPC UA target adapter data models and mapping

The OPC UA (Open Platform Communications Unified Architecture) target adapter exposes data collected from SFC source adapters as an OPC UA model. 
This model can be generated automatically by the adapter, which uses the structure and types of the received data. Alternatively,
you can configure a data model as part of the target configuration, mapping the received data to that model by specifying queries for its
variables to obtain the corresponding values.

There are two different mapping methods available for associating target data with the nodes of an OPC UA data model:

- [Automatic mapping](#automatic-model-mapping): The adapter generates the model based on incoming data structure and types.
- [Query mapping](#query-mapping): Custom models are defined, and queries are used to extract specific values from the incoming data.

## Automatic model mapping


The most straightforward method to expose the data received by the target adapter as an OPC UA model is to allow the 
adapter to  automatically generate the model based on the structure and values of the incoming data.

Below is an example of the target data received by the adapter. The schedule named "ConveyorData" collects data from a single source called "FluidConveyor," from which five values are read.

```json
{
  "schedule": "ConveyorData",
  "serial": "947a28de-d6c5-4e09-ab45-c8821fed89ba",
  "timestamp": "2024-10-29T12:55:59.665789Z",
  "sources": {
    "FluidConveyor": {
      "values": {
        "Power": {
          "value": 230,
          "timestamp": "2024-10-29T12:55:59.664717Z"
        },
        "Speed": {
          "value": 234,
          "timestamp": "2024-10-29T12:55:59.664717Z"
        },
        "Temperature": {
          "value": 242.0,
          "timestamp": "2024-10-29T12:55:59.664717Z"
        },
        "Pressure": {
          "value": 246.0,
          "timestamp": "2024-10-29T12:55:59.664717Z"
        },
        "Flow": {
          "value": 250.0,
          "timestamp": "2024-10-29T12:55:59.664717Z"
        },
        "Torque": {
          "value": 254.0,
          "timestamp": "2024-10-29T12:55:59.664717Z"
        }
      },
      "timestamp": "2024-10-29T12:55:59.664717Z"
    }
  }
}
```

The configuration below illustrates the minimum requirements to use the OPC UA target adapter, utilizing all defaults and enabling automatic mapping of the
data to a dynamically created model.

```json
  "OpcuaTarget": {
    "TargetType": "OPCUA-TARGET"
  }

```

The automatic creation of OPCUA nodes is controlled by the "AutoCreate" setting, which is enabled by default.

Data is now exposed through an automatically generated model, where all nodes are read-only for clients. 
When a client connects to the server through any of the available endpoints, the model becomes viewable, subject to the configured network interfaces, security policies, and modes.

<img src="./img/opcua/opcua-1.png" />

When new nodes are created from the initial or subsequent target data, a data model change event (ns=0;i=2133) is raised for the server source (ns=0;i=2253).

---

When the target data contains metadata at the schedule, source, and/or channel levels, additional folder and variable nodes are created to store these values.

```json
{
  "schedule": "ConveyorData",
  "serial": "65e10488-d15d-46ba-9c1f-3c630a7a6b87",
  "timestamp": "2024-10-29T14:54:20.234465Z",
  "sources": {
    "FluidConveyor": {
      "values": {
        "Power": {
          "value": 230,
          "metadata": {
            "Unit": "Watt"
          },
          "timestamp": "2024-10-29T14:54:20.233361Z"
        },
        "Speed": {
          "value": 234,
          "timestamp": "2024-10-29T14:54:20.233361Z"
        },
        "Temperature": {
          "value": 242.0,
          "timestamp": "2024-10-29T14:54:20.233361Z"
        },
        "Pressure": {
          "value": 246.0,
          "metadata": {
            "Unit": "Bar"
          },
          "timestamp": "2024-10-29T14:54:20.233361Z"
        },
        "Flow": {
          "value": 250.0,
          "timestamp": "2024-10-29T14:54:20.233361Z"
        },
        "Torque": {
          "value": 254.0,
          "metadata": {
            "Unit": "NM"
          },
          "timestamp": "2024-10-29T14:54:20.233361Z"
        }
      },
      "metadata": {
        "Line": "1"
      },
      "timestamp": "2024-10-29T14:54:20.233361Z"
    }
  }
}
```

In the data above, there is metadata at the source level and for three of the six channel values.

<img src="./img/opcua/opcua-2.png" />


To accommodate metadata, an additional node named "Line" is created within the "FluidConveyor" 
source node to store the metadata value. For the three channel values that include metadata, 
a folder node is created instead of a variable node. This folder contains a variable node 
named "Value" for the actual channel value, along with additional variable nodes for the metadata items.

The rule is that when metadata or aggregated values are present for a value, a folder is created for 
that value. If these are not present, a variable node is created.

Full example at [examples/in-process-s7-opcua, config file s7-opcua-auto-create.json](../../examples/in-process-s7-opcua/README.md)

## Query mapping

For query mapping, one or more data models are created for a target. The variable nodes in these models 
include a JMESPath query, named "ValueQuery," in their definitions, which selects values for the variable nodes 
from the target data. Optionally, a transformation can be specified to apply to the selected value before it 
is written to the node.

The DataModels configuration contains the definition for a single custom model named "ConveyorDataModel," 
which has a structure different from the target data. For each variable node in this model, 
a JMESPath query is specified to query the target data. If the query returns a value, the variable node is 
updated with that value.

"Temperature" is configured as a folder node because it includes metadata. This metadata, along with the actual value, is stored as variable nodes within the "MotorTemperature" folder node. Additionally, this variable has a transformation applied to convert the value into degrees Celsius and round it to an integer to match the datatype in the model. The transformation, "ToCelsius," is defined in the "Transformations" section of the SFC configuration.

<details>
<summary>Show Transformations</summary>

```json
  "Transformations" :{
    "ToCelsius" : [
         { "Operator" : "Celsius"},
         { "Operator" : "ToInt"}

    ]
  },
```

</details>

<details>

<summary>Show target data</summary>


```json
{
  "schedule": "ConveyorData",
  "serial": "20eba6be-398c-47c2-bf1b-a7a7f14fb0c0",
  "timestamp": "2024-10-30T17:51:00.191939Z",
  "sources": {
    "FluidConveyor": {
      "values": {
        "Power": {
          "value": 230,
          "timestamp": "2024-10-30T17:51:00.191409Z"
        },
        "Speed": {
          "value": 234,
          "timestamp": "2024-10-30T17:51:00.191409Z"
        },
        "Temperature": {
          "value": 242.0,
          "metadata": {
            "Unit": "Celsius"
          },
          "timestamp": "2024-10-30T17:51:00.191409Z"
        },
        "Pressure": {
          "value": 246.0,
          "timestamp": "2024-10-30T17:51:00.191409Z"
        },
        "Flow": {
          "value": 250.0,
          "timestamp": "2024-10-30T17:51:00.191409Z"
        },
        "Torque": {
          "value": 254.0,
          "timestamp": "2024-10-30T17:51:00.191409Z"
        }
      },
      "timestamp": "2024-10-30T17:51:00.191409Z"
    }
  }
}

```

</details>

The code below shows the "DataModels" section of the OPC UA target adapter configuration. To keep this 
JSON example brief, optional configuration settings for the nodes, except for "DisplayName," are omitted.

The node configurations allow setting the identifier of NodeId by specifying the "Id" setting as a numeric, 
string, or GUID value. Alternatively, you can use "BrowseName" to set a node's browse name. If these fields are 
not specified, the key's value from the model configuration map is used as the default.

**Note that if the schedule, source, or channel name used in the JMESPath query contains non-alphanumeric characters, 
these names should be enclosed in double quotes. In JSON, double quotes need to be escaped with a \ character.**


```json
 "DataModels": {
      "ConveyorDataModel": {
        "DisplayName": "Conveyor DataModel",
        "Folders": {
          "DeviceData": {
            "DisplayName": "Pump Motor Data",
            "Variables": {
              "MotorPower": {
                "DisplayName": "Motor Power",
                "DataType": "INT32",
                "ValueQuery" : "@.sources.FluidConveyor.values.Power.value"
              },
              "MotorSpeed": {
                "DisplayName": "Motor RPM",
                "DataType": "INT32",
                "ValueQuery" : "@.sources.FluidConveyor.values.Speed.value"

              },
              "MotorTorque": {
                "DisplayName": "Motor Torque",
                "DataType": "REAL",
                "ValueQuery" : "@.sources.FluidConveyor.values.Torque.value"
              }
            },
            "Folders": {
              "MotorTemperature": {
                "DisplayName": "Motor Temperature",
                "Variables" :{
                    "Value" :{
                         "DataType" : "INT",
                         "ValueQuery" : "@.sources.FluidConveyor.values.Temperature.value",
                         "Transformation" : "ToCelsius"
                    },
                    "Unit" : {
                      "DataType" : "STRING",
                      "ValueQuery" : "@.sources.FluidConveyor.values.Temperature.metadata.Unit"
                    }
                }
              }
            }
          },
          "PumpData": {
            "DisplayName": "Pump Data",
            "Variables": {
              "PumpFlow": {
                "DisplayName": "Pump Flow",
                "DataType": "REAL",
                "ValueQuery" : "@.sources.FluidConveyor.values.Flow.value"
              },
              "PumpPressure": {
                "DisplayName": "Pump Pressure",
                "DataType": "REAL",
                "ValueQuery" : "@.sources.FluidConveyor.values.Pressure.value"
              }
            }
          }
        }
      }
    }
```

This configuration results in the model below.


<img src="./img/opcua/opcua-3.png" />

---


# OPCUA Target
<br>
<p>OpcuaTargetConfiguration extends the type <a href="../core/target-configuration.md" >TargetConfiguration</a> with specific configuration data for publishing the data through an OPC UA model. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"OPCUA-TARGET"</strong></p>
<br>


## OpcuaTargetConfiguration

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 17%" />
<col style="width: 13%" />
<col style="width: 55%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>
<tr class="even">
<td>DataModels</td>
<td>OPC UA data model definitions</td>
<td>Map[String, <a href="#datamodelconfiguration" >DataModelConfiguration]</a></td>
<td>One or more data models that will be exposed through the OPC UA server to which SFC target data can be mapped. If no models are specified then the
target adapter will build a model based on the SFC target data and values it receives.</td>
</tr>

<tr class="even">  
<td>AutoCreate</td>  
<td>When the value of this value is set to true, the adapter will automatically create nodes for target data elements that are not mapped to a node in the models, or if no models are configured. 
If the value is false, then target data elements for which there is no mapped node in the model, the values will not be stored in the model.</td>  
<td>Boolean</td>  
<td>
Default is true

By setting this value to true, and not specifying any model, the data model is completely built based on the target data received by the adapter.

</td> 
</tr>  
<tr class="odd">  
<td>ServerTcpPort</td>  
<td>TCP port used by the OPC UA server.</td>  
<td>Integer</td>  
<td>

Default is 53530

</td>  

</tr>  
<tr class="even">  
<td>ServerPath</td>  
<td>Server path section for the server endpoints</td>  
<td>String</td>  
<td>Default is "sfc"
</td>
</tr>  

<tr class="odd">  
<td>ServerAnonymousDiscoveryEndPoint</td>  
<td>When set to true a discovery-specific endpoint with no security is provided for each server address. Having these 
endpoints is a good practice and the usage of the /discovery suffix is defined by OPC UA Part 6
</td>  
<td>Boolean</td>  
<td>Default is true</td>  
</tr>  

<tr class="even">  
<td>ServerMessageSecurityModes</td>  
<td>Supported message security modes for OPC UA server</td>  
<td>[String]</td>  
<td>This setting is an array containing one or more of the following values:

- "None"
- "Sign"
- "SignAndEncrypt"

The default value is:

["None", "Sign", "SignAndEncrypt]

</td>  
</tr>  


<tr class="odd">  
<td>ServerSecurityPolicies</td>  
<td>This setting contains the security policies for the OPC UA server</td>  
<td>[String]</td>  
<td>
This setting is an array containing one or more of the following values:

- "None"
- "Basic128Rsa15" : http://opcfoundation.org/UA/SecurityPolicy#Basic128Rsa15
- "Basic256" : http://opcfoundation.org/UA/SecurityPolicy#Basic256
- "Basic256Sha256" : http://opcfoundation.org/UA/SecurityPolicy#Basic256Sha25
- "Aes128Sha256RsaOaep" : http://opcfoundation.org/UA/SecurityPolicy#Aes128_Sha256_RsaOaep

The default value is:

["None", "Basic128Rsa15", "Basic256", "Basic256Sha256"]

</td>  
</tr> 

<tr class="odd">
<td>ServerNetworkInterfaces</td>
<td>Names of the network interfaces that can be used to access the OPC UA server</td>
<td>[String]</td>
<td>The default binds all available network interfaces to the OPC UA server.
</tr>

<tr class="even">
<td>Certificate</td>
<td>Opcua Server certificate configuration</td>
<td><a href="#certificateconfiguration">CertificateConfiguration</a></td>
<td></td>
</tr>

<tr class="odd">
<td>CertificateValidation</td>
<td>Certificate settings for the OPC UA server</td>
<td>CertificateConfiguration</td>
<td>No certificate is configured then a default self-signed certificate will be created.</td>
</tr>

<tr class="even">
<td>CertificateValidation</td>
<td>Certificate validation configuration</td>
<td><a href="#certificatevalidationconfiguration">CertificateValidationConfiguration</a></td>
<td></td>
</tr>


<tr class="odd">
<td>InitValuesWithNull</td>
<td>If set to true, variable nodes are initialized with a null value when no explicit initialization value is specified for the variable.</td>
<td>Boolean</td>
<td>Default is true
</tr>

</tbody></table>


## CertificateConfiguration

Client certificate configuration for OPC UA server

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>CertificateFile</td>
<td>Pathname to pem or pkcs12 certificate file</td>
<td>String</td>
<td></td>
</tr>

<tr class="odd">
<td>PrivateKeyFile</td>
<td>Path name to pem private key file (optional for pkcs12, required for pem)</td>
<td>String</td>
<td></td>
</tr>

<tr class="even">
<td>Alias</td>
<td>Alias to use for pkcs12 certificate files</td>
<td>String</td>
<td>Default is "alias"</td>
</tr>

<tr class="odd">
<td>Password</td>
<td>Password for pkcs12 certificate files</td>
<td>String</td>
<td></td>
</tr>

<tr class="even">
<td>SelfSignedCertificate</td>
<td>Self-signed certificate configuration used to generate a self-signed certificate fot the OPC UA server</td>
<td><a href="#selfsignedcertificateconfiguration">SelfSignedCertificateConfiguration</a></td>
<td></td>
</tr>

<tr class="odd">
<td>Format</td>
<td>Format of the certificate file, can either be "Pem" or "Pkcs12".</td>
<td>String</td>
<td>If not specified the adapter will attempt to determine the type from the filename of the key file.</td>
</tr>

<tr class="even">
<td>ExpirationWarningPeriod</td>
<td>Period in days in which the adapter will generate a daily warning and metrics value before the client certificate expires.</td>
<td>Integer</td>
<td>Default is 30, set to 0 to disable.</td>
</tr>


</tbody>
</table>

[Opcua Target Configuration](#opcua-target)

## SelfSignedCertificateConfiguration


Configuration for generating self-signed certificates

<table style="width:100%;">
<colgroup>
<col style="width: 23%" />
<col style="width: 26%" />
<col style="width: 26%" />
<col style="width: 22%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>

<tr class="even">
<td>CommonName</td>
<td><p>Common name of the certificate</p>
<p>X509 Name CN</p></td>
<td>String</td>
<td>Must be specified</td>
</tr>

<tr class="odd">
<td>Organization</td>
<td>X509 Name O</td>
<td>String</td>
<td></td>
</tr>

<tr class="even">
<td>OrganizationalUnit</td>
<td>X509 Name OU</td>
<td>String</td>
<td>Default is "alias"</td>
</tr>

<tr class="odd">
<td>LocalityName</td>
<td>X509 Name L</td>
<td>String</td>
<td></td>
</tr>

<tr class="even">
<td>StateName</td>
<td>X509 Name ST</td>
<td>String</td>
<td></td>
</tr>

<tr class="odd">
<td>CountryCode</td>
<td>X509 Name C</td>
<td>String</td>
<td></td>
</tr>

<tr class="even">
<td>ValidPeriodInDays</td>
<td>Number of days certificate is valid</td>
<td>Integer</td>
<td>Default is 1095 (=3 years)</td>
</tr>

</tbody>
</table>


If no self-signed certificate configuration is specified, the default settings for the self-signed 
certificate are as follows:

- **Common Name**: "SFC-OPCUA-TARGET-\[hostname\]"
- **DNS Names**: All hostnames for the used network interfaces
- **IP Addresses**: All IP addresses for the used network interfaces
- **Application URI**: "urn:amazonaws:sfc:opcua-target"
- **Organization**: "AWS"
- **Validity Period**: 1000 days


[Opcua Target Configuration](#opcua-target)

## CertificateValidationConfiguration

Configuration for validating certificates

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Active</td>
<td>Flag to set to enable or disable the validation of server certificates</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>

<tr class="odd">
<td>Directory</td>
<td>Pathname to the base directory under which certificates and certificate revocation lists are stored.</td>
<td>String</td>
<td>This directory must exist; subdirectories will be created by the adapter if they do not exist.</td>
</tr>


<tr class="even">
<td>ValidationOptions</td>
<td>Configuration of op optional checks</td>
<td><a href="#certificatevalidationoptions">CertificateValidationOptions</a></td>
<td>When not set then all options are enabled</td>
</tr>


</tbody>
</table>

The directory structure is:

```sh
[Configured directory name]
|----- issuers
|        |---- certs
|        |---- crl
|      trusted
|        |---- certs
|        |---- crl
|----- rejected
```

When a client connects for the first time, its certificate is rejected and stored in the rejected directory. 
Clients gain access when their certificate is moved into the trusted/certs directory.

[Opcua Target Configuration](#opcua-target)

## CertificateValidationOptions

Optional validation options configuration applied by the OPC UA server

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>HostOrIp</td>
<td>Host or IP address must be present in Alternate Subject Names and will be checked</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>

<tr class="odd">
<td>Validity</td>
<td>Check certificate expiry</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>


<tr class="even">
<td>KeyUsageEndEntity</td>
<td>Key usage extension must be present and will be validated for end-entity certificates</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>


<tr class="odd">
<td>ExtKeyUsageEndEntity</td>
<td>Extended key usage extension must be present and will be validated for end-entity certificates</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>

<tr class="odd">
<td>RevocationLists</td>
<td>Revocation list checking</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>

<tr class="odd">
<td>Revocation</td>
<td>Revocation checking</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>


<tr class="even">
<td>ApplicationUri</td>
<td>Check Application description against the ApplicationUri from Subject Alternative Names</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>


</tbody>
</table>


## DataModelConfiguration

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

Top level folder for a configured data model.

Note that all keys in all tabled in a DataModel configuration must be unique.

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Namespace</td>
<td>Namespace for the data model</td>
<td>String</td>
<td>Default is "urn:amazonaws.sfc"</td>
</tr>

<tr class="odd">
<td>Id</td>
<td>Id for the node</td>
<td>String</td>
<td>Optional, if not specified then the key for the model in the <a href="#opcua-target">OPC UA target configuration DataModels</a> table is used
The the value of the id is used as the identifier in the node id for the folder. 

- Id is a number: "ns=[namespace index];**i**= [numeric id]"
- Id is a guid: "ns=[namespace index];g= [guid id]"
- Id is a string : "ns=[namespace index];s= [guid id]"
- 
The value of the namespace index is set by the server when the model is built from the model specification.

Note that all keys in all tabled in a DataModel configuration must be unique.

</td>
</tr>

<tr class="even">
<td>DisplayName</td>
<td>Display name for the folder.</td>
<td>String</td>
<td>Optional, if not specified then the value of the id will be used as the display name</td>
</tr>

<tr class="odd">
<td>BrowseName</td>
<td>Browse name for the folder.</td>
<td>String</td>
<td>Optional, if not specified then the value of the id will be used as the display name</td>
</tr>

<tr class="odd">
<td>Description</td>
<td>Description for the folder.</td>
<td>String</td>
<td></td>
</tr>

<tr class="odd">
<td>Folders</td>
<td>Sub folder nodes to create in this top level folder</td>
<td>Map[String, <a href="foldernodeconfiguration">FolderNodeConfiguration</a>]</td>
<td></td>
</tr>

<tr class="even">
<td>Variables</td>
<td>Variable nodes to create at in this top level folder</td>
<td>Map[String, <a href="variablenodeconfiguration">VariableNodeConfiguration</a>]</td>
<td></td>
</tr>

</tbody>
</table>


[Opcua Target Configuration](#opcua-target)



## FolderNodeConfiguration

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

Folder node in a configured data model.

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>


<tr class="even">
<td>Id</td>
<td>Id for the node</td>
<td>String</td>
<td>Optional; if not specified, the key used in the folder table in the parent folder or data model table is utilized. 
The value of the ID is then used as the identifier in the node ID for the folder.

- Id is a number: "ns=[namespace index];**i**= [numeric id]"
- Id is a guid: "ns=[namespace index];g= [guid id]"
- Id is a string : "ns=[namespace index];s= [guid id]"
-
The value of the namespace index is set by the server when the model is built from the model specification.

Note that all keys in all tabled in a DataModel configuration must be unique.

</td>
</tr>

<tr class="even">
<td>DisplayName</td>
<td>Display name for the folder node.</td>
<td>String</td>
<td>Optional, if not specified then the value of the id will be used as the display name</td>
</tr>

<tr class="odd">
<td>BrowseName</td>
<td>Browse name for the folder node.</td>
<td>String</td>
<td>Optional, if not specified then the value of the id will be used as the display name</td>
</tr>

<tr class="odd">
<td>Description</td>
<td>Description for the folder node.</td>
<td>String</td>
<td></td>
</tr>

<tr class="odd">
<td>Folders</td>
<td>Map with sub folder nodes to create in this folder</td>
<td>Map[String, <a href="foldernodeconfiguration">FolderNodeConfiguration</a>]</td>
<td></td>
</tr>

<tr class="even">
<td>Variables</td>
<td>Map with variable nodes to create at top level folder of model</td>
<td>Map[String, <a href="variablenodeconfiguration">VariableNodeConfiguration</a>]</td>
<td></td>
</tr>

</tbody>
</table>


[Opcua Target Configuration](#opcua-target)




## VariableNodeConfiguration


<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

Variable node in a configured data model.

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>


<tr class="even">
<td>Id</td>
<td>Id for the node</td>
<td>String</td>
<td>Optional; if not specified then the key used in the variable table in the parent folder or data model table is used
The the value of the id is used as the identifier in the node id for the folder. 

- Id is a number: "ns=[namespace index];**i**= [numeric id]"
- Id is a guid: "ns=[namespace index];g= [guid id]"
- Id is a string : "ns=[namespace index];s= [guid id]"
-
The value of the namespace index is set by the server when the model is built from the model specification.

Note that all keys in all tabled in a DataModel configuration must be unique.

</td>
</tr>

<tr class="even">
<td>DisplayName</td>
<td>Display name for the variable node.</td>
<td>String</td>
<td>Optional, if not specified then the value of the id will be used as the display name</td>
</tr>

<tr class="odd">
<td>BrowseName</td>
<td>Browse name for the variable node.</td>
<td>String</td>
<td>Optional, if not specified then the value of the id will be used as the display name</td>
</tr>

<tr class="odd">
<td>Description</td>
<td>Description for the variable node.</td>
<td>String</td>
<td></td>
</tr>

<tr class="even">
<td>DataType</td>
<td>Data type for the node</td>
<td>String</td>
<td>The following data types are supported:

- BOOLEAN
- BYTE, SBYTE (signed byte)
- BYTE_STRING
- DATETIME
- DOUBLE (double)
- REAL (float)
- EXPANDED_NODE_ID
- FLOAT
- INT, INTEGER, INT32 (Int32)
- LOCALIZED_TEXT
- LONG, INT64 (Int64)
- NODE_ID
- QUALIFIED_NAME
- SHORT, INT16 (Int16)
- STRING
- UINT, UINT32, UINTEGER (UInt32)
- UUID
- XML_ELEMENT
- UBYTE,  (unsigned byte)
- ULONG, UINT64 ()
- USHORT, UINT16 (UInt16)
- STRUCT
- VARIANT (note that this type does not support array type)

Note that for selected types alternative names can be used.

The '_' in the type names are used for clarity and can be omitted.

Type names are not case-sensitive.

</td>
</tr>

<tr class="odd">
<td>InitValue</td>
<td>Initial value for a variable node when created</td>
<td>Any</td>
<td>
If no initial value is specified, the value of the node will be set to null 
when the "InitValuesWithNull" setting for the target is set to true, or left 
undefined when it is false. The value must match the data type for the node. 
If the value is an array, as described in "ArrayDimensions," the dimensions of 
the initial value must match those of the variable. Initial array values can be 
specified with up to three dimensions.
</td>
</tr>

<tr class="odd">
<td>ArrayDimensions</td>
<td>Dimensions of an array value</td>
<td>[Int]</td>
<td>
The array contains the sizes for the dimensions for an array variable;
e.g.

[3] : Value array of 3 element
[3,2] : Value array of 3 by 2 elements
</td>
</tr>

<tr class="even">
<td>ValueQuery</td>
<td><a href="https://jmespath.org">JMESPath Query</a> to select the value for a variable node from the target data</td>
<td>String</td>
<td>

The value must be a valid JMESPath query https://jmespath.org.

e.g. @.sources.<SourceName>.values.<ChannelName>.value

Note that if the source or channel name contain non-alphanumeric characters, then these elements must be quoted.
The quoted characters must be escaped with a \ character in the JSON configuration.

</td>
</tr>

<tr class="odd">
<td>TimestampQuery</td>
<td>JMESPath Query to select the timestamp for a variable node from the target data</td>
<td>String</td>
<td>

The value must be a valid JMESPath query https://jmespath.org.

e.g. @.sources.<SourceName>.values.<ChannelName>.timestamp

Note that if the source or channel name contain  characters, not in  the ranges A-Z, a-z, 0-9, then these must be quoted.
These quoted mst be escaped with a '\' character in the JSON configuration.

If no query is specified in addition to the ValueQuery described above then the adapter will try to build a query from that ValueQuery by replacing the 
"value" section of the query with  "timestamp". If that query does not return a timestamp then either the source or schedule timestamp will be used.

</td>
</tr>


<tr class="even">
<td>Transformation</td>
<td>Transformation to be applied to the value obtained by the ValueQuery before it is written to the variable node.</td>
<td>String</td>
<td>
The specified transformation must be the ID of an existing transformation in the "Transformations" section of the SFC 
configuration. This transformation can be specifically applied when writing the value to OPC UA variable nodes, in addition 
to the transformation that can be applied to a channel, which is used when the value is read from the source adapter.

</td>
</tr>

</tbody>
</table>



[Opcua Target Configuration](#opcua-target)


[^top](#opcua-target)