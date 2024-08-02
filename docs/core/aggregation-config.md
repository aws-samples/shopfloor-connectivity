## Aggregation

An optional aggregation can be used for a schedule to collect the results of multiple read values and combine these in a single output message, optimally applying functions to aggregate the output data.

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
<td colspan="2"><strong>Type</strong></td>
<td><strong>Comments</strong></td>

</tr>
<tr class="odd">
<td>Size</td>
<td>The number of values to aggregate before applying the aggregation and sending output to the targets.</td>
<td>Integer</td>
<td>Default is 1, must be 1 or higher</td>

</tr>
<tr class="even">
<td>Output</td>
<td><p>Output values and aggregations to apply to the output data. This element is a two-level map, where the first level contains is indexed by the source identifier. Each entry is another map that is indexed by the channel identifier. Each entry of the map at that level contains the list output elements for the aggregated values for the channel. If the actual channel value holds a list of values, then the aggregations are applied to the values in that list.</p>
<p>Wildcards can be applied at each level of the map. A "*" wildcard can be used as a source identifier and/or channel identifier. If wildcards are used are combined with more specific entries the best matching entry will be applied. The matching will be applied in the following order:</p>
<ul>
<li><p>Source and channel both match</p></li>
<li><p>Source matches, channel wildcard</p></li>
<li><p>Source wildcard, channel matches</p></li>
<li><p>Source and channel are both wildcards</p></li>
</ul>
<p>For example, if there is an entry source1/channel1 and an entry source1/*, then for aggregation of values for a value from source1, channel 1 the first entry will be used, and for any other channel values from source1 the second.</p>
<p>A list with a single "*" wildcard entry can be used to specify the aggregation output values for an entry. In that case, all applicable aggregation outputs for the data type of the channel value will be generated.</p>
<p>In the output, the aggregated values will be sub-elements that have the name of the applied aggregation. So, if the output name of the channel is "a" and the count and stddev are configured as output for that channel, the "a" output element will have two sub-elements named "count" and "stddev" containing the aggregated values.</p>
<p>Possible output elements are:</p>
<p>"<strong>values</strong>": All collected values as an array of values</p>
<p>"<strong>avg</strong>": Average value</p>
<p>"<strong>count</strong>": Number of collected values</p>
<p>"<strong>first</strong>": First collected value</p>
<p>"<strong>last</strong>": Last collected value</p>
<p>"<strong>max</strong>": Max collected value</p>
<p>"<strong>min</strong>": Min collected value</p>
<p>"<strong>median</strong>": Median for collected values</p>
<p>"<strong>mode</strong>": Mode for collected values (value van be a single value or array)</p>
<p>"<strong>stddev</strong>": Stddev for collected values</p>
<p>"<strong>sum</strong>": Sum of collected values</p>
<p>For numeric values the following aggregations can be applied:</p>
<ul>
<li><p>"avg"</p></li>
<li><p>"count"</p></li>
<li><p>"max"</p></li>
<li><p>"median"</p></li>
<li><p>"min"</p></li>
<li><p>"mode"</p></li>
<li><p>"stddev"</p></li>
<li><p>"sum"</p></li>
<li><p>"values"</p></li>
</ul>
<p>For timestamps values the following aggregations can be applied:</p>
<ul>
<li><p>"first"</p></li>
<li><p>"last"</p></li>
</ul>
<p>For other data types the following aggregations can be applied:</p>
<ul>
<li><p>"count"</p></li>
<li><p>"mode"</p></li>
<li><p>"values"</p></li>
</ul>
<p>Besides aggregation of the collected data, reducing the volume of data sent to the targets, aggregation can also be used to reduce the number of calls to the targets by setting the size and just using the "values" aggregation.</p></td>
<td>Map[String,Map[String,String[]]]</td>
<td><p>There must be at least a single output.</p>

Examples:
```json
"Output": {

	"source1": {
       "channel1": [
          "count",
          "avg",
          "min",
          "max"
       ],
       "channel2": [
          "sum",
          "count"
       ],
       "channel3": [
          "*"
       ],
       "*": [
          "values"
       ]
    }
  ``` 

<p>The aggregated values for source1, channel1 will contain the aggregated output count, avg, min, and max.</p>
<p>The aggregated values for source1, channel2 will contain the aggregated output sum and count.</p>
<p>The aggregated values for source1, channel3 will contain the aggregated output applicable to the datatype of the values.</p>
<p>For all other channels for source1, the values and timestamps will contain the values and timestamps.</p>
<p>Output aggregations first and last can have an optional timestamp, depending on the Timestamp level configured for the schedule.</p>
<p>The output values for the mod and values aggregation outputs are arrays of values.</p>
<p>The values aggregation output value is a list of the input values used for the aggregation, which can include a timestamp for each value depending on the Timestamp level configured for the schedule.</p></td>

</tr>
<tr class="odd">
<td>Transformations</td>
<td><p>Transformations applied to the aggregated data. This element is similar to the Output element, but it has an additional map level for the name of the output on which a transformation will be applied.</p>
<p>This element is a three-level map, where the first level contains is indexed by the source identifier. Each entry is another map that is indexed by the channel identifier. Each entry of the map at that level contains a map indexed by the aggregation output e.g., "values", "avg". Each entry contains a single transformation identifier of the transformation that will be applied to the aggregated output value. This transformation identifier must exist in the Transformations section.</p>
<p>Wildcards can be applied at each level of the map. A "*" wildcard can be used at source identifier, channel identifier and/or output name. If wildcards are used are combined with more specific entries the best matching entry will be applied. The matching will be applied in the following order:</p>
<ul>
<li><p>Source, channel and aggregation output name all match (source/channel/output)</p></li>
<li><p>Source matches and channel both match, aggregation output name is a wildcard. (source/channel/*)</p></li>
<li><p>Source matches, the channel is a wildcard, aggregation output name matches (source/*/output)</p></li>
<li><p>The source is wildcard, channel matches, and aggregation output name both match (*/channel/ output)</p></li>
<li><p>Source matches, channel, and aggregation output-name are both wildcards. (source/*/*)</p></li>
<li><p>The source is a wildcard, channel matches, aggregation output name is a wildcard. (*/channel/*)</p></li>
<li><p>Source and channel are wildcards, aggregation output name matches. (*/*/output)</p></li>
<li><p>Source, channel, and aggregation output names are all wildcards (*/*/*)</p></li>
</ul>
<p>If there is no matching entry no transformations will be applied.</p></td>
<td>Map[String,Map[String,Map[String,String]]</td>
<td><p>Example:</p>

```json
"Transformations": {
   "source1": {
      "channel1": {
      "avg": "tr1",
      "min": "tr2",
      "max": "tr2"
    },
    "channel2": {
        "sum": "tr3"
    },
    "channel3": {
        "*": "tr4"
    }
  }
}
```

Transformation "tr1" will be applied to the aggregated "avg" output for the values of "source1", "channel1".</p>
<p>Transformation "tr2" will be applied to the aggregated "min" and "max" output for the values of "source1", "channel1".</p>
<p>Transformation "tr3" will be applied to the aggregated "sum" output for the values of "source1", "channel2".</p>
<p>Transformation "tr4" will be applied to all aggregated values of "source1", "channel3".</p></td>

</tr>
</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)

[^top](../../README.md#toc)