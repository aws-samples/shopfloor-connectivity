## ProtocolAdapterConfiguration
<br>
The ProtocolAdapterConfiguration element contains a set of generic adapter configuration. Each protocol adapter implementation must extend this type with its specific adapter configuration type that contains the additional attributes required for that protocol.
<br>
<br>
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
<td>AdapterType</td>
<td><p>Type of the adapter. These types are predefined for each adapter type (e.g., OPCUA, MQTT,MODBUS-TCP, SNMP, S7, ADS ).</p>
<p>If the adapter is running in the same process as the SFC core module, then it must refer to an entry in the ProtocolAdapterTypes section.</p></td>
<td>String</td>
<td></td>

</tr>
<tr class="odd">
<td>AdapterServer</td>
<td>If the adapter runs as a service in a separate process, then this attribute must refer to an entry for that server in the ProtocolAdapterServers section.</td>
<td>String</td>
<td>If this attribute is not set then the SFC core will load and execute the protocol adapter in the SFC core process. If set then and IPC client will be used to communicate with the service that runs the protocol adapter.</td>

</tr>
</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)

