## HealthProbeConfiguration

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 25%" />
<col style="width: 24%" />
<col style="width: 30%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4">Configures a health probe endpoint for an SFC service</th>
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
<td>Path</td>
<td>Path for endpoint URL</td>
<td>String</td>
<td></td>
</tr>
<tr class="odd">
<td>RateLimit</td>
<td>Maximum number of requests that can be made to the endpoint.</td>
<td>Int</td>
<td><p>Default is 10</p>
<p>If this number is exceeded an HTTP 503 error is returned</p></td>
</tr>
<tr class="even">
<td>AllowedIpAddresses</td>
<td>List of IP addresses that are allowed to make calls to the endpoint</td>
<td>[String]</td>
<td><p>Default is an empty list, meaning requests can be made from any address.</p>
<p>IP addresses may contain wildcard sections, e.g., 10.10.10*</p>
<p>HTTP error 403 is returned if the ip address from where the request us made is not in this list.</p></td>
</tr>
<tr class="odd">
<td>Port</td>
<td>Port used for the endpoint</td>
<td>Int</td>
<td>Must be explicitly set and may not be the same as the port use for other endpoints on the same system/network interface</td>
</tr>
<tr class="even">
<td>Interface</td>
<td>Name of the network interface used for the endpoint (e.g., en0) which could be an alternative port as used for communicating with the source devices or other SFC components.</td>
<td>Sting</td>
<td>Default is empty, default IP4 network interface is used</td>
</tr>
<tr class="odd">
<td>RetainStatePeriod</td>
<td>Time in milliseconds to retain last evaluated service status</td>
<td>Int</td>
<td>Default is 1000</td>
</tr>
<tr class="even">
<td>Response</td>
<td>Response used to as a response to a service probe request if the service is healthy</td>
<td>String</td>
<td>Default is OK<br />
Nothing is returned if the service is not healthy.</td>
</tr>
<tr class="odd">
<td>StopAfterUnhealthyPeriod</td>
<td>Period in seconds after which repeated health probe requests did not return a positive result the process will be stopped. This option can be used if the environment which is controlling the instances does not try to stop the unhealthy service instances itself before a new instance is started.</td>
<td>Int</td>
<td>Must be explicitly set in order to stop the service after the period of not returning a healthy response to health probes.</td>
</tr>
</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)

[^top](../../README.md#toc)