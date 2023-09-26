/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.ipc.extensions

import com.amazonaws.sfc.metrics.MetricsData

val MetricsData.grpcMetricsDataMessage: com.amazonaws.sfc.ipc.Metrics.MetricsDataMessage
    get() {
        val metricsDataBuilder = com.amazonaws.sfc.ipc.Metrics.MetricsDataMessage.newBuilder()

        metricsDataBuilder.source = this.source
        metricsDataBuilder.sourceType = this.sourceType.grpcMetricSourceType

        if (!this.commonDimensions.isNullOrEmpty()) {

            (this.commonDimensions as Map<String, String>).forEach { _ ->
                metricsDataBuilder.putAllCommonDimensions(this.commonDimensions)
            }
            //  metricsDataBuilder.commonDimensionsMap.put() .putAll(this.commonDimensions!!)
        }
        metricsDataBuilder.addAllDataPoints(this.dataPoints.map { it.grpcDataPoint })

        return metricsDataBuilder.build()
    }


val com.amazonaws.sfc.metrics.MetricsSourceType.grpcMetricSourceType: com.amazonaws.sfc.ipc.Metrics.MetricsSourceType
    get() =
        when (this) {
            com.amazonaws.sfc.metrics.MetricsSourceType.SFC_CORE -> com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.SFC_CORE
            com.amazonaws.sfc.metrics.MetricsSourceType.PROTOCOL_ADAPTER -> com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.PROTOCOL_ADAPTER
            com.amazonaws.sfc.metrics.MetricsSourceType.TARGET_WRITER -> com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.TARGET_WRITER
            else -> com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.UNDEFINED
        }


