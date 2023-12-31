
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.metrics


import com.google.gson.annotations.SerializedName

/**
 * Units that metrics can be measured in. These align with the
 * Amazon CloudWatch unit types:
 * https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html
 */
enum class MetricUnits {

    /**
     * Metric is measured in Seconds
     */
    @SerializedName("Seconds")
    SECONDS,

    /**
     * Metric is measured in Microseconds
     */
    @SerializedName("Microseconds")
    MICROSECONDS,

    /**
     * Metric is measured in Milliseconds
     */
    @SerializedName("Milliseconds")
    MILLISECONDS,

    /**
     * Metric is measured in Bytes
     */
    @SerializedName("Bytes")
    BYTES,

    /**
     * Metric is measured in Kilobytes
     */
    @SerializedName("Kilobytes")
    KILOBYTES,

    /**
     * Metric is measured in Megabytes
     */
    @SerializedName("Megabytes")
    MEGABYTES,

    /**
     * Metric is measured in Gigabytes
     */
    @SerializedName("Gigabytes")
    GIGABYTES,

    /**
     * Metric is measured in Terabytes
     */
    @SerializedName("Terabytes")
    TERABYTES,

    /**
     * Metric is measured in Bites
     */
    @SerializedName("Bits")
    BITS,

    /**
     * Metric is measured in Kilobits
     */
    @SerializedName("Kilobits")
    KILOBITS,

    /**
     * Metric is measured in Megabits
     */
    @SerializedName("Megabits")
    MEGABITS,

    /**
     * Metric is measured in Gigabits
     */
    @SerializedName("Gigabits")
    GIGABITS,

    /**
     * Metric is measured in Terabits
     */
    @SerializedName("Terabits")
    TERABITS,

    /**
     * Metric is a Percentage
     */
    @SerializedName("Percent")
    PERCENT,

    /**
     * Metric is a Count
     */
    @SerializedName("Count")
    COUNT,

    /**
     * Metric is measure in Bytes/Second
     */
    @SerializedName("Bytes/Second")
    BYTES_PER_SECOND,

    /**
     * Metric is measure in Kilobytes/Second
     */
    @SerializedName("Kilobytes/Second")
    KILOBYTES_PER_SECOND,

    /**
     * Metric is measure in Megabytes/Second
     */
    @SerializedName("Megabytes/Second")
    MEGABYTES_PER_SECOND,

    /**
     * Metric is measure in Gigabytes/Second
     */
    @SerializedName("Gigabytes/Second")
    GIGABYTES_PER_SECOND,

    /**
     * Metric is measure in Terabytes/Second
     */
    @SerializedName("Terabytes/Second")
    TERABYTES_PER_SECOND,

    /**
     * Metric is measure in Bits/Second
     */
    @SerializedName("Bits/Second")
    BITS_PER_SECOND,

    /**
     * Metric is measure in Kilobits/Second
     */
    @SerializedName("Kilobits/Second")
    KILOBITS_PER_SECOND,

    /**
     * Metric is measure in Megabits/Second
     */
    @SerializedName("Megabits/Second,")
    MEGABITS_PER_SECOND,

    /**
     * Metric is measure in Gigabits/Second
     */
    @SerializedName("Gigabits/Second")
    GIGABITS_PER_SECOND,

    /**
     * Metric is measure in Terabits/Second
     */
    @SerializedName("Terabits/Second")
    TERABITS_PER_SECOND,

    /**
     * Metric is measure in Count/Second
     */
    @SerializedName("Count/Second")
    COUNT_PER_SECOND,

    /**
     * No unit associated with the metric
     */
    @SerializedName("None")
    NONE,


}

val MetricUnits.serializedNames by lazy {
    MetricUnits.values().associateWith { it.declaringJavaClass.getField(it.name).getDeclaredAnnotation(SerializedName::class.java).value }
}

val MetricUnits.serializedName
    get() = this.serializedNames[this]