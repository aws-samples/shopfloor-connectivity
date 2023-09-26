/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.modbus.adapter


import com.amazonaws.sfc.data.ChannelReadValue
import com.amazonaws.sfc.data.SourceReadError
import com.amazonaws.sfc.data.SourceReadResult
import com.amazonaws.sfc.data.SourceReadSuccess
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_TYPE
import com.amazonaws.sfc.modbus.config.ModbusChannelConfiguration
import com.amazonaws.sfc.modbus.config.ModbusChannelType
import com.amazonaws.sfc.modbus.config.ModbusSourceConfiguration
import com.amazonaws.sfc.modbus.protocol.*
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference

/**
 * Modbus device implementation (both RTU and TCP)
 * @property configuration ModbusSourceConfiguration Modbus source configuration
 * @property modbus ModbusHandler Modbus protocol handler
 * @param ownerAdapter ModbusAdapter Adapter that uses this modbus device
 */
class ModbusDevice(private val sourceID: String,
                   private val configuration: ModbusSourceConfiguration,
                   val deviceID: Int?,
                   val modbus: ModbusHandler,
                   ownerAdapter: ModbusAdapter) {

    private val className = this::class.java.simpleName

    private val metricDimensions = mapOf(
        METRICS_DIMENSION_SOURCE to "${configuration.protocolAdapterID}:$sourceID",
        METRICS_DIMENSION_TYPE to ownerAdapter::class.java.simpleName)

    /**
     * Reads values from a modbus device
     * @param channels List<String>? Channels to read, channels define the addresses range by a start address and an optional length, to read
     * @return SourceReadResult Values read from the device
     */
    suspend fun readValues(channels: List<String>? = null, metrics: MetricsCollector?): SourceReadResult = coroutineScope {

        lateinit var readResult: SourceReadResult

        val start = DateTime.systemDateTime().toEpochMilli()

        val reader = launch("Reader") {

            val logTrace = logger.get()?.getCtxTraceLog(ModbusDevice::class.java.simpleName, "reader")

            try {

                // get exclusive access to the device
                logTrace?.invoke("Acquiring lock on device \"${configuration.sourceAdapterDevice}\" on adapter \"${configuration.protocolAdapterID}\" to read data for source \"$sourceID\"")
                modbus.modbusDevice.lock()
                logTrace?.invoke("Acquired lock on device \"${configuration.sourceAdapterDevice}\"  on adapter \"${configuration.protocolAdapterID}\" for source \"${configuration.name}\"")

                // number of requests that can be sent to a without a response received for it, guarded by this semaphore
                val requestSlots = Semaphore(permits = adapter.get()?.requestDepth(configuration)?.toInt() ?: 1)

                val resultChannel = Channel<SourceReadResult>()

                // build requests to send to the device
                val requests = buildRequests(channels)

                // this coroutine will receive and process responses from the device
                launch("Response Receiver") {
                    receiveResponses(requests, requestSlots, resultChannel, channels)
                }

                // this coroutine will send the request to the device
                launch("Request Transmitter") {
                    sendRequests(requests, requestSlots)
                }

                // this coroutine will check for a timeout receiving the responses for all requests
                launch("Timeout") {
                    checkForTimeout(resultChannel)
                }

                // the receiver or timeout-coroutines will send a message to this channel if all responses are received or a timeout occurred
                readResult = resultChannel.receive()

                kotlin.coroutines.coroutineContext.cancelChildren()

                val readDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()

                createMetrics(metrics, configuration.protocolAdapterID, readDurationInMillis, readResult)


            } catch (e: Exception) {
                readResult = SourceReadError(e.toString())

            } finally {
                modbus.modbusDevice.unlock()
                logTrace?.invoke("Released lock on device\"${configuration.sourceAdapterDevice}\" on adapter \"${configuration.protocolAdapterID}\"")
            }
        }

        reader.join()



        readResult
    }

    private suspend fun createMetrics(metrics: MetricsCollector?,
                                      protocolAdapterID: String,
                                      readDurationInMillis: Double,
                                      result: SourceReadResult) {

        if (metrics == null) return

        metrics.put(protocolAdapterID,
            metrics.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READS, 1.0, MetricUnits.COUNT, metricDimensions),
            metrics.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READ_DURATION, readDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions))

        if (result is SourceReadSuccess) {
            metrics.put(protocolAdapterID,
                metrics.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_VALUES_READ, result.values.size.toDouble(), MetricUnits.COUNT, metricDimensions),
                metrics.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READ_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions))
        } else {
            metrics.put(protocolAdapterID,
                metrics.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READ_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions))
        }

    }

    /**
     * stops the device
     */
    suspend fun stop() {
        modbus.stop()
    }

    // Max number of requests that can be sent before receiving responses
    private val requestDepth = ownerAdapter.requestDepth(configuration).toInt()

    // logger for output
    private val logger = WeakReference(ownerAdapter.logger)

    // adapter using the device (weak ref to avoid circular dependencies between device and adapter)
    private val adapter = WeakReference(ownerAdapter)

    // transaction id with lock for requests
    private var _transactionID = 0
    private val transactionIdLock = Mutex()

    // generate next transaction id
    private suspend fun nextTransactionID(): UShort? {

        transactionIdLock.withLock {
            if (requestDepth == 0) {
                return null
            }

            if (_transactionID == 0xFFFF) {
                _transactionID = 0
            }
            _transactionID += 1
            return _transactionID.toTransactionID()
        }
    }

    // Combine sets of channels (addresses) to be read for a channel type into larger sets, so they can be read by a single read request
    private fun optimizedAddressRangesForChannelType(source: ModbusSourceConfiguration,
                                                     channelType: ModbusChannelType,
                                                     channels: List<String>? = null): List<Pair<UShort, UShort>> {

        val ranges = mutableListOf<Pair<UShort, UShort>>()
        val maxReadLen = maxItemsForType(channelType)
        val maxGap = source.maxReadGapForType(channelType)
        val addresses = source.addressesForType(channelType, channels)

        if (addresses.isNotEmpty()) {

            // first address to start with
            var startAddress = addresses[0]
            // last processed address
            var lastAddress = startAddress

            for (i in 1 until addresses.size) {

                // check if address of next set is adjacent to previous one with a max gap in between them
                val isAdjacentAddress = (addresses[i] <= (lastAddress + maxGap))
                if (isAdjacentAddress) {
                    // add to current set
                    lastAddress = addresses[i]
                    continue
                }

                // not adjacent, add current set to result
                ranges.addAll(constrainedAddressRanges(startAddress, lastAddress, maxReadLen))
                // start a new current set
                startAddress = addresses[i]
                lastAddress = startAddress
            }

            // add last set to the result
            ranges.addAll(constrainedAddressRanges(startAddress, lastAddress, maxReadLen))
        }
        return ranges
    }

    // get all channels for a specific type
    private fun ModbusSourceConfiguration.channelsForType(modbusChannelType: ModbusChannelType,
                                                          channels: List<String>? = null): List<ModbusChannelConfiguration> =
        this.channels
            .filter { (it.value.type == modbusChannelType) && (if (channels == null) true else (it.key in channels)) }
            .map { it.value }

    // get all addresses as a sorted list from the channels for a specific type
    private fun ModbusSourceConfiguration.addressesForType(t: ModbusChannelType, channels: List<String>? = null): List<Int> =
        this.channelsForType(t, channels)
            .flatMap { c -> c.addresses.toList() }
            .toSet()
            .sorted()

    // get all addresses for a channel type without optimizing the address ranges for reading
    private fun unOptimizedAddressRangesForChannelType(
        source: ModbusSourceConfiguration,
        channelType: ModbusChannelType,
        channels: List<String>? = null
    ): List<Pair<UShort, UShort>> {
        return source.channelsForType(channelType, channels).map { c ->
            (c.address.toAddress()) to (c.size.toAddress())
        }

    }

    // extension method for ModbusChannelConfiguration channel configuration to return all addresses, using the start address and number of addresses
    private val ModbusChannelConfiguration.addresses: Sequence<Int>
        get() {
            val end = this.address.toInt() + (this.size).toInt() - 1
            return generateSequence(this.address.toInt()) { if (it < end) it + 1 else null }
        }

    // extension for modbus configuration to return all addresses, optimized or non-optimized, for a modbus source configuration
    private fun ModbusSourceConfiguration.addressRangesForType(
        channelType: ModbusChannelType, channels: List<String>? = null
    ): List<Pair<UShort, UShort>> =
        if (this.optimization.enabled) optimizedAddressRangesForChannelType(this, channelType, channels)
        else unOptimizedAddressRangesForChannelType(this, channelType, channels)


    // returns the max gap size tor optimizing reads for a channel type
    private fun ModbusSourceConfiguration.maxReadGapForType(t: ModbusChannelType): Int {
        return when (t) {
            ModbusChannelType.COIL, ModbusChannelType.DISCRETE_INPUT -> this.optimization.coilMaxGapSize
            ModbusChannelType.HOLDING_REGISTER, ModbusChannelType.INPUT_REGISTER -> this.optimization.registerMaxGapSize
        }
    }

    // returns the maximum numbers of items than can be read for a channel type
    private fun maxItemsForType(t: ModbusChannelType): Int {
        return when (t) {
            ModbusChannelType.COIL, ModbusChannelType.DISCRETE_INPUT -> Modbus.MAX_READ_COILS_INPUTS
            ModbusChannelType.HOLDING_REGISTER, ModbusChannelType.INPUT_REGISTER -> Modbus.MAX_READ_REGISTERS
        }
    }

    // returns the addresses as a sequence of address ranges (start,size) taking in account the max items that can be read
    private fun constrainedAddressRanges(start: Int, last: Int, maxReadLen: Int) = sequence {
        var j = start
        while (j <= last) {
            val c = minOf(last - j + 1, maxReadLen)
            yield(j.toAddress() to c.toUShort())
            j += c
        }
    }

    // send timeout response to result channel if a timeout occurs
    private suspend fun checkForTimeout(resultChannel: SendChannel<SourceReadResult>) {
        delay(configuration.readTimeout)
        resultChannel.send(SourceReadError("Timeout reading from source \"${configuration.name}\""))
    }

    // build all requests to send to the device
    private suspend fun buildRequests(channels: List<String>?) =
        ModbusChannelType.values().flatMap { channelType ->
            configuration.addressRangesForType(channelType, channels).map { addressRange ->
                buildRequest(channelType, addressRange.first, addressRange.second, deviceID)
            }
        }.associateBy { request -> request.transactionID }


    // reads all responses for a set of requests that were sent to the device
    private suspend fun receiveResponses(
        requests: Map<TransactionID?, RequestBase>,
        requestSlots: Semaphore,
        resultChannel: SendChannel<SourceReadResult>,
        channels: List<String>?
    ) {
        // keeps track of number of received responses
        var receivedResponses = 0

        val log = logger.get()?.getCtxLoggers(ModbusDevice::class.java.simpleName, "responseReceiveWorker")

        // create result data structure to store results per channel type
        val responseData = ModbusChannelType.values().associateWith { mutableMapOf<UShort, Any>() }

        // loop to read all requests
        receiverLoop@ while (receivedResponses < requests.size) {

            // wait for a response from the modbus protocol handler
            val resp = modbus.receive()

            // use transaction ID to map to originating request
            val request = requests[resp.transactionID]
            if (request == null) {
                // Unexpected transaction ID, ignore response, could result in timeout receiving all requests when receiving responses with incorrect transaction ID's
                val expectedTransactions = requests.keys.joinToString(separator = ",'")
                log?.warning?.invoke("Received response for transaction ${resp.transactionID}, expected transaction must be any of $expectedTransactions}")
                continue@receiverLoop
            }

            // response has an error, stop reading responses and return error
            if (resp.error != null) {
                resultChannel.send(SourceReadError("Error reading from source \\\"${configuration.name}\\\" : ${resp.error}"))
                break@receiverLoop
            }

            // store results per channel type as maps indexed by the addresses
            when (resp) {
                is ReadCoilsResponse -> responseData[ModbusChannelType.COIL]?.putAll(resp.coilStatusMapped(request.address))
                is ReadDiscreteInputsResponse -> responseData[ModbusChannelType.DISCRETE_INPUT]?.putAll(resp.inputStatusMapped(request.address))
                is ReadInputRegistersResponse -> responseData[ModbusChannelType.INPUT_REGISTER]?.putAll(resp.inputRegistersMapped(request.address))
                is ReadHoldingRegistersResponse -> responseData[ModbusChannelType.HOLDING_REGISTER]?.putAll(resp.registerValuesMapped(request.address))
            }

            // lower requests semaphore for this device
            requestSlots.release()
            log?.trace?.invoke("Released request slot ${if (resp.transactionID == 0.toTransactionID()) "" else " with transaction ID ${resp.transactionID}"}")
            receivedResponses += 1
        }
        resultChannel.send(SourceReadSuccess(mapToChannelValues(channels, responseData)))
    }

    // sends requests to device
    private suspend fun sendRequests(requests: Map<TransactionID?, RequestBase>, requestSlots: Semaphore) {

        val logTrace = logger.get()?.getCtxTraceLog(ModbusDevice::class.java.simpleName, "requestSendWorker")
        for (r in requests.values) {

            // transaction id, required to map responses to the requests
            val s = if (r.transactionID == 0.toTransactionID()) "" else " with transaction ID ${r.transactionID}"
            // send requests taking in account the max number of requests that can be sent before getting a response from the device
            logTrace?.invoke("Acquiring request slot on device ${configuration.sourceAdapterDevice}\" on adapter \"${configuration.protocolAdapterID}\" for source \"${configuration.name}\" to send request $s")
            requestSlots.acquire()
            logTrace?.invoke("Request Slot acquired for request$s")
            modbus.send(r)
        }
    }

    // maps data from responses to types used for the channel type
    private fun mapToChannelValues(channels: List<String>?, channelValues: Map<ModbusChannelType, MutableMap<UShort, Any>>): Map<String, ChannelReadValue> {

        // channels to map, as the result could contain values that were read as additional values die to combined address ranges for reading
        val mappedChannels = configuration.channels.asSequence().filter { if (channels != null) it.key in channels else true }
            .map { it.key to it.value }

        // values per channel
        val mappedValues = mutableMapOf<String, ChannelReadValue>()

        for (ch in mappedChannels) {
            // pairs of source and channel
            val sourceChannel = ch.second
            val channelID = ch.first
            // get size of the channel
            val size = (sourceChannel.size).toInt()
            if (size == 1) {
                // single value
                val value = channelValues[sourceChannel.type!!]?.get(sourceChannel.address.toAddress())
                if (value != null) {
                    mappedValues[channelID] = ChannelReadValue(value, null)
                }
            } else {
                // sets of multiple values
                val mappedReadValue = when (sourceChannel.type) {
                    ModbusChannelType.COIL, ModbusChannelType.DISCRETE_INPUT -> mapToAddresses<DiscreteValue>(size, channelValues, sourceChannel)
                    ModbusChannelType.INPUT_REGISTER, ModbusChannelType.HOLDING_REGISTER -> mapToAddresses<RegisterValue>(size, channelValues, sourceChannel)
                    else -> {
                        null
                    }
                }
                if (mappedReadValue != null) {
                    mappedValues[channelID] = mappedReadValue
                }
            }
        }
        return mappedValues
    }

    // maps a received value to the type for that channel
    private inline fun <reified T> mapToAddresses(
        size: Int,
        channelValues: Map<ModbusChannelType, MutableMap<UShort, Any>>,
        sourceChannel: ModbusChannelConfiguration
    ) =
        ChannelReadValue(Array(size) { i ->
            (channelValues[sourceChannel.type!!]?.get((sourceChannel.address + i).toAddress())) as T
        }, null)


    // build request for reading one of more values from a device
    private suspend fun buildRequest(channelType: ModbusChannelType, address: UShort, size: UShort, deviceID: Int?): RequestBase {

        return when (channelType) {
            ModbusChannelType.COIL -> ReadCoilsRequest(
                address = address,
                quantity = size,
                deviceID = deviceID?.toUByte() ?: DEFAULT_DEVICE_ID,
                transactionID = nextTransactionID())

            ModbusChannelType.DISCRETE_INPUT -> ReadDiscreteInputsRequest(
                address = address,
                quantity = size,
                deviceID = deviceID?.toUByte() ?: DEFAULT_DEVICE_ID,
                transactionID = nextTransactionID())

            ModbusChannelType.INPUT_REGISTER -> ReadInputRegistersRequest(
                address = address,
                quantity = size,
                deviceID = deviceID?.toUByte() ?: DEFAULT_DEVICE_ID,
                transactionID = nextTransactionID())

            ModbusChannelType.HOLDING_REGISTER -> ReadHoldingRegistersRequest(
                address = address,
                quantity = size,
                deviceID = deviceID?.toUByte() ?: DEFAULT_DEVICE_ID,
                transactionID = nextTransactionID())
        }
    }


    companion object {
        val DEFAULT_DEVICE_ID = 1.toUByte()
    }
}