
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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