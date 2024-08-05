## TransformationOperator
<br>

A transformation operator consists of an operator name and an optional operand. It is a map that can must have an entry named "Operator" specifying the name of the transformation operator, with an optional entry named "Operand" for the operators that require this.</p>
<p>For any operator that takes a numeric value as its operand either a numeric value, or a string containing a decimal, hexadecimal, or octal number. (See the java Integer.decode method documentation for supported values).</p>
<p>Some operands have aliases that can be used as the operand name
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
<td>Operator</td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Operand</td>
</tr>

<tr class="even">
<td>Abs</td>
<td><p>Calculates absolute value.</p>
<p>Parameter: ""</p></td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="odd">
<td>Aos</td>
<td>Computes the arc; the returned value is an angle in the range from 0.0 to PI radians.</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="even">
<td><p>And</p>
<p>alias is "&amp;"</p></td>
<td>Bitwise and of value and parameter.</td>
<td>Datatype: Int, Byte, Short, Long</td>
<td>Mask for AND operation</td>
</tr>

<tr class="odd">
<td>Asin</td>
<td>Computes the arc; the returned value is an angle in the range from -PI/2 to PI/2 radians</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="even">
<td>Atan</td>
<td>Computes the arc; the returned value is an angle in the range from -PI/2 to PI/2 radians</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="odd">
<td><p>AtIndex</p>
<p>Alias is "[]"</p></td>
<td>Returns item from an array at the specified index.</td>
<td>Datatype: Any[]</td>
<td>Index for value to return, must be in the range of the array (0..items-1)</td>
</tr>

<tr class="even">
<td>BoolToNumber</td>
<td>Converts Boolean value to a number. False value returns 0, True value returns 1</td>
<td>Datatype: Boolean</td>
<td></td>
</tr>

<tr class="odd">
<td>BytesToInt16</td>
<td>Converts array of two bytes to a 16-bit integer (Big-Endian)</td>
<td>Datatype: byte[2]</td>
<td></td>
</tr>

<tr class="even">
<td>Ceil</td>
<td>Rounds value up to the next largest integer.</td>
<td>Datatypes: Numeric</td>
<td></td>
</tr>

<tr class="odd">
<td>Celsius</td>
<td>Converts Fahrenheit temperature to Celsius.</td>
<td>Datatypes: Numeric</td>
<td></td>
</tr>

<tr class="even">
<td>Cos</td>
<td>Computes the cosine of the angle given in radians</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="odd">
<td>Cosh</td>
<td>Computes the hyperbolic cosine</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="even">
<td>DecodeToString</td>
<td>Decodes byte arrays (and ByteStrings) into UTF-8 String</td>
<td>Datatype: Byte[]</td>
<td></td>
</tr>

<tr class="odd">
<td><p>Divide</p>
<p>Alias is "/"</p></td>
<td>Divides values</td>
<td>Datatypes: Numeric</td>
<td>Divider (must be &gt; 0)</td>
</tr>

<tr class="even">
<td>Exp</td>
<td>Computes Euler's number e raised to the power of the value</td>
<td>Datatypes: Numeric</td>
<td></td>
</tr>

<tr class="odd">
<td>Fahrenheit</td>
<td>Converts Celsius temperature to Fahrenheit.</td>
<td>Datatypes: Numeric</td>
<td></td>
</tr>

<tr class="even">
<td>EpocMilliSecondsToTimestamp</td>
<td>Obtains a DateTime using milliseconds from the epoch of 1970-01-01T00:00:00Z.</td>
<td>Datatypes: Long</td>
<td></td>
</tr>

<tr class="odd">
<td>EpocSecondsToTimestamp</td>
<td>Obtains a DateTime using seconds from the epoch of 1970-01-01T00:00:00Z.</td>
<td>Datatypes: Long</td>
<td></td>
</tr>

<tr class="even">
<td>Floor</td>
<td>Calculates the largest integer less than or equal to the value.</td>
<td>Datatypes: Numeric</td>
<td></td>
</tr>

