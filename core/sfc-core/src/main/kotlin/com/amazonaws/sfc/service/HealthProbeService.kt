/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.service

import com.amazonaws.sfc.config.HealthProbeConfiguration
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.getIp4NetworkAddress
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class HealthProbeService(
    private val healthProbeConfiguration: HealthProbeConfiguration,
    private val checkFunction: (() -> Boolean) = { true },
    private val serviceStopFunction: (() -> Unit) = {},
    private val logger: Logger) {

    private val className = this::class.simpleName.toString()

    private val scope = buildScope(className)

    private var serverJob: Job? = null
    private var server: HttpServer? = null


    private val socketAddress by lazy {
        val address = getIp4NetworkAddress(healthProbeConfiguration.networkInterface)
                      ?: throw Exception("No network address for network interface \"${healthProbeConfiguration.networkInterface}\"")
        InetSocketAddress(address, healthProbeConfiguration.port)
    }

    suspend fun restartIfInactive() {
        if (serverJob != null && !serverJob!!.isActive) {
            logger.getCtxInfoLog(className, "restartIfNotActive")("HealthProbe service is not active, restarting")
            start()
        }
    }

    suspend fun start() {

        server?.stop(0)
        delay(1000)
        server = null

        var retries = 0

        var p = healthProbeConfiguration.path
        if (!p.startsWith("/")) p = "/$p"

        val log = logger.getCtxLoggers(className, "start")
        serverJob = scope.launch {

            val addressStr = try {
                "${socketAddress.address.hostAddress}:${socketAddress.port}"
            } catch (e: Exception) {
                log.error("HealthProbe Service not started, error getting socket address on network interface, ${e.message}")
                return@launch
            }

            while (retries < 5 && server == null) {
                try {
                    server = HttpServer.create(socketAddress, 0)
                    server!!.executor = Executors.newSingleThreadExecutor()
                    server!!.createContext(p, HealthProbeHandler(
                        healthProbeConfiguration = healthProbeConfiguration,
                        stopServiceFunction = serviceStopFunction,
                        checkStateFunction = checkFunction,
                        logger = logger))
                    server!!.start()
                    log.info("Started health probe service, listening on $addressStr$p")
                } catch (e: Exception) {

                    retries += 1
                    if (retries < 10) {
                        delay(1000)
                    } else {
                        log.error("Failed to start health probe service on $addressStr, ${e.message}")
                        server = null
                    }
                }
            }
        }

        serverJob!!.join()
    }

    suspend fun stop() {
        server?.stop(0)
        serverJob?.join()
    }

}