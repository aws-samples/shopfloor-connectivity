/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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
class IpcMetricsProvider<T : IpcMetricsReaderClient>(configReader: ConfigReader,
                                                     serverConfig: ServerConfiguration,
                                                     logger: Logger,
                                                     private val isIpcServiceInitialized: () -> Boolean,
                                                     createClient: (channel: ManagedChannel) -> T) :
        IpcClientBase<T>(configReader, serverConfig, logger, createClient), MetricsProvider {

    private var metricsReader: Job? = null

    var client: IpcMetricsReaderClient? = null

    /**
     * Reads the data from the IPC service and calls the reader to process the data. Reading will stop if this function returns false.
     * @param consumer Function1<ReadResult, Boolean>
     */
    override suspend fun read(interval: Duration, consumer: MetricsConsumer): Unit = coroutineScope {

        metricsReader = launch("IPC Metrics Reader") {

            val log = logger.getCtxLoggers(IpcSourceReader::class.java.simpleName, "metrics-reader")

            // read loop, remote IPC service is streaming data
            while (isActive) {

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
                            s += if (e is StatusException) "${e.cause ?: e.message}" else e.message
                            log.error(s)
                        }
                        resetIpcClient()
                        delay(IpcSourceReader.WAIT_AFTER_ERROR)
                    }
                } finally {
                    resetIpcClient()
                }
            }
        }
        metricsReader?.join()
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