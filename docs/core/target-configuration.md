## TargetConfiguration

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 25%" />
<col style="width: 24%" />
<col style="width: 30%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4"><p>The TargetConfiguration type contains a set of generic target configuration attributes used to send the output data to the targets. Each target implementation must extend this type with its specific target configuration type that contains the additional attributes required for that target type.</p>
<p>The Targets element can contain elements for targets for different target types, where the TargetType specifies its distinct target type.</p></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>
<tr class="even">
<td>Active</td>
<td>Output to a target can be suspended by setting the Active element to false.</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>
<tr class="odd">
<td>TargetType</td>
<td><p>TargetType is a code that identifies the type of the target (e.g., "AWS-SQS", "AWS-KINESIS").</p>
<p>If a target runs in the same process as the SFC core then this type must be defined in the TargetTypes section of the configuration. The SFC core requires the information from that section to create instances of the target type.</p>
<p>Target implementations will typically define the target name, and use it to select and verify the configuration data that is passed to their instances.</p></td>
<td>String</td>
<td>Mandatory</td>
</tr>
<tr class="even">
<td>Server</td>
<td><p>Target server identifier of the server that is running the target as an IPC service in its process. The identifier must exist in the TargetServers section of the configuration.</p>
<p>If a server is used then no in-process instance of the target is created in the SFC core process and the target type does not have to be configured in the TargetTypes section.</p>
<p>The IPC server must implement the (gRPC) ProtocolAdapterService.</p></td>
<td>String</td>
<td>Set to a configured target server to use IPC to send data to a target running as an external IPC service.</td>
</tr>
<tr class="odd">
<td>Template</td>
<td>Pathname to file containing an Apache velocity template that can be applied to transform the output data of the target</td>
<td>String</td>
<td><p>Optional<br />
Context variables for template</p>
<p>$schedule, $sources, $metadata, or names specified in ElementNames configuration.</p></td>
</tr>
<tr class="even">
<td>CredentialProviderClient</td>
<td>The client is used by the target to obtain session credentials from the AWS IoT Credential provider service.</td>
<td>String</td>
<td>Must refer to an existing client configuration in AwsIotCredentialProviderClients section.</td>
</tr>
<tr class="odd">
<td>TargetChannelSize</td>
<td>Size of channel used by target to process and write items</td>
<td>Int</td>
<td>Default is 1000</td>
see <a href="#sfc-tuning">SFC TuningC</a> for more details
</tr>
<tr class="odd">
<td>TargetChannelTimeout</td>
<td>Timeout in milliseconds for writing to internal target channel if it has reached it capacity </td>
<td>Int</td>
<td>Default is 1000</td>
</tr>
</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)

[^top](../../README.md#toc)