## InProcessConfiguration
<br>
Configuration data for loading and creating in-process target instances
<br>
<br>
<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 27%" />
<col style="width: 22%" />
<col style="width: 30%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>

</tr>
<tr class="even">
<td>JarFiles</td>
<td><p>List of pathnames to JarFiles, that implement a target type, that needs to be loaded by the SFC core.</p>
<p>These entries can either be pathnames to the jar files of to the directory in which these reside. If the entry is a directory it will expand to a list of all jar files in that directory,</p></td>
<td>String[]</td>
<td></td>

</tr>
<tr class="odd">
<td>FactoryClassName</td>
<td><p>Name of the factory class used to create instances of a source protocol adapter of a target.</p>
<p>For target instances, this class must have a static method named "newInstance".</p>
<p>The signature of the method for protocol adapters have 3 parameters:</p>
<ul>
<li><p>ConfigReader, the reader that can be used by the newly created instance to read its configuration data.</p></li>
<li><p>String, Schedule name for protocol adapters instances and the target</p></li>
</ul>
<p>identifier for the target instances.</p>
<ul>
<li><p>Logger, logger for output of the newly created instance</p></li>
</ul>
<p>The newInstance method for targets has an additional parameter:</p>
<ul>
<li><p>TargetResultHandler, an instance of an object that implements this interface can be passed to let the target return the result of delivering the target data to their destinations. It can be used ACK, NACK and ERROR the serial numbers or full message by using the interface handleResult method. The method returnedData can be called to query what data the result handler expects to be returned. (serial or full message for each of these types).</p></li>
</ul></td>
<td>String</td>
<td></td>

</tr>
</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)