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
import com.google.gson.JsonSyntaxException
import io.burt.jmespath.Expression
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import java.io.Closeable
import java.time.Instant
import java.util.*
import kotlin.time.measureTime


class RestSource(private val sourceID: String,
                 private val restServerConfiguration: RestServerConfiguration,
                 private val restSourceConfiguration: RestSourceConfiguration,
                 private val metricsCollector: MetricsCollector?,
                 adapterMetricDimensions: MetricDimensions?,
                 private val logger: Logger) : Closeable {

    private val className = this::class.simpleName.toString()

    private var pausedUntil: Instant? = null

    private val client by lazy {
        buildClient()
    }

    private val url by lazy {
        val urlString = "${restServerConfiguration.serverString}/${restSourceConfiguration.restRequest.trimStart('/')}"
        Url(urlString)
    }

    private val protocolAdapterID = restSourceConfiguration.protocolAdapterID
    private val sourceDimensions =
        mapOf(MetricsCollector.METRICS_DIMENSION_SOURCE to "$protocolAdapterID:$sourceID") + adapterMetricDimensions as Map<String, String>


    suspend fun read(channels: List<String>?): Map<String, ChannelReadValue>? {

        val log = logger.getCtxLoggers(className, "read")


        if (pausedUntil != null && pausedUntil!!.isAfter(DateTime.systemDateTime())) {
            log.trace("Reading from source \"$sourceID\" is paused until $pausedUntil")
            return null
        }

        try {

            var resp : HttpResponse

            var retries = 0

            var result : Map<String, ChannelReadValue>

            while (retries < restServerConfiguration.maxRetries) {
                val serverResponseTime = measureTime {
                     resp = client.get(url)
                }

                if (resp.status == HttpStatusCode.OK) {
                    log.trace("Read from source \"$sourceID\" using url \"${url}\" took $serverResponseTime")

                    val duration = measureTime {
                        result = buildResultSet(channels, resp)
                    } + serverResponseTime

                    createMetrics(protocolAdapterID, duration.inWholeMilliseconds.toDouble(), result)
                    return result.ifEmpty { null }

                }

                retries += 1
                if (retries < restServerConfiguration.maxRetries) {
                    log.warning("Error reading data for source \"$sourceID\" using url \"$url\", response ${resp.status}, waiting ${restServerConfiguration.waitBeforeRetry} before retry")
                    delay(restServerConfiguration.waitBeforeRetry)
                } else {
                    log.error("Error reading data for source \"$sourceID\" using url \"$url\", response ${resp.status} after ${restServerConfiguration.maxRetries} retries")
                    metricsCollector?.put(protocolAdapterID, MetricsCollector.METRICS_READ_ERRORS, 1.0, MetricUnits.COUNT, sourceDimensions)
                    return null
                }

            }

        } catch (e: Exception) {
            log.error("Error reading data for source \"$sourceID\" using url \"$url\", ${e.message}")
            pausedUntil = DateTime.systemDateTime().plusMillis(restServerConfiguration.waitAfterReadError.inWholeMilliseconds)
            metricsCollector?.put(protocolAdapterID, MetricsCollector.METRICS_READ_ERRORS, 1.0, MetricUnits.COUNT, sourceDimensions)
            log.info("Reading from source \"$sourceID\" is paused until $pausedUntil")
        }

        return null
    }

    private suspend fun buildResultSet(channels: List<String>?, resp: HttpResponse): Map<String, ChannelReadValue> {

        val log = logger.getCtxLoggers(className, "buildResultSet")

        val payload = resp.bodyAsText()

        val payLoadData = if (restSourceConfiguration.channels.values.any{it.isJson})  try {
            fromJsonExtended(payload, Any::class.java)
        } catch (e: JsonSyntaxException) {
            log.error("Error parsing JSON data \"$payload\" for source \"$sourceID\", ${e.message}")
            payload
        }else{""}

        val timestamp = Instant.ofEpochMilli(resp.responseTime.timestamp)

        val channelsToRead = restSourceConfiguration.channels.filter { channels.isNullOrEmpty() || channels.contains(it.key) }

        return sequence {
            channelsToRead.forEach { (channelName, channelConfig) ->
                if (channelConfig.isJson && payLoadData != "") {
                    if (channelConfig.selector == null) {
                        yield(channelName to ChannelReadValue(payLoadData, timestamp))
                    } else {
                        try {
                            val channelData = selectData(channelConfig.selector, payLoadData)
                            if (channelData != null) {
                                log.trace("Selected data for channel \"$channelName\" using selector \"${channelConfig.selectorStr}\" from request result \"$channelData\"")
                                yield(channelName to ChannelReadValue(channelData, timestamp))
                            } else {
                                log.warning("No data selected for channel \"$channelName\" using selector \"${channelConfig.selectorStr}\" from request result \"$payload\"")
                            }
                        } catch (e: Exception) {
                            log.error("Error selecting data for channel \"$channelName\" using selector \"${channelConfig.selectorStr}\" from request result \"$payload\"")
                        }
                    }
                } else {
                    log.trace("Using raw data for channel \"$channelName\" from request result \"$payload\"")
                    yield(channelName to ChannelReadValue(payload, timestamp))
                }
            }
        }.toMap()
    }


    private fun selectData(query: Expression<Any>?, data: Any): Any? = try {
        query?.search(data)
    } catch (e: NullPointerException) {
        null
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

        val log = logger.getCtxLoggers(className, "buildClient")
        install(HttpTimeout) {
            requestTimeoutMillis = restServerConfiguration.requestTimeout.inWholeMilliseconds
            log.trace("Request timeout set to ${restServerConfiguration.requestTimeout.inWholeMilliseconds}ms")
        }
        headers {
            append(HttpHeaders.Accept, "application/json")
            log.trace("Accept header set to application/json")
            restServerConfiguration.headers.forEach { (headerName, headerValue) ->
                append(headerName, headerValue)
                log.trace
            }
        }

        val proxyConfig = restServerConfiguration.proxy
        if (proxyConfig?.proxyUrl != null) {


            engine {
                proxy = ProxyBuilder.http(proxyConfig.proxyUrl!!)
                log.trace("Proxy set to ${proxyConfig.proxyUrl}")
            }

            if (proxyConfig.proxyUsername != null && proxyConfig.proxyPassword != null) {
                defaultRequest {
                    val credentials = Base64.getEncoder().encodeToString("${proxyConfig.proxyUsername}:${proxyConfig.proxyPassword}".toByteArray())
                    header(HttpHeaders.ProxyAuthorization, "Basic $credentials")
                    log.trace("${HttpHeaders.ProxyAuthorization} header set to Basic credentials")
                }
            }
        }


    }

    override fun close() {
    }



}