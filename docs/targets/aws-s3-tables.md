# AWS S3 Tables Target

[Amazon S3 Tables](https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-tables.html) offer optimized storage for analytics workloads, with features that continuously enhance query performance and minimize storage costs for tables. Designed specifically for tabular data, such as daily purchase transactions, streaming sensor data, or ad impressions, [S3 Tables](https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-tables.html) represent data organized in columns and rows, similar to a database table.

[S3 Tables](https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-tables.html) are stored in  bucket type called a [table bucket](https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-tables-buckets.html), which serves as a subresource for storing tables. Table buckets support storing tables in the [Apache Iceberg](https://iceberg.apache.org/) format, enabling users to query their tables using standard SQL statements with query engines that support Iceberg, including Amazon [Athena](https://aws.amazon.com/athena/), [Amazon Redshift](https://aws.amazon.com/redshift/), and [Apache Spark](https://aws.amazon.com/emr/features/spark/).

The adapter can map data from a TargetData set into one or more tables, as defined in its configuration. Each table has one or more mappings that describe how the target data for the records in that table is retrieved from the TargetData set. This allows generating multiple records from a single set of target data for a specific table.

In order to use this target as in [in-process](../sfc-running-targets.md#running-targets-in-process) type target the type must be added to the [TargetTypes](../core/sfc-configuration.md#TargetTypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"TargetTypes" :{
   "AWS-S3-TABLES": {
      "JarFiles" : ["<location of deployment>/aws-s3-tables-target/lib"],
      "FactoryClassName": "com.amazonaws.sfc.awss3tables.AwsS3TablesTargetWriter"
   }
}
```



**Configuration:**


- [AwsS3TablesTargetConfiguration](#awss3tablestargetconfiguration)
- [TableConfiguration](#tableconfiguration)
- [ColumnConfiguration](#columnconfiguration)
- [ColumnMappingConfiguration](#columnmappingconfiguration)

---

## AwsS3TablesTargetConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Targets](../core/sfc-configuration.md#targets) >  [Target](../core/target-configuration.md) 

AwsS3TablesTargetConfiguration extends the type TargetConfiguration with specific configuration data for sending data to S3 tables. The Targets configuration element can contain entries of this type; the TargetType of these entries must be set to **"AWS-S3-TABLES"**.

Requires IAM permissions:

```
"ListNamespaces", "ListTables", "ListTableBuckets", "CreateTableBucket", 
"CreateNamespace", "CreateTable", 
"GetTableBucket", "GetTableData", "GetTable", "GetTableMetadataLocation", 
"PutTableData",
"UpdateTableMetadataLocation"
```

- [Schema](#awss3tablestargetconfiguration-schema)
- [Examples](#awss3tablestargetconfiguration-examples)

**Properties:**

- [AutoCreate](#autocreate)
- [BufferCount](#buffercount)
- [CredentialProviderClient](#credentialproviderclient)
- [Endpoint](#endpoint)
- [Interval](#interval)
- [Namespace](#namespace)
- [TableBucket](#tablebucket)
- [Tables](#tables)
- [WarnIfValueMissing](#warnifvalueismissing)

---
### AutoCreate

Specify whether to create table buckets, tables, and namespaces if they do not exist.

**Type** : Boolean

Default is true

---

### BufferCount

The number of records accumulated before writing them as a batch to the table. This number is the sum of all records to be written for all configured [tables](#tables). Batching records optimizes write operations.

**Type**: Integer

Default is 100 records in total for all tables

---
### CredentialProviderClient

The CredentialProviderClient property specifies which AWS credential provider client to use for authentication. It references a client defined in the SFC's top-level configuration under [AwsIotCredentialProviderClients](../core/sfc-configuration.md#awsiotcredentialproviderclients) section. This client uses X.509 certificates to obtain temporary AWS credentials through the  [AWS IoT credentials provider](../sfc-aws-service-credentials.md).

If no CredentialProviderClient is configured the [AWS Java SDK credential provider chain is used](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)

**Type:** String

---
### Endpoint

The EndPoint property specifies the VPC endpoint URL used to access AWS services privately through AWS PrivateLink without requiring an internet gateway or NAT device. When not specified, the service's default public endpoint for the configured region will be used.

https://docs.aws.amazon.com/vpc/latest/privatelink/aws-services-privatelink-support.html

**Type:** String

---

### Interval

The time interval in milliseconds that triggers writing buffered records to Timestream, even if the [BufferCount](#buffercount) hasn't been reached. If not specified, records are only written when the [BufferCount](#buffercount) is reached. 

**Type**: Integer

Optional, if not set only [BufferCount](#buffercount)#batch count is used.


---
### Namespace

A logical grouping identifier for organizing tables within an AWS S3 Tables bucket. The namespace acts as a container that helps organize and manage related tables, similar to a database schema. It must be a unique identifier within the table bucket that follows specific naming conventions: 1-225 characters long, starting and ending with a letter or number, containing only lowercase letters, numbers, and underscores, and cannot be the reserved name 'aws_s3_metadata' or start with an underscore.

See https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-tables-buckets-naming.html for namespace naming constraints.

If [AutoCreate](#autocreate) is set to false, then the namespace must already exist in the table bucket.

**Type**: String

---
### TableBucket
The name of the AWS S3 Tables bucket that serves as the top-level container for storing table data and metadata. This bucket is specifically designed for S3 Tables and acts as the warehouse location for all namespaces and tables within this target configuration. The bucket name must follow S3 naming conventions: 3-63 characters long, containing only lowercase letters, numbers, periods, and hyphens, starting and ending with a letter or number, with no consecutive periods, and cannot use reserved prefixes like 'xn--', 'sthree-', 'amzn-s3-demo-' or suffixes like '-s3alias', '--ol-s3', '--x-s3'.

See https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-tables-buckets-naming.html for table bucket naming constraints.

If [AutoCreate](#autocreate) is set to true, then the table bucket is created if it does not exist.

**Type**: String

---

### Tables

An array of [table configurations](#tableconfiguration) that define the destination tables to which target data is written within the S3 Tables namespace. At least one table must be specified for a target configuration. By specifying multiple tables, records can be selectively written to different tables based on the data , enabling flexible data distribution and organization within the same target configuration.

**Type** : [TableConfiguration](#tableconfiguration)

---

### WarnIfValueIsMissing

Determines whether a warning is generated if a value cannot be retrieved from the target data for a non-optional column. In such cases, no record is written to the table. However, this setting can be intentionally set to false for a specific mapping to create an optional record based on the existence of certain values, eliminating the need for warnings.

**Type:** Boolean

Default value is true

---



### AwsS3TablesTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "AWS S3 Tables Target Configuration",
  "allOf": [
    {
      "$ref": "#/definitions/TargetConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "TableBucket": {
          "type": "string",
          "minLength": 3,
          "maxLength": 63,
          "pattern": "^[a-z0-9][a-z0-9.-]*[a-z0-9]$",
          "description": "S3 bucket name for storing tables"
        },
        "Endpoint": {
          "type": "string",
          "format": "uri",
          "description": "AWS service endpoint URL"
        },
        "Region": {
          "type": "string",
          "description": "AWS region"
        },
        "Namespace": {
          "type": "string",
          "minLength": 1,
          "maxLength": 225,
          "pattern": "^(?!(?i)aws_s3_metadata$)(?!_)[a-z0-9]([a-z0-9_]*[a-z0-9])?$",
          "description": "Namespace for the tables"
        },
        "Tables": {
          "type": "array",
          "minItems": 1,
          "items": {
            "$ref": "#/definitions/TableConfiguration"
          },
          "description": "List of table configurations"
        },
        "AutoCreate": {
          "type": "boolean",
          "default": true,
          "description": "Whether to automatically create tables if they don't exist"
        },
        "WarnIfValueMissing": {
          "type": "boolean",
          "default": true,
          "description": "Whether to warn if non-optional values are missing"
        },
        "BufferCount": {
          "type": "integer",
          "minimum": 1,
          "default": 50,
          "description": "Number of records to buffer before writing"
        },
        "Interval": {
          "type": "integer",
          "minimum": 1,
          "default": 10000,
          "description": "Interval in milliseconds for writing data"
        }
      }
    }
  ],
  "required": [
    "TableBucket",
    "Namespace",
    "Tables"
  ]
}

```

### AwsS3TablesTargetConfiguration Examples

```json
"S3TablesTarget": {
      "TargetType": "AWS-S3-TABLES",
      "#TargetServer": "S3TablesServer",
      "CredentialProviderClient": "CredentialProviderClient",
      "Interval": 60,
      "BufferCount": 100,

      "Region": "eu-west-1",
      "TableBucket": "sfc-table-bucket",
      "Namespace": "sfc",
      "AutoCreate": true,
       
      "Tables": [
        {
          "TableName": "sfc_table",

          "Schema": [
            {
              "Name": "event_time",
              "Type": "timestamptz",
              "Optional": false
            },
            {
              "Name": "counter",
              "Type": "float",
              "Optional": false
            },
            {
              "Name": "random",
              "Type": "float",
              "Optional": false
            },
            {
              "Name": "sawtooth",
              "Type": "float",
              "Optional": false
            },
            {
              "Name": "square",
              "Type": "int",
              "Optional": false
            },
            {
              "Name": "build_info",
              "Type": [
                {
                  "Name": "number",
                  "Type": "string"
                },
                {
                  "Name": "version",
                  "Type": "string"
                }
              ],
              "Optional": false
            }
          ],
          "Mappings": [
            {
                "event_time": {
                "ValueQuery": "@.timestamp"
              },
                "counter": {
                "ValueQuery": "@.sources.OPCUA.values.SimulationCounter.value"
              },
                "random": {
                "ValueQuery": "@.sources.OPCUA.values.SimulationRandom.value"
              },
                "sawtooth": {
                "ValueQuery": "@.sources.OPCUA.values.SimulationSawtooth.value"
              },
                "sinus": {
                "ValueQuery": "@.sources.OPCUA.values.SimulationSinus.value"
              },
                "square": {
                "ValueQuery": "@.sources.OPCUA.values.SimulationSquare.value"
              },
              "build_info": {
                "Mappings": {
                  "number": {
                    "ValueQuery": "@.sources.OPCUA.values.ServerStatus.value.buildInfo.buildNumber"
                  },
                  "version": {
                    "ValueQuery": "@.sources.OPCUA.values.ServerStatus.value.buildInfo.softwareVersion"
                  }
                }
              }
            }
          ],
          "Partition": {
            "day": "event_time"
          }
        }
      ]
    }
```

[^top](#aws-s3-tables-target)

## TableConfiguration

[AwsS3TablesTargetConfiguration](#awss3tablestargetconfiguration) > [Tables](#tables)

Defines the structure and properties of an individual table within an AWS S3 Tables namespace. It specifies the table name, schema definition with column types and structures, optional partitioning strategy for performance optimization, and field mappings that define how source data is transformed and written to table columns. The configuration includes partition optimization settings and validates all components to ensure proper table creation and data ingestion. This configuration serves as the blueprint for creating and writing to Iceberg tables in the S3 Tables service.

- [Schema](#tableconfiguration-schema)
- [Examples](#tableconfiguration-example)

**Properties:**

- [Mappings](#mappings)

- [Partition](#partition)
- [PartitioningOptimization](#partitioningoptimization)
- [Schema](#schema)
- [TableName](#tablename)


---
### Mappings
An array of field mapping configurations that define how source data fields are transformed and mapped to the corresponding table columns. Each mapping entry is a key-value object where the key represents the source field identifier and the value contains the field mapping configuration specifying transformation rules, data type conversions, and column assignments. At least one mapping must be specified to establish the relationship between incoming data and the table schema. Optionally, multiple mappings can be defined, in which case multiple records can be created from a single target dataset, enabling data duplication, transformation variations, or record splitting scenarios during the write process.

**Type**: List of Map<String, [ColumnMappingConfiguration](#columnmappingconfiguration) >

---
### Partition
An optional configuration that defines the partitioning strategy for the table to optimize query performance and data organization. The partition configuration specifies transform functions applied to table columns to create partition keys. Available transforms include identity (use column value as-is), temporal transforms (year, month, day, hour for date/time columns), bucket transforms for hash-based partitioning with a specified number of buckets, and truncate transforms for string truncation with a specified length. Partitioning helps improve query performance by allowing data pruning and enables more efficient data storage and retrieval patterns in the S3 Tables service.

The configuration is a map, where the key is the name of the partition transform, and the value is the name of the column to use for the transform.

Supported transforms are:

- "identity", Source value.
   e.g. `{"identity "<columnname>"}`

- "truncate[`w`]" , Value truncated to width `w`,
  e.g. `{"truncate[3]", "<columnname>"}`

- "bucket[`n`]", Hash of value, mod `n`
   e.g. `{"bucket[16]", "<columnname>"}`

- "day", Extract a date or timestamp day, as days from 1970-01-01
   e.g. `{"day", "<columnname>"}`

- "hour", Extract a timestamp hour, as hours from 1970-01-01 00:00:00,
  e.g. `{"hour", "<columnname>"}`

- "month", Extract a date or timestamp month, as months from 1970-01-01
   e.g. `{"month", "<columnname>"}`

- "year", Extract a date or timestamp year, as years from 1970
   e.g. `{"year", "<columnname>"}`

Iceberg table partitioning and transforms are described in detail at https://iceberg.apache.org/spec/#partitioning

**Type**: Map<String,String> where the key string is one of the transforms,  and in the format as listed above.

---
### PartitioningOptimization
When writing buffered records data to the table, the records are grouped based on the values of the partitioning used for these records, requiring a separate write action for each group. However, when using fine-grained partitioning, this may result in numerous write actions for small groups of records. To address this issue, setting the PartitioningOptimization to false ensures that all records are written in a single write action, utilizing the values of the first record as the values for the partitioning transform, but affecting the optimization from partitioning.

**Type**: Boolean

Default is true

---
### Schema
A required array of field configurations that defines the structure and data types of the table columns. The schema specifies each column's name, data type (including primitive types like string, int, double, boolean, as well as complex types like list, map, and struct), and whether the column is optional or required. This schema definition is used to create the underlying Iceberg table structure and must be specified before the table can be created or data can be written. The schema supports all Iceberg-compatible data types and nested structures, enabling flexible data modeling for various use cases. At least one field must be defined in the schema to create a valid table configuration.

**When the table doesn't exist and the AutoCreate property is set to true, the table is created using the specified schema. If an existing table is used, ensure that the schema matches the schema of the existing table. Otherwise, writing to the table is guaranteed to fail.**

**Type**: List of [ColumnConfiguration](#columnconfiguration)

---
### TableName
A required string that specifies the unique name of the table within the namespace. The table name must follow AWS S3 Tables naming conventions: 1-225 characters long, starting and ending with a letter or number, containing only lowercase letters, numbers, and underscores, and cannot be the reserved name 'aws_s3_metadata' or start with an underscore. This name, combined with the namespace, creates a unique table identifier within the S3 Tables bucket and is used for all table operations including data writes, queries, and schema management.

If [AutoCreate](#autocreate) is set to true, then the namespace is created in  [table bucket](#tablebucket) according to the configured [schema](#schema) if it does not exist. 

**Type**: String

---



### TableConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Table Configuration",
  "type": "object",
  "properties": {
    "TableName": {
      "type": "string",
      "minLength": 1,
      "maxLength": 225,
      "pattern": "^(?!(?i)aws_s3_metadata$)(?!_)[a-z0-9]([a-z0-9_]*[a-z0-9])?$",
      "description": "Name of the table within the namespace"
    },
    "Partition": {
      "$ref": "#/definitions/TablePartitionConfiguration",
      "description": "Optional partition configuration for the table"
    },
    "Schema": {
      "type": "array",
      "minItems": 1,
      "items": {
        "$ref": "#/definitions/FieldConfiguration"
      },
      "description": "Table schema definition specifying the structure and data types of table columns"
    },
    "PartitionOptimization": {
      "type": "boolean",
      "default": true,
      "description": "Whether to enable partition optimization for improved query performance"
    },
    "Mappings": {
      "type": "array",
      "minItems": 1,
      "items": {
        "type": "object",
        "additionalProperties": {
          "$ref": "#/definitions/FieldMappingConfiguration"
        }
      },
      "description": "Mapping configurations that define how source data fields are mapped to table columns"
    }
  },
  "required": [
    "TableName",
    "Schema",
    "Mappings"
  ]
}


```

### TableConfiguration Example

```json
{
  "TableName": "sfc_table",
  "Schema": [
    {
      "Name": "event_time",
      "Type": "timestamptz",
      "Optional": false
    },
    {
      "Name": "counter",
      "Type": "float",
      "Optional": false
    },
    {
      "Name": "random",
      "Type": "float",
      "Optional": false
    },
    {
      "Name": "sawtooth",
      "Type": "float",
      "Optional": false
    },
    {
      "Name": "square",
      "Type": "int",
      "Optional": false
    },
    {
      "Name": "build_info",
      "Type": [
        {
          "Name": "number",
          "Type": "string"
        },
        {
          "Name": "version",
          "Type": "string"
        }
      ],
      "Optional": false
    }
  ],
  "Mappings": [
    {
      "event_time": {
        "ValueQuery": "@.timestamp"
      },
      "counter": {
        "ValueQuery": "@.sources.OPCUA.values.SimulationCounter.value"
      },
      "random": {
        "ValueQuery": "@.sources.OPCUA.values.SimulationRandom.value"
      },
      "sawtooth": {
        "ValueQuery": "@.sources.OPCUA.values.SimulationSawtooth.value"
      },
      "sinus": {
        "ValueQuery": "@.sources.OPCUA.values.SimulationSinus.value"
      },
      "square": {
        "ValueQuery": "@.sources.OPCUA.values.SimulationSquare.value"
      },
      "build_info": {
        "Mappings": {
          "number": {
            "ValueQuery": "@.sources.OPCUA.values.ServerStatus.value.buildInfo.buildNumber"
          },
          "version": {
            "ValueQuery": "@.sources.OPCUA.values.ServerStatus.value.buildInfo.softwareVersion"
          }
        }
      }
    }
  ]
}
```



[^top](#aws-s3-tables-target)

## ColumnConfiguration

[AwsS3TablesTargetConfiguration](#awss3tablestargetconfiguration) > [Tables](#tables) -> [Schema](#schema) >items

Defines the structure and properties of an individual column within a table schema for AWS S3 Tables. It specifies the column name, data type (supporting primitive types like string, int, double, boolean, as well as complex types like list, map, and struct), and whether the column is optional or required. The configuration automatically generates unique column identifiers and builds the appropriate Iceberg field definitions based on the specified type. It supports nested structures, lists and map datatypes, enabling flexible schema design for various data modeling requirements in S3 Tables.



- [Schema](#columnconfiguration-schema)
- [Example](#columnconfiguration-examples)

**Properties:**

- [Name](#name)
- [Optional](#optional)
- [Type](#type)


---
### Name
required string that specifies the column name within the table schema. The column name must follow Iceberg naming conventions: it should be a valid identifier that starts with a letter or underscore, contains only letters, numbers, and underscores, and is case-sensitive. Column names cannot be empty and should avoid reserved keywords or special characters that might conflict with SQL queries or Iceberg's internal operations. The name will be used for data mapping, query operations, and schema evolution within the Iceberg table structure.

**Type**: String

---
### Optional
A boolean flag that determines whether the column allows null values, defaulting to true. When set to true, the column is created as an optional Iceberg field that can contain null values, providing flexibility for incomplete or missing data. When set to false, the column becomes a required field that must always contain a value, enforcing data integrity constraints at the schema level. This setting directly affects how the Iceberg NestedField is constructed and impacts query behavior and data validation during write operations.

When mapping a [ValueQuery](#valuequery) to retrieve a value from a non-optional column, and the returned value is null, it indicates that the value is not present in the target data. Consequently, no record is generated for that specific mapping. This intentional approach allows for the use of multiple [mappings](#mappings) for a table, depending on the availability of the fields for each mapping.

**Type**: Boolean

Default is true

---
### Type
A required field that specifies the data type of the column using valid Iceberg type specifications. Supports primitive types (boolean, int, long, float, double, decimal, date, time, timestamp, timestamptz, string, uuid, fixed, binary), complex types including list for arrays, map<keyType,valueType> for key-value pairs, and struct types defined as arrays of nested field configurations. The type specification must be compatible with Iceberg's type system and is validated during configuration processing. For parameterized types, use syntax like fixed[16] for fixed-length binary or decimal(10,2) for decimal with precision and scale. This type definition directly maps to the underlying Iceberg table schema and determines how data is stored and queried.

For more info on data types see https://iceberg.apache.org/spec/#primitive-types.

**Type**: String

---



### ColumnConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Column Configuration",
  "type": "object",
  "properties": {
    "Name": {
      "type": "string",
      "minLength": 1,
      "description": "Name of the column"
    },
    "Type": {
      "oneOf": [
        {
          "type": "string",
          "enum": [
            "boolean", "int", "long", "float", "double", "date", "time", 
            "timestamp", "timestamptz", "string", "uuid", "binary"
          ],
          "description": "Primitive data type"
        },
        {
          "type": "string",
          "pattern": "^fixed\\[\\d+\\]$",
          "description": "Fixed-length binary type, e.g., fixed[16]"
        },
        {
          "type": "string",
          "pattern": "^decimal\\(\\d+,\\d+\\)$",
          "description": "Decimal type with precision and scale, e.g., decimal(10,2)"
        },
        {
          "type": "string",
          "pattern": "^list<.+>$",
          "description": "List type with element type, e.g., list<string>"
        },
        {
          "type": "string", 
          "pattern": "^map<.+,.+>$",
          "description": "Map type with key and value types, e.g., map<string,int>"
        },
        {
          "type": "array",
          "items": {
            "$ref": "#"
          },
          "description": "Struct type defined as array of nested field configurations"
        }
      ],
      "description": "Data type of the column - can be primitive, list, map, or struct"
    },
    "Optional": {
      "type": "boolean",
      "default": true,
      "description": "Whether the column allows null values"
    }
  },
  "required": [
    "Name",
    "Type"
  ],
  "additionalProperties": false
}

```

### ColumnConfiguration Examples

Non-optional column, named "source" of type string.

```json
{
  "Name": "source",
  "Type": "String",
  "Optional": false
}
```
Non-optional column, named "timestamp" of type timestamp.
```json
  {
    "Name": "timestamp",
    "Type": "timestampt",
    "Optional": false
  }
```

Optional column named "messages" containing a list of strings.

```json
{
  "Name": "messages",
  "Type": "list<string>",
  "Optional": true
}
```

Optional column named "metadata" that holds a map of string values with string-typed keys.

```json
{
  "Name": "metadata",
  "Type": "map<string,string>",
  "Optional": true
}
```

Non-optional column, named "identifier" of type fixed[8].

```json
{  "Name": "identifier",
  "Type": "fixed[8]",
  "Optional": false
}
```

Non-optional column, named "measurement" of type decimal(4,2).

```json
{
  "Name": "measuremant_value",
  "Type": "decimal(4,2)",
  "Optional": false
}
```

Non-optional column named "device" that holds a structure containing a nested values "name" and "version", both non-optional and of type string.Because both nested values are non-optimal, both must be specified in order to fulfill the non-optional requirement for the device column.

```json
{
  "Name": "device",
  "Type": [
    {
      "Name": "name",
      "Type": "string",
      "Optional" : false
    },
    {
      "Name": "version",
      "Type": "string",
      "Optional" : false
    }
  ],
  "Optional": false
}
```

[^top](#aws-s3-tables-target)

## ColumnMappingConfiguration

[AwsS3TablesTargetConfiguration](#awss3tablestargetconfiguration) > [Tables](#tables) > [Mappings](#mappings)  > values

Defines the structure and properties of an individual column within a table schema for AWS S3 Tables. It specifies the column name, data type (supporting primitive types like string, int, double, boolean, as well as complex types like list, map, and struct), and whether the column is optional or required. The configuration automatically generates unique column identifiers and builds the appropriate Iceberg field definitions based on the specified type. It supports nested structures, lists and map datatypes, enabling flexible schema design for various data modeling requirements in S3 Tables.



- [Schema](#columnmappingconfiguration-schema)
- [Example](#columnmappingconfiguration-examples)

**Properties:**

- [Mappings](#mappings-1)

- [Transformation](#transformation)
- [ValueFilter](#valuefilter)
- [ValueQuery](#valuequery)


---

### Mappings

A key-value object used for mapping nested values within complex data structures such as struct types or hierarchical source data. Each key represents a field name in the nested structure, and the corresponding value is a ColumnMappingConfiguration that defines how to extract and transform data for that specific field. This property is mutually exclusive with ValueQuery and Transformation properties - when Mappings is used, it indicates a complex mapping scenario where multiple sub-fields need individual mapping configurations. The nested mappings enable recursive data extraction from deeply structured source data, allowing each sub-field to have its own JMESPath query, transformation, and filtering rules for flexible data mapping to table columns.

Type: Map<String, [ColumnMappingConfiguration](#columnmappingconfiguration)>

---

### Transformation

 An optional string that specifies the name of a transformation to apply to the extracted value after the [ValueQuery](#valuequery) has been executed. The transformation name must correspond to a transformation definition that exists in the [Transformations](../core/sfc-configuration.md#transformations) property of the [SFC Configuration](../core/sfc-configuration.md). This allows for data processing operations such as format conversion, mathematical calculations, string manipulation, or custom business logic to be applied to the raw extracted data before it is written to the table column. The transformation is applied after value extraction but before any value filtering, enabling a pipeline of data processing operations during the mapping process.

References a transformation defined in the  [Transformations](../core/sfc-configuration.md#transformations) property of the [SFC configuration]()

**Type**: String

---

### ValueFilter

An optional string that specifies the name of a value filter to apply to the extracted data after the ValueQuery has been executed and any transformation has been applied. The value filter name must correspond to a filter definition that exists in the  [ValueFilters](../core/sfc-configuration.md#valuefilters) property of the [SFC Configuration](../core/sfc-configuration.md). Value filters are used to conditionally include or exclude data based on specific criteria, such as range checks, pattern matching, or custom validation rules.  

When any of the filters configured for a mapping for a record doesn't match the value retrieved by the ValueQuery and, optionally, transformed by the Transformation, no record is generated for that mapping. This feature enables the selection of the generation of a specific record for one of the mappings for a table based on data values.

References a transformation defined in the  [ValueFilters](../core/sfc-configuration.md#valuefilters) property of the [SFC configuration]()

**Type**: String

Default is true

---

### ValueQuery

JMESPath expression used to extract the value gor a columns from the target data structure.

**Type**: String

The value must be a valid JMESPath query https://jmespath.org.

Note that if the source or channel name contains non-alphanumeric characters, then these elements must be quoted.
The quoted characters must be escaped with a \ character in the JSON configuration.

**Type**: String

---



### ColumnMappingConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Column Mapping Configuration",
  "oneOf": [
    {
      "type": "object",
      "title": "Simple Mapping",
      "properties": {
        "ValueQuery": {
          "type": "string",
          "minLength": 1,
          "description": "JMESPath expression to extract value from source data"
        },
        "Transformation": {
          "type": "string",
          "description": "Optional transformation ID to apply to the extracted value"
        },
        "ValueFilter": {
          "type": "string",
          "description": "Optional value filter ID to apply to the extracted value"
        }
      },
      "required": ["ValueQuery"]
    },
    {
      "type": "object",
      "title": "Sub Mappings",
      "properties": {
        "Mappings": {
          "type": "object",
          "minProperties": 1,
          "additionalProperties": {
            "$ref": "#"
          },
          "description": "Nested mappings for complex data structures"
        }
      },
      "required": ["Mappings"]
    }
  ],
  "description": "Configuration for mapping source data fields to table columns, supporting either simple value extraction or nested sub-mappings for complex structures"
}

```

### ColumnMappingConfiguration Examples

Mapping to retrieve the value from the SimulationRandom channel from the OPCUA source. 

```json
{
    "ValueQuery": "@.sources.OPCUA.values.SimulationRandom.value"
}
```

Mapping to retrieve the metadata value "meta-1" from the SimulationRandom channel.

```json
{
    "ValueQuery": "@.sources.OPCUA.values.SimulationRandom.metadata.meta-1"
}
```

Mapping to retrieve the value from the SimulationRandom channel in the OPCUA source, with a value filter named NotZero. If the value is 0, no record is generated for the record this mapping is for.

```json
{
    "ValueQuery": "@.sources.OPCUA.values.SimulationRandom.value",
    "ValueFilter": "NotZero"
}
```

The definition of the "NotZero" value filter.

```json
"ValueFilters" :{
  "NotZero": {
    "Operator": "ne",
    "Value": 0
  }
}
```

Mapping to retrieve a value and then applying the "AsInteger" transformer to the retrieved value.

```json
{
    "ValueQuery": "@.sources.OPCUA.values.SimulationRandom.value",
    "Transformation": "AsInteger"
}
```

Definition for the "AsInteger" transformation

```json
  "Transformations":{
    "AsInteger" : [
      {"Operator" : "ToInt"}
    ]
  },
```

Mapping to retrieve a value, applying the "AsInteger" transformer to the retrieved value, and then applying the "NotZero" filter on the result.

```json
{
    "ValueQuery": "@.sources.OPCUA.values.SimulationRandom.value",
    "ValueFilter": "NotZero",
    "Transformation": "AsInteger"
}
```



[^top](#aws-s3-tables-target)

