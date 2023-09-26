/*
 Copyright (c) 2022. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc

/**
 * Factory class to create in process instances of sources readers for an input protocol using a factory class in external jar(s)
 */
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ServiceConfiguration
import com.amazonaws.sfc.data.SourceValuesReader
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.InstanceFactory


class SourceReaderFactory(private val configReader: ConfigReader, private val logger: Logger) {

    private val className = this::class.java.simpleName


    //  private val targetTypes = config.protocolAdapterTypes

    fun createInProcessReader(schedule: String, adapterID: String, readerLog: Logger? = logger): SourceValuesReader? {

        val log = logger.getCtxLoggers(className, "createInProcessReader")

        val config = configReader.getConfig<ServiceConfiguration>()
        val adapterConfig = config.protocolAdapters[adapterID]
        if (adapterConfig == null) {
            log.error("Adapter ID \"$adapterID\" does not exists, existing protocol adapters are ${config.protocolAdapters.keys}")
            return null
        }

        if (adapterConfig.protocolAdapterType.isNullOrEmpty()) {
            log.error("Adapter type is missing for adapter with ID \"$adapterID\"")
            return null
        }

        // get the configuration for the target type needed to create an in process instance
        if (adapterConfig.protocolAdapterType !in config.protocolAdapterTypes.keys) {
            log.error("Adapter type \"${adapterConfig.protocolAdapterType}\" is not configured, configured types are ${config.protocolAdapters.keys}")
            return null
        }

        val factory = InstanceFactory<SourceValuesReader>(config.protocolAdapterTypes[adapterConfig.protocolAdapterType]!!, logger)
        return try {
            factory.createInstance(configReader, schedule, adapterID, readerLog) as SourceValuesReader
        } catch (_: Exception) {
            null
        }
    }

    companion object {

        private val className = this::class.java.simpleName

        // create an instance of the factory
        fun createSourceReaderFactory(configReader: ConfigReader, logger: Logger): SourceReaderFactory? {

            try {
                configReader.getConfig<ServiceConfiguration>()
            } catch (e: Exception) {
                val errorLog = logger.getCtxErrorLog(className, "createSourceReaderFactory")
                errorLog("Error loading configuration: ${e.message}")
                return null
            }

            return SourceReaderFactory(configReader, logger)
        }
    }

}
