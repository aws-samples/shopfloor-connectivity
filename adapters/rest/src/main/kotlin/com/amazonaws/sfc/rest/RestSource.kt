// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.rest

import com.amazonaws.sfc.data.ChannelReadValue
import com.amazonaws.sfc.data.JsonHelper.Companion.fromJsonExtended
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricDimensions
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.rest.config.RestServerConfiguration
import com.amazonaws.sfc.rest.config.RestSourceConfiguration
import com.amazonaws.sfc.system.DateTime
import com.google.api.Authentication
import com.google.gson.JsonSyntaxException
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.delay
import java.io.Closeable
import java.lang.NumberFormatException
import java.net.Proxy
import java.sql.ResultSet
import java.time.Instant
import java.util.*
import kotlin.time.measureTime


class RestSource(private val sourceID: String,
                 private val serverID: String,
                 private val restServerConfiguration: RestServerConfiguration,
                 private val restSourceConfiguration: RestSourceConfiguration,
                 private val metricsCollector: MetricsCollector?,
                 adapterMetricDimensions: MetricDimensions?,
                 private val logger: Logger) : Closeable {

    private val className = this::class.simpleName.toString()

    private var pausedUntil: Instant? = null

    private var resultSet: ResultSet? = null

    private val protocolAdapterID = restSourceConfiguration.protocolAdapterID
    private val sourceDimensions =
        mapOf(MetricsCollector.METRICS_DIMENSION_SOURCE to "$protocolAdapterID:$sourceID") + adapterMetricDimensions as Map<String, String>


    suspend fun read(channels: List<String>?): Map<String, ChannelReadValue>? {

        val log = logger.getCtxLoggers(className, "read")


        if (pausedUntil != null && pausedUntil!!.isAfter(DateTime.systemDateTime())) {
            log.trace("Reading from source \"$sourceID\" is paused until $pausedUntil")
            return null
        }

        val urlString = "${restServerConfiguration.serverString}/${restSourceConfiguration.restRequest.trimStart('/')}"
        try {
            val url = Url(urlString)

            val client = buildClient()

            var resp = client.get(url)

            var retries = 0

            var result = emptyMap<String, ChannelReadValue>()

            while (resp.status != HttpStatusCode.OK && retries < restServerConfiguration.maxRetries) {
                val serverResponseTime = measureTime {
                    resp = client.get(url)
                }
                log.trace("Read from source \"$sourceID\" using url \"$urlString\" took $serverResponseTime")

                if (resp.status == HttpStatusCode.OK) {
                    log.trace("Data read from source \"$sourceID\" using url \"$urlString\"")
                    val payload = resp.bodyAsText()

                    try {

                        val duration = measureTime {

                            val payLoadData = fromJsonExtended(payload, Map::class.java)
                            val timestamp = Instant.ofEpochMilli(resp.responseTime.timestamp)

                            val channelsToRead = restSourceConfiguration.channels.filter { channels.isNullOrEmpty() || channels.contains(it.key) }

                            result = sequence {
                                channelsToRead.forEach { (channelName, channelConfig) ->
                                    if (channelConfig.selector == null && payLoadData.isNotEmpty()) {
                                        yield(channelName to ChannelReadValue(payload, timestamp))
                                    } else {
                                        val channelData = channelConfig.selector?.search(payLoadData)
                                        if (channelData != null) {
                                            yield(channelName to ChannelReadValue(channelData, timestamp))
                                        } else {
                                            log.trace("No data selected for channel \"$channelName\" using selector \"${channelConfig.selectorStr}\" from request result \"$payload\"")
                                        }
                                    }
                                }


                            }.toMap()
                        }

                        createMetrics(protocolAdapterID, duration.inWholeMilliseconds.toDouble(), result)
                        return result.ifEmpty { null }


                    } catch (e: JsonSyntaxException) {
                        log.error("Error reading data for source \"$sourceID\" using url \"$urlString\", ${e.message}, payload is not valid JSON")
                        metricsCollector?.put(protocolAdapterID, MetricsCollector.METRICS_READ_ERRORS, 1.0, MetricUnits.COUNT, sourceDimensions)
                        return null
                    }
                }

                delay(restServerConfiguration.waitBeforeRetry)
                retries++
            }

        } catch (e: Exception) {
            log.error("Error reading data for source \"$sourceID\" using url \"$urlString\", ${e.message}")
            pausedUntil = DateTime.systemDateTime().plusMillis(restServerConfiguration.waitAfterReadError.inWholeMilliseconds)
            metricsCollector?.put(protocolAdapterID, MetricsCollector.METRICS_READ_ERRORS, 1.0, MetricUnits.COUNT, sourceDimensions)
            log.info("Reading from source \"$sourceID\" is paused until $pausedUntil")
        }

        return null
    }


    private fun createMetrics(
        protocolAdapterID: String,
        readDurationInMillis: Double,
        values: Map<String, ChannelReadValue>
    ) {
        metricsCollector?.put(
            protocolAdapterID,
            metricsCollector.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READS, 1.0, MetricUnits.COUNT, sourceDimensions),
            metricsCollector.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_READ_DURATION,
                readDurationInMillis,
                MetricUnits.MILLISECONDS,
                sourceDimensions
            ),
            metricsCollector.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_VALUES_READ,
                values.size.toDouble(),
                MetricUnits.COUNT,
                sourceDimensions
            ),
            metricsCollector.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READ_SUCCESS, 1.0, MetricUnits.COUNT, sourceDimensions)
        )
    }


    private fun buildClient() = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = restServerConfiguration.requestTimeout.inWholeMilliseconds
        }
        headers {
            append(HttpHeaders.Accept, "application/json")
            restServerConfiguration.headers.forEach { (headerName, headerValue) ->
                append(headerName, headerValue)
            }
        }

        val proxyConfig = restServerConfiguration.proxy
        if (proxyConfig?.proxyUrl != null) {
            engine {
                proxy = ProxyBuilder.http(proxyConfig.proxyUrl!!)
            }

            if (proxyConfig.proxyUsername != null && proxyConfig.proxyPassword != null) {
                defaultRequest {
                    val credentials = Base64.getEncoder().encodeToString("${proxyConfig.proxyUsername}:${proxyConfig.proxyPassword}".toByteArray())
                    header(HttpHeaders.ProxyAuthorization, "Basic $credentials")
                }
            }
        }


    }

    override fun close() {
    }

    private fun channelFilter(channelID: String, channels: List<String>?): Boolean {
        return channels.isNullOrEmpty() || channels.contains(channelID)
    }


}