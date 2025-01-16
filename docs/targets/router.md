# Router Target

[SFC Configuration](../core/sfc-configuration) > [Targets](../core/sfc-configuration#Targets) >  [Target](../core/target-configuration.md) 




## RouterTargetConfiguration

- [Schema](#RouterTargetConfiguration-Schema)
- [Examples](#RouterTargetConfiguration-Examples)

RouterTargetConfiguration extends the type  [TargetConfiguration](../core/target-configuration.md) with specific configuration data for routing target data to next (primary) targets and alternative and success targets for this these targets. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to **"ROUTER"**.


**Properties:**

- [ResultHandlerPolicy](#ResultHandlerPolicy)
- [Routes](#Routes)


---
### ResultHandlerPolicy
Policy used to determine result of routing the data.

- AllTargets: Routing is successful if data is written to all configured primary targets or an alternative route for the primary targets.
- AnyTarget: Routing is successful if data is written to at least one configured primary target or an alternative route for any of the primary targets.

**Type**: String

Default is "AllTargets"

---
### Routes
Target routing.
This map is indexed by the target IDs of primary targets to which data is routed.
Each entry can have an alternative route to which the data is routed if writing to the primary target fails.
A success target can be specified to which data is routed if the data has been written successfully to the primary or alternative target.

**Type**: Map[String, [RoutesConfiguration](#RoutesConfiguration)]

The targets must be targets that are configured either as in-process or IPC service targets in the same configuration.

### RouterTargetConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "RouterTargetConfiguration",
  "type": "object",
  "allOf": [
    {
      "$ref": "#/definitions/TargetConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "ResultHandlerPolicy": {
          "type": "string",
          "enum" : ["AllTargets", "AnyTarget"],
          "default" : "AllTargets",
          "description": "Policy for handling routing results"
        },
        "Routes": {
          "type": "object",
          "description": "Map of route configurations",
          "additionalProperties": {
            "$ref": "#/definitions/RoutesConfiguration"
          },
          "minProperties": 1
        }
      },
      "required": ["Routes"]
    }
  ]
}

```

### RouterTargetConfiguration Examples
```json
{
  "TargetType" : "ROUTER",
  "ResultHandlerPolicy": "AllSuccess",
  "Routes": {
    "s3-target": {
      "Alternate": "fallback-file-target",
      "Success": "archive-file-taraget"
    }
  }
}

```

[^top](#router-target)

## RoutesConfiguration

[RouterTarget](#RouterTargetConfiguration) > [Routes](#Routes)



- [Schema](#RoutesConfiguration-Schema)
- [Examples](#RoutesConfiguration-Examples)

**Properties:**

- [Alternate](#Alternate)
- [Success](#Success)

---

### Alternate

TargetID for a target to which data is routed if the data cannot be written to a primary target.

Type: String

Optional	

---

### Success

TargetID for a target to which data is routed if the data is written to a primary target or its alternative target

Type: String

Optional

### RoutesConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "RoutesConfiguration",
  "type": "object",
  "properties": {
    "Alternate": {
      "type": "string",
      "description": "Target ID for routing when primary target is unavailable"
    },
    "Success": {
      "type": "string", 
      "description": "Target ID for routing on successful write to primary or alternate target"
    }
  }
}
```

### RoutesConfiguration Examples

Basic Routing:

```json
{
  "Alternate": "fallback-target",
  "Success": "archive-target"
}
```

Failover Only:

```json
{
  "Alternate": "fallback-target"
}
```



Success Route Only:

```json
{
  "Success": "analytics-target"
}
```

