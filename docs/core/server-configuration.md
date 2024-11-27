## ServerConfiguration
<br>
Configuration for an external server running an IPC service
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
<td>Port</td>
<td>Port number</td>
<td>Integer</td>
<td>Valid and unique port number per server.</td>

</tr>
<tr class="odd">
<td>Address</td>
<td>IP address or host name</td>
<td>String</td>
<td>The default address is "localhost". If this address is used the IP4 address is resolved to use the local IP address.</td>

</tr>
<tr class="even">
<td>ConnectionType</td>
<td><p>Connection (security) type</p>
<p>-PlainText : No encryption of network traffic between SFC core and protocol adapter or target server</p>
<p>-ServerSideTLS: Encryption of network traffic between SFC core and protocol adapter or target server. Server provides its certificate to client. Requires servers to be started with parameters</p>
<p>-connection set to ServerSideTLS and -key and -cert parameters set to the server's private key and certificate files.</p>
<p>-MutualTLS : Encryption of network traffic between SFC core and protocol adapter or target server. Server and client provide certificate to each other.</p>
<p>Requires servers to be started with parameters</p>
<p>-connection set to MutualTLS, -key and -cert parameters set to the server's private key and certificate files and the -ca parameter set to the ca certificate file. For MutualTLS the client must configure the ClientCertificate, ClientPrivateKey and CaCertificate which are used for the connection with the server.</p></td>
<td>String</td>
<td>Default is "PlainText"</td>

</tr>
<tr class="odd">
<td>ClientCertificate</td>
<td>The pathname of the file containing the certificate used by the client to encrypt network traffic. This parameter only needs to be set when the communication type is MutualTLS.</td>
<td>String</td>
<td>Only required when MutualTLS is used to secure network traffic between SFC core and protocols adapter or target services</td>

</tr>
<tr class="even">
<td>ClientPrivateKey</td>
<td>The pathname of the file containing the private key used by the client to encrypt network traffic. This parameter only needs to be set when the communication type is MutualTLS.</td>
<td>String</td>
<td>Only required when MutualTLS is used to secure network traffic between SFC core and protocols adapter or target services</td>

</tr>
<tr class="odd">
<td>CaCertificate</td>
<td>The pathname of the file containing the CA certificate used by the client to encrypt network traffic. This parameter only needs to be set when the communication type is MutualTLS.</td>
<td>String</td>
<td>Only required when MutualTLS is used to secure network traffic between SFC core and protocols adapter or target services</td>

</tr>
<tr class="even">
<td>ExpirationWarningPeriod</td>
<td>Period in days in which SFC will generate a daily warning and metrics value before a used certificate expires.</td>
<td>Integer</td>
<td>Default is 30, set to 0 to disable.</td>

</tr>
<tr class="odd">
<td>Compression</td>
<td>Enable or disable compression of data exchanged between services. Use this option to reduce the volume of the data exchanged between the services at the cost of CPU load to compress and decompress the data.</td>
<td>Boolean</td>
<td>Default is false</td>

</tr>
<tr class="even">
<td>HealthProbe</td>
<td>Configures the health probe endpoint when the address is used for an SFC service process.</td>
<td><a href="#healthprobeconfiguration">HealthProbeConfiguration</a></td>
<td></td>

</tr>
<tr class="odd">
<td>ServerResultsChannelSize</td>
<td>Size of internal buffer used by IPC servers to send results to the SFC core</td>
<td>Int</td>
<td>Default is 1000</td>

</tr>
<tr class="even">
<td>ServerResultsChannelSize</td>
<td>Timeout in milliseconds to send data to internal results buffer</td>
<td>Int</td>
<td>Default is  10000</td>

</tr>
</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)

