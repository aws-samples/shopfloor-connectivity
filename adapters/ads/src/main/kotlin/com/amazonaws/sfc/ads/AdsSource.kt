// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ads

import com.amazonaws.sfc.ads.config.AdsDeviceConfiguration
import com.amazonaws.sfc.ads.config.AdsSourceConfiguration
import com.amazonaws.sfc.ads.protocol.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.WILD_CARD
import com.amazonaws.sfc.data.ChannelReadValue
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricDimensions
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import kotlinx.coroutines.runBlocking
import java.io.Closeable


class AdsSource(
    private val tcpClientCache: TcpClientCache,
    private val sourceID: String,
    private val deviceID: String,
    private val deviceConfiguration: AdsDeviceConfiguration,
    private val sourceConfiguration: AdsSourceConfiguration,
    private val metricsCollector: MetricsCollector?,
    adapterMetricDimensions: MetricDimensions?,
    private val logger: Logger
) : Closeable {

    private var _client: Client? = null
    private var _symbols: List<Symbol>? = null
    private var _symbolToChannelMap: Map<String, String>? = null

    private val className = this::class.simpleName.toString()

    private val protocolAdapterID = sourceConfiguration.protocolAdapterID

    private val sourceDimensions =
        mapOf(MetricsCollector.METRICS_DIMENSION_SOURCE to "$protocolAdapterID:$sourceID") + adapterMetricDimensions as Map<String, String>

    private val requestBuilder = RequestBuilder(
        config =
        ClientConfig(
            targetAmsPort = sourceConfiguration.targetAmsPort,
            targetAmsNetId = sourceConfiguration.targetAmsNet,
            sourceAmsPort = sourceConfiguration.sourceAmsPort,
            sourceAmsNetId = sourceConfiguration.sourceAmsNet,
            commandTimeout = deviceConfiguration.commandTimeout,
            readTimeout = deviceConfiguration.readTimeout
        )
    )


    private suspend fun <K, V> buildMap(mapName: String, fn: (String, Symbol) -> Pair<K, V>): Map<K, V>? {

        val log = logger.getCtxLoggers(className, mapName)

        if (symbols == null) return null

        return sequence {
            sourceConfiguration.channels.forEach { (channelID, channel) ->
                val symbol = symbols?.find { it.symbolName == channel.symbolName }
                if (symbol != null) {
                    val pair = fn(channelID, symbol)
                    yield(pair)
                } else {
                    log.error("Source \"$sourceID\", channel \"$channelID\", symbol \"${channel.symbolName}\" does not exists, available symbols are ${symbols?.joinToString { it.symbolName }}")
                }
            }
        }.toMap()
    }


    private val symbolToChannelMap: Map<String, String>?
        get() {

            if (_symbolToChannelMap == null) {
                _symbolToChannelMap = runBlocking {
                    buildMap("symbolToChannelMap") { channelID, symbol ->
                        symbol.symbolName to channelID
                    }
                }
            }
            return _symbolToChannelMap
        }

    private var _channelToSymbolMap: Map<String, Symbol>? = null
    private val channelToSymbolMap: Map<String, Symbol>?
        get() {

            if (_channelToSymbolMap == null) {
                _channelToSymbolMap = runBlocking {
                    buildMap("channelToSymbolMap") { channelID, symbol ->
                        channelID to symbol
                    }
                }
            }
            return _channelToSymbolMap

        }


    private val symbols: List<Symbol>?
        get() {

            val log = logger.getCtxLoggers(className, "getSymbols")

            if (adsClient == null) return null

            return if (_symbols != null) _symbols
            else {

                _symbols = try {
                    log.info("Getting symbols for ADS source \"$sourceID\"")

                    _symbols = runBlocking { _client?.getSymbols() }
                    log.trace(
                        "Available symbols for ADS source \"$sourceID\" are ${
                            _symbols?.joinToString { it.symbolName }
                        }"
                    )
                    _symbols

                } catch (e: Exception) {
                    log.error("Error getting symbols for ADS source \"$sourceID\", $e")
                    null
                }
                _symbols
            }


        }


    private val adsClient: Client?
        get() {
            return runBlocking {
                val log = logger.getCtxLoggers(className, "adsClient")
                if (_client != null) return@runBlocking _client!!
                return@runBlocking try {
                    log.info("Creating ADS client for source \"$sourceID\"")
                    _client =
                        createAdsClient()

                    _client!!
                } catch (e: Exception) {
                    log.error("Error creating ADS client for source \"$sourceID\", $e")
                    null
                }
            }
        }


    private suspend fun createAdsClient(): Client? {


        return try {
            val adsConfiguration = ClientConfig(
                targetAmsNetId = sourceConfiguration.targetAmsNet,
                targetAmsPort = sourceConfiguration.targetAmsPort,
                sourceAmsNetId = sourceConfiguration.sourceAmsNet,
                sourceAmsPort = sourceConfiguration.sourceAmsPort,
                commandTimeout = deviceConfiguration.commandTimeout,
                readTimeout = deviceConfiguration.readTimeout
            )

            val tcpClient = runBlocking {
                tcpClientCache.getItemAsync(deviceConfiguration.address, deviceConfiguration).await()
                    ?: throw AdsAdapterException("Can not create tcp client")
            }

            val client = Client(tcpClient, adsConfiguration, logger)

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
            throw Exception("Error creating ADS client \"$deviceID\" (${deviceConfiguration.address}:${deviceConfiguration.port}) for reading fromm source \"$sourceID\"")
        }

    }

    override fun close() {
    }


    private var previousReadChannels: List<String>? = null
    private var previousReadRequest: MultiReadRequest? = null

    private fun buildReadRequest(channels: List<String>?): MultiReadRequest? {
        val request: MultiReadRequest? =
            if (previousReadRequest == null || previousReadChannels == null || previousReadChannels != channels) {
                val readAllChannels = (channels == null || (channels.size == 1 && channels[0] == WILD_CARD))
                val tr = if (readAllChannels) sourceConfiguration.channels else sourceConfiguration.channels.filter {
                    channels?.contains(it.key) == true
                }
                val rs = sequence {
                    tr.keys.forEach { channelID ->
                        val symbol = channelToSymbolMap?.get(channelID)
                        if (symbol != null) yield(symbol)
                    }
                }.toList()

                if (rs.isEmpty()) return null
                val request = requestBuilder.multiReadRequest(rs)
                previousReadChannels = channels
                previousReadRequest = request
                request
            } else {
                previousReadRequest
            }
        return request
    }

    suspend fun read(channels: List<String>?): Map<String, ChannelReadValue> {

        val log = logger.getCtxLoggers(className, "read")

        if (adsClient == null || symbols == null) return emptyMap()

        val request = buildReadRequest(channels) ?: return emptyMap()



        val values = try {
            adsClient?.readValues(request) ?: emptyList()
        } catch (e: AdsException) {
            throw Exception("Error reading from source \"$sourceID\", ${e.message}")
        } catch (e: Exception) {
            throw Exception("Error reading from source \"$sourceID\", $e")
        }

        return sequence {

            values.forEach { symbolReadResult ->
                if (symbolReadResult.result == 0) {
                    val channelID = symbolToChannelMap?.get(symbolReadResult.symbol.symbolName)
                    if (channelID != null) {
                        yield(channelID to ChannelReadValue(symbolReadResult.value, symbolReadResult.timestamp))
                    }
                } else
                    log.error(
                        "Error reading symbol ${symbolReadResult.symbol.symbolName} from source \"$sourceID\", " +
                                adsErrorString(symbolReadResult.result)
                    )

            }
        }.toMap()

    }


}

