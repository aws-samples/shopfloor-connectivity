## SFC top level configuration

<table>
	<colgroup>
		<col style="width: 22%" />
		<col style="width: 17%" />
		<col style="width: 37%" />
		<col style="width: 22%" />
	</colgroup>
	<thead>
		<tr class="header">
			<th>
				<strong>Name</strong>
			</th>
			<th>
				<strong>Description</strong>
			</th>
			<th>
				<strong>Type</strong>
			</th>
			<th>Comments</th>
		</tr>
	</thead>
	<tbody>
		<tr class="odd">
			<td>AWSVersion</td>
			<td>Software version must be set to "2022-04-02"</td>
			<td>String</td>
			<td>User for compatibility with future extensions and updates</td>
		</tr>
		<tr class="even">
			<td>Name</td>
			<td>User-defined name of the configuration</td>
			<td>String</td>
			<td>Optional</td>
		</tr>
		<tr class="odd">
			<td>Version</td>
			<td>User-defined version</td>
			<td>Number</td>
			<td>Optional</td>
		</tr>
		<tr class="even">
			<td>LogLevel</td>
			<td>Detail of logged output information</td>
			<td>String, any of "Trace", "Info", "Warning", "Error"</td>
			<td>Optional, the default value is "Info"</td>
		</tr>
		<tr class="odd">
			<td>ElementNames</td>
			<td>
				<p>Names of the output elements. This is a map containing the following entries:</p>
				<p>"
					<strong>Metadata</strong>": Name of metadata added at source level as configured in the optional "Metadata" configured for a schedule, source or channel
				</p>
				<p>
					<strong>"Schedule":</strong> Name of the root element for the schedule.
				</p>
				<p>
					<strong>"Sources":</strong> Name the of the element that contains the sources in the output of a schedule.
				</p>
				<p>"
					<strong>Timestamp</strong>": Name of the timestamp elements.
				</p>
				<p>"
					<strong>Value</strong>": Name of the elements that contains a value.
				</p>
				<p>
					<strong>"Values":</strong> Name of the element in a source that contains the map of channel values for a source.
				</p>
				<p>
					<strong>"Serial"</strong> : Name of the element that contains a unique serial number for target data transmitted to the targets.
				</p>
			</td>
			<td>
				<p>Map(String, String)</p>
				<p>indexed by name of the element</p>
			</td>
			<td>
				<p>Optional, default values for missing element are:</p>
				<p>"Metadata" -&gt; "metadata"</p>
				<p>"Schedule" -&gt; "schedule"</p>
				<p>"Sources" -&gt; "sources"</p>
				<p>"Timestamp" -&gt; "timestamp"</p>
				<p>"Value" -&gt; "value"</p>
				<p>"Values" -&gt; "values"</p>
				<p>"Serial" -&gt; "serial"</p>
			</td>
		</tr>
		<tr class="even">
			<td>Schedules</td>
			<td>List of one or more schedules that define how data is collected from their sources, processed, and send to the targets</td>
			<td><a href="schedule-config.md">Schedule</a></td>
			<td>At least one active schedule needs to be present</td>
		</tr>
		<tr class="odd">
			<td>Sources</td>
			<td>
				<p>Input sources to read data from. This element is a map indexed by the source identifiers of the sources.</p>
				<p>For controlling the schedule and processing the data from the source, the SFC core uses a set of generic configuration attributes which are common for all protocol implementations.</p>
				<p>Implementations of input protocols will define their specific source configurations with additional specific attributes required for that protocol additionally to the common attributes (See SourceConfiguration type)</p>
				<p>The entries of this Sources element will contain protocol-specific entries for the used protocol implementation. The protocol implementation is responsible for reading and handling the protocol-specific attributes.</p>
			</td>
			<td>Map[String, <a href="./source-configuration.md">SourceConfiguration</a>]</td>
			<td>At least 1 source must be configured.</td>
		</tr>
		<tr class="even">
			<td>ProtocolAdapters</td>
			<td>Protocol adapters are the sources to read data from and abstract the actual protocol that is us used to read the data. Each source used in a schedule must have a reference to a protocol adapter. As protocol adapters can be of different types, each inherited type has additional specific attributes for the protocol.</td>
			<td>Map[String, <a href="./protocol-adapter-configuration.md">ProtocolAdapterConfiguration</a>]</td>
			<td></td>
		</tr>
		<tr class="odd">
			<td>ProtocolAdapterTypes</td>
			<td>
				<p>This section includes the information for each protocol adapter type that is used by the SFC core to create instances of that adapter type if that adapter runs in the same process as the SFC core.</p>
				<p>The element is a map indexed by the adapter type (e.g., OPCUA, MODBUS). Each entry contains information on which jar files, that contain the protocol adapter implementation, to load and the factory class to create the instances.</p>
				<p>The SFC core itself is not aware of the actual target implementations and only uses this configuration data to explicitly load the jar files to create and use the adapter instances. This makes it possible to add new protocol adapter types without modifications to the SFC core.</p>
				<p>Note Only types that run in the same process as the SFC core need to be configured. If the core uses IPC to send the data to a target that runs in its process, the type does not have to be defined in the ProtocolAdapterTypes section.</p>
				<p>Only JVM implementations of protocol adapters can be used to run in the same process as the SFC core.</p>
			</td>
			<td>Map[String,<a href="./in-procecess-configuration.md">InProcessConfiguration</a>]</td>
			<td></td>
		</tr>
		<tr class="even">
			<td>ProtocolAdapterServers</td>
			<td>
				<p>Servers that run protocol adapter instances as separate processes. The SFC core will read the data from these servers using a streaming IPC protocol(gRPC) This section is a map indexed by the protocol adapter server identifier. The entries contain the address information that the SFC Core will use to connect and communicate with the IPC service.</p>
				<p>The protocol-servers can be referenced by their identifier from the ProtocolAdapters section of the configuration.</p>
			</td>
			<td>Map[String,<a href="./server-configuration.md">ServerConfiguration</a>]</td>
			<td></td>
		</tr>
		<tr class="odd">
			<td>Targets</td>
			<td>
				<p>Targets are the destinations for data collected and processed by the SFC. Targets defined in this section can be referred to by the target identifier in schedules as their output destinations.</p>
				<p>The element is a map, indexed by the target identifier. The entries contain the target configuration data.</p>
				<p>Targets can be of different types that have specific configuration attributes. Target implementations define their specific configuration types containing the attributes required for communicating with the target.</p>
				<p>For sending the data to the targets the SFC core only uses a subset of attributes that are common between all target types.</p>
			</td>
			<td>Map[String,<a href="./target-configuration.md">TargetConfiguration</a>]</td>
			<td></td>
		</tr>
		<tr class="even">
			<td>TargetTypes</td>
			<td>
				<p>The TargetTypes section includes the information for each target type that is used by the SFC core to create instances of that target if that target runs in the same process as the SFC core.</p>
				<p>The element is a map indexed by the TargetType (e.g., AWS-SQS). Each entry contains information on which jar files, that contain the target implementation, to load and the factory class to create the instances.</p>
				<p>The SFC core itself is not aware of the actual target implementations and only uses this configuration data to explicitly load the jar files to create and use the target instances. This makes it possible to add new target types without modifications to the SFC core.</p>
				<p>Note Only types that run in the same process as the SFC core need to be included in the configuration. If the core uses IPC to send the data to a target that runs in its process, the type does not have to be defined in the TargetTypes section.</p>
				<p>Only JVM implementations of targets can be used to run in the same process as the SFC core.</p>
			</td>
			<td>Map[String,<a href="./in-procecess-configuration.md">InProcessConfiguration</a>]</td>
			<td></td>
		</tr>
		<tr class="odd">
			<td>TargetServers</td>
			<td>
				<p>Servers that run target instances as separate processes. The SFC core will send the data to these targets using IPC (gRPC) This section is a map indexed by the target server identifier. The entries contain the address information that the SFC Core will use to connect and communicate with the IPC service.</p>
				<p>The targets-servers can be referenced by their identifier from the Targets section of the configuration.</p>
			</td>
			<td>Map[String,<a href="./server-configuration.md">ServerConfiguration</a>]</td>
			<td></td>
		</tr>
		<tr class="even">
			<td>SecretsManager</td>
			<td>Gives access to secrets stored in AWS secrets manager which are used to replace placeholders in the configuration</td>
			<td><a href="./secrets-manager-configuration.md">SecretsManagerConfiguration</a></td>
			<td></td>
		</tr>
		<tr class="odd">
			<td>Transformations</td>
			<td>
				<p>Transformations are a sequence of one or more transformation operators that can be applied to values read from input channels and/or aggregated output values.</p>
				<p>This element is a map indexed by transformation identifiers which can be referred to in case a transformation needs to be applied to the data.</p>
				<p>Each entry is a list of one or more transformation operators. An operator consists of the name of the operator and operator-specific parameters. The operators are applied in the order in which they are listed. The output type of the operator must be compatible with the input type of the next operator in the list.</p>
				<p>If a value the transformation is applied to is an array of values, the transformation will be applied to each value in the array.</p>
			</td>
			<td>Map[String,<a href="./transformation-operator-configuration.md">TransformationOperator</a>[]]</td>
			<td>
				<p>Example:</p>
				<p>{</p>
				<p>"DivBy2Add1Round": [</p>
				<p>{</p>
				<p>"Operator" : "Divide",</p>
				<p>"Operand" : 2</p>
				<p>},</p>
				<p>{</p>
				<p>"Operator" : "Add",</p>
				<p>"Operand" : 1</p>
				<p>},</p>
				<p>{</p>
				<p>"Operator" : "Round"</p>
				<p>}</p>
				<p>]</p>
				<p>}</p>
				<p>The transformation with identifier DivBy2Add1Round above Divides the input value by 2, then adds 1 and rounds the result.</p>
			</td>
		</tr>
		<tr class="even">
			<td>ChangeFilters</td>
			<td>Filters that can be applied at source or channel value level to let pass values only if they have changed at all or an absolute or percentage from the previously passed value, or since a time interval.</td>
			<td>Map[String,<a href="./change-filter-configuration.md">ChangeFilterConfiguration</a>]</td>
			<td>
				<p>Example:</p>
				<p>"ChangeFilters" : {
					<br />
"Change1PercentOrPer10Sec" : {
					<br />
"Type": "Percent",
					<br />
"Value": 1,
					<br />
"AtLeast": 10000
					<br />
},
					<br />
"AllChangesOrOncePer10Sec" : {
					<br />
"Type": "Always",
					<br />
"AtLeast": 10000
					<br />
}
					<br />
}
				</p>
			</td>
		</tr>
		<tr class="odd">
			<td>ValueFilters</td>
			<td>Filters that can be applied at channel values level. Values are passed if the value matches the filter expression</td>
			<td>Map[String,<a href="./value-filter-configuration.md">ValueFilterConfiguration</a>]</td>
			<td>
				<p>Example</p>
				<p>"ValueFilters": {</p>
				<p>"ValueEqual5": {</p>
				<p>"Operator": "eq",</p>
				<p>"Value": 5</p>
				<p>},</p>
				<p>"ValueEqualYes": {</p>
				<p>"Operator": "eq",</p>
				<p>"Value": "Yes"</p>
				<p>},</p>
				<p>"ValueInRange0-10": {</p>
				<p>"Operator": "and",</p>
				<p>"Value": [</p>
				<p>{</p>
				<p>"Operator": "ge",</p>
				<p>"Value": 0</p>
				<p>},</p>
				<p>{</p>
				<p>"Operator": "le",</p>
				<p>"Value": 10</p>
				<p>}</p>
				<p>]</p>
				<p>}</p>
				<p>}</p>
			</td>
		</tr>


