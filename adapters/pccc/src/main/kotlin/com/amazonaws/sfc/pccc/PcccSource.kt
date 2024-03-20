
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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
            throw (e)
        }
    }
}

