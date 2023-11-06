
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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