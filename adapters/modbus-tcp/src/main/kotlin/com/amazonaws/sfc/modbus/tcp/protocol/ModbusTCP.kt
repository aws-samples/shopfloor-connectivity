
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.tcp.protocol


import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.modbus.protocol.*
import com.amazonaws.sfc.modbus.protocol.Modbus.asHex
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlin.time.Duration

/**
 * Handler for Modbus TCP protocol
 */
class ModbusTCP(
    override val modbusDevice: ModbusTransport,
    private val readTimeout: Duration,
    private val logger: Logger) : ModbusHandler {

    private val className = this::class.java.simpleName

    private val scope = buildScope("Modbus TCP Protocol Handler")

    // request to transmit by the handler
    override val requests: Channel<Request> = Channel(1024)

    // responses received by the handler
    override val responses: Channel<Response> = Channel(1024)

    // coroutine for sending te requests to the device
    private val handleRequestsJob = scope.launch("Request Handler") {
        handleRequests()
    }

    // coroutine for receiving the responses from the device
    private val handleResponsesJob = scope.launch("Response Handler") {
        handleResponses()
    }

    // Encodes and sens requests to the device
    private suspend fun CoroutineScope.handleRequests() {
        val trace = logger.getCtxTraceLog(ModbusTCP::class.java.simpleName, "handleRequestsJob")
        trace("Modbus TCP request handler started")

        try {
            while (isActive) {
                val request = requests.receive()
                trace("Sending request $request")

                val bytes = MBAPHeader.create(request).payload + request.payload
                modbusDevice.write(bytes)
            }

        } catch (e: Exception) {
            if (e::class.java.simpleName != "JobCancellationException") {
                println(e)
            }
        } finally {
            trace("Modbus TCP request handler stopped")
        }
    }

    // reads and decodes responses from the device
    private suspend fun CoroutineScope.handleResponses() {
        val log = logger.getCtxLoggers(ModbusTCP::class.java.simpleName, "handleResponsesJob")

        var state = ReadResponseState.ReadMBAPHeaderState
        var header: MBAPHeader? = null


        while (isActive) {
            try {
                val buf = ResponseBase.readResponseBytes(modbusDevice, 1, Modbus.READ_TIMEOUT)
                if (buf == null) {
                    state = ReadResponseState.ReadMBAPHeaderState
                    continue
                }

                val b = buf[0]
                state = when (state) {

                    ReadResponseState.ReadMBAPHeaderState -> {
                        header = MBAPHeader.read(device = modbusDevice, readTimeout = readTimeout, transactionIDHigh = b)
                        ReadResponseState.ReadFunctionResponseState
                    }

                    ReadResponseState.ReadFunctionResponseState -> {
                        val resp = Modbus.readResponseForFunctionCode(
                            functionCode = b and 0x7Fu,
                            deviceID = header?.unitID ?: 0u,
                            transactionID = header?.transactionID
                        )

                        if (resp == null) {
                            log.error("${asHex(b)} is not a recognized function/error code")
                        } else {

                            if (!Modbus.isErrorCode(b)) {
                                resp.readResponse(modbusDevice)
                            } else {
                                resp.readError(modbusDevice, b)
                            }
                            log.trace("Received response $resp")
                            responses.send(resp)
                        }
                        ReadResponseState.ReadMBAPHeaderState
                    }
                }
            } catch (ex: Modbus.ModbusException) {
                log.errorEx("${ex.message} whilst reading ${if (state == ReadResponseState.ReadMBAPHeaderState) "header" else "response or error"}", ex)

            }
        }
    }

    // reads the unit ID
    private suspend fun readUnitID(readTimeout: Duration): DeviceID {

        val unitID = ResponseBase.readResponseBytes(modbusDevice, timeout = readTimeout, n = 1)
                     ?: throw Modbus.ModbusException("timeout reading MBAP unit ID")
        return unitID[0]
    }


    /**
     * Stops the handler
     * @param timeout Duration Timeout to wait for the handler to stop
     * @return Boolean Returns true if handler was stopped within timeout period
     */
    override suspend fun stop(timeout: Duration): Boolean = coroutineScope {
        handleRequestsJob.cancel()
        handleResponsesJob.cancel()

        val logTrace = logger.getCtxTraceLog(className, "stop")

        select {
            launch("Stop Handlers") {
                joinAll(handleResponsesJob, handleRequestsJob)
            }.onJoin {
                logTrace("Modbus TCP protocol handler stopped")
                true
            }
            launch {
                delay(1000)
            }.onJoin {
                logTrace("Modbus TCP protocol handler not stopped")
                false
            }
        }
    }

    companion object {

        enum class ReadResponseState {
            ReadMBAPHeaderState,
            ReadFunctionResponseState
        }
    }

}


