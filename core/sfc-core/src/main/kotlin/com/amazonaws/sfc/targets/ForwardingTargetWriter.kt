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
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.ServiceConfiguration
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.data.TargetResultHandler
import com.amazonaws.sfc.data.TargetWriter
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricsProcessor
import com.amazonaws.sfc.metrics.MetricsProvider
import com.amazonaws.sfc.metrics.MetricsWriter
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@Suppress("unused")
abstract class ForwardingTargetWriter(protected val targetID: String,
                                      protected val configReader: ConfigReader,
                                      protected val resultHandler: TargetResultHandler?,
                                      private val logger: Logger) :
        TargetWriter, MetricsWriter, TargetResultHandler {

    private val targetMetricProviders = mutableMapOf<String, MetricsProvider>()
    private var metricsProcessor: MetricsProcessor? = null

    private val className = this::class.java.simpleName

    private val targetDataChannel = Channel<TargetData>(100)

    protected val targetScope = buildScope("ForwardingTargetWriter")

    private var _targets: Map<String, TargetWriter>? = null

    private val config: ServiceConfiguration
        get() {
            try {
                return configReader.getConfig()
            } catch (e: Exception) {
                throw ConfigurationException("Could not load target configuration: ${e.message}", "ServiceConfiguration")
            }
        }


    // Creates a single configured target writer
    // The actual creation of the writer is done by the abstract createTargetWriter method.
    // This method does take care of getting and checking the required configuration for
    // the writer in a consistent way for all possible inherited forwarding target implementations.
    private suspend fun createTargetWriter(targetID: String): TargetWriter? {

        return withTimeoutOrNull(60.toDuration(DurationUnit.SECONDS)) {

            val log = logger.getCtxLoggers(className, "createTargetWriter")

            // get configuration for specific target
            val targetConfig = config.targets[targetID]

            if (targetConfig == null) {
                log.error("Target \"$targetID\" does not exist, existing targets are ${config.targets.keys}}")
                null
            } else {

                val targetWriter = createTargetWriter(targetID, targetConfig)
                setupMetricsProviderForTarget(targetID, targetConfig, targetWriter)
                targetWriter
            }
        }
    }

    // This method is abstracted to keep this a generic core class that can be used for other forwarding targets.
    // Creating the actual writers here, which could be IPC clients would cause a circular dependency between the core and ipc packages
    abstract fun createTargetWriter(targetID: String, targetConfig: TargetConfiguration): TargetWriter?


    private fun setupMetricsProviderForTarget(targetID: String,
                                              targetConfig: TargetConfiguration?,
                                              targetWriter: TargetWriter?) {
        if (targetWriter != null) {
            val isCollectingMetricsFromAdapter = (targetConfig?.metrics != null) && targetConfig.metrics.enabled
            if (isCollectingMetricsFromAdapter && targetWriter.metricsProvider != null) {
                if (!targetMetricProviders.containsKey(targetID)) {
                    targetMetricProviders[targetID] = targetWriter.metricsProvider!!
                }
            }
        }
    }

    private fun startTargetMetricsProcessor() {
        val infoLog = logger.getCtxInfoLog(className, "startMetricsProcessor")
        if (config.metrics != null && (targetMetricProviders.isNotEmpty())) {

            metricsProcessor = MetricsProcessor(configReader, logger, targetMetricProviders) {
                this
            }

            metricsProcessor?.start()
            infoLog("Metrics processor started for targets ${targetMetricProviders.keys}")
        } else {
            metricsProcessor = null
            infoLog("No adapter or target metrics are collected")
        }
    }

    protected open fun subTargetsToSetup(targetID: String): List<String> {
        val forwardingTargetConfig = config.targets[targetID] ?: return emptyList()
        return forwardingTargetConfig.subTargets ?: emptyList()
    }

    // Create target writers
    private suspend fun setupTargets(): Map<String, TargetWriter> {

        val targets = subTargetsToSetup(targetID).toSet().mapNotNull { subTargetID ->
            val writer = createTargetWriter(subTargetID)
            if (writer != null) subTargetID to writer else null
        }.toMap()

        return targets

    }

    protected abstract suspend fun forwardTargetData(targetData: TargetData)

    // Gets/creates a map with instances of target writers for each configured target
    protected suspend fun getTargets(): Map<String, TargetWriter> {
        if (_targets == null) {
            _targets = setupTargets()
            if (!_targets.isNullOrEmpty()) {
                startTargetMetricsProcessor()
            }
        }
        return _targets as Map<String, TargetWriter>
    }

    private val writer = targetScope.launch("Writer") {

        val logInfo = logger.getCtxInfoLog(ForwardingTargetWriter::class.java.simpleName, "writer")
        logInfo("AWS stream writer for target \"$targetID\" writing to \"\" ")

        for (item in targetDataChannel) {
            forwardTargetData(item)
        }
    }

    override suspend fun close() {
        writer.cancel()
        _targets?.values?.forEach { it.close() }
    }


    /**
     * Write message to stream target.
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.send(targetData)
    }


}