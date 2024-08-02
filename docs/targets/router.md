# Router Target
<br>
RouterTargetConfiguration extends the type TargetConfiguration with specific configuration data for routing target data to next (primary) targets and alternative and success targets for this these targets. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"ROUTER".</strong>
<br>

- [RouterTargetConfiguration](#routertargetconfiguration)
- [RoutesConfiguration](#routesconfiguration)


[Targets](./targets.md)

## RouterTargetConfiguration

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
<td>Routes</td>
<td><p>Target routing.</p>
<p>This map is indexed by the target IDs of primary targets to which data is routed.</p>
<p>Each entry can have an alternative route to which the data is routed if writing to the primary target fails.</p>
<p>A success target can be specified to which data is routed if the data was written successfully to the primary or alternative target.</p></td>
<td>Map[String, <a href="#routesconfiguration">RoutesConfiguration</a>]</td>
<td>The targets must be targets that are configured either as in-process or IPC service targets in the same configuration.</td>

</tr>
<tr class="odd">
<td>ResultHandlerPolicy</td>
<td><p>Policy used to determine result of routing the data.</p>
<p>AllTargets: Routing is successful if data is written to all configured primary targets or an alternative route for the primary targets</p>
<p>AnyTarget: Routing is successful if data is written to at least one configured primary target or an alternative route for any of the primary targets</p></td>
<td>String</td>
<td>Default is "AllTargets"</td>

</tr>
</tbody>
</table>

## RoutesConfiguration

| RoutesConfiguration contains alternative and success routes for primary targets. |                                                                                                                    |          |          |
|----------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|----------|----------|
| **Name**                                                                         | **Description**                                                                                                    | **Type** | Comments |
| Alternate                                                                        | TargetID for a target to which data is routed if the data cannot be written to a primary target.                   | String   | Optional |
| Success                                                                          | TargetID for a target to which data is routed if the data is written to a primary target or its alternative target |          | Optional |

[^top](#router-target)
