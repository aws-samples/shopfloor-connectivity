# OPCUA Protocol adapter

The SFC OPC UA protocol adapter provides read-only access to OPC UA servers, supporting both synchronous data reads and asynchronous monitoring through subscriptions. The adapter can be configured to either poll data points on demand or subscribe to data changes and events, enabling efficient real-time data acquisition from industrial automation systems.

Configuration types for the OPCUA protocol adapter and contains the extensions and specific configuration types.

- [OPCUA Alarm and Events types](#opcua-alarm-and-event-types)
- [OPCUA security profiles and certificates](#opcua-security-profiles-and-certificates)



In order to use this adapter as in [in-process](../sfc-running-adapters.md#running-protocol-adapters-in-process) type adapter the type must be added to the [AdapterTypes](../core/sfc-configuration.md#adaptertypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"AdapterTypes" :{
  "OPCUA" : {
    "JarFiles" : ["<location of deployment>/opcua/lib"]
  },
  "FactoryClassName" : "com.amazonaws.sfc.opcua.OpcuaAdapter"
}
```



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

To enable the generation of these self-signed certificates the SelfSignedCertificate section must be present in the server configuration. In this section the CommonName of the certificate must be set and optionally the X.509Name fields for Organization, OrganizationalUnit, LocalityName, StateName and CountryCode. The default period in which the generated certificate is valid start from (notBefore) the current date to an end date (notAfter) of the current date plus 3 years. The duration in which the certificate is valid can be modified by setting the ValidPeriodDays attribute.

A number of days can be set in ExpirationWarningPeriod. At startup and at midnight the OPCUA adapter will check if the client certificate will expire within that period and generate a warning and metric for an expiring (or expired) certificate.

If the OPCUA server does validate the DNS name or the DNS name and IP addresses of the client must be present in the certificate Subject Alternative Names. A list of IP Addresses and DNS names can be set in the SelfSignedCertificate IpAddresses and DnsNames attributes. If these are not set then all known IP addresses and DNS name of the host on which the OPCUA adapter generates the certificate will be set as Subject Alternative Names. To exclude the IP addresses and DNS names from the generated certificate, specify an empty list for these attributes.

If the certificate contains an ApplicationUri as an Alternative Subject Name, the Application Description used by the OPCUA client will be the name part from that URI. For self-signed certificates the alternative subject name for the application uri will be set to urn:aws-sfc-opcua@[hostname]. (Application Name used by client is aws-sfc-opcua@[hostname]). OPCUA servers van validate the application name used by the client against the ApplicationUri from the certificate.

*NOTE: The certificate used by the client must be trusted by the OPCUA server, for which the procedure depends on the used sever. As an example, when a ProSys OPCUA (simulation) server is used, an unknown certificate is rejected but stored on the server, where it can be manually marked through the UI as trusted.*

The OPCUA adapter can also validate the certificate it receives from the OPCUA server. It will validate it using a set of know trusted certificates and issuers and certificate revocation lists (CRL). To enable the validation a CertificateValidation section must be present in the configuration. The Directory attribute in this section is set to the location where the certificates and revocation lists are stored in a number of subdirectories, which will be created by the adapter if these do not exist. 

**In the event of an initial connection failure to a server, the corresponding certificate for that server must be manually transferred from the rejected directory to the trusted/certs directory.**

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

Example of OPCUA server configuration using Basic256Sha256 security profile for signed and encrypted traffic using an X.509 certificate and private key, which can be generated by the adapter as a self-signed certificated which is valid for 365 days. A daily warning and metric value will be generated staring 30 days before the certificate expires. Server certificates will be checked using certificates and certificate revocation lists stored in subdirectories under the specified base directory for that server.

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
  }

```



## OpcuaSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) >  [Source](../core/source-configuration.md) 

The OpcuaSourceConfiguration class specifies which OPC UA server to read from within the protocol adapters section, and defines the channels (data nodes) to be read or subscribed to from the configured server.

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
The AdapterOpcuaServer property specifies the server identifier for the OPC UA server to read from. This identifier must match a server configuration defined in the OpcuaServers section of the referenced protocol adapter.

**Type**: String

---
### Channels

The Channels property is a map of channel configurations, where each key is a unique channel identifier that maps to its corresponding channel settings

**Type**: Map[String,[OpcuaNodeChannelConfiguration](#opcuanodechannelconfiguration)

At least 1 channel must be configured.

---
### EventQueueSize
The EventQueueSize property defines the maximum number of events that can be queued during a reading interval. When this limit is exceeded, older events are discarded to make room for new ones.

**Type**: Integer

Default is 10

---
### EventSamplingInterval
The EventSamplingInterval property specifies the sampling interval for events in milliseconds. The default value is 0, which indicates the server will use its best effort cyclic rate to sample the item from its source

**Type**: Integer

---
### SourceReadingMode
The SourceReadingMode property determines how values are read from the OPC UA server. In `"Subscription"` mode (default), the connector monitors configured node items and returns only changed values between schedule intervals, except for the initial read which returns all items. In `"Polling"` mode, the connector performs batch reads of all configured nodes at the scheduled interval. Note that nodes collecting alarm or event data always use subscription mode regardless of this setting, while data nodes follow the specified mode


**Type**: String
Values can be "Subscription" or "Polling".

Default is "Subscription".



---
### SubscribePublishingInterval
The SubscribePublishingInterval property sets the publishing interval in milliseconds for subscription-mode communication with the OPC UA server. While it defaults to the schedule interval, it can be explicitly set to a shorter duration to prevent server-side subscription timeouts.

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

The OpcuaNodeChannelConfiguration class defines the configuration for OPC UA nodes to read from or subscribe to, based on the[ SourceReadingMode](#sourcereadingmode) setting.

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
The EventSamplingInterval property specifies the sampling interval in milliseconds for events on this specific node. This value overrides the [event sampling interval defined at the source level](#eventsamplinginterval).

**Type**: Integer

Default is 0

---
### EventType
For collecting data from event or alarm nodes the type of the event must be specified. This can either be the name of the event (e.g., BaseEventType) or the node identifier (e.g., ns=0;i=17).

**Type**: String (name of the event or node identifier)

Valid OPCUA defined event and alarm names can be found at https://reference.opcfoundation.org/Core/Part9/v105/docs/5.8, and https://reference.opcfoundation.org/Core/Part3/v104/docs/9.1

If an event type is not an OPCUA or server profile-defined event type, a warning is generated, and the OPCUA-defined "BaseEventType" is utilized.

If a server profile has been defined and used for the server the source for the channel is reading from, the names and identifiers for event types in that profile can be used as well.

The event type is used to filter the events that are raised by a node and to determine the values that can be read from the event. To receive multiple event types from a node, separate channels need to be configured for each event type.

---
### IndexRange
The IndexRange property specifies the range used to read subsets of array values from the OPC UA node.

**Type**: String

If not set all values from an array are read. For syntax see https://reference.opcfoundation.org/v104/Core/docs/Part4/7.22/

---
### NodeChangeFilter
The NodeChangeFilter property defines the conditions that determine when a value change should be reported in subscription mode for the node.

In addition to this, a  non-OPCUA-specific SFC [ValueFilter](../core/channel-configuration.md#valuefilter), [ChangeFilter](../core/channel-configuration.md#changefilter), and [ConditionFilter](../core/channel-configuration.md#conditionfilter) can be applied 

**Type**: [OpcuaNodeChangeFilter](#opcuanodechangefilter)

Optional

---
### NodeId
The NodeId property specifies a string containing the identifier of the node to read values from or monitor.

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
The Selector property allows you to specify a JMESpath query to evaluate against structured data types. It can be used to restructure or extract specific values from structured data types.

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

The OpcuaNodeChangeFilter class defines the filtering conditions for detecting and reporting value changes in OPC UA node subscriptions.

- [Schema](#opcuanodechangefilter-schema)
- [Examples](#opcuanodechangefilter-examples)

**Properties:**

- [Type](#type)
- [Value](#value)



---
### Type

The Type property specifies how value changes are measured, with two possible options:

- "Absolute": Monitors changes based on absolute value differences
- "Percent": Monitors changes based on percentage differences

**Type**: String

---
### Value
The Value property specifies the data change threshold that triggers a notification. This value is interpreted according to the Type property - either as an absolute value difference or as a percentage change.

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

The OpcuaAdapterConfiguration class defines adapter-wide settings and contains definitions of OPC UA server configurations that can be used by the configured OPC UA sources.

OpcuaAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the OPCUA Protocol adapter.

- [Schema](#opcuaadapterconfiguration-schema)
- [Examples](#opcuaadapterconfiguration-examples)

**Properties:**

- [MaxEventRetainSize](#maxeventretainsize)
- [MaxEventRetainPeriod](#maxeventretainperiod)

- [OpcuaServers](#opcuaservers)
- [ServerProfiles](#serverprofiles)



---

### MaxEventRetainPeriod

The MaxEventRetainPeriod property specifies the maximum time period (in milliseconds) for which events are stored between adapter reads. The default value is 3,600,000 milliseconds (1 hour). Setting this value to 0 removes the time limit, allowing events to be stored indefinitely.

**Type** : Integer

---

### MaxEventRetainSize

The MaxEventRetainSize property specifies the maximum number of events that can be stored between adapter reads. The default value is 10,000 events. Setting this value to 0 removes the limit on the number of stored events.

**Type**: Integer

---
### OpcuaServers
The OpcuaServers property defines the OPC UA servers configured for this adapter. Each OPC UA source using the adapter must reference one of these server configurations through its AdapterOpcuaServer attribute.

**Type**: Map[String,[OpcuaServerConfiguration](#opcuaserverconfiguration)]

---
### ServerProfiles
The ServerProfiles property defines the profiles configured for this adapter. OPC UA servers configured in this adapter can reference one of these profiles through their [ServerProfile](#serverprofile) attribute. These profiles are used to define additional event types (from companion specifications) for a server.

**Type**: Map[String,[OpcuaServerProfileConfiguration](#opcuaserverprofileconfiguration)]

### OpcuaAdapterConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for OPC UA adapter",
  "allOf": [
    {
      "$ref": "#/definitions/TargetAdapterConfiguration"
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
        "MaRetainEventsPeriod" :{
          "type" : "integer",
          "description": "Max events retain period",
          "default" : 0
        },
        "MaxEventsRetainSize" :{
          "type" : "integer",
          "description": "Max events retain size",
          "default" : 0
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
  "AdapterType" : "OPCUA-TARGET",
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

The OpcuaServerProfileConfiguration class defines additional event types (from companion specifications) that can be used by OPC UA servers referencing this profile through their ServerProfile attribute.

- [Schema](#opcuaserverprofileconfiguration-schema)
- [Examples](#opcuaserverprofileconfiguration-examples)

**Properties:**

- [EventTypes](#eventtypes)

  

---
### EventTypes
The EventTypes property defines additional event types (from companion specifications) that can be used by OPC UA servers that reference this profile. These event types extend the standard event types available to the server.

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

The OpcuaEventTypeConfiguration class defines the configuration for an additional event type that extends the standard OPC UA event types. This allows for handling custom events defined in companion specifications.

- [Schema](#opcuaeventtypeconfiguration-schema)
- [Examples](#opcuaeventtypeconfiguration-examples)

**Properties:**
- [Inherits](#inherits)
- [NodeId](#nodeid)
- [Properties](#properties)

---
### Inherits
The Inherits property specifies the event type from which this event type inherits properties. When set, all properties from the referenced type (and any types it inherits from) will be included in this event type's properties. The value can be either the name or node identifier of a standard OPC UA event type, or reference another event type defined in the same profile.

**Type** : String

Optional.

Must refer to an existing OPC UA event type or another event type defined in the same profile.

---
### NodeId
The NodeId property defines the node identifier for the event type. This identifier must match the identifier used for the event type on the OPC UA server. It can be used in place of the event name when specifying the event type for a node to read in source channels, or as the type to inherit from in other events within the same profile.

**Type**: String

Required

Syntax is 

`ns=namespaceindex;type=value`

e.g., `ns=0;i=9000`

---
### Properties
The Properties property defines the properties for the event type. Each property is specified as a string using a Qualified name format that combines the namespace and browse name. The format is  `ns:browsename`, where ns is the `namespace` and `browsename`  is the browse name for that property.

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

The OpcuaServerConfiguration class defines the connection and security settings for an OPC UA server. This configuration can be referenced by OPC UA sources through their AdapterOpcuaServer attribute to establish communication with the server.

When connecting to a server for the first time fails due to a certificate validation error or an error message indicating that *"the trustAnchors parameter must be non-empty,"* the server's certificate must be moved from the "rejected" subdirectory under the [directory](#directory) configured in the [CertificateValidation](#certificatevalidation) for the server to the "trusted/certs" directory.

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
- [Password]()
- [Path](#path)
- [Port](#port)
- [ReadBatchSize](#readbatchsize)
- [ReadTimeout](#readtimeout)
- [SecurityPolicy](#securitypolicy)
- [ServerProfile](#serverprofile)
- [UserCertificate](#usercertificate)
- [Username](#username)
- [WaitAfterConnectError](#waitafterconnecterror)
- [WaitAfterReadError](#waitafterreaderror)

---
### Address
The Address property specifies the network address or endpoint URL of the OPC UA server. This is used to establish the connection to the server.

**Type**: String

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
### ConnectionWatchdogInterval
The ConnectionWatchdogInterval property defines how frequently, in milliseconds, the client checks the server connection when operating in Subscription reading mode. The watchdog performs these checks by attempting to read the server status from the OPC UA server. This monitoring is particularly important because when using subscriptions, a lost connection to a stopped server might not be detected through normal subscription operations alone.

**Type** : Integer

Default value is 1000 milliseconds (1 second)
Setting the value to 0 will disable the watchdog functionality

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

### Password

Password credential used for authentication with the OPC UA server. Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.

**Type:** String

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
### ReadBatchSize
The ReadBatchSize property specifies the maximum number of nodes to read in a single batch read.

**Type** : Integer

Default value is 500

---
### ReadTimeout
The ReadTimeout property specifies the timeout in milliseconds when reading from the server. 

**Type** : Integer

Default value is 10000

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
### ServerProfile
The ServerProfile property specifies the server profile to use from the adapters [ServerProfiles](#serverprofiles) section.

**Type**: String

---

### UserCertificate

A certificate configuration object that defines the X.509 certificate and private key used for certificate-based client authentication with the OPC UA server. This configuration includes the certificate file path, private key file path, certificate format (PEM, PKCS12, etc.), and optional password for encrypted private keys. The certificate represents the client's identity . The certificate configuration must contain valid certificate and private key files that are accessible and properly formatted for OPC UA client authentication.

This authentication method is mutually exclusive with [username](#username)/[password](#password) authentication.

**Type**: [CertificateConfiguration](../core/certificate-configuration.md)

---

### Username

Username credential used for authentication with the OPC UA server. Username and password should not be included as clear text in the configuration. It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.

Username and [UserCertificateFile](#usercertificate) are mutally exclusive.

**Type:** String

---
### WaitAfterConnectError
The WaitAfterConnectError property specifies the time in milliseconds to wait before attempting to reconnect after a connection error. 

**Type**: Integer

Default is 10000, the minimum value is 1000

---
### WaitAfterReadError
The WaitAfterReadError property specifies the time in milliseconds to wait before attempting to reconnect after a read error.

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
    "UserCertificate": {
      "$ref": "#/definitions/CertificateConfiguration",
      "description": "Certificate configuration for user authentication"
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

    

---

### ValidationOptions

The ValidationOptions property configures optional certificate validation checks.

 If this property is not set, all validation options are enabled by default.

**Type** : [OpcuaCertificateValidationOptions](#opcuacertificatevalidationoptions-type)

Note: If this property is not set, all validation options are enabled by default.

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

[OpcuaAdapterConfiguration](#opcuaadapterconfiguration) >   [OpcuaServers](#opcuaservers) > [OpcUaServer](#opcuaservers) > [CertificateValidation](#certificatevalidation) > [ValidationOptions](#validationoptions)

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

- When enabled (true):

  - The Application URI from the server's application description will be checked against the ApplicationUri specified in the Subject Alternative Names field of the certificate 
  - This validation helps ensure the certificate belongs to the expected application

  When disabled (false):

  - This specific validation check will be skipped

- 

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

