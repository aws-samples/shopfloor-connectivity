// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp

import com.amazonaws.sfc.config.BaseConfiguration.Companion.WILD_CARD
import com.amazonaws.sfc.data.ChannelReadValue
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricDimensions
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.slmp.config.SlmpChannelConfiguration
import com.amazonaws.sfc.slmp.config.SlmpControllerConfiguration
import com.amazonaws.sfc.slmp.config.SlmpSourceConfiguration
import com.amazonaws.sfc.slmp.protocol.SlmpDeviceItem
import com.amazonaws.sfc.slmp.protocol.SlmpDeviceRead
import com.amazonaws.sfc.slmp.protocol.SlmpDeviceReadRandom
import com.amazonaws.sfc.slmp.protocol.SlmpHeader
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.tcp.TcpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class SlmpSource(
    private val sourceID: String,
    private val controllerConfiguration: SlmpControllerConfiguration,
    private val sourceConfiguration: SlmpSourceConfiguration,
    private val metricsCollector: MetricsCollector?,
    adapterMetricDimensions: MetricDimensions?,
    private val logger: Logger
) : Closeable {


    private var _tcpClient: TcpClient? = null

    private val className = this::class.simpleName.toString()

    private val protocolAdapterID = sourceConfiguration.protocolAdapterID

    private val headerBuilder by lazy {
        SlmpHeader.newBuilder()
            .networkNumber(controllerConfiguration.networkNumber)
            .stationNumber(controllerConfiguration.stationNumber)
            .moduleNumber(controllerConfiguration.moduleNumber)
            .multiDropStationNumber(controllerConfiguration.multiDropStationNumber)
            .monitoringTimer(controllerConfiguration.monitoringTimer)
    }

    private val sourceDimensions =
        mapOf(MetricsCollector.METRICS_DIMENSION_SOURCE to "$protocolAdapterID:$sourceID") + adapterMetricDimensions as Map<String, String>

    private var closing = false

    private val tcpClient: TcpClient?
        get() {
            return runBlocking {
                val log = logger.getCtxLoggers(className, "tcpClient")
                if (_tcpClient != null) {
                    return@runBlocking _tcpClient!!
                }

                return@runBlocking try {
                    log.info("Creating TCP client for source \"$sourceID\"")
                    _tcpClient = TcpClient(controllerConfiguration, TCP_READ_BUFFER_SIZE, TCP_WRITE_BUFFER_SIZE, logger)
                    metricsCollector?.put(
                        protocolAdapterID,
                        MetricsCollector.METRICS_CONNECTIONS,
                        1.0,
                        MetricUnits.COUNT,
                        sourceDimensions
                    )
                    tcpClient?.start()
                    _tcpClient!!
                } catch (e: Exception) {
                    log.errorEx("Error creating TCP client for SLMP source \"$sourceID\"", e)
                    metricsCollector?.put(
                        protocolAdapterID,
                        MetricsCollector.METRICS_CONNECTION_ERRORS,
                        1.0,
                        MetricUnits.COUNT,
                        sourceDimensions
                    )
                    null
                }
            }
        }


    override fun close() {
            closing = true
            runBlocking {
                _tcpClient?.close(1.toDuration(DurationUnit.SECONDS))
            }
            _tcpClient = null
    }


    private fun buildReadRequests(channels: Map<String, SlmpChannelConfiguration>): Pair<List<Pair<String, SlmpDeviceRead>>, List<Pair<List<String>, SlmpDeviceReadRandom>>> {

        val deviceRead = mutableListOf<Pair<String, SlmpDeviceRead>>()
        val deviceRandom = mutableListOf<Pair<List<String>, SlmpDeviceReadRandom>>()


        // build device read command for every set of max 192 items
        channels.filter { !it.value.deviceItem.canBeReadByDeviceReadRandom || it.value.forceDeviceRead }
            .forEach {
                deviceRead.add(it.key to SlmpDeviceRead(it.value.deviceItem, headerBuilder))
            }

        // build device read random command for every set of max 192 items
        channels.filter { it.value.deviceItem.canBeReadByDeviceReadRandom && !it.value.forceDeviceRead }.toList()
            .chunked(192).forEach { channelSet: List<Pair<String, SlmpChannelConfiguration>> ->
                val channelIDs: List<String> = channelSet.map { it.first }
                val channelDeviceItems: List<SlmpDeviceItem> = channelSet.map { it.second.deviceItem }
                deviceRandom.add(channelIDs to SlmpDeviceReadRandom(channelDeviceItems, headerBuilder))
            }


        return Pair(deviceRead, deviceRandom)
    }


    suspend fun read(channels: List<String>?): Map<String, ChannelReadValue> {

        val log = logger.getCtxLoggers(className, "read")

        val results = mutableMapOf<String, ChannelReadValue>()

        if (tcpClient == null) return emptyMap()

        val channelsToRead: Map<String, SlmpChannelConfiguration> =
            if (channels.isNullOrEmpty() || (channels.size == 1 && channels[0] == WILD_CARD)) sourceConfiguration.channels
            else sourceConfiguration.channels.filter {
                channels.contains(it.key)
            }.toMap()

        val (deviceReadRequests, deviceReadRandomRequests) = buildReadRequests(channelsToRead)

        deviceReadRequests.forEach { (channelID, deviceReadRequest) ->
            log.trace("Reading channel \"$channelID\" from SLMP source \"$sourceID\" by executing SLMP DeviceReadRequest")
            try {
                results[channelID] =
                    ChannelReadValue(deviceReadRequest.execute(tcpClient!!, controllerConfiguration))
            } catch (e: Exception) {
                log.error("Error reading channel \"$channelID\" from SLMP source \"$sourceID\", $e")
                flagClientForReconnect()
                delay(controllerConfiguration.waitAfterReadError)
            }
        }

        deviceReadRandomRequests.forEach { (channelIDs, deviceRandomReadRequest) ->
            log.trace("Reading channels \"$channelIDs\" from SLMP source \"$sourceID\" by executing SLMP DeviceReadRandomRequest")
            val timestamp = systemDateTime()
            try {
                val values = deviceRandomReadRequest.execute(tcpClient!!, controllerConfiguration)
                channelIDs.zip(values).forEach { (channelID, value) ->
                    results[channelID] = ChannelReadValue(value, timestamp)
                }
            } catch (e: Exception) {
                log.error("Error reading channels \"$channelIDs\" from SLMP source \"$sourceID\", $e")
                flagClientForReconnect()
                delay(controllerConfiguration.waitAfterReadError)
            }
        }
        return results

    }

    private suspend fun flagClientForReconnect() {
        tcpClient?.close(1.toDuration(DurationUnit.SECONDS))
        _tcpClient = null
    }

    companion object {
        const val TCP_READ_BUFFER_SIZE = 1024 * 16
        const val TCP_WRITE_BUFFER_SIZE = 1024
    }


}