
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.service


import com.amazonaws.sfc.config.HealthProbeConfiguration
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.util.ItemCacheHandler
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import kotlinx.coroutines.runBlocking
import java.io.OutputStream
import java.nio.charset.Charset
import java.time.Instant


class HealthProbeHandler(private val healthProbeConfiguration: HealthProbeConfiguration,
                         private val checkStateFunction: (() -> Boolean) = { true },
                         private val stopServiceFunction: (() -> Unit) = {},
                         private val logger: Logger) : HttpHandler {

    private var className = this::class.simpleName.toString()

    private var firstUnhealty: Instant? = null

    private val rateLimiter by lazy { RateLimiter(healthProbeConfiguration.rateLimit) }
    private val ipFilter = if (healthProbeConfiguration.allowedIps.isNullOrEmpty()) null else Ip4Filter(healthProbeConfiguration.allowedIps)
    private val cachedState = ItemCacheHandler<Boolean, Nothing>(
        supplier = {
            val log = logger.getCtxLoggers(className, "checkStateFunction")
            log.trace("Checking service state")
            val state = try {
                checkStateFunction()
            } catch (e: Exception) {
                log.error("Error checking service state, $e")
                false
            }
            log.trace("Service state is ${if (state) "HEALTHY" else "UNHEALTHY"}")
            return@ItemCacheHandler state
        },
        validFor = healthProbeConfiguration.retainStatePeriod
    )

    override fun handle(exchange: HttpExchange) {

        val log = logger.getCtxLoggers(className, "handle")

        val remoteAddress = exchange.remoteAddress.address.hostAddress
        log.trace("Health check probe received from $remoteAddress")

        if (!rateLimiter.tryAcquire()) {
            log.warning("Too many requests on health probe handler, max requests per second is ${rateLimiter.ratePerSecond}, sending response $TOO_MANY_REQUESTS (Too many requests)")
            exchange.sendResponseHeaders(TOO_MANY_REQUESTS, -1)
            exchange.close()
            return
        }

        if (ipFilter != null) {
            if (!ipFilter.isIp4AddressAllowed(remoteAddress)) {
                log.warning("Health probe request from unauthorized ip address $remoteAddress, sending response $FORBIDDEN (Forbidden)")
                exchange.sendResponseHeaders(FORBIDDEN, -1)
                exchange.close()
                return
            }
        }

        val serviceIsHealth = runBlocking {
            cachedState.getAsync().await()
        }

        if (!serviceIsHealth!!) {
            issueServiceStopIfNeeded()
            exchange.close()
            return
        }

        firstUnhealty = null

        exchange.responseHeaders.add("Cache-Control", "no-cache")
        if (exchange.requestMethod == "HEAD") {
            exchange.sendResponseHeaders(HTTP_OK, -1)
            log.trace("Reply to HEAD method health check probe response to $remoteAddress")
            return
        }

        val response = healthProbeConfiguration.okResponse
        exchange.responseHeaders.add("Content-Type", "text/plain")

        exchange.sendResponseHeaders(HTTP_OK, response.length.toLong())
        val os: OutputStream = exchange.responseBody
        os.write(response.toByteArray(Charset.defaultCharset()))
        os.close()
        log.trace("Reply to GET method health check probe response with response \"$response\" to $remoteAddress")

    }

    private fun issueServiceStopIfNeeded() {
        if (healthProbeConfiguration.stopAfterUnhealthyPeriod != null) {
            if (firstUnhealty != null) {
                if (firstUnhealty!!.plusSeconds(healthProbeConfiguration.stopAfterUnhealthyPeriod!!.inWholeSeconds) < systemDateTime()) {
                    stopServiceFunction()
                    firstUnhealty = null
                }
            } else {
                firstUnhealty = systemDateTime()
            }
        }
    }

    companion object {
        private const val TOO_MANY_REQUESTS = 429
        private const val FORBIDDEN = 403
        private const val HTTP_OK = 200


    }
}

