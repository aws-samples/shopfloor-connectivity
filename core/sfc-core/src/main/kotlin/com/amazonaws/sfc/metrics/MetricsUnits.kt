/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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