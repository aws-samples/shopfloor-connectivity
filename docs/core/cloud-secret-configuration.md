## CloudSecretConfiguration


Configured secret in AWS Secrets Manager service to be used to replace placeholders in SFC configuration. Note that the credentials obtained through a credentials client or AWS SDK credential chain must give permission to read the configured values.

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
<td>SecretId</td>
<td>Name or ARN of the secret</td>
<td>String</td>
<td>If the ARN of a secret is used, both the ARN or the name of the read secret can be used as a reference in the placeholder.</td>
</tr>

<tr class="odd">
<td>Alias</td>
<td>Alias for the secret</td>
<td>String</td>
<td>Optional. Alternative local name to reference a secret from a placeholder.</td>
</tr>

<tr class="even">
<td>Labels</td>
<td>Secret labels</td>
<td>String</td>
<td>Labels to specify specific values to read for the secret. Default is AWSCURRENT which is the current value of a secret</td>
</tr>

</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)

