/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.modbus.adapter

import com.amazonaws.sfc.data.ProtocolAdapter
import com.amazonaws.sfc.data.SourceReadError
import com.amazonaws.sfc.data.SourceReadResult
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.modbus.config.ModbusConfiguration
import com.amazonaws.sfc.modbus.config.ModbusSourceConfiguration
import com.amazonaws.sfc.system.DateTime.systemDateTime
import kotlin.time.Duration

/**
 * Baseclass Modbus adapter, implements common Modbus protocol shared by TCP and RTU implementations
 * @property logger Logger Logger for output
 * @see ProtocolAdapter
 */
abstract class ModbusAdapter(val logger: Logger) : ProtocolAdapter {

    private val className = this::class.java.simpleName

    /**
     * Number of requests that can be sent before receiving output
     * @param sourceConfiguration: ModbusSourceConfiguration source
     * @return UShort Number of requests
     */
    abstract fun requestDepth(sourceConfiguration: ModbusSourceConfiguration): UShort

    /**
     * Source devices to read from
     */
    abstract val sourceDevices: Map<String, ModbusDevice>
    abstract override val metricsCollector: MetricsCollector?

    /**
     * Read values from channels from a source device.
     * @param sourceID String ID of source to read from
     * @param channels List<String>? Names of channels to read values for
     * @return SourceReadResult Result of the read action
     */
    override suspend fun read(sourceID: String, channels: List<String>?): SourceReadResult {

        val trace = logger.getCtxTraceLog(className, "read")

        val channelsStr = channels?.joinToString(prefix = "[", postfix = "]") { "\"$it\"" } ?: "all"
        trace("Reading source: \"$sourceID\", channels: $channelsStr")

        val readDevice = sourceDevices[sourceID]
        return readDevice?.readValues(channels, metricsCollector) ?: SourceReadError("Unknown source $sourceID", systemDateTime())
    }

    /**
     * Stop the adapter.
     * @param timeout Duration Timeout for stopping the adapter
     */
    abstract override suspend fun stop(timeout: Duration)

    // Extension property for modbus configurations used to retrieve all active devices used by the adapter
    protected val ModbusConfiguration.activeAdapters: Set<String>
        get() = this.activeSources.map { it.value.protocolAdapterID }.toSet()

    // Extension property for modbus configurations used to retrieve all active sources used by the adapter
    protected val ModbusConfiguration.activeSources: Map<String, ModbusSourceConfiguration>
        get() = activeSourceIDs.mapNotNull { sourceID ->
            val source = sources[sourceID]
            if (source == null) {
                null
            } else {
                sourceID to source
            }
        }.toMap()

    companion object {

        val ModbusConfiguration.activeSourceIDs: Set<String>
            get() = if (schedules.isNotEmpty())
                this.schedules.filter { it.active }.flatMap { s -> s.activeSourceIDs }.toHashSet()
            else
                this.sources.keys.toHashSet()
    }

}

