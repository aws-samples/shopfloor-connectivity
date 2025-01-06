## MetricsConfiguration


**Properties:**
- [CollectCoreMetrics](#CollectCoreMetrics)
- [CommonDimensions](#CommonDimensions)
- [Enabled](#Enabled)
- [Interval](#Interval)
- [Namespace](#Namespace)
- [Writer](#Writer)

---
### CollectCoreMetrics
Collection of core detailed metrics enabled or disabled

**Type**: Boolean

**Default,Constraints,Examples**: Default is true

---
### CommonDimensions
Set of extra dimensions added to every datapoint

**Type**: Map(String,String)

**Default,Constraints,Examples**: Optional

---
### Enabled
Collection enabled or disabled

**Type**: Boolean

**Default,Constraints,Examples**: Default is true

---
### Interval
Interval in seconds for reading metrics from adapters, targets and core

**Type**: Integer

**Default,Constraints,Examples**: Default is 10

---
### Namespace
Namespace for collected metrics

**Type**: String

**Default,Constraints,Examples**: Default is "SFC"

---
### Writer
Writer for writing collect metrics data

**Type**: InprocessConfiguration

[^top](#MetricsConfiguration)

