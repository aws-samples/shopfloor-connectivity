/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.metrics

sealed class MetricsDataValue

class MetricsValue(val value: Double) : MetricsDataValue() {

    constructor(value: Int) : this(value.toDouble())

    override fun toString(): String {
        return "MetricsValue(value=$value)"
    }
}

class MetricsValues(val values: List<Double>,
                    val counts: List<Double>? = null) : MetricsDataValue() {
    override fun toString(): String {
        return "MetricsValues(values=$values, counts=$counts)"
    }
}

class MetricsStatistics(val maximum: Double,
                        val minimum: Double,
                        val sampleCount: Double,
                        val sum: Double) : MetricsDataValue() {
    override fun toString(): String {
        return "MetricsStatistics(maximum=$maximum, minimum=$minimum, sampleCount=$sampleCount, sum=$sum)"
    }
}