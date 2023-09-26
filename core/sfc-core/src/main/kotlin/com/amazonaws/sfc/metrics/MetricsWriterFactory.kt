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

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ConfigWithMetrics
import com.amazonaws.sfc.config.InProcessConfiguration
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.InstanceFactory


class MetricsWriterFactory(config: InProcessConfiguration, private val logger: Logger) {

    val factory = InstanceFactory<MetricsWriter>(config, logger)

    fun createInProcessWriter(configReader: ConfigReader): MetricsWriter {
        return factory.createInstance(configReader, logger) as MetricsWriter

    }


    companion object {


        // create an instance of the factory
        fun createMetricsWriterFactory(configReader: ConfigReader, logger: Logger): MetricsWriterFactory? {

            val config = configReader.getConfig<ConfigWithMetrics>()
            val metricsConfig = config.metrics
            val inProcessWriterConfig = metricsConfig?.writer?.metricsWriter ?: return null
            return MetricsWriterFactory(inProcessWriterConfig, logger)
        }
    }
}