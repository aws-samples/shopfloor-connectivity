# SFC Output data formats



## Output data format

```
[schedule]  -- schedule name
[serial]    -- serial number
[timestamp] -– processing timestamp
[sources]   -- source name* --- [values] -- value name* --- [value]-- value
                            |                           |- [metadata]--name* -- meta value
                            |                           |- [timestamp]-- value timestamp
                            |
                            |- [timestamp] -- source timestamp
                            |- [metadata] -- name* -- value
[metadata] --name* -- value
```



## Aggregated output data format 

```

[schedule]  -- schedule name
[serial]    -- serial number
[sources]   -- source name* --- [values] -- value name* --- [value]-- **aggregation name*** --  [value] --- value
                            |                           |                                       [timestamp] -timestamp
                            |                           |- [metadata] -- name* -- meta value
                            |                           
                            |- [metadata] --- name* --- value
[metadata] --name* -- value
```

Custom element names in brackets can be set for all elements above in brackets using the [ElementNames](../docs/core/sfc-configuration.md#elementnames) configuration
setting. The name keys for the sources and value maps get the value of the "Name" element for the source and channel in
their configuration (default is the key used as the id for the source/value in the configuration).

The root contains 6 elements

- **schedule**: This element contains the name of the schedule that outputs the data
- **serial**: A unique serial number for the target data
- **timestamp**: Timestamp when the target output data was created
- **sources**: This element contains a map with a node for each source of the schedule that has output data
    - **values**: The values node contains a map for each channel of its source that has an output value
    
        - **value**: This node contains the actual value of a channel or an aggregated value
    
        - **metadata**: This node contains a map with (optional) metadata for a channel
    
        - **timestamp**: Timestamp for the value (only if timestamp level = "value" or "both")
          For aggregated data the timestamp is only available for the aggregation outputs first, last and values.


- **metadata**: This node contains a map with (optional) metadata for a schedule

When using output transformations using a [velocity template](./core/target-configuration.md#template) for a target additional epoch timestamp fields, at message can be used in the transformation by setting the [TemplateEpochTimestamp](./core/target-configuration.md#templateepochtimestamp) property for the configuration of that target to true.
