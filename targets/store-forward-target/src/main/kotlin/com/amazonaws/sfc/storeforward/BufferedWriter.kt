/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.storeforward

import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.data.TargetResult
import com.amazonaws.sfc.data.TargetWriter
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGE_BUFFERED_COUNT
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGE_BUFFERED_DELETED
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SUCCESS
import com.amazonaws.sfc.metrics.MetricsCollectorMethod
import com.amazonaws.sfc.metrics.MetricsValue
import com.amazonaws.sfc.metrics.MetricsValueParam
import com.amazonaws.sfc.storeforward.config.MessageBufferConfiguration
import com.amazonaws.sfc.util.byteCountString
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.io.FileNotFoundException


typealias BufferModeChangedMethod = (target: String, mode: BufferedWriter.WriterMode) -> Unit

class BufferedWriter(private val targetID: String,
                     private val targetScope: CoroutineScope,
                     private val writer: TargetWriter,
                     private val buffer: MessageBuffer,
                     private val bufferConfiguration: MessageBufferConfiguration,
                     private val metricsCollectorMethod: MetricsCollectorMethod?,
                     private val onModeChanged: BufferModeChangedMethod?,
                     private val logger: Logger) {

    private val className = this::class.java.simpleName

    enum class WriterMode {
        FORWARDING,
        BUFFERING
    }

    // Channel to pass data to be forwarded to targets
    private val forwardChannel = Channel<TargetData>(100)

    // Channel to pass resubmitted data
    private val resubmitChannel = Channel<TargetData>(100)

    // Channel to pass result data from target
    private val resultChannel = Channel<TargetResult>(100)

    // Writer can be in forwarding ot buffering mode
    private var _mode = WriterMode.FORWARDING
    private var mode
        get() = _mode
        set(value) {
            val info = logger.getCtxInfoLog(className, "mode")
            info("Mode for target \"$targetID\" switching from $mode to $value")
            _mode = value
            try {
                onModeChanged?.let { it(targetID, _mode) }
            } catch (_: Exception) {
            }
        }

    val isBuffering
        get() = mode == WriterMode.BUFFERING

    private val usesCleanup = (bufferConfiguration.retainSize != null ||
                               bufferConfiguration.retainPeriod != null ||
                               bufferConfiguration.retainNumber != null)

    // Worker to process received results from target
    private val resultWorker = launchResultHandlerWorker()

    // Worker to forward data to targets
    private val forwardWorker = launchForwardWorker()

    // Worker to clean up buffered data whilst buffering
    private var bufferCleanupWorker: Job? = null

    // Worker to resubmit buffered data
    private var resubmitWorker: Job? = null

    // Start by resubmitting stored data that might be buffered
    init {
        targetScope.launch { resubmittingBufferedData() }
    }

    // Start handling results from targets
    private fun launchResultHandlerWorker() = targetScope.launch("Result processor for target $targetID") {

        while (isActive) {

            // Read ACK/NACK returned by target
            val resultData = resultChannel.receive()

            // Handle received results
            handleTargetResults(resultData)

            // when receiving NACKs go into buffering mode
            if (resultData.containsNacks) {

                if (mode != WriterMode.BUFFERING) {
                    mode = WriterMode.BUFFERING
                    // Activate cleanup process whilst buffering
                    bufferCleanupWorker = if (usesCleanup) launchBufferCleanupWorker() else null
                }
                continue
            }

            // if ACKs are received then go switch to forwarding mode when we were in buffering mode
            if (resultData.containsAcks) {

                if (mode == WriterMode.BUFFERING) {
                    mode = WriterMode.FORWARDING
                    // Stop cleanup of buffered data
                    bufferCleanupWorker?.cancel()
                    // Resubmit data that was stored in whilst in buffering mode
                    resubmittingBufferedData()
                }
            }
        }
    }

    // Starts processing resubmitting of buffered data
    private suspend fun resubmittingBufferedData() {
        resubmitWorker = if (resubmitWorker == null) {
            launchResubmitWorker()
        } else {
            resubmitWorker?.join()
            launchResubmitWorker()
        }
    }

    // Starts cleaning worker, cleans buffer at configured interval
    private fun launchBufferCleanupWorker() = targetScope.launch("Buffer cleanup $targetID") {

        while (isActive) {
            cleanupBuffer()
            delay(bufferConfiguration.cleanupInterval)
        }
    }

    // Worker forwarding data to targets
    private fun launchForwardWorker() = targetScope.launch("Forwarder for target $targetID") {

        var timeout: Job? = null

        val errLog = logger.getCtxErrorLog(className, "forwardWorker")
        while (isActive) {

            // job to check for timeouts
            timeout?.cancel()
            timeout = launch("timeout") {
                delay(10000)
            }

            try {

                select<Unit> {

                    // read target data which is re-submitted
                    resubmitChannel.onReceive { targetData ->
                        timeout.cancel()
                        forwardDataToTarget(targetData)
                    }

                    // read data which is forwarded
                    forwardChannel.onReceive { targetData ->

                        timeout.cancel()

                        when (mode) {

                            WriterMode.FORWARDING -> {
                                // Forward data to target, buffer when it can not be delivered to the target
                                forward(targetData)
                            }

                            WriterMode.BUFFERING -> {
                                buffer.add(targetID, targetData)
                                metricsCollectorMethod?.let { it(listOf(MetricsValueParam(METRICS_MESSAGE_BUFFERED_COUNT, MetricsValue(1), MetricUnits.COUNT))) }
                                // use oldest or latest target data to send to the target in order to get an ACK when it is available again
                                submitBufferedItem()
                            }

                        }
                    }

                    // No data, pick first (fifo) or last item from buffer and submit it to target in order to get ACK/NAC response
                    timeout.onJoin { submitBufferedItem() }

                }

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    errLog("$e")
                }
            }
        }
    }

    // re-submits first (fifo) or last buffered item
    private suspend fun submitBufferedItem() {

        val bufferedTargetData = try {
            if (bufferConfiguration.fifo)
                buffer.first(targetID, bufferConfiguration.retainPeriod)
            else
                buffer.last(targetID)
        } catch (_: FileNotFoundException) {
            null
        }

        if (bufferedTargetData != null) {
            // set flag to disable buffering so target can send item directly in order to get an ACK/NACK result to find out about status of target
            bufferedTargetData.noBuffering = true
            forwardDataToTarget(bufferedTargetData)
        }
    }


    private suspend fun forward(targetData: TargetData) {

        val one = MetricsValue(1)
        val metrics = mutableListOf(MetricsValueParam(METRICS_WRITES, one, MetricUnits.COUNT),
            MetricsValueParam(METRICS_MESSAGES, one, MetricUnits.COUNT))

        targetData.noBuffering = false
        if (forwardDataToTarget(targetData)) {
            metrics.add(MetricsValueParam(METRICS_WRITE_SUCCESS, one, MetricUnits.COUNT))
        } else {
            metrics.add(MetricsValueParam(METRICS_MESSAGE_BUFFERED_COUNT, one, MetricUnits.COUNT))
            buffer.add(targetID, targetData)
            mode = WriterMode.BUFFERING
        }
        metricsCollectorMethod?.let { it(metrics) }
    }

    private fun launchResubmitWorker(): Job {
        return targetScope.launch("Resubmit worker for $targetID") {

            cleanupBuffer()
            buffer.list(targetID, bufferConfiguration.fifo).filterNotNull().chunked(10).forEach { chunkedItems ->
                chunkedItems.forEach {
                    it.noBuffering = false
                    resubmitChannel.send(it)
                }
                delay(1)
            }
        }
    }


    fun stop() {
        listOfNotNull(
            bufferCleanupWorker,
            forwardWorker,
            resubmitWorker,
            resultWorker).forEach {
            try {
                it.cancel()
            } catch (_: CancellationException) {
            }
        }
    }

    // perform a buffer cleanup based on buffer retain configuration
    private suspend fun cleanupBuffer() {

        if (!usesCleanup) return

        val log = logger.getCtxLoggers(className, "cleanupBuffer")

        log.trace("Running buffer cleanup for target $targetID, number of files in number is ${buffer.count(targetID)}, buffer size is ${buffer.size(targetID).byteCountString}")

        try {
            var deletedFiles = 0
            when {
                bufferConfiguration.retainNumber != null -> {
                    deletedFiles = buffer.cleanByNumber(targetID, bufferConfiguration)
                    if (deletedFiles != 0) log.info("Deleted $deletedFiles files for target \"${targetID}\" by max number of files ${bufferConfiguration.retainNumber}")
                }

                bufferConfiguration.retainPeriod != null -> {
                    deletedFiles = buffer.cleanByAge(targetID, bufferConfiguration)
                    if (deletedFiles != 0) log.info("Deleted $deletedFiles files for target \"${targetID}\" by max number of files ${bufferConfiguration.retainNumber}")
                }

                bufferConfiguration.retainSize != null -> {
                    val deletedBySize = buffer.cleanBySize(targetID, bufferConfiguration)
                    deletedFiles += deletedBySize.first
                    if (deletedBySize.first != 0) log.info("Deleted $deletedBySize files for target \"${targetID}\" with a total size of ${deletedBySize.second.byteCountString} by max retain size of files ${bufferConfiguration.retainSize!!.byteCountString}")
                }
            }
            if (deletedFiles > 0) {
                log.info("Finished buffer cleanup for target $targetID, number of files in number is ${buffer.count(targetID)}, buffer size is ${buffer.size(targetID).byteCountString}")
                metricsCollectorMethod?.let { it(listOf(MetricsValueParam(METRICS_MESSAGE_BUFFERED_DELETED, MetricsValue(1), MetricUnits.COUNT))) }
            }

        } catch (e: Exception) {
            log.error("Error cleaning up buffer for target $targetID, $e")
        }
    }


    // Writes data to a single target
    private suspend fun forwardDataToTarget(targetData: TargetData): Boolean = coroutineScope {

        val log = logger.getCtxLoggers(className, "writeDataToTarget")
        var didForwardToTarget = false

        log.trace("Forwarding target data with serial ${targetData.serial} to target \"$targetID\"")
        try {
            withTimeout(StoreForwardTargetWriter.TIMEOUT_TARGET_FORWARD) {
                didForwardToTarget = if (writer.isInitialized) {
                    writer.writeTargetData(targetData)
                    true
                } else {
                    log.trace("Can not forward to target $targetID as it has not been initialized yet")
                    false
                }
            }
        } catch (t: TimeoutCancellationException) {
            log.trace("Timeout forwarding to target \"${targetID}\"")
        } catch (e: Throwable) {
            log.error("Error forwarding to target \"${targetID}\", $e")
        }
        return@coroutineScope didForwardToTarget

    }

    private suspend fun handleTargetResults(resultData: TargetResult) {

        val logs = logger.getCtxLoggers(className, "handleRTargetResults")

        if (resultData.containsAcks) {
            logs.trace("Received ACK for target data with serials ${resultData.ackSerialList} from target $targetID")
            removeFromStore(resultData.ackSerialList)
        }

        if (resultData.containsErrors) {
            logs.warning("Received ERROR for target data with serials ${resultData.errorSerialList} from target $targetID")
            removeFromStore(resultData.errorSerialList)
        }

        if (resultData.containsNacks) {
            logs.trace("NACK received for target data with serials ${resultData.nackSerials} from target $targetID")
            addToStore(resultData.nackMessageList)
        }

    }

    private suspend fun removeFromStore(list: Iterable<String>?) {
        if (list != null && (list as List<String>).isNotEmpty()) buffer.remove(targetID, list)
    }

    private suspend fun addToStore(list: Iterable<TargetData>?) {
        if (list != null && (list as List<TargetData>).isNotEmpty())
            buffer.add(targetID, list)
    }

    suspend fun write(targetData: TargetData) {
        forwardChannel.send(targetData)
    }

    fun handleResult(result: TargetResult) {
        runBlocking {
            resultChannel.send(result)
        }
    }

}