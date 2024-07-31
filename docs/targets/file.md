# File Target

[Targets](./targets.md)

## FileConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 26%" />
<col style="width: 28%" />
<col style="width: 26%" />
</colgroup>
<thead>
<tr class="header">
<th colspan="4">FileConfiguration extends the type TargetConfiguration with specific configuration data for writing data to the local file system. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"FILE-TARGET".</strong></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>
<tr class="even">
<td>BufferSize</td>
<td>Size in KB after which the internal buffer is written to an output file</td>
<td>Int</td>
<td>Must be in range 1-1024KB, default is 16KB</td>
</tr>
<tr class="odd">
<td>Directory</td>
<td>Directory where the output files are created.</td>
<td>String</td>
<td>The name of the output files in the directory will be yyyy/mm/dd/hh/mn/uuid.&lt;extension&gt;</td>
</tr>
<tr class="even">
<td>Extension</td>
<td>Extension used for the output files</td>
<td>String</td>
<td>If no extension is specified, but the file is compressed then the corresponding extension for the compression method is used. For compression types that support entry names (e.g., zip) the extension of the entry will be set to ".json" if the Json field is true,</td>
</tr>
<tr class="odd">
<td>Interval</td>
<td>Interval in seconds after which the internal buffer is written to an output file.</td>
<td>Int</td>
<td>Must be in range 60-900 seconds, default is 60 seconds</td>
</tr>
<tr class="even">
<td>Compression</td>
<td>Compression used to compress the data in the file</td>
<td>"None" | "GZip" | "Zip"</td>
<td>Default is "None"</td>
</tr>
<tr class="odd">
<td>Json</td>
<td><p>Flag to indicate if the lines in the output file must form a valid JSON document. The target does this by wrapping the output in a '[' and ']' character and separating each line by a ',' character, making the output a JSON array.</p>
<p>If not set the output may be processed as JSONP or text file.</p></td>
<td>Boolean</td>
<td>Default is true</td>
</tr>
<tr class="even">
<td>UtcTime</td>
<td>If set to true then UTC time is used to build the name of the output file, otherwise the local date and time of the system running the adapter is used.</td>
<td>Boolean</td>
<td>Default is false</td>
</tr>
</tbody>
</table>

[^top](#file-target)
