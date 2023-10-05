/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.ipc

import io.grpc.ManagedChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.channelFlow
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

// Client class for TargetWriter Service
class IpcTargetWriterClient(internal val channel: ManagedChannel) : IpcMetricsReaderClient {

    private val stub: TargetAdapterServiceGrpcKt.TargetAdapterServiceCoroutineStub =
        TargetAdapterServiceGrpcKt.TargetAdapterServiceCoroutineStub(channel)

    private var _lastError: Exception? = null
    private var _isInitialized = false

    val isInitialized: Boolean
        get() = _isInitialized

    override var lastError: Exception?
        get() = _lastError
        set(value) {
            _lastError = value
        }


    /**
     * Reads metrics from the target IPC service. Note that this is a server side streaming call. The server will send the metrics results
     * to the returned flow with the specified interval after the read method is called.
     * @param interval Duration Read interval
     * @return kotlinx.coroutines.flow.Flow<ReadMetricsReply> Returns flow to which the metrics read from the service are streamed
     */
    override suspend fun readMetrics(interval: Duration): Flow<Metrics.MetricsDataMessage> = channelFlow {
        val readRequest = buildReadMetricsRequest(interval)

        try {
            stub.readMetrics(readRequest).buffer(100).cancellable().collect {
                send(it)
            }

        } catch (e: Exception) {
            _lastError = e
            throw e
        }
    }

    // Builds the read request
    private fun buildReadMetricsRequest(interval: Duration): Metrics.ReadMetricsRequest {

        return Metrics.ReadMetricsRequest.newBuilder()
            .setInterval(interval.inWholeSeconds)
            .build()
    }

    // writes all data that is read from the internal channel to a flow that is required by the GRPC streaming client method
    suspend fun writeTargetData(targetDataFlow: Flow<WriteValuesRequest>, function: (TargetResultResponse) -> Unit) {
        try {
            stub.writeValues(
                targetDataFlow
            ).collect { r -> function(r) }
        } catch (e: Exception) {
            _lastError = e
            throw e
        }
    }

    suspend fun initializeAdapter(initializeTargetRequest: InitializeTargetRequest) {

        try {
            val result = stub.initializeTarget(initializeTargetRequest)
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


    override fun close() {
        channel.shutdownNow().awaitTermination(15, TimeUnit.SECONDS)
    }

}