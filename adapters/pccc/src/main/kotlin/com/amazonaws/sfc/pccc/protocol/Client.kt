/*
 *
 *
 *     Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *      Licensed under the Amazon Software License (the "License"). You may not use this file except in
 *      compliance with the License. A copy of the License is located at :
 *
 *      http://aws.amazon.com/asl/
 *
 *      or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 *      language governing permissions and limitations under the License.
 *
 *
 *
 */

package com.amazonaws.sfc.pccc.protocol


import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.pccc.config.PcccControllerConfiguration
import com.amazonaws.sfc.pccc.protocol.Address.Companion.addressBytes
import com.amazonaws.sfc.pccc.protocol.Address.Companion.totalLength
import com.amazonaws.sfc.pccc.protocol.Decoders.bytes
import com.amazonaws.sfc.pccc.protocol.Decoders.readInt16
import com.amazonaws.sfc.pccc.protocol.Decoders.readInt32
import com.amazonaws.sfc.pccc.protocol.Decoders.toInt32
import com.amazonaws.sfc.tcp.TcpClient
import com.amazonaws.sfc.util.asHexString
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class Client(private val config: PcccControllerConfiguration, private val logger: Logger) {

    private val className = this::class.simpleName.toString()

    private var isConnected = false
    private var lastControllerConnectAttempt = 0L
    private var requestSequenceNum = AtomicInteger(0)
    private var sessionHandle: ByteArray? = null
    private var tcpClient: TcpClient? = null
    private val connectingMutex = Mutex()


    private val nextRequestSequenceNumberAsBytes: ByteArray
        get() {
            val n = requestSequenceNum.incrementAndGet().toShort()
            if (n == Short.MAX_VALUE) {
                requestSequenceNum.set(0)
            }
            return n.bytes
        }

    val connected: Boolean
        get() {
            return isConnected
        }
    private val connectPath by lazy {
        if (config.connectPathConfig != null) {
            ConnectPath(
                backplane = config.connectPathConfig.backplane.toByte(),
                slot = config.connectPathConfig.slot.toByte()
            )
        } else null
    }

    suspend fun close() {
        isConnected = false
        sessionHandle = null
        tcpClient?.close(ONE_SECOND)
    }

    suspend fun connect() {

        val log = logger.getCtxLoggers(className, "connect")

        connectingMutex.withLock {

            log.info("Connecting to controller at ${config.address}")

            if (isConnected) return

            val controllerAddress = " ${config.address}:${config.port}"

            // Wait after failed connection at EIP level, note that tcp client will use same logic at TCP level
            // The wait here will prevent flood of connections when tcp connection can be established but EIP connection fails
            val waitPeriod: Long =
                ((lastControllerConnectAttempt + config.waitAfterConnectError.inWholeMilliseconds) - System.currentTimeMillis())

            if (waitPeriod > 0) {
                log.trace("Waiting ${waitPeriod.toDuration(DurationUnit.MILLISECONDS)} to reconnect to$controllerAddress")
                delay(waitPeriod)
            }

            lastControllerConnectAttempt = System.currentTimeMillis()

            sessionHandle = null

            tcpClient?.close(ONE_SECOND)
            isConnected = false

            tcpClient = TcpClient(config, logger = logger)
            tcpClient?.start()

            tcpClient?.write(PCCC_CONNECT_REQUEST)

            val connectResponse = ByteArrayOutputStream(PCCC_CONNECT_RESPONSE_LEN)

            try {
                // wait for first response byte
                withTimeout(config.connectTimeout) {
                    while (connectResponse.size() < PCCC_CONNECT_RESPONSE_LEN) {
                        // after receiving the first byte the timeout is much shorter to breakout quicker if controller stops responding or network is disconnected
                        val readByte = withTimeoutOrNull(10) {
                            tcpClient?.read()
                        }
                        if (readByte != null) {
                            connectResponse.write(readByte.toInt())
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                throw ClientException("Timeout connecting to$controllerAddress after ${config.connectTimeout}")
            }

            val responsePacket = connectResponse.toByteArray()

            try {
                validateConnectResponsePacket(responsePacket)
            } catch (e: ClientException) {
                close()
                throw e
            }

            isConnected = true
            log.info("Connected to controller at ${config.address}")

            // Reset because a successful connection was made, so we can reconnect immediately without delay
            lastControllerConnectAttempt = 0L

            sessionHandle = responsePacket.sliceArray(4..7)
        }
    }


    @JvmName("readOptimized")
    // This function takes a list of (optimized) address sets, this allows the application to build, cache and re-use
    // the read packets build for each a list of addresses.
    suspend fun read(addressSets: List<OptimizedAddressSet>): Map<Address, Any> {

        val log = logger.getCtxLoggers(className, "read")

        if (!isConnected) throw ClientException("Can not read if client is not connected")

        val fromControllerStr = "from controller at ${config.address}"

        val readValues = sequence {
            addressSets.forEach { addressSet ->

                val addressStr = "address${if (addressSet.addresses.size == 1) "" else "es"} ${addressSet.addresses}"

                log.trace("Reading $addressStr $fromControllerStr")
                val readResponseStream = ByteArrayOutputStream()

                try {

                    runBlocking {
                        tcpClient?.write(addressSet.readPacket)
                        try {
                            withTimeout(config.readTimeout) {
                                val firstByte = readFirstReadResponseByte()
                                readResponseStream.write(byteArrayOf(firstByte))
                                // the next bytes contain the length of the data to read
                                val len = readReadResponseLength(readResponseStream)
                                readReadPackageData(readResponseStream, len)
                            }
                        } catch (_: TimeoutCancellationException) {
                            throw ClientException("Timeout reading read response package $fromControllerStr")
                        }
                    }

                    val responsePacket = readResponseStream.toByteArray()

                    validateReadResponsePackage(responsePacket)
                    val responseValueBytes =
                        validatedResponseValueData(responsePacket, addressSet.addresses, addressSet.sequenceNumber)

                    // At his point all codes in and length of package are checked
                    // Get raw data and decode data for all addresses

                    val valuesOfReadAddresses = processReadResponsePacketData(addressSet.addresses, responseValueBytes)
                    // ZIp addresses and data together, the order of the addresses might have been modified because of optimization
                    yieldAll(addressSet.addresses.zip(valuesOfReadAddresses))

                } catch (e: Exception) {
                    log.error("Error reading $addressStr $fromControllerStr, $e")
                }
            }

        }.toMap()

        log.trace("Read ${readValues.size} value(s) $readValues $fromControllerStr")

        return readValues
    }

    // Read the data for a list of addresses, consider to use method with takes a pre-build set of (optimized) addresses for repeated reads the same addresses
    suspend fun read(addresses: List<Address>): Map<Address, Any> {
        // Build a list of lists of addresses to read. If read optimization is used then each list contains the address which
        // can be combined in a single read from the controller
        val addressSets = buildAddressSets(addresses)
        return read(addressSets)
    }

    fun buildAddressSets(addresses: List<Address>): List<OptimizedAddressSet> {
        val addressSets = if (config.optimizeReads) optimizeAddresses(addresses)
        else addresses.map {
            val a = listOf(it)
            OptimizedAddressSet(a, buildReadPacket(a), requestSequenceNum.get().toShort())
        }
        return addressSets
    }

    // Reads data for a single address
    suspend fun read(address: Address): Any? {
        val a = listOf(address)
        val o = OptimizedAddressSet(a, buildReadPacket(a), requestSequenceNum.get().toShort())
        return read(listOf(o))[address]
    }

    private fun validatedResponseValueData(
        responsePacket: ByteArray,
        addressesToRead: List<Address>,
        sequenceNumber: Short
    ): ByteArray {

        if (responsePacket[0] != 0x6F.toByte())
            throw ClientException("Packet read response type 0x6F expected but received ${responsePacket[0].asHexString()}")

        val dataLen = responsePacket.readInt16(2)
        val readResponseData = responsePacket.sliceArray(44..<EIP_HEADER_LEN + dataLen)
        checkPcccReadResponseData(addressesToRead, sequenceNumber, readResponseData)

        return readResponseData.sliceArray(11..<readResponseData.size)
    }

    private fun validateReadResponsePackage(responsePacket: ByteArray) {
        checkPacketSessionHandle(responsePacket)
        checkReadPacketForErrorCodes(responsePacket)
    }

    private suspend fun CoroutineScope.readReadPackageData(responsePackageStream: ByteArrayOutputStream, len: Int) {
        while (isActive && responsePackageStream.size() < len) {
            val b = withTimeoutOrNull(1) {
                tcpClient!!.read()
            }
            if (b != null) {
                withContext(Dispatchers.IO) {
                    responsePackageStream.write(byteArrayOf(b))
                }
            }
        }
    }

    private suspend fun CoroutineScope.readReadResponseLength(responsePackageStream: ByteArrayOutputStream): Int {
        // now read the next 3 bytes, so we can get length of packet
        while (isActive && responsePackageStream.size() < 4) {
            val b = withTimeoutOrNull(1) {
                tcpClient!!.read()
            }
            if (b != null) {
                withContext(Dispatchers.IO) {
                    responsePackageStream.write(byteArrayOf(b))
                }
            }
        }

        return responsePackageStream.toByteArray().readInt16(2) + EIP_HEADER_LEN
    }

    private fun buildCipHeaderWithSessionHandle(): ByteArray {
        val buffer = ByteArrayOutputStream(PCCC_CIP_HEADER.size)
        buffer.write(PCCC_CIP_HEADER.sliceArray(0..3))
        buffer.write(sessionHandle!!)
        buffer.write(PCCC_CIP_HEADER.sliceArray(8..<PCCC_CIP_HEADER.size))
        return buffer.toByteArray()
    }

    private fun buildEncapsulationHeaderWithSequenceNumber(): ByteArray {
        val buffer = ByteArrayOutputStream(PCCC_CIP_HEADER.size)
        // PCCC header minus 2 bytes
        buffer.write(PCCC_ENCAPSULATION_HEADER.sliceArray(0..PCCC_ENCAPSULATION_HEADER.size - 3))
        // Sequence number
        buffer.write(nextRequestSequenceNumberAsBytes)
        return buffer.toByteArray()
    }

    private fun buildReadPacket(addresses: List<Address>): ByteArray {
        val requestByteStream = ByteArrayOutputStream(255)

        // CIP Header with session handle
        requestByteStream.write(buildCipHeaderWithSessionHandle())

        var routerLength = 0
        if (connectPath != null) {
            requestByteStream.write(connectPath!!.bytes)
            routerLength = PCCC_ROUTING_HEADER.size
        }

        // PCCC header minus 2 bytes
        requestByteStream.write(buildEncapsulationHeaderWithSequenceNumber())

        // Address
        val addressBytes = addresses.addressBytes
        requestByteStream.write(addressBytes)

        // pad for even number of bytes
        if (routerLength > 0 && ((addressBytes.size + PCCC_ENCAPSULATION_HEADER.size) % 2) == 1) {
            requestByteStream.write(byteArrayOf(0x00.toByte()))
            routerLength += 1
        }

        // Connection path
        if (connectPath != null) {
            requestByteStream.write(connectPath!!.bytes)
            routerLength += connectPath!!.bytes.size
        }

        val requestBytes = requestByteStream.toByteArray()
        setPacketLengthFields(requestBytes, addressBytes.size, routerLength)

        return requestBytes

    }

    private fun checkPacketSessionHandle(readResponsePacket: ByteArray) {
        val responseSessionHandle = readResponsePacket.sliceArray(4..7)
        if (!responseSessionHandle.contentEquals(sessionHandle)) {
            throw ClientException("Expected session handle ${sessionHandle!!.toInt32.asHexString()}, received session handle was ${responseSessionHandle.toInt32.asHexString()} }")
        }
    }

    private fun checkPcccReadResponseData(
        addresses: List<Address>,
        expectedSequenceNumber: Short,
        readResponseData: ByteArray
    ) {

        if (readResponseData.size < 9) throw ClientException("PCCC data in read response must be at least 9 bytes, but only ${readResponseData.size} received  ")

        if ((readResponseData[0] != 0x07.toByte()) || (readResponseData[7] != 0x4f.toByte()))
            throw ClientException("Expected value 0x07 at data offset 0 and value 0x4f (REPLY) at offset 7, received 0x${readResponseData[0].asHexString()}, 0x${readResponseData[7].asHexString()}")

        val pcccErrorCode = readResponseData[PCCC_STATUS_FIELD]
        if (pcccErrorCode != 0.toByte()) {
            throw ClientException("PCCC error code 0x${pcccErrorCode.asHexString()}${if (pcccErrorCode == 0x10.toByte()) " (Illegal command or format)" else ""} received ")
        }

        val expectedDataSize = addresses.totalLength
        if (expectedDataSize != (readResponseData.size - 11)) {
            throw ClientException("PCCC data length is ${readResponseData.size - 11} , expected $expectedDataSize")
        }

        val seqNum = readResponseData.readInt16(9)

        if (seqNum != expectedSequenceNumber) {
            throw ClientException("Expected sequence number $expectedSequenceNumber but received $seqNum")
        }
    }

    private fun checkReadPacketForErrorCodes(responsePacket: ByteArray) {

        val errorCodes = responsePacket.sliceArray(8..11)
        if (errorCodes.any { it.toInt() != 0 }) {
            throw ClientException("PCCC read error, received error codes ${errorCodes.asHexString()}}")
        }

        val responseCodes = responsePacket.sliceArray(40..43)
        val expectedResponseCodes = byteArrayOf(0xcb.toByte(), 0x00, 0x00, 0x00)
        if (!responseCodes.contentEquals(expectedResponseCodes)) {
            throw ClientException("Invalid PCCC response codes at offset 40, expected ${expectedResponseCodes.asHexString()}, received ${responseCodes.asHexString()}")
        }

        val cipError1 = responsePacket.readInt32(24)
        val cipError2 = responsePacket.readInt16(34)
        if (cipError1 != 0 || cipError2 != 0.toShort()) {
            throw ClientException("CIP Error $cipError1 at offset 24, $cipError2 at offset 34 ")
        }
    }

    // Calculates max gap between the address and other addresses in the list
    private fun List<Address>.maxGap(address: Address): Int {
        if (this.isEmpty()) return 0
        val dataLenPerItem = this[0].dataFile.sizeOfSingleItemInBytes
        val startOfAddress = address.dataOffsetInItem + (address.index * dataLenPerItem)
        return maxOf {
            val end = it.dataOffsetInItem + (it.index * dataLenPerItem) + it.arrayLen * dataLenPerItem
            (startOfAddress - end - 1)
        }
    }

    private fun optimizeAddresses(addresses: List<Address>): List<OptimizedAddressSet> {

        val log = logger.getCtxLoggers(className, "optimizeAddresses")
        val optimized = sequence {

            // Group by type and number of the data files
            val s: Map<DataFile, List<Address>> = addresses.toSet().groupBy { it.dataFile }
            val t = s.map { (df, n) -> df to n.groupBy { it.dataFileNumber } }.toMap()

            t.forEach { (dataFile: DataFile, addressesForDataType: Map<Short, List<Address>>) ->

                // optimize reads for supported data types
                if (dataFile in optimizedDataTypes) {
                    addressesForDataType.forEach { (_, addressesForDataFileNumber) ->

                        val addressesSortedByIndex = addressesForDataFileNumber.sortedBy { it.index }

                        val firstAddress = addressesSortedByIndex.first()
                        val combinedAddresses = mutableListOf(firstAddress)

                        addressesSortedByIndex.drop(1).forEach { address ->

                            // combined total length of combined address if this address is added, note that memory buffers of addresses may overlap
                            val totalCombinedLengthWithinLimit = (combinedAddresses + address).totalLength <= 220
                            // it within range then calculate max gap between this and any other combined address
                            val gapBetweenAddressesWithinLimit = if (totalCombinedLengthWithinLimit) {
                                val gap = combinedAddresses.maxGap(address)
                                gap <= config.readMaxGap
                            } else false

                            if (totalCombinedLengthWithinLimit && gapBetweenAddressesWithinLimit) {
                                // add to combined addresses
                                combinedAddresses.add(address)
                            } else {
                                yield(
                                    OptimizedAddressSet(
                                        combinedAddresses.map { it }, // map for deep copy
                                        buildReadPacket(combinedAddresses),
                                        requestSequenceNum.get().toShort()
                                    )
                                )
                                combinedAddresses.clear()
                                combinedAddresses.add(address)
                            }

                        }
                        // At the end of the list create set for remaining combined addresses
                        if (combinedAddresses.isNotEmpty()) {
                            yield(
                                OptimizedAddressSet(
                                    combinedAddresses.map { it },
                                    buildReadPacket(combinedAddresses),
                                    requestSequenceNum.get().toShort()
                                )
                            )
                        }
                    }
                } else {
                    // Logic for types that cannot be optimized
                    addressesForDataType.entries.forEach { (_, aa) ->
                        aa.forEach { a ->
                            yield(
                                OptimizedAddressSet(
                                    arrayListOf(a),
                                    buildReadPacket(listOf(a)),
                                    requestSequenceNum.get().toShort()
                                )
                            )
                        }
                    }
                }
            }
        }.toList()

        log.trace("Optimized list of ${addresses.size} into ${optimized.size} sets of combined addresses")
        return optimized
    }

    private fun processReadResponsePacketData(addresses: List<Address>, valueBytes: ByteArray): List<Any> =

        sequence {
            val offset =
                addresses.first().index * addresses.first().dataFile.sizeOfSingleItemInBytes + addresses.first().dataOffsetInItem
            addresses.forEach { address ->

                // Get the slice of data for this address from the buffer
                val startPositionOfData = (address.index * address.dataFile.sizeOfSingleItemInBytes) - offset
                val endPositionOfData =
                    (startPositionOfData + address.dataFile.sizeOfSingleItemInBytes * address.arrayLen) - 1
                val addressData = valueBytes.sliceArray(startPositionOfData..endPositionOfData)

                // Decode either as single value or array
                val decoded = if (address.arrayLen == 1) {
                    address.dataFile.decodeValue(addressData, address.addressOffset)
                } else {
                    address.dataFile.decodeArrayValue(addressData)
                }

                yield(decoded)
            }
        }.toList()


    // Reads the first byte of a read response and checks type
    private suspend fun readFirstReadResponseByte(): Byte {
        val firstByte = tcpClient!!.read()
        if (firstByte != 0x6f.toByte()) throw ClientException("Expected first byte read response value 0x6f, received 0x${firstByte.asHexString()}")
        return firstByte
    }

    // Set length of router information in read packet
    private fun setLengthForRouter(readPacketBuffer: ByteArray, routerLength: Int) {
        if (routerLength > 0) {
            val l = (readPacketBuffer.size + PCCC_ENCAPSULATION_HEADER.size).toShort().bytes
            l.copyInto(readPacketBuffer, PCCC_CIP_HEADER.size + routerLength - 2, 2)
        }
    }

    // Set overall length in EIP header of read packet
    private fun setOverallLengthInEipHeader(readPacketBuffer: ByteArray) {
        val l = (readPacketBuffer.size - EIP_HEADER_LEN).toShort().bytes
        l.copyInto(readPacketBuffer, 2, 0, 2)
    }

    // Set overall message length
    private fun setOverallMessageLength(readPacketBuffer: ByteArray, addrLen: Int, routerLength: Int) {
        val l = (addrLen + PCCC_ENCAPSULATION_HEADER.size + routerLength).toShort().bytes
        l.copyInto(readPacketBuffer, 38, 0, 2)
    }

    private fun setPacketLengthFields(readPacketBuffer: ByteArray, addrLen: Int, routerLength: Int) {
        setOverallLengthInEipHeader(readPacketBuffer)
        setOverallMessageLength(readPacketBuffer, addrLen, routerLength)
        setLengthForRouter(readPacketBuffer, routerLength)
    }

    private fun validateConnectResponsePacket(responsePacket: ByteArray) {
        val errorCodes = responsePacket.sliceArray(8..11)
        if (errorCodes.any { it.toInt() != 0 }) {
            throw ClientException("EIP connection error, received error codes ${errorCodes.asHexString()}}")
        }

        if ((responsePacket[0] != 0x65.toByte()) || (responsePacket[2] != 0x04.toByte())) {
            throw ClientException("Invalid packet received, ${responsePacket.asHexString()}")
        }
    }

    companion object {
        val ONE_SECOND = 1.toDuration(DurationUnit.SECONDS)

        private const val PCCC_CONNECT_RESPONSE_LEN = 28
        private const val PCCC_STATUS_FIELD = 8

        private const val EIP_HEADER_LEN = 24

        private val PCCC_CONNECT_REQUEST = byteArrayOf(
            // Register session
            0x65, 0x00,
            // Length of data
            0x04, 0x00,
            // Session handle
            0x00, 0x00, 0x00, 0x00,
            // Status
            0x00, 0x00, 0x00, 0x00,
            // Sender/client context
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // Header options
            0x00, 0x00, 0x00, 0x00,
            // Register protocol version
            0x01, 0x00,
            // Register options
            0x00, 0x00
        )

        private val PCCC_CIP_HEADER = byteArrayOf(
            // EIP command RR Data
            0x6f, 0x00,
            // Length of attached data
            0x00, 0x00,
            // Session handle
            0x00, 0x00, 0x00, 0x00,
            // Status
            0x00, 0x00, 0x00, 0x00,
            // Client context
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // Header options
            0x00, 0x00, 0x00, 0x00,
            // Interface handle
            0x00, 0x00, 0x00, 0x00,
            // Command timeout
            0x0a, 0x00,
            // CFP Item count
            0x02, 0x00,
            // CPF Address Item
            0x00, 0x00, 0x00, 0x00,
            // CPF Data item B2
            0xb2.toByte(), 0x00,
            // Length
            0x17, 0x00
        )

        private val PCCC_ROUTING_HEADER = byteArrayOf(
            // CIP unconnected send service
            0x52,
            // Length in words
            0x02,
            // Logical segment 6 connection manager
            0x20, 0x06,
            // Logical segment instance 0x01
            0x24, 0x01,
            // Priority
            0x0a, 0x09,
            // length
            0x20, 0x00
        )

        private val optimizedDataTypes = setOf(
            DataFileType.INPUT,
            DataFileType.OUTPUT,
            DataFileType.INT32,
            DataFileType.INT16,
            DataFileType.STRING,
            DataFileType.FLOAT,
            DataFileType.STATUS,
            DataFileType.BINARY,
            DataFileType.TIMER,
            DataFileType.COUNTER,
            DataFileType.CONTROL,
            DataFileType.ASCII
        )


        private val PCCC_ENCAPSULATION_HEADER = byteArrayOf(
            // PCCC execute
            0x4b,
            // Path len
            0x02,
            // Path for instance 1
            0x20, 0x67, 0x24, 0x01,
            // Vendor
            0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            // PCC command
            0x0f, 0x00,
            // Transaction ID
            0x00, 0x00
        )
    }

}
