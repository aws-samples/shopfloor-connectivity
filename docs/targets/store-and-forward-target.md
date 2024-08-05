# Store and Forward Target
<br>
StoreForwardTargetConfiguration extends the type TargetConfiguration with specific configuration data for forwarding and buffering target data to next targets configured for this target. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"STORE-FORWARD".</strong>
<br>
<br>

[Targets](./README.md)


## StoreForwardTargetConfiguration

<table>
<colgroup>
<col style="width: 18%" />
<col style="width: 26%" />
<col style="width: 28%" />
<col style="width: 26%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>
<tr class="even">
<td>Directory</td>
<td>Pathname of the directory in which to store buffered messages.</td>
<td>String</td>
<td>This directory must already exist and the process running the Store and Forward target must have read and write access</td>
</tr>
<tr class="odd">
<td>Targets</td>
<td>Targets for which to store and forward target data messages.</td>
<td>String[]</td>
<td>The targets must be targets that are configured either as in-process or IPC service targets in the same configuration.</td>
</tr>
<tr class="even">
<td>WriteTimeout</td>
<td>Timeout for write actions to the storage device in seconds</td>
<td>Int</td>
<td>Default is 10</td>
</tr>
<tr class="odd">
<td>RetainPeriod</td>
<td>Period in minutes in which buffered messages are kept in the buffer before deleted.</td>
<td>Int</td>
<td><p>Minimum is 1 (minute)</p>
<p>At least one, but not more than one retention strategy must be used.</p></td>
</tr>
<tr class="even">
<td>RetainFiles</td>
<td>Number of files per target to buffer for a target before they are deleted from the buffer.</td>
<td>Int</td>
<td><p>Minimum is 100</p>
<p>At least one, but not more than one retention strategy must be used.</p></td>
</tr>
<tr class="odd">
<td>RetainSize</td>
<td>Size of files in MB per target to buffer for a target before they are deleted from the buffer.</td>
<td>Int</td>
<td><p>Minimum is 1 (MB)</p>
<p>At least one, but not more than one retention strategy must be used.</p></td>
</tr>
<tr class="even">
<td>Fifo</td>
<td>If true the buffer operates in FIFO mode, meaning oldest messages that fall into the retention strategy are resubmitted first. If false then the most recent messages are sent first.</td>
<td>Boolean</td>
<td>Default is true</td>
</tr>
<tr class="odd">
<td>CleanupInterval</td>
<td>Interval in seconds in which the internal cleanup procedure is executed when the target is in buffering mode.</td>
<td>Int</td>
<td>Default is 60</td>
</tr>
</tbody>
</table>

[^top](#store-and-forward-target)