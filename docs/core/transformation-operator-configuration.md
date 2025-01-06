## TransformationOperator


**Properties:**
- [Abs](#Abs)
- [And
alias is "&"](#And
alias is "&")
- [Aos](#Aos)
- [Asin](#Asin)
- [AtIndex
Alias is "[]"](#AtIndex
Alias is "[]")
- [Atan](#Atan)
- [BoolToNumber](#BoolToNumber)
- [BytesToDoubleBE](#BytesToDoubleBE)
- [BytesToDoubleLE](#BytesToDoubleLE)
- [BytesToFloatBE](#BytesToFloatBE)
- [BytesToFloatLE](#BytesToFloatLE)
- [BytesToInt16](#BytesToInt16)
- [Ceil](#Ceil)
- [Celsius](#Celsius)
- [Chunked](#Chunked)
- [Cos](#Cos)
- [Cosh](#Cosh)
- [DecodeToString](#DecodeToString)
- [Divide
Alias is "/"](#Divide
Alias is "/")
- [EpocMilliSecondsToTimestamp](#EpocMilliSecondsToTimestamp)
- [EpocSecondsToTimestamp](#EpocSecondsToTimestamp)
- [Equals](#Equals)
- [Exp](#Exp)
- [Fahrenheit](#Fahrenheit)
- [Flatten](#Flatten)
- [Floor](#Floor)
- [Int16ToBytes](#Int16ToBytes)
- [Int16sToInt32](#Int16sToInt32)
- [Int32ToInt16s](#Int32ToInt16s)
- [IsoTimeStrSeconds](#IsoTimeStrSeconds)
- [IsoTimeStrToEpocSeconds](#IsoTimeStrToEpocSeconds)
- [IsoTimeStrToMilliSeconds](#IsoTimeStrToMilliSeconds)
- [IsoTimeStrToNanoSeconds](#IsoTimeStrToNanoSeconds)
- [IsoTimeStrToSeconds](#IsoTimeStrToSeconds)
- [Ln](#Ln)
- [Log10](#Log10)
- [LowerCase](#LowerCase)
- [MapRange](#MapRange)
- [MapStringToNumber](#MapStringToNumber)
- [Max](#Max)
- [Min](#Min)
- [Minus
alias is "-"](#Minus
alias is "-")
- [Mod
alias is "%"](#Mod
alias is "%")
- [Multiply
alias is "*"](#Multiply
alias is "*")
- [Not
alias is "!"](#Not
alias is "!")
- [NumbersToFloatBE](#NumbersToFloatBE)
- [NumbersToFloatLE](#NumbersToFloatLE)
- [Operator](#Operator)
- [Or
Alias = "|"](#Or
Alias = "|")
- [OutsideRangeExclusive](#OutsideRangeExclusive)
- [OutsideRangeInclusive](#OutsideRangeInclusive)
- [ParseInt](#ParseInt)
- [ParseNumber](#ParseNumber)
- [Plus
alias is "+" or "Add"](#Plus
alias is "+" or "Add")
- [Query](#Query)
- [ReverseList](#ReverseList)
- [Round](#Round)
- [Shl](#Shl)
- [Shr](#Shr)
- [Sign](#Sign)
- [Sin](#Sin)
- [Sinh](#Sinh)
- [Sqrt](#Sqrt)
- [Str](#Str)
- [StrEquals](#StrEquals)
- [SubString](#SubString)
- [Tan](#Tan)
- [Tanh](#Tanh)
- [TimestampToEpocMilliSeconds](#TimestampToEpocMilliSeconds)
- [TimestampToEpocSeconds](#TimestampToEpocSeconds)
- [ToByte](#ToByte)
- [ToDouble](#ToDouble)
- [ToFloat](#ToFloat)
- [ToInt](#ToInt)
- [ToLong](#ToLong)
- [ToShort](#ToShort)
- [ToSigned](#ToSigned)
- [ToUnsigned](#ToUnsigned)
- [Trunc](#Trunc)
- [TruncAt](#TruncAt)
- [UpperCase](#UpperCase)
- [WithinRangeExclusive](#WithinRangeExclusive)
- [WithinRangeInclusive](#WithinRangeInclusive)
- [Xor
alias is "^"](#Xor
alias is "^")

---
### Abs
Calculates absolute value.

**Type**: Datatype: Numeric

**Oparand**: None

---
### And
Alias is "&"
Bitwise and of value and parameter.

**Type**: Datatype: Int, Byte, Short, Long

**Operand**: Mask for AND operation

---
### Arc
Computes the arc; the returned value is an angle in the range from 0.0 to PI radians.

**Type**: Datatype: Numeric

**Oparand**: None

---
### Asin
Computes the arc; the returned value is an angle in the range from -PI/2 to PI/2 radians

**Type**: Datatype: Numeric

**Oparand**: None

---
### AtIndex
Alias is "[]"
Returns item from an array at the specified index.

**Type**: Datatype: Any[]

**Operand:** Index for value to return, must be in the range of the array (0..items-1)

---
### Atan
Computes the arc; the returned value is an angle in the range from -PI/2 to PI/2 radians

**Type**: Datatype: Numeric

**Oparand**: None

---
### BoolToNumber
Converts Boolean value to a number. False value returns 0, True value returns 1

**Type**: Datatype: Boolean

**Oparand**: None

---
### BytesToDoubleBE
Converts array of 8 bytes to a float value (Big-Endian)

**Type**: Datatype: byte[8]

**Oparand**: None

---
### BytesToDoubleLE
Converts array of 8 bytes to a float value (Little-Endian)

**Type**: Datatype: byte[8]

**Oparand**: None

---
### BytesToFloatBE
Converts array of 4 bytes to a float value (Big-Endian)

**Type**: Datatype: byte[4]

**Oparand**: None

---
### BytesToFloatLE
Converts array of 4 bytes to a float value (Little-Endian)

**Type**: Datatype: byte[4]

**Oparand**: None

---
### BytesToInt16
Converts array of two bytes to a 16-bit integer (Big-Endian)

**Type**: Datatype: byte[2]

**Oparand**: None

---
### Ceil
Rounds value up to the next largest integer.

**Type**: Datatypes: Numeric

**Oparand**: None

---
### Celsius
Converts Fahrenheit temperature to Celsius.

**Type**: Datatypes: Numeric

**Oparand**: None

---
### Chunked
Splits a value, which contains a list of values, into a lists of smaller lists, containing the specified chunk size. The last list may contain items less than the specified chunk size.

**Type**: Datatypes: Lists

**Oparand**: Chunck size

---
### Cos
Computes the cosine of the angle given in radians

**Type**: Datatype: Numeric

**Oparand**: None

---
### Cosh
Computes the hyperbolic cosine

**Type**: Datatype: Numeric

**Oparand**: None

---
### DecodeToString
Decodes byte arrays (and ByteStrings) into UTF-8 String

**Type**: Datatype: Byte[]

**Oparand**: None

---
### Divide
Alias is "/"
Divides values

**Type**: Datatypes: Numeric

**Operand:** Divider (must be > 0)

---
### EpocMilliSecondsToTimestamp
Obtains a DateTime using milliseconds from the epoch of 1970-01-01T00:00:00Z.

**Type**: Datatypes: Long

**Oparand**: None

---
### EpocSecondsToTimestamp
Obtains a DateTime using seconds from the epoch of 1970-01-01T00:00:00Z.

**Type**: Datatypes: Long

**Oparand**: None

---
### Equals
Compares two numbers. To compare unsigned numbers convert number to a signed number using ToSigned operator.

**Type**: Datatype: Number

Operand:  Number to test for equality

---
### Exp
Computes Euler's number e raised to the power of the value

**Type**: Datatypes: Numeric

**Oparand**: None

---
### Fahrenheit
Converts Celsius temperature to Fahrenheit.

**Type**: Datatypes: Numeric

**Oparand**: None

---
### Flatten
Flattens multi-dimensional array values into a single dimensional array value 

**Type**: Datatypes: Any

**Oparand**: None

---
### Floor
Calculates the largest integer less than or equal to the value.

**Type**: Datatypes: Numeric

**Oparand**: None

---
### Int16ToBytes
Converts a 16-bit value in an array of 2 8-bit values.

**Type**: Datatype: 16-bit value

**Oparand**: None

---
### Int16sToInt32
Converts an array of two 16-bit values to a single 32-bit value.

**Type**: Datatypes: int16[2]

**Oparand**: None

---
### Int32ToInt16s
Converts a 32-bit value in an array of 2 16-bit values.

**Type**: Datatype: 32-bit Value

**Oparand**: None

---
### IsoTimeStrSeconds
Converts a string in ISO-8601 duration format into milliseconds.
Converts a string in a format such as 2007-12-03T10:15:30.00Z into the number of seconds from the epoch of 1970-01-01T00:00:00Z.
The string must represent a valid instant in UTC

**Type**: Datatype : String

**Oparand**: None

---
### IsoTimeStrToEpocSeconds
Converts a string in ISO-8601 duration format into milliseconds.
Converts a string in a format such as 2007-12-03T10:15:30.00Z into the number of milliseconds from the epoch of 1970-01-01T00:00:00Z.
The string must represent a valid instant in UTC

**Type**: Datatype : String

---
### IsoTimeStrToMilliSeconds
Converts a string in ISO-8601 duration format into milliseconds

**Type**: Datatype : String

**Oparand**: None

---
### IsoTimeStrToNanoSeconds
Converts a string in ISO-8601 duration format into nanoseconds

**Type**: Datatype : String

**Oparand**: None

---
### IsoTimeStrToSeconds
Converts a string in ISO-8601 duration format into seconds

**Type**: Datatype : String

**Oparand**: None

---
### Ln
Computes the natural logarithm (base E)

**Type**: Datatypes: Numeric

**Oparand**: None

---
### Log10
Computes the common logarithm (base 10)

**Type**: Datatype: Numeric

**Oparand**: None

---
### LowerCase
Converts string to lowercase.

**Type**: Datatype: String

**Oparand**: None

---
### MapRange
Maps numeric ranges.

**Type**: Datatype: Numeric

**Operand:**  Structure containing both input and output range.

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
Maps range 0-1024 to range 1-100

---
### MapStringToNumber
Maps a string value to an integer value or default value.

**Type**: Datatype: String

**Default,Constraints,Examples**: 

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

**Operand**:  Map containing the string to value mapping
Default: Default value is there is no mapping (default is 0)

---
### Max
Returns greater of value or parameter value.

**Type**: Datatype: Numeric

**Operand**: Numeric test value

---
### Min
Return smallest of value or parameter value.

**Type**: Datatype: Numeric

Operand: Numeric test value

---
### Minus
alias is "-"
Subtracts parameter value from value.

**Type**: Datatype: Numeric

**Operand:**: Numeric value to subtract

---
### Mod
alias is "%"
Calculates the remainder of dividing value by parameter value.

**Type**: Datatype: Numeric

Operand:  Parameter: Divider

---
### Multiply
alias is "*"
Multiplies value by parameter value.

**Type**: Datatype: Numeric

**Oparand**: Parameter: Multiplier

---
### Not
alias is "!"
Inverts a Boolean value

**Type**: Datatype : Boolean

**Oparand**: None

---
### NumbersToFloatBE
Takes a list of two numbers and converts the individual bytes of these numbers into a float value using Big Endian encoding.

**Type**: Datatype : List of size 2 containing 2 numeric values. These values are first converted into 16 bit words from which the float value is decoded. 

**Oparand**: None

---
### NumbersToFloatLE
Takes a list of two numbers and converts the individual bytes of these numbers into a float value using Little Endian encoding.

**Type**: Datatype : List of size 2 containing 2 numeric values. These values are first converted into 16 bit words from which the float value is decoded. 

**Oparand**: None

---
### Or
Alias = "|"
Bitwise or of value and parameter.

**Type**: Datatype: Int, Byte, Short, Long.

**Operand**:  or mask

---
### OutsideRangeExclusive
Test if a value is outside an exclusive range

**Type**: Datatype: Numeric

**Oparand**: None

```json
{
   "Operator": "OutsideRangeExclusive",
   	  "Operand": {
        "MinValue": 0,
        "MaxValue": 100
     }
}
```



---
### OutsideRangeInclusive
Datatype: Numeric

**Type**: Datatype: Numeric

**Oparand**: None

```json
{
   "Operator": "OutsideRangeInclusive",
   "Operand": {
      "MinValue": 0,
      "MaxValue": 100
   }
}
```




---
### ParseInt
Parses string value as an integer number. The string must be a valid representation of a number.

**Type**: Datatype: String

**Oparand**: None

---
### ParseNumber
Parses string value as a double number. The string must be a valid representation of a number.

**Type**: Datatype: String

**Oparand**: None

---
### Plus
alias is "+" or "Add"
Adds the value of the parameter to value.

**Type**: Datatype: Numeric

**Operand**: Numeric value to add

---
### Query
Evaluate a JMESpath query against structured data type and returns the result.

**Type**: String

**Default,Constraints,Examples**: Parameter: JMESPath expression, see https://jmespath.org/

---
### ReverseList
Reverses the elements in a list value.

**Type**: Datatype: Lists

**Oparand**: None

---
### Round
Rounds the given value towards the closest integer.

**Type**: Datatype: Numeric

**Oparand**: None

---
### Shl
Shifts this value left by a bit-count number of bits.

**Type**: Datatype: Int, Byte, Short, Long

**Oparand**: bit-count

---
### Shr
Shifts this value right by a bit-count number of bits.

**Type**: Datatype: Int, Byte, Short, Long

**Oparand**: bit-count

---
### Sign
Returns the sign of the value.

- -1.0 if the value is negative
- zero if the value is zero
- 1.0 if the value is positive

**Type**: Datatype: Numeric

**Oparand**: None

---
### Sin
Computes the sine of the angle given in radians

**Type**: Datatype: Numeric

**Oparand**: None

---
### Sinh
Computes the hyperbolic sine of the value

**Type**: Datatype: Numeric

**Oparand**: None

---
### Sqrt
Computes the positive square root

**Type**: Datatype: Numeric

**Oparand**: None

---
### Str
String representation of a number.

**Type**: Datatype: Numeric

**Oparand**: None

---
### StrEquals
Compares string value with a string parameter.

**Type**: Datatype: String

**Operand**: Parameter: String to test for equality

---
### SubString
Returns the substring of string value starting at the start and ending right before the end.
Start and End are zero based indexes when positive and automatically limited to the max length of the input string.

When using negative values, it is the offset from end of the input string (-1 is last character)

If Start is omitted its default value is 0, for the beginning of the string.

If End is omitted the default value is the end of the input string (length + 1)

**Type**: Datatype: String

**Operand:** Start and end position

```json

{
   "Operator": "SubString",
   "Operand": {
       "Start": 2,
       "End": 4
   }
}
```



---
### Tan
Computes the tangent of the angle given in radians

**Type**: Datatype: Numeric

**Oparand**: None

---
### Tanh
Computes the hyperbolic tangent of the value

**Type**: Datatype: Numeric

**Oparand**: None

---
### TimestampToEpocMilliSeconds
Converts a datetime value  to the number of milliseconds from the epoch of 1970-01-01T00:00:00Z.

**Type**: Datatype: DateTime/Timestamp

**Oparand**: None

---
### TimestampToEpocSeconds
Converts a datetime value  to the number of seconds from the epoch of 1970-01-01T00:00:00Z.

**Type**: Datatype: DateTime/Timestamp

**Oparand**: None

---
### ToByte
Converts numeric value to a byte value

**Type**: Datatype: Numeric

**Default,Constraints,Examples**: No operand

**Oparand**: None

---
### ToDouble
Converts numeric value to a double value

**Type**: Datatype: Numeric

**Oparand**: None

---
### ToFloat
Converts numeric value to a float value

**Type**: Datatype: Numeric

**Oparand**: None

---
### ToInt
Converts numeric value to an int value

**Type**: Datatype: Numeric

**Oparand**: None

---
### ToLong
Converts numeric value to a long 64 bits value

**Type**: Datatype: Numeric

**Default,Constraints,Examples**: No operand

---
### ToShort
Converts numeric value to a short 16 bits value

**Type**: Datatype: Numeric

**Oparand**: None

---
### ToSigned
Converts numeric value to a signed value

**Type**: Datatype: Numeric

**Oparand**: None

---
### ToUnsigned
Converts numeric value to an unsigned value

**Type**: Datatype: Numeric

**Oparand**: None

---
### Trunc
Rounds the given value to an integer towards zero.

**Type**: Datatype: Numeric

**Oparand**: None

---
### TruncAt
Rounds the given value to a value with a specified number of decimals.

**Type**: Datatype: Numeric

**Operand**: Number of decimals to truncate value at

---
### UpperCase
Converts a string value to uppercase.

**Type**: Datatype: String

**Oparand**: None

---
### WithinRangeExclusive
Datatype: Numeric

**Type**: Datatype : Numeric

**Operand**: Range

```json
{
    "Operator": "WithinRangeExclusive",
    "Operand": {
        "MinValue": 0,
        "MaxValue": 100
    }
}
```



---
### WithinRangeInclusive
Datatype: Numeric

**Type**: Datatype: Numeric

**Default,Constraints,Examples**: 

```json
{
"Operator": "WithinRangeInclusive",
"Operand": {
"MinValue": 0,
"MaxValue": 100
}
}
```

**Operand** : Range

---
### Xor
alias is "^"
Bitwise xor of value and parameter.

**Type**: Datatype: Int, Byte, Short, Long

**Operand** : Parameter: xor mask

[^top](#TransformationOperator)

