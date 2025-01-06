
# Router Target



## RouterTargetConfiguration

RouterTargetConfiguration extends the type <a href="../core/target-configuration.md" >TargetConfiguration</a> with specific configuration data for routing target data to next (primary) targets and alternative and success targets for this these targets. The Targets configuration element can contain entries of this type, the TargetType of these entries must be set to <strong>"ROUTER".</strong>


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
A success target can be specified to which data is routed if the data was written successfully to the primary or alternative target.

**Type**: Map[String, RoutesConfiguration]

The targets must be targets that are configured either as in-process or IPC service targets in the same configuration.

[^top](#Router Target)

## RoutesConfiguration

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
