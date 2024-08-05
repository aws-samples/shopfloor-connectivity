 
## MetricsWriterConfiguration

Writer used to write collected metrics data. This writer can be configured as an in-process instance or a client to and IPC service. If both options are configured the IPC service is used.
<br><br>

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
<td>MetricsWriter</td>
<td>Jar files implementing the writer</td>
<td><a href="in-process-configuration.md">InProcessConfigurarion</a></td>
<td></td>
</tr>

<tr class="odd">
<td>MetricsServer</td>
<td>Server providing the metrics writer service</td>
<td><a href="server-configuration.md">ServerConfigurartion</a></td>
<td></td>
</tr>

<

</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)

