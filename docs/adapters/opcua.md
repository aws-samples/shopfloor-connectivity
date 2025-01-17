# OPCUA Protocol adapter

Configuration types for the OPCUA protocol adapter and contains the extensions and specific configuration types.

- [OPCUA Alarm and Events types](#opcua-alarm-and-event-types)
- [OPCUA security profiles and certificates](#opcua-security-profiles-and-certificates)


**Configuration**


- [OpcuaSourceConfiguration](#opcuasourceconfiguration)
- [OpcuaNodeChannelConfiguration](#opcuanodechannelconfiguration)
- [OpcuaNodeChangeFilter](#opcuanodechangefilter)
- [OpcuaAdapterConfiguration](#opcuaadapterconfiguration)
- [OpcuaServerProfileConfiguration](#opcuaserverprofileconfiguration)
- [OpcuaEventTypeConfiguration](#opcuaeventtypeconfiguration)
- [OpcuaServerConfiguration](#opcuaserverconfiguration)



## OPCUA Alarm and Event types

The OPCUA protocol adapter supports the collection of data from events and alarms. This can be done by adding the event name or identifier of the alarm or event type to a node channel configuration. The name of the event can be the name of the OPCUA alarms from the model at https://reference.opcfoundation.org/Core/Part9/v105/docs/5.8, or an OPCUA event from the model at https://reference.opcfoundation.org/Core/Part3/v104/docs/9.1

The adapter will monitor nodes with a specified event type the adapter and add the received to the collected data for the OPCUA source, using the name for that node. The event data consist of a map of properties, which are based on the type of the event used for the node. As multiple events may be received during a read interval, the value of these event nodes is always of type array, containing one or more maps with the event data. The maximum number of items that can be collected is configurable. If more events are received the oldest event is omitted from the output.

The OPCUA adapter can operate in Polling or Subscription mode to collect data values from the OPCUA server. For events the adapter will use a subscription with monitored event nodes, independent of in which mode the adapter collects the data nodes.

As industry specific companion specification define additional event and alarm types, SFC allows configuration of additional types, which are grouped in server profiles. An event is configured by a given name, the node identifier of the event type (e.g., ns=99;i=9999), and a list of properties for that event with their qualified names consisting of a namespace and browse name (e.g., 9:Property1)

In order to reduce the configuration for these events it is possible to inherit from other events in the profile or the types defined in the OPCUA specifications, by specifying that that type by its type name or node identifier. All properties defined in the type a type inherits from are added, as well as all other properties in types up in the type hierarchy.

The names or node identifiers can be used as event types in the nodes for which event and alarm data needs to be collected. The event name is used to:

- Filter the evens raised by the node, if multiple event types need to be received then a channel needs to be configured for each of these event types.
- Collect the values from the received events as defined for that event type.

As for data nodes selectors, it is possible to use a selector to filter specific properties from the events and add additional metadata at node level. Index ranges and node change filters are not supported for events data.

Example of mixed OPCUA source nodes for an alarm event and two data nodes.

```json
"Channels": {
  "LevelAlarm": {
     "Name": "LevelAlarm",
     "NodeId": "ns=6;s=MyLevel.Alarm",
     "EventType": "ExclusiveLevelAlarmType"
  },
  "SimulationRandom": {
     "Name": "Random",
     "NodeId": "ns=3;i=1002"
   }, 
     "SimulationCounter": {
     "Name": "Counter",
     "NodeId": "ns=3;i=1001"
  }
}
```

The collected data from the event and data nodes is shown below.

```json
{
  "OPCUA-SOURCE": {
    "values": {
      "Random": {
        "value": 0.675842,
        "timestamp": "2023-03-15T11:34:42Z"
      },
      "Counter": {
        "value": 0,
        "timestamp": "2023-03-15T11:34:42Z"
      },
      "LevelAlarm": {
        "value": [
          {
            "HighHighLimit": 90.0,
            "HighLimit": 70.0,
            "LowLimit": 30.0,
            "LowLowLimit": 10.0,
            "InputNode": "ns=0;i=0",
            "Retain": true,
            "EventId": [0, 0, 0, 0, 0, 0, 6, 72, 0, 0, 0, 0, 0, 0, 6, 71],
            "EventType": "ns=0;i=9482",
            "SourceNode": "ns=6;s=MyLevel",
            "SourceName": "MyLevel",
            "Time": "2023-03-15T11:34:42.328Z",
            "ReceiveTime": "2023-03-15T11:34:42.328Z",
            "Message": "Level exceeded",
            "Severity": 500
          }
        ],
        "timestamp": "2023-03-15T11:34:42.848Z"
      }
    },
    "timestamp": "2023-03-15T11:34:42.848Z"
  }
}
```

[^top](#opcua-protocol-adapter)

The snippet below shows the configuration of an OPCUA adapter with a profile named "CustomEventsProfile" that defines two additional event types, "CustomEventType1" and "CustomEventType2", each with two properties. CustomEventType1 inherits from the OPCUA defined BaseEventType type and will contain all properties from that class in addition to the two properties defined for the event. CustomEventType2 will inherit from and therefore contain all properties from CustomEventTYpe1 and the two properties defined for the event.

Sources are configured to read from adapter "OPCUA" and server "OPCUA-SERVER", which has a service profile set to " CustomEventsProfile", can use both defined event types in addition to all OPCUA defined event types, as event type for their nodes to collect the data in the properties for these events.

```json
{
  "ProtocolAdapters": {
    "OPCUA": {
      "AdapterType": "OPCUA",
      "OpcuaServers": {
        "OPCUA-SERVER": {
          "Address": "opc.tcp://localhost",
          "Path": "OPCUA/SimulationServer",
          "Port": 53530,
          "ServerProfile": "CustomEventsProfile"
        }
      },
      "ServerProfiles": {
        "CustomEventsProfile": {
          "EventTypes": {
            "CustomEventType1": {
              "NodeId": "ns=9;i=9000",
              "Properties": [
                "99:CustomProperty1",
                "99:CustomProperty2"
              ],
              "Inherits": "BaseEventType"
            },
            "CustomEventType2": {
              "NodeId": "ns=9;i=9001",
              "Properties": [
                "99:CustomProperty3",
                "99:CustomProperty4"
              ],
              "Inherits": "CustomEventType1"
            }
          }
        }
      }
    }
  }
}
```




# OPCUA security profiles and certificates

In order to secure the traffic between the OPCUA protocol adapter and the OPCUA Server it can be signed and encrypted using certificates.

In the configuration for the OPCUA server in the adapter the security policies can be used by setting the [SecurityPolicy](#securitypolicy) of the server to any of the following policy names:

| Name                | Sign / Encrypt   | Security Policy                                                  |
|---------------------|------------------|------------------------------------------------------------------|
| None                |                  |                                                                  |
| Basic128Rsa15       | Sign             | http://opcfoundation.org/UA/SecurityPolicy#Basic128Rsa15         |
| Basic256            | Sign and encrypt | http://opcfoundation.org/UA/SecurityPolicy#Basic256              |
| Basic256Sha256      | Sign and encrypt | http://opcfoundation.org/UA/SecurityPolicy#Basic256Sha25         |
| Aes128Sha256RsaOaep | Sign             | http://opcfoundation.org/UA/SecurityPolicy#Aes128_Sha256_RsaOaep |

The Certificate section of the OPCUA Server contains the settings for the certificate used by the client of the adapter.

The CertificateName contains the filename of the client certificate, which can be in pem or Pkcs12 format. If a pem format file is used, additionally the name of the corresponding private key file must be set in PrivateKeyFile. This is not required for PFX certificates as this type of file is a container which holds the certificate and private key. If the PFX file is password protected then the Password attribute must be set. (Avoid clear passwords in the configuration, use placeholders for secrets obtained from AWS Secrets manager instead). If an alias is used in the PFX container the value of that alias must be set in the Alias attribute of the configuration.

The type of the certificate can be determined by the prefix of the filename (either ".pem" or ".pfx") optionally followed by ".cer", ".cert" or ".crt". If another extension is used then the type can be explicitly set by setting the server configuration's Format attribute to either "Pem" or "Pkcs12".

If either the PEM or PFX certificate file does not exist, it is possible to let the OPCUA adapter generate a self-signed certificate and store that certificate in the specified file name. For PEM format certificates the name of the private key file must be set as well. If the private key file does exist it will be used to generate a pem or Pkcs12 formatted certificate. If it does not exist the keypair is generated and, if a pem formatted certificate is generated, stored in the specified file. For Pkcs12 formatted certificates the key will be stored with the certificate in the pfx file.

To enable the generation of these self-signed certificates the SelfSignedCertificate section must be present in the server configuration. In this section the CommonName of the certificate must be set and optionally the X509Name fields for Organization, OrganizationalUnit, LocalityName, StateName and CountryCode. The default period in which the generated certificate is valid start from (notBefore) the current date to an end date (notAfter) of the current date plus 3 years. The duration in which the certificate is valid can be modified by setting the ValidPeriodDays attribute.

A number of days can be set in ExpirationWarningPeriod. At startup and at midnight the OPCUA adapter will check if the client certificate will expire within that period and generate a warning and metric for an expiring (or expired) certificate.

If the OPCUA server does validate the DNS name or the DNS name and IP addresses of the client must be present in the certificate Subject Alternative Names. A list of IP Addresses and DNS names can be set in the SelfSignedCertificate IpAddresses and DnsNames attributes. If these are not set then all known IP addresses and DNS name of the host on which the OPCUA adapter generates the certificate will be set as Subject Alternative Names. To exclude the IP addresses and DNS names from the generated certificate, specify an empty list for these attributes.

If the certificate contains an ApplicationUri as an Alternative Subject Name, the Application Description used by the OPCUA client will be the name part from that URI. For self-signed certificates the alternative subject name for the application uri will be set to urn:aws-sfc-opcua@[hostname]. (Application Name used by client is aws-sfc-opcua@[hostname]). OPCUA servers van validate the application name used by the client against the ApplicationUri from the certificate.

*NOTE: The certificate used by the client must be trusted by the OPCUA server, for which the procedure depends on the used sever. As an example, when a ProSys OPCUA (simulation) server is used, an unknown certificate is rejected but stored on the server, where it can be manually marked through the UI as trusted.*

The OPCUA adapter can also validate the certificate it receives from the OPCUA server. It will validate it using a set of know trusted certificates and issuers and certificate revocation lists (CRL). To enable the validation a CertificateValidation section must be present in the configuration. The Directory attribute in this section is set to the location where the certificates and revocation lists are stored in a number of subdirectories, which will be created by the adapter if these do not exist.

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

The certs directories contain trusted certificates and certificates of issuers in order to validate signed certificates. The crl directories contain the certification revocation lists. When a server certificate does not pass the validation it will be stored in PEM format in the rejected directory, from where it can after inspection be moved into the trusted certificate directory.

A number of optional checks (see https://reference.opcfoundation.org/v104/Core/docs/Part4/6.1.3/) can be configured in a ValidationOptions section in the CertificateValidation section. It can contain the following attributes that can be set to a value of false to disable the optional validation, which by default are all enabled.

Validation options:

- HostOrIP: End certificates must contain their host name or IP address in the Subject Alternate Names which will be validated
- Validity: Checks certificate expiry
- KeyUsageEndEntity: Key usage extensions for end entity certificates must be present and will be checked.
- ExtKeyUsageEndEntity: : Extended key usage extensions for end entity certificates must be present and will be checked.
- KeyUsageIssuer: Key usage extensions must be present and will be checked for CA certificates.
- Revocation: Revocation will be checked against CLRs.
- ApplicationUri: Checks the Application name in the Subject Alternative Names against the Application description.

Example of OPCUA server configuration using Basic256Sha256 security profile for signed and encrypted traffic using a X509 certificate and private key, which can be generated by the adapter as a self-signed certificated which is valid for 365 days. A daily warning and metric value will be generated staring 30 days before the certificate expires. Server certificates will be checked using certificates and certificate revocation lists stored in subdirectories under the specified base directory for that server.

```json
  "OPCUA-SERVER-1": {
    "Address": "opc.tcp://myserver.com",
    "Path": "OPCUA/SimulationServer",
    "Port": 53530,
    "SecurityPolicy": "Basic256Sha256",
    "CertificateValidation": {
      "Directory": "/etc/certificates/opcua1 ",
      "ValidationOptions": {
        "HostOrIP": true,
        "Validity": true,
        "KeyUsageEndEntity": true,
        "ExtKeyUsageEndEntity": true,
        "KeyUsageIssuer": true,
        "Revocation": true,
        "ApplicationUri": true
      }
    }
  },
  "Certificate": {
    "CertificateFile": "/etc/certificates/certificate.pem",
    "PrivateKeyFile": "/etc/certificates/ /private-key.pem",
    "ExpirationWarningPeriod": 30,
    "SelfSignedCertificate": {
      "CommonName": "OPCUA-CONNECTOR",
      "Organization": "AWS",
      "OrganizationalUnit": "AIP",
      "LocalityName": "AMS",
      "StateName": "NH",
      "CountryCode": "NL",
      "ValidPeriodDays": 365
    }
  }

```


---

## OpcuaSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) >  [Source](../core/source-configuration.md) 



Source configuration for the OPCUA protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type.

- [Schema](#opcuasourceconfiguration-schema)
- [Examples](#opcuasourceconfiguration-examples)

**Properties:**
- [AdapterOpcuaServer](#adapteropcuaserver)
- [Channels](#channels)
- [EventQueueSize](#eventqueuesize)
- [EventSamplingInterval](#eventsamplinginterval)
- [SourceReadingMode](#sourcereadingmode)
- [SubscribePublishingInterval](#subscribepublishinginterval)

---
### AdapterOpcuaServer
Server Identifier for the OPCUA server to read from. This referenced server must be present in the OpcuaServers section of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a server in the OpcuaServers section of the OPCUA adapter used by the source.

---
### Channels

The element is a map indexed by the channel identifier.

**Type**: Map[String,OpcuaNodeChannelConfiguration]

At least 1 channel must be configured.

---
### EventQueueSize
Queue size for events that can be received in a reading interval. More events are received, the oldest events are discarded.

**Type**: Integer

Default is 10

---
### EventSamplingInterval
Sampling interval for events in milliseconds

**Type**: Integer

Default is 0 (0 stands for best effort cyclic rate that the Server uses to sample the item from its source)

---
### SourceReadingMode
Mode for reading values from OPCUA server.

- "Subscription": connector will create a subscription and will monitor the node items configured in the channels for the source. When reading from the adapter in this mode, only items that have been changed in the schedule interval period will be returned, except for the initial read that will return all monitored items.
- "Polling", the connector will batch-read all nodes configured in the channels for the source with the interval defined in the schedule.


**Type**: String
Values can be "Subscription" or "Polling".

Default is "Subscription".

When the source has any nodes that collect data for alarm or event, these nodes are always read in subscription mode, even if the mode is set to Polling.
Data nodes are always read in the specified mode.

---
### SubscribePublishingInterval
Time in milliseconds that will be used as the SubscribePublishingInterval when creating the subscription with the server when reading in subscription mode. By default, the interval of the schedule is used. An OPCUA server might time out the subscription if this period is too long, in which case the SubscribePublishingInterval can be explicitly set to a shorter period to avoid the subscription timeout.

**Type**: Integer



[^top](#opcua-protocol-adapter)

### OpcuaSourceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for OPC UA source",
  "allOf": [
    {
      "$ref": "#/definitions/SourceConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AdapterOpcuaServer": {
          "type": "string",
          "description": "Reference to the OPC UA server configuration in the adapter"
        },
        "Channels": {
          "type": "object",
          "description": "Map of channel configurations",
          "additionalProperties": {
            "$ref": "#/definitions/OpcuaNodeChannelConfiguration"
          }
        },
        "EventQueueSize": {
          "type": "integer",
          "description": "Size of the event queue"
        },
        "EventSamplingInterval": {
          "type": "integer",
          "description": "Sampling interval for events in milliseconds"
        },
        "SourceReadingMode": {
          "type": "string",
          "description": "Reading mode for the source",
          "enum": ["Polling", "Subscription"],
          "default" : "Subscription"
        },
        "SubscribePublishingInterval": {
          "type": "integer",
          "description": "Publishing interval for subscriptions in milliseconds",
          "minimum": 0
        }
      },
      "required": ["AdapterOpcuaServer", "Channels"]
    }
  ]
}


```

### OpcuaSourceConfiguration Examples

```json
{
  "Name": "ProductionLine1",
  "SourceReadingMode" : "Subscription",
  "ProtocolAdapter": "OpcuaAdapter",
  "AdapterOpcuaServer" : "OpcuaServer1",
  "Channels": {
    "Temperature": {
      "Name": "temperature",
      "NodeId": "ns=2;i=1234",
      "Description": "Temperature sensor reading"
    },
    "Pressure": {
      "Name": "pressure",
      "NodeId": "ns=2;i=1235",
      "Description": "Pressure sensor reading"
    }
  }
}
```

## OpcuaNodeChannelConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#channels) > [Channel](../core/channel-configuration.md)



The OpcuaNodeChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the OPCUA protocol adapter.

- [Schema](#opcuanodechannelconfiguration-schema)
- [Examples](#opcuanodechannelconfiguration-examples)


**Properties:**
- [EventSamplingInterval](#eventsamplinginterval)
- [EventType](#eventtype)
- [IndexRange](#indexrange)
- [NodeChangeFilter](#nodechangefilter)
- [NodeId](#nodeid)
- [Selector](#selector)

---
### EventSamplingInterval
Sampling interval for events in milliseconds. Use this value to specify a specific interval for this node which overrides the interval at source level.

**Type**: Integer

Default is 0

---
### EventType
For collecting data from event or alarm nodes the type of the event must be specified. This can either be the name of the event (e.g., BaseEventType) or the node identifier (e.g., ns=0;i=17).

**Type**: String (name of the event or node identifier)

Valid OPCUA defined event and alarm names can be found at https://reference.opcfoundation.org/Core/Part9/v105/docs/5.8, and https://reference.opcfoundation.org/Core/Part3/v104/docs/9.1

If an event type is used which is not an OPCUA or server profile defined event type a warning is generated and the OPCUA defined "BaseEventType" is used.

If a server profile has been defined and used for the server the source for the channel is reading from, the names and identifiers for event types in that profile can be used as well.
The event type is used to filter the events that are raised by a node and to determine the values that can be read from the event. To receive multiple event types from a node, separate channels need to be configured for each event type.

---
### IndexRange
Range to read subsets from array values.

**Type**: String

If not set all values from an array are read. For syntax see https://reference.opcfoundation.org/v104/Core/docs/Part4/7.22/

---
### NodeChangeFilter
Change filter used in subscription for node that defines the conditions when a value change must be reported.

**Type**: [OpcuaNodeChangeFilter](#opcuanodechangefilter)

Optional

---
### NodeId
A string containing the id of the node to read the value from or to monitor.

**Type**: String

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


---
### Selector
Evaluate a JMESpath query against the value of a structured data type and returns the result.
The selector can be used to restructure or select values from structured data types.

**Type**: String

Parameter: JMESPath expression, see https://jmespath.org/

[^top](#opcua-protocol-adapter)

### OpcuaNodeChannelConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for OPC UA node channel",
  "allOf": [
    {
      "$ref": "#/definitions/NodeChannelConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "EventSamplingInterval": {
          "type": "integer",
          "description": "Sampling interval for events in milliseconds"
        },
        "EventType": {
          "type": "string",
          "description": "Type of event to monitor"
        },
        "IndexRange": {
          "type": "string",
          "description": "Index range for array elements",
          "pattern": "^\\d+:\\d+$|^\\d+$"
        },
        "NodeChangeFilter": {
          "$ref": "#/definitions/OpcuaNodeChangeFilter",
          "description": "Filter configuration for node value changes"
        },
        "NodeId": {
          "type": "string",
          "description": "OPC UA node identifier",
          "pattern": "^(ns=\\d+;)?(i|s|g|b)=[^;]+$"
        },
        "Selector": {
          "type": "string",
          "description": "Selector for specific elements or properties"
        }
      },
      "required": ["NodeId"]
    }
  ]
}

```

### OpcuaNodeChannelConfiguration Examples

Basic node configuration:

```json
{
  "Name": "Temperature",
  "NodeId": "ns=2;i=1234",
  "Description": "Temperature sensor reading"
}
```



Node with change filter:

```json
{
  "Name": "Pressure",
  "NodeId": "ns=3;s=Pressure_Sensor_01",
  "NodeChangeFilter": {
    "Type": "Percent",
    "Value": 5.0
  },
  "Description": "Pressure sensor with 5% change filter"
}
```

Event monitoring configuration:

```json
{
  "Name": "AlarmEvent",
  "NodeId": "ns=2;i=1000",
  "EventType": "AlarmType",
  "EventSamplingInterval": 1000,
  "Description": "Equipment alarm monitoring"
}
```



Array element monitoring:

```json
{
  "Name": "VibrationArray",
  "NodeId": "ns=4;s=VibrationSensors",
  "IndexRange": "0:3",
  "Description": "First 4 elements of vibration sensor array"
}
```



## OpcuaNodeChangeFilter

[OpcuaNodeChannel](#opcuanodechannelconfiguration) > [NodeChangeFilter](#nodechangefilter)



- [Schema](#opcuanodechangefilter-schema)
- [Examples](#opcuanodechangefilter-examples)

**Properties:**

- [Type](#type)
- [Value](#value)



---
### Type

- "Absolute": Value is absolute value change.
- "Percent": Value is data change in percent.

**Type**: String

---
### Value
Data change value

**Type**: Double

Default is 0.0

### OpcuaNodeChangeFilter Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for OPC UA node change filter",
  "properties": {
    "Type": {
      "type": "string",
      "description": "Type of the change filter",
      "enum": ["Absolute", "Percent"],
      "default": "Absolute"
    },
    "Value": {
      "type": "number",
      "description": "The value for the change filter"
    }
  },
  "required": ["Value"]
}

```

### OpcuaNodeChangeFilter Examples



 Basic absolute change filter:

```json
{
  "Value": 10.0
}
```

Percentage 5% change filter:

```json
{
  "Type": "Percent",
  "Value": 5.0
}
```

Explicit absolute change filter:

```json
{
  "Type": "Absolute",
  "Value": 2.5
}
```



[^top](#opcua-protocol-adapter)

## OpcuaAdapterConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [ProtocolAdapters](../core/sfc-configuration.md#protocoladapters) > [Adapter](../core/protocol-adapter-configuration.md) 



OpcuaAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the OPCUA Protocol adapter.

- [Schema](#opcuaadapterconfiguration-schema)
- [Examples](#opcuaadapterconfiguration-examples)

**Properties:**

- [OpcuaServers](#opcuaservers)
- [ServerProfiles](#serverprofiles)

---
### OpcuaServers
Opcua servers configured for this adapter. The opcua source using the adapter must have a reference to one of these in its AdapterOpcuaServer attribute.

**Type**: Map[String,[OpcuaServerConfiguration](#opcuaserverconfiguration)]

---
### ServerProfiles
Profiles configured for this adapter. Servers in this adapter can have a reference to one if its profiles in their ServerProfile attribute.

**Type**: Map[String,[OpcuaServerProfileConfiguration](#opcuaserverprofileconfiguration)]

### OpcuaAdapterConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for OPC UA adapter",
  "allOf": [
    {
      "$ref": "#/definitions/AdapterConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "AdapterType" : "OpcusAdapterType",
        "OpcuaServers": {
          "type": "object",
          "description": "Map of OPC UA server configurations",
          "additionalProperties": {
            "$ref": "#/definitions/OpcuaServerConfiguration"
          },
          "minProperties": 1
        },
        "ServerProfiles": {
          "type": "object",
          "description": "Map of OPC UA server profile configurations",
          "additionalProperties": {
            "$ref": "#/definitions/OpcuaServerProfileConfiguration"
          }
        }
      },
      "required": ["OpcuaServers"]
    }
  ]
}

```



### OpcuaAdapterConfiguration Examples

```json
{
  "AdapterType" : "OpcuaAdapterType",
  "OpcuaServers": {
    "Server1": {
      "Address": "site1.company.com",
      "Port": 4840
    },
    "Server2": {
      "Address": "site2.company.com",
      "Port": 4840
    },
  }
}

```


```json
{
  AdapterType" : "OpcuaAdapterType",
  "OpcuaServers": {
    "Server1": {
      "Address": "site1.company.com",
      "Port": 4840,
      "ServerProfile" : "StandardProfile"
    },
    "Server2": {
      "Address": "site2.company.com",
      "Port": 4840,
      "ServerProfile" : "StandardProfile"
    },
  },
  "ServerProfiles": {
    "StandardProfile": {
      "EventTypes": {
        "ProcessEvent": {
          "NodeId": "ns=2;s=ProcessEventType",
          "Properties": ["ProcessId", "Value", "Timestamp", "Quality"]
        },
        "SystemEvent": {
          "NodeId": "ns=2;s=SystemEventType",
          "Properties": ["EventId", "Severity", "Message"]
        }
      }
    }
  }
}

```



[^top](#opcua-protocol-adapter)



## OpcuaServerProfileConfiguration

[OpcuaAdapter](#opcuaadapterconfiguration) > [Servers](#opcuaservers) > [OpcuaServer](#opcuaserverconfiguration) > [OpcuaServer](#opcuaserverconfiguration) > [ServerProfile](#serverprofile)

- [Schema](#opcuaserverprofileconfiguration-schema)
- [Examples](#opcuaserverprofileconfiguration-examples)

**Properties:**

- [EventTypes](#eventtypes)

  

---
### EventTypes
Additional event types that can be used for a server,

**Type**: Map[ String,  [OpcuaEvenTypeConfiguration](#opcuaeventtypeconfiguration)]

### OpcuaServerProfileConfiguration Schema

```json

{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for OPC UA server profile",
  "properties": {
    "EventTypes": {
      "type": "object",
      "description": "Map of event type configurations",
      "additionalProperties": {
        "$ref": "#/definitions/OpcuaEventTypeConfiguration"
      },
      "minProperties": 1
    }
  },
  "required": ["EventTypes"]
}
```

### OpcuaServerProfileConfiguration Examples

```json
{
  "EventTypes": {
    "CustomEventType1": {
      "NodeId": "ns=9;i=9000",
      "Properties": [
        "99:CustomProperty1",
        "99:CustomProperty2"
      ],
      "Inherits": "BaseEventType"
    },
    "CustomEventType2": {
      "NodeId": "ns=9;i=9001",
      "Properties": [
        "99:CustomProperty3",
        "99:CustomProperty4"
      ],
      "Inherits": "CustomEventType1"
    }
  }
}
```

[^top](#opcua-protocol-adapter)



## OpcuaEventTypeConfiguration

[OpcuaAdapter](#opcuaadapterconfiguration) > [ServerProfiles](#serverprofiles) > [EventTypes](#eventtypes)



- [Schema](#opcuaeventtypeconfiguration-schema)
- [Examples](#opcuaeventtypeconfiguration-examples)

**Properties:**
- [Inherits](#inherits)
- [NodeId](#nodeid)
- [Properties](#properties)

---
### Inherits
Event type to inherit from.
When this attribute is set then all the properties of the referred type (and types that type inherits from) will be added to the properties of the event.
This field can include the name or node identifier of an OPCUA defined event type, or an event in the same profile.

**Type**: String

Optional.

Must refer to and existing OPCUA or other vent type in the same profile.



---
### NodeId
Node identifier for the event type.
This identifier must match the identifier used for the event on the OPCUA server.
It can be used instead of the event name to specify the event type for a node to read in the channels of a source, or as the type to inherit from in other events in the same profile.

**Type**: String

Required

Syntax is 

`ns=namespaceindex;type=value`

e.g., `ns=0;i=9000`

---
### Properties
Properties defined for the event type. Each property is defined as a string which is a Qualified name, consisting of the namespace for that property and the browse name. The format is ns:browsename

**Type**: List of String

Required, an at least one property must be defined.

### OpcuaEventTypeConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for OPC UA event type",
  "properties": {
    "Inherits": {
      "type": "string",
      "description": "The node ID of the parent event type to inherit from"
    },
    "NodeId": {
      "type": "string",
      "description": "The node ID of the event type"
    },
    "Properties": {
      "type": "array",
      "description": "List of property names to monitor for this event type",
      "items": {
        "type": "string"
      },
      "minItems": 1
    }
  },
  "required": ["NodeId", "Properties"]
}

```

### OpcuaEventTypeConfiguration Examples

```json
{
  "NodeId": "ns=9;i=9000",
  "Properties": [
    "99:CustomProperty1",
    "99:CustomProperty2"
  ],
  "Inherits": "BaseEventType"
}
```

[^top](#opcua-protocol-adapter)



## OpcuaServerConfiguration

[OpcuaAdapter](#opcuaadapterconfiguration) > [OpcuaServers](#opcuaservers)



- [Schema](#opcuaserverconfiguration-schema)
- [Examples](#opcuaserverconfiguration-examples)

**Properties:**
- [Address](#address)
- [Certificate](#certificate)
- [CertificateValidation](#certificatevalidation)
- [ConnectTimeout](#connecttimeout)
- [ConnectionWatchdogInterval](#connectionwatchdoginterval)
- [MaxChunkCount](#maxchunkcount)
- [MaxChunkSize](#maxchunksize)
- [MaxMessageSize](#maxmessagesize)
- [Path](#path)
- [Port](#port)
- [ReadBatchSize](#readbatchsize)
- [ReadTimeout](#readtimeout)
- [SecurityPolicy](#securitypolicy)
- [ServerProfile](#serverprofile)
- [WaitAfterConnectError](#waitafterconnecterror)
- [WaitAfterReadError](#waitafterreaderror)

---
### Address
Address of the OPCUA server

**Type**: String

---
### Certificate
Client certificate configuration

**Type**: [CertificateConfiguration](../core/certificate-configuration.md)

---
### CertificateValidation
Certificate validation configuration

**Type**: [CertificateValidationConfiguration](#opcuacertificatevalidationconfiguration)

---
### ConnectTimeout
Timeout in milliseconds connecting to the server

**Type**: Integer

Default is 10000, the minimum value is 1000

---
### ConnectionWatchdogInterval
Interval period in milliseconds for checking the server connection when the source is reading in Subscription reading mode. It will check the connection by trying to read the server status of the OPCUA server. This is needed in case the connection to a (stopped) server is lost, which will not be detected by a client that uses a subscription to obtain server values.

**Type**: Integer

Default is 1000
Set to 0 to disable the watchdog

---
### MaxChunkCount
The maximum number of chunks that a message can break down into

**Type**: Integer

Default is (MaxMessageSize / MaxChunkSize) * 2. More than chunks than constitute MaxMessageSize are needed because of overhead when constructing chunks; not all the chunk size is dedicated to message bytes.

---
### MaxChunkSize
The maximum size of a single chunk of a message in bytes

**Type**: Integer

Default is 65535 (64KB)
Min is 8196 (8KB)
Max is 2,147,483,639 (MaxInt-8 is approx. 2048GB)

---
### MaxMessageSize
The maximum message size in bytes

**Type**: Integer

Default is 2,097,152 (2MB)
Min is 8196 (8 KB)
Max is 2,147,483,639 (MaxInt-8 is approx. 2048GB)

---
### Path
Server path or name

**Type**: String

The connection address that will be used is `<Address>:<Port>[/<Path>]`

Optional

---
### Port
OPCUA server port

**Type**: Integer

Default is 53530

---
### ReadBatchSize
Max number of nodes to read in a single batch read

**Type**: Integer

Default is 500

---
### ReadTimeout
Timeout in milliseconds reading from the server

**Type**: Integer

Default is 10000

---
### SecurityPolicy
Any of the following policy names

- None
- Basic128Rsa15
- Basic256
- Basic256Sha256
- Aes128ShaRsaOaep

For other value other than None a client certificate must be configured

**Type**: String

Default is None

---
### ServerProfile
Any of the profiles in the adapters [ServerProfiles](#serverprofiles) section.

**Type**: String

---
### WaitAfterConnectError
Time in milliseconds to wait to reconnect after a connection error

**Type**: Integer

Default is 10000, the minimum value is 1000

---
### WaitAfterReadError
Time in milliseconds to wait after a read error

**Type**: Integer

Default is 10000, the minimum value is 1000

### OpcuaServerConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for OPC UA server",
  "properties": {
    "Address": {
      "type": "string",
      "description": "The IP address or hostname of the OPC UA server"
    },
    "Certificate": {
      "$ref": "#/definitions/CertificateConfiguration",
      "description": "Certificate configuration for secure connections"
    },
    "CertificateValidation": {
      "$ref": "#/definitions/CertificateValidationConfiguration",
      "description": "Configuration for certificate validation"
    },
    "ConnectTimeout": {
      "type": "integer",
      "description": "Connection timeout in milliseconds"
    },
    "ConnectionWatchdogInterval": {
      "type": "integer",
      "description": "Interval for connection watchdog in milliseconds"
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
    "Path": {
      "type": "string",
      "description": "Path component of the OPC UA server URL"
    },
    "Port": {
      "type": "integer",
      "description": "Port number of the OPC UA server"
    },
    "ReadBatchSize": {
      "type": "integer",
      "description": "Number of items to read in a single batch"
    },
    "ReadTimeout": {
      "type": "integer",
      "description": "Timeout for read operations in milliseconds"
    },
    "SecurityPolicy": {
      "type": "string",
      "description": "Security policy for the OPC UA connection",
      "enum": ["None", "Basic128Rsa15", "Basic256", "Basic256Sha256", "Aes128_Sha256_RsaOaep""]
      "default" : "None"         
    },
    "ServerProfile": {
      "type": "string",
      "description": "Profile of the OPC UA server"
    },
    "WaitAfterConnectError": {
      "type": "integer",
      "description": "Wait time after a connection error in milliseconds"
    },
    "WaitAfterReadError": {
      "type": "integer",
      "description": "Wait time after a read error in milliseconds"
    }
  },
  "required": ["Address", "Port"]
}


```

### OpcuaServerConfiguration Examples



Basic configuration:

```json

{
    "Address": "opc.tcp://localhost",
    "Path": "OPCUA/SimulationServer",
    "Port": 53530
}

```

[^top](#opcua-protocol-adapter)


## OpcuaCertificateValidationConfiguration

[OpcuaAdapterConfiguration](#opcuaadapterconfiguration) >   [OpcuaServers](#opcuaservers) > [OpcUaServer](#opcuaservers) > [CertificateValidation](#certificatevalidation)



- [Schema](#opcuacertificatevalidationconfiguration-schema)
- [Examples](#opcuacertificatevalidationconfiguration-example)

**Properties:**

- [Active](#active)
- [Directory](#directory)
- [ValidationOptions](#validationoptions)

------

### Active

Flag to set to enable or disable the validation of server certificates

**Type**: Boolean

Default is true

------

### Directory

Pathname to base directory under which certificates and certificate revocation lists are stored

**Type**: String~~~~

This directory must exist, subdirectories will be created by the adapter if they do not exist.

------

### ValidationOptions

Configuration of op optional checks

**Type**: OpcuaCertificateValidationOptions

When not set then all options are enabled

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
      "$ref": "#/definitions/ValidationOptions",tificate-vatificate-validation-configuration
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

[OpcuaAdapterConfiguration](#opcuaadapterconfiguration) >   [OpcuaServers](#opcuaservers) > [OpcUaServer](#opcuaservers) > [CertificateValidation](#certificatevalidation) > [ValidationOptions](#validationoptions)



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

Check Application description against the ApplicationUri from Subject Alternative Names

**Type**: Boolean

Default is true

---

### ExtKeyUsageEndEntity

Extended key usage extension must be present and will be validated for end-entity certificates

**Type**: Boolean

Default is true

---

### HostOrIp

Host or IP address must be present in Alternate Subject Names and will be checked

**Type**: Boolean

Default is true

---

### KeyUsageEndEntity

Key usage extension must be present and will be validated for end-entity certificates

**Type**: Boolean

Default is true

---

### KeyUsageIssuer

Key usage must be present and will be checked for CA certificates

**Type**: Boolean

Default is true



---

### Revocation

Revocation checking

**Type**: Boolean

Default is true

---

### Validity

Check certificate expiry

**Type**: Boolean

Default is true

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