<tr class="odd">
<td>Int16sToInt32</td>
<td>Converts an array of two 16-bit values to a single 32-bit value.</td>
<td>Datatypes: int16[2]</td>
<td></td>
</tr>

<tr class="even">
<td>Int16ToBytes</td>
<td>Converts a 16-bit value in an array of 2 8-bit values.</td>
<td>Datatype: 16-bit value</td>
<td></td>
</tr>

<tr class="odd">
<td>Int32ToInt16s</td>
<td>Converts a 32-bit value in an array of 2 16-bit values.</td>
<td>Datatype: 32-bit Value</td>
<td></td>
</tr>

<tr class="even">
<td>IsoTimeStrToMilliSeconds</td>
<td>Converts a string in ISO-8601 duration format into milliseconds.
Converts a string in a format such as 2007-12-03T10:15:30.00Z into the number of milliseconds from the epoch of 1970-01-01T00:00:00Z.
The string must represent a valid instant in UTC</td>
<td>Datatype : String</td>
<td></td>
</tr>

<tr class="odd">
<td>IsoTimeStrSeconds</td>
<td>Converts a string in ISO-8601 duration format into milliseconds.
Converts a string in a format such as 2007-12-03T10:15:30.00Z into the number of seconds from the epoch of 1970-01-01T00:00:00Z.
The string must represent a valid instant in UTC</td>
<td>Datatype : String</td>
<td></td>
<tr class="even">
<td>IsoTimeStrToMilliSeconds</td>
<td>Converts a string in ISO-8601 duration format into milliseconds</td>
<td>Datatype : String</td>
<td></td>
</tr>

<tr class="odd">
<td>IsoTimeStrToNanoSeconds</td>
<td>Converts a string in ISO-8601 duration format into nanoseconds</td>
<td>Datatype : String</td>
<td></td>
</tr>

<tr class="even">
<td>IsoTimeStrToSeconds</td>
<td>Converts a string in ISO-8601 duration format into seconds</td>
<td>Datatype : String</td>
<td></td>
</tr>

<tr class="odd">
<td>Ln</td>
<td>Computes the natural logarithm (base E)</td>
<td>Datatypes: Numeric</td>
<td></td>
</tr>

<tr class="even">
<td>Log10</td>
<td>Computes the common logarithm (base 10)</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="odd">
<td>LowerCase</td>
<td>Converts string to lowercase.</td>
<td>Datatype: String</td>
<td></td>
</tr>

<tr class="even">
<td>MapRange</td>
<td>Maps numeric ranges.</td>
<td>Datatype: Numeric</td>
<td><p>Structure containing both input and output range.</p>

```json
{
   "Operator" : "MapRange",
   "Operand" : {
        "From": {
           "MinValue": 0,
           "MaxValue": 1024
        },
        "To": {
          "MinValue": 0,
          "MaxValue": 100
       }
   }
}
```
<p>Maps range 0-1024 to range 1-100</p></td>
</tr>

<tr class="odd">
<td>MapStringToNumber</td>
<td>Maps a string value to an integer value or default value.</td>
<td>Datatype: String</td>
<td><p>

```json
{
   "Operator": "MapStringToNumber",
   "Operand": {
     "Mapping": {
       "A": 1,
       "B": 2
     },
     "Default": 0
   }
}

```

<p>Mappings: Map containing the string to value mapping</p>
<p>Default: Default value is there is no mapping (default is 0)</p></td>
</tr>

<tr class="even">
<td>Max</td>
<td>Returns greater of value or parameter value.</td>
<td>Datatype: Numeric</td>
<td>Numeric test value</td>
</tr>

<tr class="odd">
<td>Min</td>
<td>Return smaller of value or parameter value.</td>
<td>Datatype: Numeric</td>
<td>Numeric test value</td>
</tr>

<tr class="even">
<td><p>Minus</p>
<p>alias is "-"</p></td> 
<td>Subtracts parameter value from value.</td>
<td>Datatype: Numeric</td>
<td>Numeric value to subtract</td>
</tr>

