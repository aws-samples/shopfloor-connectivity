/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.ipc

import com.amazonaws.sfc.config.SecretsManagerConfiguration.Companion.CONFIG_CLOUD_SECRETS
import com.amazonaws.sfc.config.ServerConfiguration
import com.amazonaws.sfc.ipc.Metrics.ReadMetricsRequest
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.ItemCacheHandler
import io.grpc.ManagedChannel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Client for source reading values from an IPC service
 * @property channel ManagedChannel IPC channel to read from
 */
class IpcSourceReadClient(internal val channel: ManagedChannel, usedSecrets: Map<String, String>) : IpcMetricsReaderClient, Closeable {

    private var _lastError: Exception? = null
    override var lastError: Exception?
        get() = _lastError
        set(value) {
            _lastError = value
        }

    private var initializeStatus = ItemCacheHandler<Boolean, Triple<String, ServerConfiguration, Logger>>(
        initializer = { _, initializeData ->
            val adapterConfigStr = initializeData?.first.toString()
            val server = initializeData?.second
            if (_isInitialized) {
                true
            } else {
                runBlocking {
                    val logger = initializeData?.third
                    val log = logger?.getCtxLoggers("IpcSourceReadClient", "initializeStatus")
                    log?.trace?.invoke("Sending configuration $\"$adapterConfigStr\" to protocol adapter service")
                    initializeAdapter(adapterConfigStr, usedSecrets)
                    log?.info?.invoke("IPC source service adapter for server ${server?.addressStr} initialized")
                }
                _isInitialized = true
                true
            }
        }
    )

    private var _isInitialized = false

    suspend fun initializeAsync(adapterConfigStr: String, serverConfig: ServerConfiguration, logger: Logger): Deferred<Boolean?> {
        return initializeStatus.getAsync(Triple(adapterConfigStr, serverConfig, logger))
    }


    private val stub: ProtocolAdapterServiceGrpcKt.ProtocolAdapterServiceCoroutineStub =
        ProtocolAdapterServiceGrpcKt.ProtocolAdapterServiceCoroutineStub(channel)

    /**
     * Reads values from the IPC service. Note that this is a server side streaming call. The server will send the read results
     * to the returned flow with the specified interval after the read method is called.
     * @param sourceChannels Mapping<String, ArrayList<String>> Mapping with sources and their channels to read from.
     * @param interval Duration Read interval
     * @return kotlinx.coroutines.flow.Flow<ReadValuesReply> Returns flow to which the values read from the service are streamed
     */
    suspend fun readValues(sourceChannels: Map<String, ArrayList<String>>, interval: Duration): kotlinx.coroutines.flow.Flow<ReadValuesReply> = coroutineScope {
        val readRequest = buildReadValuesRequest(sourceChannels, interval)
        try {
            stub.readValues(readRequest)
        } catch (e: Exception) {
            _lastError = e
            throw e
        }
    }


    // Builds the read request
    private fun buildReadValuesRequest(
        sourceChannels: Map<String, ArrayList<String>>, interval: Duration
    ): ReadValuesRequest {

        val sourcesList = sourceChannels.map { source ->
            SourceReadValueRequest.newBuilder()
                .setSourceID(source.key)
                .addAllChannels(source.value)
                .build()
        }.toList()

        return ReadValuesRequest.newBuilder()
            .addAllSources(sourcesList)
            .setInterval(interval.inWholeMilliseconds)
            .build()
    }

    /**
     * Reads metrics from the IPC service. Note that this is a server side streaming call. The server will send the metrics results
     * to the returned flow with the specified interval after the read method is called.
     * @param interval Duration Read interval
     * @return kotlinx.coroutines.flow.Flow<ReadMetricsReply> Returns flow to which the metrics read from the service are streamed
     */
    override suspend fun readMetrics(interval: Duration): kotlinx.coroutines.flow.Flow<Metrics.MetricsDataMessage> = coroutineScope {
        val readRequest = buildReadMetricsRequest(interval)
        try {
            stub.readMetrics(readRequest)
        } catch (e: Exception) {
            _lastError = e
            throw e
        }
    }

    // Builds the read request
    private fun buildReadMetricsRequest(interval: Duration): ReadMetricsRequest {

        return ReadMetricsRequest.newBuilder()
            .setInterval(interval.inWholeMilliseconds)
            .build()
    }


    private suspend fun initializeAdapter(configuration: String, secrets: Map<String, String>) {

        val usedSecretsInConfig = secrets.filter { configuration.contains(it.value) }
        val initializeAdapterRequest = InitializeAdapterRequest.newBuilder()
            .setAdapterConfiguration(configuration)
            .putAuxiliarySettings(CONFIG_CLOUD_SECRETS, AuxiliarySettings.newBuilder().putAllSettings(usedSecretsInConfig).build())
            .build()
        try {
            val result = stub.initializeAdapter(initializeAdapterRequest)
            if (!result.initialized) {
                _lastError = Exception(result.error)
                throw IpcException(result.error)
            }
        } catch (e: Exception) {
            _lastError = e
            throw e
        }

        _isInitialized = true
    }


    // Closes the reader
    override fun close() {
        channel.shutdownNow().awaitTermination(15, TimeUnit.SECONDS)
    }
}
