/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.ipc.extensions

import com.amazonaws.sfc.metrics.MetricsData
import com.amazonaws.sfc.metrics.MetricsSourceType

val com.amazonaws.sfc.ipc.Metrics.MetricsDataMessage.asNativeMetricsData: MetricsData
    get() = MetricsData(
        source = this.source,
        sourceType = this.sourceType.nativeSourceType,
        dataPoints = this.dataPointsList.mapNotNull { it.metricsDataPoint },
        commonDimensions = if (!this.commonDimensionsMap.isNullOrEmpty())
            commonDimensionsMap.entries.associate { it.key to it.value }
        else
            null
    )

private val com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.nativeSourceType: MetricsSourceType
    get() = when (this) {
        com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.SFC_CORE -> MetricsSourceType.SFC_CORE
        com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.PROTOCOL_ADAPTER -> MetricsSourceType.PROTOCOL_ADAPTER
        com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.TARGET_WRITER -> MetricsSourceType.TARGET_WRITER
        else -> MetricsSourceType.UNDEFINED
    }