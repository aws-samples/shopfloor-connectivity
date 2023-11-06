
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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
