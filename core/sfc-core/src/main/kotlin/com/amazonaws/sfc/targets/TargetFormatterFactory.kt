// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.targets

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.InProcessConfiguration
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.InstanceFactory


class TargetFormatterFactory(config: InProcessConfiguration, private val logger: Logger) {

    val factory = InstanceFactory<TargetFormatter>(config, logger)

    fun createFormatter(configuration : String, logger : Logger): TargetFormatter {
        return factory.createInstance(configuration , logger) as TargetFormatter
    }

    companion object {

        fun createTargetFormatter(configReader: ConfigReader, targetID: String, targetConfiguration: TargetConfiguration, logger: Logger) : TargetFormatter? {
            val configuration = TargetConfiguration.targetConfig(configReader, targetID )
            return if (targetConfiguration.formatter != null && configuration != null) {
                val factory = TargetFormatterFactory(targetConfiguration.formatter!!, logger)
                factory.createFormatter(configuration, logger)
            } else null
        }
    }
}



