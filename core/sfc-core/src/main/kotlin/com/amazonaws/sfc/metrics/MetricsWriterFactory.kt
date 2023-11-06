
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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