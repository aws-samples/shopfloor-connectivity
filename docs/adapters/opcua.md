# OPCUA Protocol Configuration

This section describes the configuration types for the OPCUA protocol adapter and contains the extensions and specific
configuration types


  - [OpcuaSourceConfiguration](#opcdasourceconfiguration)
  - [OpcuaNodeChannelConfiguration](#opcdachannelconfiguration)
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

The OpcuaSourceConfiguration extends the common <a href="../core/source-configuration.md" >Source configuration</a> with OPCUA specific source configuration data

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