<tr class="odd">
			<td>ConditionFilters</td>
			<td>Filters that can be applied at channel values level. Values are passed if the value matches the filter expression</td>
			<td>Map[String,<a href="./condition-filter-configuration.md">ConditionFilterConfiguration</a>]</td>
			<td>
			</td>
		</tr>

<tr class="even">
			<td>AwsIotCredentialProviderClients</td>
			<td>Configuration for clients using the AWS IoT Credential Provider Service to obtain session credentials.</td>
			<td>Map[String,<a href="./aws-iot-credential-provider-configuration.md">AwsIotCredentialProviderClientConfiguration</a>]</td>
			<td></td>
		</tr>
		<tr class="odd">
			<td>Metrics</td>
			<td>Metrics collection configuration</td>
			<td></td>
			<td></td>
		</tr>
		<tr class="even">
			<td>ConfigProvider</td>
			<td>Configuration for custom configuration handler</td>
			<td><a href="./in-procecess-configuration.md">InProcessConfiguration</a></td>
			<td></td>
		</tr>
		<tr class="odd">
			<td>LogWriter</td>
			<td>Configuration for custom log writer</td>
			<td><a href="./in-procecess-configuration.md">InProcessConfiguration</a></td>
			<td>Default writer logs to console</td>
		</tr>
		<tr class="even">
			<td>HealthProbe</td>
			<td>Configuration for main process health probe endpoint</td>
			<td><a href="./in-procecess-configuration.md">HealthProbeConfiguration</a></td>
			<td></td>
		</tr>
        <tr class="odd">
			<td>Templates</td>
			<td><a href="../README.md#configuration-templates">Configuration Templates</a></td>
			<td>Map[String,String]</td>
			<td>Map indexed by template names containing JSON objects used as <a href="#configuration templates">SFC configuration templates</a></td>
		</tr>
        <tr class="even">
			<td>Tuning</td>
			<td>SFC tuning parameters</td>
			<td><a href="./tuning-configuration.md">TuningConfiguration</a></td>
			<td></td>
		</tr>
		<tr class="even">
			<td>MonitorIncludedConfigFiles</td>
			<td>Controls the monitoring of included configuration files see <a href="../README.md#including-configuration-sections">Including configuration sections</a></td>
			<td>Boolean</td>
			<td>Default value is true, set value to false to disable monitoring</td>
		</tr>
        <tr class="odd">
			<td>MonitorIncludedConfigContentInterval</td>
		<td>Set the interval in seconds of checking the content from external configuration sources loaded by configured urls, see <a href="../README.md#including-configuration-sections">Including configuration sections</a>	</td>
           <td>Integer</td>
           <td>Default is 60, set to 0 to disable</td>
		</tr>



</table>

[SFC Core Configuration](sfc-core-configuration.md)

[^top](../../README.md#toc)