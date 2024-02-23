@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)

package com.amazonaws.sfc.tcp

import com.amazonaws.sfc.config.TcpConfiguration
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
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


open class TcpClient(private val config: TcpConfiguration, readBufferSize : Int = READ_BUFFER_SIZE, writeBufferSize : Int = WRITE_BUFFER_SIZE, private val logger: Logger) {

    private val className = this::class.java.simpleName

    // channels to pass bytes to be sent and bytes read
    private val transmitChannel: Channel<ByteArray> = Channel(writeBufferSize)
    private val receiveChannel: Channel<Byte> = Channel(readBufferSize)

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
    suspend fun write(bytes: ByteArray) = transmitChannel.send(bytes)

    /**
     * Reads bytes from transport
     * @return UByte
     */
    suspend fun read(): Byte = receiveChannel.receive()

    /**
     * Get exclusive access to transport
     */
    suspend fun lock() = transportLock.lock()

    /**
     * Release exclusive access to transport
     */
    fun unlock() = transportLock.unlock()

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
        val waitPeriod: Long =
            ((lastConnectAttempt + config.waitAfterConnectError.inWholeMilliseconds) - System.currentTimeMillis())
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

        val logs = logger.getCtxLoggers(className, "connect")

        clientConnectMutex.withLock {

            // only one concurrent attempt to connect

            if (isConnected) {
                // some other process has connected whilst waiting for the mutex
                return true
            }

            coroutineScope {
                val connectJob = launch( context =Dispatchers.IO, name = "Connect") {
                    try {

                        // receives message when connected
                        val connectedChannel = Channel<Any?>()

                        launch(context = Dispatchers.IO, name = "Setup Connection") {
                            try {
                                setupConnection(logs.trace, connectedChannel)
                            }catch(e : Exception){
                                logs.error("Error setting up connection, ${e.message}")
                            }
                        }

                        launch(context = Dispatchers.IO, name = "Timeout") {
                            try {
                                // signals when connecting takes too long (calling Socket() withing withTimeout is not reliable as it runs on IO dispatcher)
                                delay(config.connectTimeout.inWholeMilliseconds)
                                connectedChannel.send(null)
                            }catch (e : Exception){
                                logs.error("Error setting up connection timeout, ${e.message}")
                            }
                        }

                        when (val result = connectedChannel.receive()) {
                            is Socket -> {
                                logs.info("Connected to ${config.address}:${config.port}")
                                // Rest so we can reconnect without delay after a successful connection
                                lastConnectAttempt = 0L
                            }

                            is Throwable -> error("Error connecting to ${config.address}:${config.port}, ${result.message}")
                            else -> error("Timeout connecting to ${config.address}:${config.port}")
                        }
                        coroutineContext.cancelChildren()
                    } catch (e : Exception){
                        logs.error("Error connecting to ${config.address}:${config.port}, ${e.message}")
                    }
                }
                connectJob.join()
            }
        }

        return isConnected
    }


    // setup socket connection
    private suspend fun setupConnection(trace: ((String) -> Unit), connectedChannel: SendChannel<Any?>) {

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

        receiver = scope.launch(context = Dispatchers.IO, name = "TCP Receiver") {
            try {
                receiveData()
            }catch(e : Exception){
                logger.getCtxErrorLog(className,"receiver")("Error receiving data, ${e.message}")
            }
        }

        transmitter = scope.launch(context = Dispatchers.IO, name = "TCP Transmitter") {
            try {
                transmitData()
            }catch(e : Exception){
                logger.getCtxErrorLog(className, "transmitter")("Error transmitting data, ${e.message}")
            }
        }
    }

    // writes the data to the socket
    private suspend fun transmitData() {

        val log = logger.getCtxLoggers(className, "transmitData")

        withContext(Dispatchers.IO) {
            while (isActive) {
                if (isConnected) {
                    val bytes = transmitChannel.receive()
                    if (logger.level == LogLevel.TRACE) {
                        logTransmittedBytes(log.trace, bytes)
                    }
                    try {
                        outputStream?.write(bytes)
                        outputStream?.flush()

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

        val log = logger.getCtxLoggers(className, "receiveData")

        withContext(Dispatchers.IO) {

            val buffer = ByteArray(READ_BUFFER_SIZE)
            while (isActive) {
                if (ensureConnection()) {
                    try {
                        val bytesRead = inputStream?.read(buffer) ?: 0
                        if ((bytesRead) > 0) {
                            if (logger.level == LogLevel.TRACE) {
                                logReceivedBytes(log.trace, bytesRead, buffer)
                            }

                        }

                        for (i in 0 until (bytesRead)) {
                            receiveChannel.send(buffer[i])
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
    private fun logTransmittedBytes(trace: ((String) -> Unit), bytes: ByteArray) {
        trace(
            "Sending ${bytes.size} bytes ${
                bytes.joinToString(prefix = "[", postfix = "]", separator = ",") { "0x%02X".format(it) }
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
    suspend fun close(timeout: Duration?): Boolean = coroutineScope {

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

        const val READ_BUFFER_SIZE = 1024
        const val WRITE_BUFFER_SIZE = 1024
        const val READ_BLOCKING_DURATION = 1000
        const val DEFAULT_CLOSE_TIMEOUT = 10000L
    }
}