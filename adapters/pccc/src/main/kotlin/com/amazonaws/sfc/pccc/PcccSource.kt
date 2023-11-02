/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.pccc

import com.amazonaws.sfc.data.ChannelReadValue
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricDimensions
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.pccc.config.PcccControllerConfiguration
import com.amazonaws.sfc.pccc.config.PcccSourceConfiguration
import com.amazonaws.sfc.pccc.protocol.Address
import com.amazonaws.sfc.pccc.protocol.Client
import com.amazonaws.sfc.pccc.protocol.OptimizedAddressSet
import com.amazonaws.sfc.system.DateTime.systemDateTime
import kotlinx.coroutines.runBlocking
import java.io.Closeable


class PcccSource(
    private val sourceID: String,
    private val controllerID: String,
    private val controllerConfiguration: PcccControllerConfiguration,
    private val sourceConfiguration: PcccSourceConfiguration,
    private val metricsCollector: MetricsCollector?,
    adapterMetricDimensions: MetricDimensions?,
    private val logger: Logger
) : Closeable {

    private val className = this::class.simpleName.toString()

    private var _client: Client? = null

    private val protocolAdapterID = sourceConfiguration.protocolAdapterID
    private val sourceDimensions =
        mapOf(MetricsCollector.METRICS_DIMENSION_SOURCE to "$protocolAdapterID:$sourceID") + adapterMetricDimensions as Map<String, String>

    private val pcccClient: Client
        get() {
            if (_client != null) return _client!!
            _client = runBlocking { createPcccClient() }
            return _client!!
        }

    private var _readSets: List<OptimizedAddressSet>? = null
    private val readSets: List<OptimizedAddressSet>?
        get() {
            if (_readSets == null) {
                val sourceAddresses = sourceConfiguration.channels.map { it.value.address }
                _readSets = pcccClient.buildAddressSets(sourceAddresses)
            }
            return _readSets
        }

    private suspend fun createPcccClient(): Client {
        val log = logger.getCtxLoggers(className, "createPcccClient")

        return try {
            val client = Client(controllerConfiguration, logger)
            metricsCollector?.put(
                protocolAdapterID,
                MetricsCollector.METRICS_CONNECTIONS,
                1.0,
                MetricUnits.COUNT,
                sourceDimensions
            )
            client
        } catch (e: Exception) {
            metricsCollector?.put(
                protocolAdapterID,
                MetricsCollector.METRICS_CONNECTION_ERRORS,
                1.0,
                MetricUnits.COUNT,
                sourceDimensions
            )
            throw Exception("Error connecting to controller \"$controllerID\" (${controllerConfiguration.address}) for reading for source \"$sourceID\"")
        }
    }


    private fun resetConnection() {
        try {
            close()
        } finally {
            _client = null
        }
    }

    override fun close() {
        runBlocking {
            _client?.close()
        }
    }

    suspend fun read(channels: List<String>?): Map<String, ChannelReadValue> {

        val log = logger.getCtxLoggers(className, "read")

        if (!pcccClient.connected) {
            pcccClient.connect()
        }

       // at this point we hava a connected client

        // cached sets of addresses to read
        val dataToRead = readSets
        if (dataToRead.isNullOrEmpty()) return emptyMap()

        return try {

            // get the values
            val readValues: Map<Address, Any> = pcccClient.read(dataToRead)

            // map to values to channels
            sequence {
                channels?.forEach { channelID ->
                    val channel = sourceConfiguration.channels[channelID]
                    val channelAddress = channel?.address
                    if (channelAddress != null) {
                        val value = readValues[channelAddress]
                        if (value != null) {
                            yield(channelID to ChannelReadValue(value, systemDateTime()))
                        }
                    }
                }
            }.toMap()


        } catch (e: Exception) {
            resetConnection()
            //  log.error("Error reading data for source \"$sourceID\", $e")
            throw (e)
        }
    }
}

