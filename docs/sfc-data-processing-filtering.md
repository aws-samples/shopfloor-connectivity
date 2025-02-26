# SFC data processing and filtering


- [SFC Dataflow](#sfc-dataflow)

- [Transformations](#transformations)

- [Data Filtering](#data-filtering)
    - [Data Change Filters](#data-change-filters)

    - [Value Change Filters](#data-change-filters)

    - [Condition Filters](#condition-filters)

- [Template transformations](#template-transformations)

    

## SFC dataflow

The data collected by the SFC source connector is processed by an internal data pipeline that consists of the following
steps:

- Data collected is read from connector
- Data transformations are applied on individual values if a transformation for a value has been configured.
- Data change filtering is applied at value or source level. Data change filters only let values pass if the new value
  differs from a previously passed value with at least a configured percentage or absolute value, or when a configured
  time period has passed since passing the last value. If a change filter is configured at source and value level, then
  the filter at value level takes precedence.
- Data value filter is applied at value level is applied if a filter has been configured for that value. The value is
  passed if it matches the filter expression which can consist of a combination of one or
  more `==`, `!=`, `>`, `>=`, `<`, `<=`, `&&`, `||` operators. For non-numeric values only the `==` and `!=` operators
  van be used.
- If data aggregation is specified the values are buffered until the specified aggregation size is reached. The output
  of an aggregation can be one or more output values from an aggregation (`avg`, `min`, `max`, etc.) on the collected
  values and/or the collected values.
- Data transformations are applied on the aggregated data output values if a transformation is configured for that
  specific output.
- Composition of values into structures or decomposing from structures into structures is applied based on source and channel configurations.
- Spreading elements from list values into separate values based on channel configuration.
- Data values are named according to their configured names. Metadata and timestamp information is added at configured
  levels (top, source and value) as configured.
- The data is transmitted to the configured targets where additional buffering or target specific processing is done.
  Selected targets support the transformation of the data submitted to their destinations by configuring an Apache
  Velocity template that is applied on that data.

```
Source data -> Transformation(value)(*) -> Change Filter (*) -> Value Filter(*) -> Aggregation(*) -> Transformation (value)(*) -> Naming of data and adding timestamp and metadata -> Transformation template (structure) (*) ->  Data to Target
```

(`*`) optional, only applied if configured

[^top](#sfc-data-processing-and-filtering)

# Transformations

Individual values can be transformed by configuring a transformation for the channel.

The configuration snippet below shows how a transformation named "ToInteger" is applied to the channels by setting the "Transformation" setting to the name of this transformation.

```json
"Channels" : {

    "SimulationSawtoothInt": {
       "NodeId": "ns=3;i=1003",
       "Transformation": "ToInteger"
     }
   },
   "SimulationSquareInt": {
      "NodeId": "ns=3;i=1005",
      "Transformation": "ToInteger"
   }
}
```

Transformations, which are lists of transformation operands, are defined at the top-level of the sfc-configuration. The operators in a transformation are applied on the values in the listed order.

Below is an example of a "Transformations" section defining 3 transformations, including the "ToInteger" one mentioned above. This transformation first gets the absolute value from the input value, it then rounds it and explicitly converts it into an Integer value. SFC will validate if the input value, or the resulting value of an operator, is valid for the input of the first or next operator of a transformation.

A configured operator consists of the name of the operator specified by the "Operator" setting and in case the operator takes arguments, the value of the argument specified by the "Operand" setting.
Transformations can also be applied to aggregated data if a schedule has an aggregation setup. See the setting "Transformations" in  [Aggregation](./core/aggregation-configuration.md) for more details.


See [TransformationOperator configuration](./core/transformation-operator-configuration.md) for a list of all available operators.


```json
  "Transformations": {
    "ToInteger": [
      {"Operator": "Abs"},
      {"Operator": "Round"},
      {
        "Operator": "ToInt"
      },
    ],
    "ToDegreesCelsius": [
      {"Operator": "Celsius"},
      {"Operator": "TruncAt", "Operand": "2"}
    ],
    "TwoDigits": [
      {"Operator": "TruncAt", "Operand": 2}
    ]
  }
```


# Data Filtering

The data read from the source can be filtered in two steps. First data change filtering is applied, then data value
filtering. Both steps are optional and can be applied individually.

## Data Change Filters

A [data change filter](./core/change-filter-configuration.md) can be configured at source and channel values level. If a filter is configured at source level it
is applied on all values for that source. Filters configured at value level take precedence over a filter at source
level. Values only pass a filter if a value has changed at least, or beyond, a configured value since the last value
that was passed. This value can be a percentage or absolute value. The initial value will always pass the filter. It is
also possible to specify a time interval in which at least a value will pass the filter. These filters can only be
applied on single numeric values.


## Value Filters

A [value change filter](./core/value-filter-configuration.md) will pass a value if it matches a filter expression. A filter expression can consist of one or
more operators like `==`,`!=`,`>`,`>=`,`<`,`<=`, combined in `&&` and `||` groups. For non-numeric values, only the ==
and != operators can be used.

## Condition Filters

After applying the Data Change and Value Change filters, if any, [Condition filters](./core/condition-filter-configuration.md) can be used to select values based on other values from the same source. This makes it possible to include or exclude values if other values or combinations of values exist or do not exist in the same source. Operators that can be used include:

- ***any*** : Any of a list of values must exist

```json
  {
        "Operator" : "any",
        "Value"    : ["a","b"]
  }
```

Both field a and b must exist for this source to include the value on which this filter is applied,

- ***none*** : None of a list of values must exist
```json
{
  "Operator": "none",
  "Value": ["a", "b" ]
}
```

Value a and b must not exist for source to include the value on which this filter is applied

- ***all*** : All values of a list of other values must exist

```json
{
  "Operator": "all",
  "Value": ["a", "b"]
}
```

Both value a and b must exist for source to include the value on which this filter is applied

- ***present*** : A specified value must exist

```json
{
  "Operator": "present",
  "Value": [
    "a"
  ]
}
```

Value a must exist for source to include the value on which this filter is applied

- **absent**:  A specified value may not exist

```json
{
  "Operator": "absent",
  "Value": [
    "a"
  ]
}
```

Value a must not exist for source to include the value on which this filter is applied

- ***only*** : The value must be the only value from a source

```json
{
  "Operator": "only",
  "Value": true
}
```

If value is true then the value on which the filter is applied is only included if it is the only value for that source.

If value is false then the value on which the filter is applied is only included if it is not the only value for that
source.

- ***notonly*** : The value must not be the only value from a source

```json
{
  "Operator": "notonly",
  "Value": true
}
```

If value is true then the value on which the filter is applied is only included if it not the only value for that
source.

If value is false then the value on which the filter is applied is only included if it is the only value for that
source.

All the operators above can be combined using the ***and*** and ***or*** operator, which take filter or a list of
filters as the filter value.

```json
{
  "Operator": "and",
  "Value": [
    {
      "Operator": "only",
      "Value": "false"
    },
    {
      "Operator": "all",
      "Value": [
        "a",
        "b"
      ]
    }
  ]
}
```



Filters are applied to values in the source configuration. A value is included when it is not the only value for that source, and both values 'a' and 'b' must exist for that source.

The names used as values for the filters correspond to the keys in the channel configuration of the source (not the 'name' value used to set the name of the value in the output). For structured values with sub-values, these can be specified by adding a '.' followed by the name of these fields, e.g., 'ServerStatus.state'.

Condition filters use JMESPath syntax (https://jmespath.org/) to match the names of values and their sub-values, allowing the use of full JMESPath syntax to build complex filters.

If a field name, or part of it, contains non-alphanumeric characters, it must be enclosed in double quotes, e.g., "System-Status", "System-Status".state, "System.Status".state.

Condition filters are defined as a map in the 'ConditionFilters' section of the configuration. The name of an entry defining a filter can be used as the value of the 'ConditionFilter' for a channel to apply that filter to the channel.



# Template Transformations

A subset of the SFC targets, can apply a transformation using a [velocity](https://velocity.apache.org/) template by setting the name for the [template](./core/target-configuration.md#template) in the configuration for the adapter. In the template, the following Velocity tools can be used:



