## ConditionFilterConfiguration
<br>
Configuration for value filter that let only pass values if they match the condition filter expression. When log level is trace then there will be log entries that show the filter expression how it is interpreted by SFC.
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
<td>Operator</td>
<td>Filter operator to apply</td>
<td><p>String, value must be any of these operators:</p>
<ul>
<li><p>"all" or "##"</p></li>
<li><p>"any" or "**"</p></li>
<li><p>"none" or "!!"</p></li>
<li><p>"present" or "#"</p></li>
<li><p>"absent" or "&!"</p></li>
<li><p>"only" or "^"</p></li>
<li><p>"notonly" or "$"</p></li>
<li><p>"and" or "&amp;&amp;"</p></li>
<li><p>"or" or "||"</p></li>
</ul></td>
<td>Must be specified</td>

</tr>
<tr class="odd">
<td>Value</td>
<td><p>Filter value.</p>
<p>If the operator is "and" ("&amp;&amp;") or "or" ("||")it is a nested list of Condition that all (and) or any (or) must match for the value to pass. Each filter that is part of an "and" or "or" list can have additional nested "and" ("&amp;&amp;") or "or" ("||") operators.</p></td>
<td>String, String[], Boolean or list of Conditions</td>
<td>Value used by the filter operator, or a list of nested ConditionConfigurations if the operator is "and" ("&amp;&amp;") or "or" ("||"). If the value for an operator is a value name or a list of value names, the name is the key of the value in the channels table for a source. Valid JMESPath expressions van be used as well to specify values names to match against.</td>

</tr>
</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)
