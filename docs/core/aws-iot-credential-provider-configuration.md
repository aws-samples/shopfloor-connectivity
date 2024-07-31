## AwsIotCredentialProviderClientConfiguration

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 25%" />
<col style="width: 24%" />
<col style="width: 0%" />
<col style="width: 30%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="5">Configuration data for clients accessing the AWS IoT Credential Provider service. Obtaining the credentials from this service is described in https://aws.amazon.com/blogs/security/how-to-eliminate-the-need-for-hardcoded-aws-credentials-in-devices-by-using-the-aws-iot-credentials-provider/</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td colspan="2"><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>
<tr class="even">
<td>IotCredentialEndpoint</td>
<td>Endpoint for credential provider service</td>
<td colspan="2">String</td>
<td><p>Can be obtained by CLI command<br />
aws iot describe-endpoint --endpoint-type iot:CredentialProvider.</p>
<p>Format is &lt;your_aws_account_specific_prefix&gt;.credentials.&lt;region&gt;.amazonaws.com</p></td>
</tr>
<tr class="odd">
<td>RoleAlias</td>
<td>Alias pointing to an IAM role. The credentials provider request must include a role alias name to indicate which IAM role to assume for obtaining a security token from AWS</td>
<td colspan="2">String</td>
<td></td>
</tr>
<tr class="even">
<td>ThingName</td>
<td>AWS IoT thing name using the device certificate</td>
<td colspan="2">String</td>
<td></td>
</tr>
<tr class="odd">
<td>CertificatesByFileReference</td>
<td>Option to pass credential provider-client certificate and key file by filename (true) or by content (false).</td>
<td>Boolean</td>
<td colspan="2">Default is false</td>
</tr>
<tr class="even">
<td>CertificateFile</td>
<td>The pathname of the device certificate file</td>
<td colspan="2">String</td>
<td></td>
</tr>
<tr class="odd">
<td>PrivateKeyFile</td>
<td>Path to the private key file for the device</td>
<td colspan="2">String</td>
<td></td>
</tr>
<tr class="even">
<td>RootCA</td>
<td>The pathname of the root CA certificate file</td>
<td colspan="2">String</td>
<td></td>
</tr>
<tr class="odd">
<td>GreenGrassDeploymentPath</td>
<td>Pathname for the root of Greengrass deployment. If set then the ClientProxyConfiguration IotCredentialEndpoint, RoleAlias, ThingName, Certificate, PrivateKey, RootCA, and Proxy will be read from the GreenGrass configuration file. If any of these is specified then it will override the setting read from the configuration file.</td>
<td colspan="2">String</td>
<td>The typical root directory for Greengrass 2 deployment is /greengrass/v2. The process running the core or target must have access to the file effectiveConfig.yaml in subdirectory config. Note that these directories and files have restricted access.</td>
</tr>
<tr class="even">
<td>SkipCredentialsExpiryCheck</td>
<td>For systems that don't have a reliable clock time, this setting can be set to true to skip the expiry date of the credentials. This may result in failing API Service calls due to expired session credentials, which must be handled in the target implementation.</td>
<td colspan="2">Boolean</td>
<td>Default = false</td>
</tr>
<tr class="odd">
<td>ExpiryClockSkewSeconds</td>
<td>Seconds that will be added to the system time when checking the expiration of the credentials. New credentials will be retrieved when the clock time of the system plus these number of seconds is beyond the credentials expiration time. This value can be set to avoid using expired credentials when the system clock runs slightly behinds</td>
<td colspan="2">Int</td>
<td>Default = 300</td>
</tr>
<tr class="even">
<td>Proxy</td>
<td>Proxy configuration if the client is using a proxy server to access the internet.</td>
<td colspan="2"></td>
<td>Optional</td>
</tr>
</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)

[^top](../../README.md#toc)