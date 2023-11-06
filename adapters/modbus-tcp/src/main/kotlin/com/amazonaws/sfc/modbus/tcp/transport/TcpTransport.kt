
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.tcp.transport

import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricDimensions
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.modbus.protocol.ModbusTransport
import com.amazonaws.sfc.modbus.tcp.config.ModbusTcpDeviceConfiguration
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


/**
 * TCP data-transport to send/receive data over TCP
 */
class TcpTransport(private val config: ModbusTcpDeviceConfiguration,
                   private val adapterID: String,
                   private val metricDimensions: MetricDimensions,
                   private val metrics: MetricsCollector?,
                   val logger: Logger) : ModbusTransport {

    private val className = this::class.java.simpleName

    // channels to pass bytes to be sent and bytes read
    private val transmitChannel: Channel<UByteArray> = Channel(WRITE_BUFFER_SIZE)
    private val receiveChannel: Channel<UByte> = Channel(READ_BUFFER_SIZE)

    // coroutine workers for reading and writing bytes
    private var receiver: Job? = null
    private var transmitter: Job? = null

    // TCP socket with IO streams
    private var clientSocket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // last moment transport tried to connect
    private var lastConnectAttempt = 0L

    // lock for exclusive access to the transport
    private val transportLock = Mutex()

    private val scope = buildScope("TCP traffic handler")

    /**
     * Writes bytes to transport
     * @param bytes UByteArray
     */
    override suspend fun write(bytes: UByteArray) = transmitChannel.send(bytes)

    /**
     * Reads bytes from transport
     * @return UByte
     */
    override suspend fun read(): UByte = receiveChannel.receive()

    /**
     * Get exclusive access to transport
     */
    override suspend fun lock() = transportLock.lock()

    /**
     * Release exclusive access to transport
     */
    override suspend fun unlock() = transportLock.unlock()

    // mutex to prevent simultaneous connection attempts
    private val clientConnectMutex = Mutex()

    // after a failure close socket and set to null for reconnection at next read/write action
    private suspend fun flagForReconnect() = coroutineScope {

        clientConnectMutex.withLock {
            clientSocket = null
            withContext(Dispatchers.IO) {
                inputStream?.close()
                outputStream?.close()
            }
        }
    }

    // call to test if connected, if not attempt to reconnect
    private suspend fun ensureConnection(): Boolean {
        if (isConnected) {
            return true
        }

        val trace = logger.getCtxTraceLog(className, "ensureConnection")

        // wait to avoid flood of connection retries
        val waitPeriod = (lastConnectAttempt + config.waitAfterConnectError.inWholeMilliseconds) - System.currentTimeMillis()
        if (waitPeriod > 0) {
            trace("Waiting ${waitPeriod.toDuration(DurationUnit.MILLISECONDS)} to reconnect")
            delay(waitPeriod)
        }
        lastConnectAttempt = System.currentTimeMillis()

        return connect()
    }

    // test if socket is connected
    private inline val isConnected
        get() = (clientSocket?.isConnected ?: false)

    // create socket connection
    private suspend fun connect(): Boolean {

        val log = logger.getCtxLoggers(className, "connect")

        clientConnectMutex.withLock {

            // only one concurrent attempt to connect

            if (isConnected) {
                // some other process has connected whilst waiting for the mutex
                return true
            }

            coroutineScope {
                val connectJob = launch("Connect") {

                    // receives message when connected
                    val connectedChannel = Channel<Any?>()

                    launch("Setup Connection") {
                        setupConnection(log.trace, connectedChannel)
                    }

                    launch("Timeout") {
                        // signals when connecting takes too long (calling Socket() withing withTimeout is not reliable as it runs on IO dispatcher)
                        delay(config.connectTimeout.inWholeMilliseconds)
                        connectedChannel.send(null)
                    }

                    when (val result = connectedChannel.receive()) {
                        is Socket -> {
                            log.info("Connected to ${config.address}:${config.port}")
                        }

                        is Throwable -> log.error("Error connecting to ${config.address}:${config.port}, ${result.message}")
                        else -> log.error("Timeout connecting to ${config.address}:${config.port}")
                    }
                    coroutineContext.cancelChildren()
                }
                connectJob.join()
            }
        }

        createConnectionMetrics()

        return isConnected
    }

    private suspend fun createConnectionMetrics() {

        metrics?.put(adapterID,
            if (isConnected)
                MetricsCollector.METRICS_CONNECTIONS
            else
                MetricsCollector.METRICS_CONNECTION_ERRORS,
            1.0,
            MetricUnits.COUNT,
            metricDimensions)
    }

    // setup socket connection
    private suspend fun setupConnection(
        trace: ((String) -> Unit),
        connectedChannel: SendChannel<Any?>
    ) {
        try {
            withContext(Dispatchers.IO) {

                inputStream?.close()
                outputStream?.close()
                clientSocket = null

                trace("Trying to connect to ${config.address}:${config.port}")
                clientSocket = Socket(config.address, config.port)
                clientSocket?.soTimeout = READ_BLOCKING_DURATION
                inputStream = clientSocket?.getInputStream()
                outputStream = clientSocket?.getOutputStream()

                if (clientSocket != null) {
                    connectedChannel.send(clientSocket!!)
                }

            }
        } catch (e: Throwable) {
            clientSocket = null
            connectedChannel.send(e)
        }
    }

    /**
     * Start the transport
     */
    fun start() {

        receiver = scope.launch("TCP Receiver") {
            receiveData()
        }

        transmitter = scope.launch("TCP Transmitter") {
            transmitData()
        }
    }

    // writes the data to the socket
    private suspend fun transmitData() {
        val log = logger.getCtxLoggers(TcpTransport::class.java.simpleName, "receiver")

        withContext(Dispatchers.IO) {
            while (isActive) {
                if (isConnected) {
                    val bytes = transmitChannel.receive()
                    if (logger.level == LogLevel.TRACE) {
                        logTransmittedBytes(log.trace, bytes)
                    }
                    try {
                        outputStream?.write(bytes.asByteArray())
                        outputStream?.flush()
                        metrics?.put(adapterID, MetricsCollector.METRICS_BYTES_SEND, bytes.size.toDouble(), MetricUnits.BYTES, metricDimensions)
                    } catch (e: SocketException) {
                        log.error("Error writing data to ${config.address}:${config.port}, ${e.message}")
                        flagForReconnect()
                        delay(config.waitAfterWriteError.inWholeMilliseconds)
                    }
                } else {
                    // wait to avoid tight spinning loop if not connected, the read loop will (re)connect to server
                    if (!isConnected) {
                        delay(1000L)
                    }
                }
            }
        }
    }

    // reads the data from the socket the data
    private suspend fun receiveData() {
        withContext(Dispatchers.IO) {

            val log = logger.getCtxLoggers(TcpTransport::class.java.simpleName, "receiver")

            val buffer = ByteArray(READ_BUFFER_SIZE)
            while (isActive) {
                if (ensureConnection()) {
                    try {
                        val bytesRead = inputStream?.read(buffer) ?: 0
                        if ((bytesRead) > 0) {
                            if (logger.level == LogLevel.TRACE) {
                                logReceivedBytes(log.trace, bytesRead, buffer)
                            }
                            metrics?.put(adapterID, MetricsCollector.METRICS_BYTES_RECEIVED, bytesRead.toDouble(), MetricUnits.BYTES, metricDimensions)
                        }

                        for (i in 0 until (bytesRead)) {
                            receiveChannel.send(buffer[i].toUByte())
                        }

                    } catch (t: SocketTimeoutException) {
                        // Timeout reading data, nu further action required. Timeout is used to keep read loop responsive and checking isActive flag frequently.
                    } catch (e: Throwable) {
                        log.error("Error reading data from ${config.address}:${config.port}, ${e.message}")
                        flagForReconnect()
                        delay(config.waitAfterReadError.inWholeMilliseconds)
                    }
                }
            }
        }
    }

    // logs transmitted data to the logger trace
    private fun logTransmittedBytes(trace: ((String) -> Unit), bytes: UByteArray) {
        trace(
            "Sending ${bytes.size} bytes ${
                bytes.toByteArray()
                    .joinToString(prefix = "[", postfix = "]", separator = ",") { "0x%02X".format(it) }
            }")
    }

    // logs received bytes to the logger trace
    private fun logReceivedBytes(
        trace: ((String) -> Unit),
        bytesRead: Int?,
        buffer: ByteArray
    ) {
        trace(
            "Received $bytesRead bytes ${
                buffer.slice(IntRange(0, (bytesRead ?: 1) - 1)).joinToString(
                    prefix = "[",
                    postfix = "]",
                    separator = ","
                ) { "0x%02X".format(it) }
            }"
        )
    }

    /**
     * Close the transport
     * @param timeout Duration? Period to wait for transport to close
     * @return Boolean Returns true if closed within timeout
     */
    override suspend fun close(timeout: Duration?): Boolean = coroutineScope {

        val waitForClose = timeout?.inWholeMilliseconds ?: DEFAULT_CLOSE_TIMEOUT

        receiver?.cancel()
        transmitter?.cancel()

        val closeResult: Deferred<Boolean> = async {
            select {

                launch("Close Transport") {
                    transmitter?.join()
                    receiver?.join()
                }.onJoin { true }

                launch("Timeout") {
                    delay(waitForClose)

                }.onJoin { false }
            }
        }

        closeResult.await()
    }

    companion object {

        const val READ_BUFFER_SIZE = 128
        const val WRITE_BUFFER_SIZE = 128
        const val READ_BLOCKING_DURATION = 1000
        const val DEFAULT_CLOSE_TIMEOUT = 10000L
    }
}