<tr class="odd">
<td><p>Mod</p>
<p>alias is "%"</p></td>
<td>Calculates the remainder of dividing value by parameter value.</td>
<td>Datatype: Numeric</td>
<td>Parameter: Divider</td>
</tr>

<tr class="even">
<td><p>Multiply</p>
<p>alias is "*"</p></td>
<td>Multiplies value by parameter value.</td>
<td>Datatype: Numeric</td>
<td>Parameter: Multiplier</td>
</tr>

<tr class="odd">
<td><p>Not</p>
<p>alias is "!"</p></td>
<td>Inverts a Boolean value</td>
<td>Datatype : Boolean</td>
<td></td>
</tr>

<tr class="even">
<td><p>Or</p>
<p>Alias = "|"</p></td>
<td>Bitwise or of value and parameter.</td>
<td>Datatype: Int, Byte, Short, Long</td>
<td>Parameter: or value</td>
</tr>

<tr class="odd">
<td>OutsideRangeExclusive</td>
<td>Test if a value is outside an exclusive range</td>
<td>Datatype : Numeric</td>
<td><p>

```json
{
   "Operator": "OutsideRangeExclusive",
   "Operand": {
      "MinValue": 0,
      "MaxValue": 100
   }
}
```

</td>
</tr>

<tr class="even">
<td>OutsideRangeInclusive</td>
<td>Test if a value is outside an inclusive range</td>
<td>Datatype: Numeric</td>
<td>

```json
{
   "Operator": "OutsideRangeInclusive",
   "Operand": {
      "MinValue": 0,
      "MaxValue": 100
   }
}
```


</td>
</tr>

<tr class="odd">
<td><p>Plus</p>
<p>alias is "+" or "Add"</p></td>
<td>Adds the value of the parameter to value.</td>
<td>Datatype: Numeric</td>
<td>Numeric value to add</td>
</tr>

<tr class="even">
<td>ParseInt</td>
<td>Parses string value as an integer number. The string must be a valid representation of a number.</td>
<td>Datatype: String</td>
<td></td>
</tr>

<tr class="odd">
<td>ParseNumber</td>
<td>Parses string value as a double number. The string must be a valid representation of a number.</td>
<td>Datatype: String</td>
<td></td>
</tr>

<tr class="even">
<td>Query</td>
<td>Evaluate a <a href="https://jmespath.org/">JMESpath</a> query against structured data type and returns the result.</td>
<td>Datatype: Structure or array</td>
<td>Parameter: <a href="https://jmespath.org/">JMESPath</a> expression, see https://jmespath.org/</td>
</tr>

<tr class="odd">
<td>Round</td>
<td>Rounds the given value towards the closest integer.</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="even">
<td>Shl</td>
<td>Shifts this value left by a bit-count number of bits.</td>
<td>Datatype: Int, Byte, Short, Long</td>
<td>Parameter: bit-count</td>
</tr>

<tr class="odd">
<td>Shr</td>
<td>Shifts this value right by a bit-count number of bits.</td>
<td>Datatype: Int, Byte, Short, Long</td>
<td>Parameter: bit-count</td>
</tr>

<tr class="even">
<td>Sin</td>
<td>Computes the sine of the angle given in radians</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="odd">
<td>Sign</td>
<td><p>Returns the sign of the value.</p>
<p>-1.0 if the value is negative,</p>
<p>zero if the value is zero,</p>
<p>1.0 if the value is positive</p></td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="even">
<td>Sinh</td>
<td>Computes the hyperbolic sine of the value</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="odd">
<td>Sqrt</td>
<td>Computes the positive square root</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="even">
<td>Str</td>
<td>String representation of a number.</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="odd">
<td>StrEquals</td>
<td>Compares string value with a string parameter.</td>
<td>Datatype: String</td>
<td>Parameter: String test for equality</td>
</tr>

