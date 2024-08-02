## SecretsManagerConfiguration

Configuration data for reading secrets stored in AWS Secrets manger and storing these locally in situations where connectivity is lost<br><br>

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
<td>Secrets</td>
<td>Configured secrets</td>
<td><a href="cloud-secret-configuration.md">[CloudSecretConfiguration]</a></td>
<td></td>
</tr>

<tr class="odd">
<td>PrivateKeyFile</td>
<td>Name of file containing the private key used to encrypt locally stores secrets</td>
<td>String</td>
<td>Default is "sfc-secrets-manager-private-key.pem"</td>
</tr>

<tr class="even">
<td>CertificatesAndKeysByFileReference</td>
<td>Can be set to true to transmit private key by filename to external IPC services. The file name must exist and be accessible in the environment running the service</td>
<td>Boolean</td>
<td>Default is false</td>
</tr>

<tr class="odd">
<td>StoredSecretsFile</td>
<td>Name of the file used to store secrets.</td>
<td>String</td>
<td>Default is " sfc-secrets-manager-secrets"</td>
</tr>

<tr class="even">
<td>StoredSecretsDir</td>
<td>Name of the directory where stored secrets file and optionally also the private key file are created,</td>
<td>String</td>
<td>Default is home directory of user running the process</td>
</tr>

<tr class="odd">
<td>CreatePrivateKeyIfNotExists</td>
<td>If set the file containing a secret key that will be used to encrypt locally stored secrets will be created if it does not exist.</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>

<tr class="even">
<td>CredentialProviderClient</td>
<td>Name of configured credentials client that will be used to read secrets stored in the <a href="aws-iot-credential-provider-configuration.md">AWS Secrets Manager service</a>.</td>
<td>String</td>
<td>If not set the AWS SDK credential provider chain is used.</td>
</tr>

<tr class="odd">
<td>Region</td>
<td>Region of the used AWS Secrets Manager Service</td>
<td>String</td>
<td>AWS Service region</td>
</tr>

<tr class="even">
<td>GreenGrassDeploymentPath</td>
<td>Path to an existing and accessible GreenGrass V2 deployment. If set then the private key used in that deployment is used to encrypt local secrets.</td>
<td>String</td>
<td>Optional<br><br>The typical root directory for Greengrass 2 deployment is /greengrass/v2. The process running the core or target must have access to the file effectiveConfig.yaml in subdirectory config. Note that these directories and files have restricted access.</td>
</tr>


</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)

[^top](../../README.md#toc)


