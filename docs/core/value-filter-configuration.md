## ValueFilterConfiguration
<br>
Configuration for value filter that let only pass values if they match the filter expression. When log level is trace then there will be log entries that show the filter expression how it is interpreted by SFC
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
<li><p>"eq" or "=="</p></li>
<li><p>"ne" or "!="</p></li>
<li><p>"gt" or "&gt;"</p></li>
<li><p>"ge" or "&gt;="</p></li>
<li><p>"lt" or "&lt;"</p></li>
<li><p>"le" or "&lt;="</p></li>
<li><p>"and" or "&amp;&amp;"</p></li>
<li><p>"or" or "||"</p></li>
</ul></td>
<td>Must be specified</td>

</tr>
<tr class="odd">
<td>Value</td>
<td><p>Filter value.</p>
<p>If the operator is "and" ("&amp;&amp;") or "or" ("||")it is a nested list of ValueFilterConfigurations that all (and) or any (or) must match for the value to pass. Each filter that is part of an "and" or "or" list can have additional nested "and" ("&amp;&amp;") or "or" ("||") operators.</p></td>
<td>Value to test against using the operator, or a list of nested ValueFilterConfigurations if the operator is "and" ("&amp;&amp;") or "or" ("||").</td>
<td><p>Example of a more complex filter that passes a value if the is equal to 0, or in the range 5 to 10 except when the value is 8:</p>

```json
{
    "Operator": "or",
    "Value": [
       {
         "Operator": "eq",
         "Value": 0
       },
       {
          "Operator": "and",
          "Value": [
             {
               "Operator": "ge",
               "Value": 5
             },
             {
               "Operator": "ne",
               "Value": 8
            },
            {
             "Operator": "le",
             "Value": 10
            }
          ]
       }
    ]
}

```

</td>

</tr>
</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)

[^top](../../README.md#toc)