<tr class="even">
<td>SubString</td>
<td><p>Returns the substring of string value starting at the start and ending right before the end.</p>
<p>Start and End are zero based indexes when positive and automatically limited to the max length of the input string.</p>
<p>Extracts first 4 characters of a string</p>
<p>When using negative values, it is the offset from end of the input string (-1 is last character).</p>
<p>If Start is omitted its default value is 0, for the beginning of the string.</p>
<p>If End is omitted the default value is the end of the input string (length + 1)</p></td>
<td>Datatype: String</td>
<td>

```json

{
   "Operator": "SubString",
   "Operand": {
       "Start": 2,
       "End": 4
   }
}
```

<p>Start and End are zero based indexes when positive and automatically limited to the max length of the input string.</p>
<p>Extracts first 4 characters of a string</p>
<p>When using negative values, it is the offset from end of the input string (-1 is last character).</p>
<p>If Start is omitted its default value is 0, for the beginning of the string.</p>
<p>If End is omitted the default value is the end of the input string (length + 1)</p></td>
</tr>

<tr class="odd">
<td>Tan</td>
<td>Computes the tangent of the angle given in radians</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="even">
<td>Tanh</td>
<td>Computes the hyperbolic tangent of the value</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="odd">
<td>TimestampToEpocMilliSeconds</td>
<td>Converts a datetime value  to the number of milliseconds from the epoch of 1970-01-01T00:00:00Z.</td>
<td>Datatype: DateTime/Timestamp</td>
<td></td>
</tr>

<tr class="even">
<td>TimestampToEpocSeconds</td>
<td>Converts a datetime value  to the number of seconds from the epoch of 1970-01-01T00:00:00Z.</td>
<td>Datatype: DateTime/Timestamp</td>
<td></td>
</tr>

<tr class="odd">
<td>ToByte</td>
<td>Converts numeric value to a byte value</td>
<td>Datatype: Numeric</td>
<td>No operand</td>
</tr>

<tr class="even">
<td>ToDouble</td>
<td>Converts numeric value to a double value</td>
<td>Datatype: Numeric</td>
<td>No operand</td>
</tr>

<tr class="odd">
<td>ToFloat</td>
<td>Converts numeric value to a float value</td>
<td>Datatype: Numeric</td>
<td>No operand</td>
</tr>

<tr class="even">
<td>ToInt</td>
<td>Converts numeric value to an int value</td>
<td>Datatype: Numeric</td>
<td>No operand</td>
</tr>

<tr class="odd">
<td>ToLong</td>
<td>Converts numeric value to a long value</td>
<td>Datatype: Numeric</td>
<td>No operand</td>
</tr>

<tr class="even">
<td>Trunc</td>
<td>Rounds the given value to an integer towards zero.</td>
<td>Datatype: Numeric</td>
<td></td>
</tr>

<tr class="odd">
<td>TruncAt</td>
<td>Rounds the given value to a value with a specified number of decimals.</td>
<td>Datatype: Numeric</td>
<td>Number of decimals to truncate value at</td>
</tr>

<tr class="even">
<td>UpperCase</td>
<td>Converts a string value to uppercase.</td>
<td>Datatype: String</td>
<td></td>
</tr>

<tr class="odd">
<td>WithinRangeExclusive</td>
<td>Test if a value is within an exclusive range</td>
<td>Datatype : Numeric</td>
<td>

```json
{
    "Operator": "WithinRangeExclusive",
    "Operand": {
        "MinValue": 0,
        "MaxValue": 100
    }
}
```

</td>
</tr>

<tr class="even">
<td>WithinRangeInclusive</td>
<td>Test if a value is within an inclusive range</td>
<td>Datatype: Numeric</td>
<td>

```json
{
"Operator": "WithinRangeInclusive",
"Operand": {
"MinValue": 0,
"MaxValue": 100
}
}
```

</td>
</tr>

<tr class="odd">
<td><p>Xor</p>
<p>alias is "^"</p></td>
<td>Bitwise xor of value and parameter.</td>
<td>Datatype: Int, Byte, Short, Long</td>
<td>Parameter: xor value</td>
</tr>

</tbody>
</table>

[SfcTopLevelConfiguration](sfc-top-level-config.md)
