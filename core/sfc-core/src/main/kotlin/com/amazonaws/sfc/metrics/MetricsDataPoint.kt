/*

Copyright (c) 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.   
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.metrics

import com.amazonaws.sfc.system.DateTime
import java.time.Instant

class MetricsDataPoint(val name: String,
                       var dimensions: MetricDimensions? = null,
                       val units: MetricUnits,
                       val value: MetricsDataValue,
                       val timestamp: Instant = DateTime.systemDateTimeUTC()) {


    override fun toString(): String {
        return "MetricsDataPoint(name='$name', dimensions=$dimensions, units=$units, value=$value, timestamp=$timestamp)"
    }
}