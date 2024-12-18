# OPCUA Protocol adapter

- [OPCUA Alarm and Events types](#opcua-alarm-and-events-types)
- [OPCUA security profiles and certificates](#opcua-security-profiles-and-certificates)
- [OPCUA Protocol Configuration](#opcua-protocol-configuration)
  - [OpcuaSourceConfiguration](#opcuasourceconfiguration)
  - [OpcuaNodeChannelConfiguration](#opcuanodechannelconfiguration)
  - [OpcuaNodeChangeFilter](#opcuanodechangefilter)
  - [OpcuaAdapterConfiguration](#opcuaadapterconfiguration)
  - [OpcuaServerProfileConfiguration](#opcuaserverprofileconfiguration)
  - [OpcuaEventTypeConfiguration](#opcuaeventtypeconfiguration)
  - [OpcuaServerConfiguration](#opcuaserverconfiguration)
  - [CertificateConfiguration](#certificateconfiguration)
  - [SelfSignedCertificateConfiguration](#selfsignedcertificateconfiguration)
  - [CertificateValidationConfiguration](#certificatevalidationconfiguration)
  - [CertificateValidationOptions](#certificatevalidationoptions)
  - 
# OPCUA Alarm and Events types

The OPCUA protocol adapter supports the collection of data from events and alarms. This can be done by adding the event
name or identifier of the alarm or event type to a node channel configuration. The name of the event can be the name of
the OPCUA alarms from the model at <https://reference.opcfoundation.org/Core/Part9/v105/docs/5.8>, or an OPCUA event
from the model at <https://reference.opcfoundation.org/Core/Part3/v104/docs/9.1>

The adapter will monitor nodes with a specified event type the adapter and add the received to the collected data for
the OPCUA source, using the name for that node. The event data consist of a map of properties, which are based on the
type of the event used for the node. As multiple events may be received during a read interval, the value of these event
nodes is always of type array, containing one or more maps with the event data. The maximum number of items that can be
collected is configurable. If more events are received the oldest event is omitted from the output.

The OPCUA adapter can operate in Polling or Subscription mode to collect data values from the OPCUA server. For events
the adapter will use a subscription with monitored event nodes, independent of in which mode the adapter collects the
data nodes.

As industry specific companion specification define additional event and alarm types, SFC allows configuration of
additional types, which are grouped in server profiles. An event is configured by a given name, the node identifier of
the event type (e.g., ns=99;i=9999), and a list of properties for that event with their qualified names consisting of a
namespace and browse name (e.g., 9:Property1)

In order to reduce the configuration for these events it is possible to inherit from other events in the profile or the
types defined in the OPCUA specifications, by specifying that that type by its type name or node identifier. All
properties defined in the type a type inherits from are added, as well as all other properties in types up in the type
hierarchy.

The names or node identifiers can be used as event types in the nodes for which event and alarm data needs to be
collected. The event name is used to:

- Filter the evens raised by the node, if multiple event types need to be received then a channel needs to be configured
  for each of these event types.

- Collect the values from the received events as defined for that event type.

As for data nodes selectors, it is possible to use a selector to filter specific properties from the events and add
additional metadata at node level. Index ranges and node change filters are not supported for events data.

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

[^top](#quicklinks)

The snippet below shows the configuration of an OPCUA adapter with a profile named "CustomEventsProfile" that defines
two additional event types, "CustomEventType1" and "CustomEventType2", each with two properties. CustomEventType1
inherits from the OPCUA defined BaseEventType type and will contain all properties from that class in addition to the
two properties defined for the event. CustomEventType2 will inherit from and therefore contain all properties from
CustomEventTYpe1 and the two properties defined for the event.

Sources are configured to read from adapter "OPCUA" and server "OPCUA-SERVER", which has a service profile set to "
CustomEventsProfile", can use both defined event types in addition to all OPCUA defined event types, as event type for
their nodes to collect the data in the properties for these events.

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

Further details on OPCUA configuring alarms and events and [creating custom event types](./adapters/opcua.md#opcuaeventtypeconfiguration) can be found in the [OPCUA configuration](./adapters/opcua.md#opcuaadapterconfiguration).

# OPCUA security profiles and certificates

In order to secure the traffic between the OPCUA protocol adapter and the OPCUA Server it can be signed and encrypted
using certificates.

In the configuration for the OPCUA server in the adapter the security policies can be used by setting the
[SecurityPolicy](./adapters/opcua.md#opcuaserverconfiguration)
of the server to any of the following policy names:

| Name                | Sign / Encrypt   | Security Policy                                                  |
|---------------------|------------------|------------------------------------------------------------------|
| None                |                  |                                                                  |
| Basic128Rsa15       | Sign             | http://opcfoundation.org/UA/SecurityPolicy#Basic128Rsa15         |
| Basic256            | Sign and encrypt | http://opcfoundation.org/UA/SecurityPolicy#Basic256              |
| Basic256Sha256      | Sign and encrypt | http://opcfoundation.org/UA/SecurityPolicy#Basic256Sha25         |
| Aes128Sha256RsaOaep | Sign             | http://opcfoundation.org/UA/SecurityPolicy#Aes128_Sha256_RsaOaep |

The Certificate section of the OPCUA Server contains the settings for the certificate used by the client of the adapter.

The CertificateName contains the filename of the client certificate, which can be in pem or Pkcs12 format. If a pem
format file is used, additionally the name of the corresponding private key file must be set in PrivateKeyFile. This is
not required for PFX certificates as this type of file is a container which holds the certificate and private key. If
the PFX file is password protected then the Password attribute must be set. (Avoid clear passwords in the configuration,
use placeholders for secrets obtained from AWS Secrets manager instead). If an alias is used in the PFX container the
value of that alias must be set in the Alias attribute of the configuration.

The type of the certificate can be determined by the prefix of the filename (either ".pem "or ".pfx") optionally
followed by ".cer", ".cert" or ".crt". If another extension is used then the type can be explicitly set by setting the
server configuration's Format attribute to either "Pem" or "Pkcs12".

If either the PEM or PFX certificate file does not exist, it is possible to let the OPCUA adapter generate a self-signed
certificate and store that certificate in the specified file name. For PEM format certificates the name of the private
key file must be set as well. If the private key file does exist it will be used to generate a pem or Pkcs12 formatted
certificate. If it does not exist the keypair is generated and, if a pem formatted certificate is generated, stored in
the specified file. For Pkcs12 formatted certificates the key will be stored with the certificate in the pfx file.

To enable the generation of these self-signed certificates the SelfSignedCertificate section must be present in the
server configuration. In this section the CommonName of the certificate must be set and optionally the X509Name fields
for Organization, OrganizationalUnit, LocalityName, StateName and CountryCode. The default period in which the generated
certificate is valid start from (notBefore) the current date to an end date (notAfter) of the current date plus 3 years.
The duration in which the certificate is valid can be modified by setting the ValidPeriodDays attribute.

A number of days can be set in ExpirationWarningPeriod. At startup and at midnight the OPCUA adapter will check if the
client certificate will expire within that period and generate a warning and metric for an expiring (or expired)
certificate.

If the OPCUA server does validate the DNS name or the DNS name and IP addresses of the client must be present in the
certificate Subject Alternative Names. A list of IP Addresses and DNS names can be set in the SelfSignedCertificate
IpAddresses and DnsNames attributes. If these are not set then all known IP addresses and DNS name of the host on which
the OPCUA adapter generates the certificate will be set as Subject Alternative Names. To exclude the IP addresses and
DNS names from the generated certificate, specify an empty list for these attributes.

If the certificate contains an ApplicationUri as an Alternative Subject Name, the Application Description used by the
OPCUA client will be the name part from that URI. For self-signed certificates the alternative subject name for the
application uri will be set to urn:aws-sfc-opcua@\[hostname\]. (Application Name used by client is
aws-sfc-opcua@\[hostname\]). OPCUA servers van validate the application name used by the client against the
ApplicationUri from the certificate.

*NOTE: The certificate used by the client must be trusted by the OPCUA server, for which the procedure depends on the
used sever. As an example, when a ProSys OPCUA (simulation) server is used, an unknown certificate is rejected but
stored on the server, where it can be manually marked through the UI as trusted.*

The OPCUA adapter can also validate the certificate it receives from the OPCUA server. It will validate it using a set
of know trusted certificates and issuers and certificate revocation lists (CRL). To enable the validation a
CertificateValidation section must be present in the configuration. The Directory attribute in this section is set to
the location where the certificates and revocation lists are stored in a number of subdirectories, which will be created
by the adapter if these do not exist.

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

The certs directories contain trusted certificates and certificates of issuers in order to validate signed certificates.
The crl directories contain the certification revocation lists. When a server certificate does not pass the validation
it will be stored in PEM format in the rejected directory, from where it can after inspection be moved into the trusted
certificate directory.

A number of optional checks (see <https://reference.opcfoundation.org/v104/Core/docs/Part4/6.1.3/>) can be configured in
a ValidationOptions section in the CertificateValidation section. It can contain the following attributes that can be
set to a value of false to disable the optional validation, which by default are all enabled.

Validation options:

- HostOrIP: End certificates must contain their host name or IP address in the Subject Alternate Names which will be
  validated
- Validity: Checks certificate expiry
- KeyUsageEndEntity: Key usage extensions for end entity certificates must be present and will be checked.
- ExtKeyUsageEndEntity: : Extended key usage extensions for end entity certificates must be present and will be checked.
- KeyUsageIssuer: Key usage extensions must be present and will be checked for CA certificates.
- Revocation: Revocation will be checked against CLRs.
- ApplicationUri: Checks the Application name in the Subject Alternative Names against the Application description.

Example of OPCUA server configuration using Basic256Sha256 security profile for signed and encrypted traffic using a
X509 certificate and private key, which can be generated by the adapter as a self-signed certificated which is valid for
365 days. A daily warning and metric value will be generated staring 30 days before the certificate expires. Server
certificates will be checked using certificates and certificate revocation lists stored in subdirectories under the
specified base directory for that server.

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
    "ExtKeyUsageEndEntity" : true,
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



## OPCUA Protocol Configuration

This section describes the configuration types for the OPCUA protocol adapter and contains the extensions and specific
configuration types


  - [OpcuaSourceConfiguration](#opcuasourceconfiguration)
  - [OpcuaNodeChannelConfiguration](#opcuanodechannelconfiguration)
  - [OpcuaNodeChangeFilter](#opcuanodechangefilter)
  - [OpcuaAdapterConfiguration](#opcuaadapterconfiguration)
  - [OpcuaServerProfileConfiguration](#opcuaserverprofileconfiguration)
  - [OpcuaEventTypeConfiguration](#opcuaeventtypeconfiguration)
  - [OpcuaServerConfiguration](#opcuaserverconfiguration)
  - [CertificateConfiguration](#certificateconfiguration)
  - [SelfSignedCertificateConfiguration](#selfsignedcertificateconfiguration)
  - [CertificateValidationConfiguration](#certificatevalidationconfiguration)
  - [CertificateValidationOptions](#certificatevalidationoptions)


[Protocol Adapters](./README.md)


## OpcuaSourceConfiguration

The OpcuaSourceConfiguration extends the common <a href="../core/source-configuration.md" >Source configuration</a> with 
OPCUA specific source configuration data. The AdapterType for the source must be set to **OPCUA**.

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 26%" />
<col style="width: 30%" />
<col style="width: 23%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Channels</td>
<td><p></p>
<p>The element is a map indexed by the channel identifier.</p></td>
<td>Map[String,<a href="#opcuanodechannelconfiguration">OpcuaNodeChannelConfiguration</a>]</td>
<td>At least 1 channel must be configured.</td>
</tr>

<tr class="odd">
<td>AdapterOpcuaServer</td>
<td>Server Identifier for the OPCUA server to read from. This referenced server must be present in the OpcuaServers section of the adapter referred to by the ProtocolAdapter attribute of the source.</td>
<td>String</td>
<td>Must be an identifier of a server in the OpcuaServers section of the OPCUA adapter used by the source.</td>
</tr>

<tr class="even">
<td>SourceReadingMode</td>
<td><p>Mode for reading values from OPCUA server.</p>
<ul>
<li><p>"Subscription": connector will create a subscription and will monitor the node items configured in the channels for the source. When reading from the adapter in this mode, only items that have been changed in the schedule interval period will be returned, except for the initial read that will return all monitored items.</p></li>
<li><p>"Polling", the connector will batch-read all nodes configured in the channels for the source with the interval defined in the schedule.</p></li>
</ul></td>
<td>A string that can have the value “Subscription" or "Polling".</td>
<td><p>Default is "Subscription".</p>
<p>When the source has any nodes that collect data for alarm or event, these nodes are always read in subscription mode, even if the mode is set to Polling.</p>
<p>Data nodes are always read in the specified mode.</p></td>
</tr>

<tr class="odd">
<td>SubscribePublishingInterval</td>
<td>Time in milliseconds that will be used as the SubscribePublishingInterval when creating the subscription with the server when reading in subscription mode. By default, the interval of the schedule is used. An OPCUA server might time out the subscription if this period is too long, in which case the SubscribePublishingInterval can be explicitly set to a shorter period to avoid the subscription timeout.</td>
<td>Integer</td>
<td></td>
</tr>

<tr class="even">
<td>EventQueueSize</td>
<td>Queue size for events that can be received in a reading interval. More events are received, the oldest events are discarded.</td>
<td>Integer</td>
<td>Default is 10</td>
</tr>

<tr class="odd">
<td>EventSamplingInterval</td>
<td>Sampling interval for events in milliseconds</td>
<td>Integer</td>
<td>Default is 0 (0 stands for best effort cyclic rate that the Server uses to sample the item from its source)</td>
</tr>

</tbody>
</table>

[Opcua Protocol Configuration](#opcua-protocol-configuration)

## OpcuaNodeChannelConfiguration

The OpcuaNodeChannelConfiguration extends the common Channel configuration with OPCUA specific channel configuration data

<table>
<colgroup>
<col style="width: 16%" />
<col style="width: 19%" />
<col style="width: 21%" />
<col style="width: 43%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>NodeId</td>
<td>A string containing the id of the node to read the value from or to monitor.</td>
<td>String</td>
<td><p>The id must have the format:</p>
<p>ns=&lt;namespaceIndex&gt;;&lt;identifiertype&gt;=&lt;identifier&gt;</p>
<p>with the fields:</p>
<ul>
<li><p>&lt;namespace index&gt;: The namespace index formatted as a number.</p></li>
<li><p>&lt;identifier type&gt;: A flag that specifies the identifier type. The flag has the following values:</p>
<ul>
<li><p>I: Integer</p></li>
<li><p>S: String</p></li>
<li><p>G: Guid</p></li>
<li><p>B: ByteString</p></li>
</ul></li>
</ul>
<ul>
<li><p>&lt;identifier&gt;: The identifier encoded as string.</p></li>
</ul></td>
</tr>

<tr class="odd">
<td>IndexRange</td>
<td>Range to read subsets from array values.</td>
<td>String</td>
<td>If not set all values from an array are read. For syntax see https://reference.opcfoundation.org/v104/Core/docs/Part4/7.22/</td>
</tr>

<tr class="even">
<td>NodeChangeFilter</td>
<td>Change filter used in subscription for node that defines the conditions when a value change must be reported.</td>
<td><a href="#opcuanodechangefilter">OpcuaNodeChangeFilter</a></td>
<td>Optional</td>
</tr>

<tr class="odd">
<td>Selector</td>
<td><p>Evaluate a <a href="https://jmespath.org/">JMESpath</a> query against the value of a structured data type and returns the result.</p>
<p>The selector can be used to restructure or select values from structured data types.</p></td>
<td>String</td>
<td>Parameter: <a href="https://jmespath.org/">JMESPath</a> expression, see https://jmespath.org/</td>
</tr>

<tr class="even">
<td>EventSamplingInterval</td>
<td>Sampling interval for events in milliseconds. Use this value to specify a specific interval for this node which overrides the interval at source level.</td>
<td>Integer</td>
<td>Default is 0 value configured for the OPCUA source</td>
</tr>

<tr class="odd">
<td>EventType</td>
<td>For collecting data from event or alarm nodes the type of the event must be specified. This can either be the name of the event (e.g., BaseEventType) or the node identifier (e.g., ns=0;i=17).</td>
<td>String (name of the event or node identifier)</td>
<td><p>Valid OPCUA defined event and alarm names can be found at https://reference.opcfoundation.org/Core/Part9/v105/docs/5.8, and <a href="https://reference.opcfoundation.org/Core/Part3/v104/docs/9.1">https://reference.opcfoundation.org/Core/Part3/v104/docs/9.1</a></p>
<p>If an event type is used which is not an OPCUA or server profile defined event type a warning is generated and the OPCUA defined "BaseEventType" is used.</p>
<p>If a server profile has been defined and used for the server the source for the channel is reading from, the names and identifiers for event types in that profile can be used as well.</p>
<p>The event type is used to filter the events that are raised by a node and to determine the values that can be read from the event. To receive multiple event types from a node, separate channels need to be configured for each event type.</p></td>
</tr>

</tbody>
</table>

[Opcua Protocol Configuration](#opcua-protocol-configuration)

## OpcuaNodeChangeFilter

Data change filter for OPCUA node channels when running adapter in subscription mode.

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 25%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Value</td>
<td>Data change value</td>
<td>Double</td>
<td>Default is 0.0</td>
</tr>

<tr class="odd">
<td>Type</td>
<td><ul>
<li><p>"Absolute": Value is absolute value change.</p></li>
<li><p>"Percent": Value is data change in percent.</p></li>
</ul></td>
<td>String</td>
<td>Optional</td>
</tr>

</tbody>
</table>

[Opcua Protocol Configuration](#opcua-protocol-configuration)

## OpcuaAdapterConfiguration

The OpcuaAdapterConfiguration extends the common adapter configuration with OPCUA specific adapter configuration settings. The AdapterType to use for this adapter is "OPCUA".

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 18%" />
<col style="width: 30%" />
<col style="width: 36%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>OpcuaServers</td>
<td>Opcua servers configured for this adapter. The opcua source using the adapter must have a reference to one of these in its AdapterOpcuaServer attribute.</td>
<td>Map[String,<a href="#opcuaserverconfiguration">OpcuaServerConfiguration</a>]</td>
<td></td>
</tr>

<tr class="odd">
<td>ServerProfiles</td>
<td>Profiles configured for this adapter. Servers in this adapter can have a reference to one if its profiles in their ServerProfile attribute.</td>
<td>Map[String,<a href="#opcuaserverprofileconfiguration">OpcuaServerProfileConfiguration</a>]</td>
<td></td>
</tr>


</tbody>
</table>

## OpcuaServerProfileConfiguration


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
<td>EventTypes</td>
<td>	Additional event types that can be used for a server,</td>
<td>Map[ String, <a href="#opcuaeventtypeconfiguration"> OpcUaEvenTypeConfiguration</a> ]]</td>
<td></td>
</tr>

</tbody>
</table>
 

[Opcua Protocol Configuration](#opcua-protocol-configuration)

## OpcuaEventTypeConfiguration

Defines an event or alarm type that can be used in a server profile to read custom events and alarms (e.g., from industry specific companion specifications)

<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 18%" />
<col style="width: 30%" />
<col style="width: 36%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>NodeId</td>
<td><p>Node identifier for the event type.</p>
<p>This identifier must match the identifier used for the event on the OPCUA server.</p>
<p>It can be used instead of the event name to specify the event type for a node to read in the channels of a source, or as the type to inherit from in other events in the same profile.</p></td>
<td>String</td>
<td><p>Required</p>
<p>Syntax is ns=namespaceindex;type=value</p>
<p>e.g., ns=0;i=9000</p></td>
</tr>

<tr class="odd">
<td>Properties</td>
<td>Properties defined for the event type. Each property is defined as a string which is a Qualified name, consisting of the namespace for that property and the browse name. The format is ns:browsename</td>
<td>List of String</td>
<td>Required, an at least one property must be defined.</td>
</tr>

<tr class="even">
<td>Inherits</td>
<td><p>Event type to inherit from.</p>
<p>When this attribute is set then all the properties of the referred type (and types that type inherits from) will be added to the properties of the event.</p>
<p>This field can include the name or node identifier of an OPCUA defined event type, or an event in the same profile.</p></td>
<td>String</td>
<td><p>Optional.</p>
<p>Must refer to and existing OPCUA or other vent type in the same profile.</p></td>
</tr>

</tbody>
</table>

[Opcua Protocol Configuration](#opcua-protocol-configuration)

## OpcuaServerConfiguration

Configuration data for connecting to and reading from source OPCUA servers

<table>
<colgroup>
<col style="width: 20%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>

<tr class="even">
<td>Address</td>
<td>Address of the OPCUA server</td>
<td>String</td>
<td></td>
</tr>

<tr class="odd">
<td>Port</td>
<td>OPCUA server port</td>
<td>Integer</td>
<td>Default is 53530</td>
</tr>

<tr class="even">
<td>Path</td>
<td>Server path or name</td>
<td>String</td>
<td>The connection address that will be used is &lt;Address&gt;:&lt;Port&gt;[/Path]</td>
</tr>

<tr class="odd">
<td>ConnectTimeout</td>
<td>Timeout in milliseconds connecting to the server</td>
<td>Integer</td>
<td>Default is 10000, the minimum value is 1000</td>
</tr>

<tr class="even">
<td>ReadTimeout</td>
<td>Timeout in milliseconds reading from the server</td>
<td>Integer</td>
<td>Default is 10000</td>
</tr>

<tr class="odd">
<td>WaitAfterConnectError</td>
<td>Time in milliseconds to wait to reconnect after a connection error</td>
<td>Integer</td>
<td>Default is 10000, the minimum value is 1000</td>
</tr>

<tr class="even">
<td>WaitAfterReadError</td>
<td>Time in milliseconds to wait after a read error</td>
<td>Integer</td>
<td>Default is 10000, the minimum value is 1000</td>
</tr>

<tr class="odd">
<td>ReadBatchSize</td>
<td>Max number of nodes to read in a single batch read</td>
<td>Integer</td>
<td>Default is 500</td>
</tr>

<tr class="even">
<td>ConnectionWatchdogInterval</td>
<td>Interval period in milliseconds for checking the server connection when the source is reading in Subscription reading mode. It will check the connection by trying to read the server status of the OPCUA server. This is needed in case the connection to a (stopped) server is lost, which will not be detected by a client that uses a subscription to obtain server values.</td>
<td>Integer</td>
<td><p>Default is 1000</p>
<p>Set to 0 to disable the watchdog</p></td>
</tr>

<tr class="odd">
<td>MaxMessageSize</td>
<td>The maximum message size in bytes</td>
<td>Integer</td>
<td><p>Default is 2,097,152 (2MB)</p>
<p>Min is 8196 (8 KB)</p>
<p>Max is 2,147,483,639 (MaxInt-8 is approx. 2048GB)</p></td>
</tr>

<tr class="even">
<td>MaxChunkSize</td>
<td>The maximum size of a single chunk of a message in bytes</td>
<td>Integer</td>
<td><p>Default is 65535 (64KB)</p>
<p>Min is 8196 (8KB)</p>
<p>Max is 2,147,483,639 (MaxInt-8 is approx. 2048GB)</p></td>
</tr>

<tr class="odd">
<td>MaxChunkCount</td>
<td>The maximum number of chunks that a message can break down into</td>
<td>Integer</td>
<td>Default is (MaxMessageSize / MaxChunkSize) * 2. More than chunks than constitute MaxMessageSize are needed because of overhead when constructing chunks; not all the chunk size is dedicated to message bytes.</td>
</tr>

<tr class="even">
<td>ServerProfile</td>
<td>Any of the profiles in the adapters ServerProfiles section.</td>
<td>String</td>
<td></td>
</tr>

<tr class="odd">
<td>SecurityPolicy</td>
<td><p>Any of the following policy names</p>
<ul>
<li><p>None</p></li>
<li><p>Basic128Rsa15</p></li>
<li><p>Basic256</p></li>
<li><p>Basic256Sha256</p></li>
<li><p>Aes128ShaRsaOaep</p></li>
</ul>
<p>For other value other than None a client certificate must be configured</p></td>
<td>String</td>
<td>Default is None</td>
</tr>

<tr class="even">
<td>Certificate</td>
<td>Client certificate configuration</td>
<td><a href="#certificateconfiguration">CertificateConfiguration</a></td>
<td></td>
</tr>

<tr class="odd">
<td>CertificateValidation</td>
<td>Certificate validation configuration</td>
<td><a href="#certificatevalidationconfiguration">CertificateValidationConfiguration</a></td>
<td></td>
</tr>

</tbody>
</table>

[Opcua Protocol Configuration](#opcua-protocol-configuration)

## CertificateConfiguration

Client certificate configuration for OPCUA client

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
<td>Self-signed certificate configuration used to generate a self-signed certificate</td>
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

[Opcua Protocol Configuration](#opcua-protocol-configuration)

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
<td>Pathname to base directory under which certificates and certificate revocation lists are stored</td>
<td>String</td>
<td>This directory must exist, subdirectories will be created by the adapter if they do not exist.</td>
</tr>

<tr class="even">
<td>ValidationOptions</td>
<td>Configuration of op optional checks</td>
<td><a href="#certificatevalidationoptions">CertificateValidationOptions</a></td>
<td>When not set then all options are enabled</td>
</tr>


</tbody>
</table>


[Opcua Protocol Configuration](#opcua-protocol-configuration)

## CertificateValidationOptions

Optional validation options configuration

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


<tr class="even">
<td>KeyUsageIssuer</td>
<td>Key usage must be present and will be checked for CA certificates</td>
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


[Opcua Protocol Configuration](#opcua-protocol-configuration)


[^top](#opcua-protocol-configuration)
