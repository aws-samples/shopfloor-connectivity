/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.targets

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ServiceConfiguration
import com.amazonaws.sfc.data.TargetResultHandler
import com.amazonaws.sfc.data.TargetWriter
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.InstanceFactory

/**
 * Factory class for creating in process instances target writers from a configured factory class name and jar file(s)
 * @property configReader ConfigReader Reader for configuration
 * @property logger Logger Logger for output
 */
class TargetWriterFactory(private val configReader: ConfigReader, config: ServiceConfiguration, private val logger: Logger) {

    private val className = this::class.java.simpleName

    private val targets = config.activeTargets
    private val targetTypes = config.targetTypes

    fun createInProcessWriter(targetID: String, writerLogger: Logger? = logger, targetResultHandler: TargetResultHandler? = null): TargetWriter? {

        val log = logger.getCtxLoggers(className, "createInProcessWriter")

        // get the target config, which is virtual and points to a "physical" target configuration
        val targetConfig = targets[targetID]
        if (targetConfig == null) {
            log.error("Target with ID \"$targetID\" does not exists, existing targets are ${targets.keys}")
            return null
        }

        if (targetConfig.targetType.isNullOrEmpty()) {
            log.error("TargetType is missing for target with ID \"$targetID\"")
            return null
        }

        // get the configuration for the target type needed to create an in process instance
        if (targetConfig.targetType !in targetTypes.keys) {
            log.error("TargetType \"${targetConfig.targetType} is not configured, configured types are ${targetTypes.keys}")
            return null
        }

        val factory = InstanceFactory<TargetWriter>(targetTypes[targetConfig.targetType]!!, logger)
        return factory.createInstance(configReader, targetID, writerLogger, targetResultHandler) as TargetWriter

    }

    companion object {

        private val className = this::class.java.simpleName

        // create an instance of the factory
        fun createTargetWriterFactory(configReader: ConfigReader, logger: Logger): TargetWriterFactory? {

            val config: ServiceConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                val errorLog = logger.getCtxErrorLog(className, "createTargetWriterFactory")
                errorLog("Error loading configuration: ${e.message}")
                return null
            }

            return TargetWriterFactory(configReader, config, logger)
        }

    }
}