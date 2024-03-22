// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ServerConfiguration
import com.amazonaws.sfc.ipc.extensions.asNativeMetricsData
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricsConsumer
import com.amazonaws.sfc.metrics.MetricsProvider
import com.amazonaws.sfc.util.launch
import io.grpc.ManagedChannel
import io.grpc.StatusException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlin.time.Duration


// Reads metrics from a client created by calling the createClient parameter method, then calling a consumer method for every read metrics data message
class IpcMetricsProvider<T : IpcMetricsReaderClient>(
    configReader: ConfigReader,
    serverConfig: ServerConfiguration,
    logger: Logger,
    private val isIpcServiceInitialized: () -> Boolean,
    createClient: (channel: ManagedChannel) -> T
) :
    IpcClientBase<T>(configReader, serverConfig, logger, createClient), MetricsProvider {

    private var metricsReader: Job? = null

    var client: IpcMetricsReaderClient? = null

    /**
     * Reads the data from the IPC service and calls the reader to process the data. Reading will stop if this function returns false.
     * @param consumer Function1<ReadResult, Boolean>
     */
    override suspend fun read(interval: Duration, consumer: MetricsConsumer): Unit = coroutineScope {

        metricsReader = launch(context = Dispatchers.IO, name = "IPC Metrics Reader") {
            metricsReaderTask(interval, consumer, this)
        }
        metricsReader?.join()
    }

    private suspend fun metricsReaderTask(interval: Duration, consumer: MetricsConsumer, scope : CoroutineScope) {
        val log = logger.getCtxLoggers(IpcSourceReader::class.java.simpleName, "metrics-reader")

        // read loop, remote IPC service is streaming data
        while (scope.isActive) {

            // wait with reading until the related source reader or target writer client has been initialized
            if (!isIpcServiceInitialized()) {
                delay(WAIT_FOR_METRICS_SOURCE_INTERVAL)
                continue
            }

            try {
                client = getIpcClient()

                client?.readMetrics(interval)?.buffer(100)?.cancellable()?.collect { r ->

                    // call handler to process the data
                    if (!consumer(r.asNativeMetricsData)) {
                        // if handler returns false stop reading from client
                        metricsReader?.cancel()
                    }
                }

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    if (client?.lastError == null) {
                        client?.lastError = e
                    }
                    if (e.message?.contains("shutdownNow") == true) {
                        log.info("Metrics source service is shutting down")
                    } else {
                        var s = "Error communicating with metrics IPC service on ${serverConfig.addressStr}, "
                        s += if (e is StatusException) "${e.cause?.message ?: e.message}" else e.message
                        log.errorEx(s, e)
                    }
                    resetIpcClient()
                    delay(IpcSourceReader.WAIT_AFTER_ERROR)
                }
            } finally {
                resetIpcClient()
            }
        }
    }

    override suspend fun close() {
        super.close()
        withTimeoutOrNull(1000L) {
            metricsReader?.cancel()
            metricsReader?.join()
        }
    }

    companion object {
        private const val WAIT_FOR_METRICS_SOURCE_INTERVAL = 1000L
    }
}