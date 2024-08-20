// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.tcp.transport

import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricDimensions
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.modbus.protocol.ModbusTransport
import com.amazonaws.sfc.modbus.tcp.config.ModbusTcpDeviceConfiguration
import com.amazonaws.sfc.tcp.LockableTcpClient
import com.amazonaws.sfc.tcp.TcpClient
import com.amazonaws.sfc.util.MemoryMonitor.Companion.getUsedMemoryMB
import com.amazonaws.sfc.util.buildScope
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


/**
 * TCP data-transport to send/receive data over TCP
 */
class TcpTransport(
    private val config: ModbusTcpDeviceConfiguration,
    private val adapterID: String,
    private val metricDimensions: MetricDimensions,
    private val metrics: MetricsCollector?,
    val logger: Logger
) : ModbusTransport {

    private val scope = buildScope("TcpTransport", Dispatchers.IO)

    private var _tcpClient : LockableTcpClient? = null
    private val tcpClient : TcpClient?
        get(){
            if (_tcpClient == null){
                _tcpClient = LockableTcpClient(config, logger)
                _tcpClient?.start()
                runBlocking {
                    createConnectionMetrics()
                }

            }
            return _tcpClient
        }

    private var bytesWritten = 0
    private var bytesRead = 0
    private var lastMem = 0L

    var metricsJob = if (metrics != null ) {
        scope.launch {
            while (scope.isActive) {
                if (bytesWritten==0 && bytesRead == 0) {
                    delay(1.toDuration(DurationUnit.SECONDS))
                } else {
                    writeMetrics(bytesRead, bytesWritten)
                    bytesWritten = 0
                    bytesRead = 0
                }
            }
        }
    } else null

    private suspend fun writeMetrics(read: Int, written : Int) {
        val mem = getUsedMemoryMB()
        if (mem!= lastMem) {
            lastMem = mem
            metrics?.put(adapterID, MetricsCollector.METRICS_MEMORY, getUsedMemoryMB().toDouble(), MetricUnits.MEGABYTES, metricDimensions)
        }
        if (written!= 0) metrics?.put(adapterID, MetricsCollector.METRICS_BYTES_SEND, written.toDouble(), MetricUnits.BYTES, metricDimensions)
        if (read != 0) metrics?.put(adapterID, MetricsCollector.METRICS_BYTES_RECEIVED, read.toDouble(), MetricUnits.BYTES, metricDimensions)
    }


    private suspend fun createConnectionMetrics() {

        metrics?.put(
            adapterID,
            if (tcpClient?.isConnected == true)
                MetricsCollector.METRICS_CONNECTIONS
            else
                MetricsCollector.METRICS_CONNECTION_ERRORS,
            1.0,
            MetricUnits.COUNT,
            metricDimensions
        )
    }

    /**
     * Writes bytes to transport
     * @param bytes UByteArray
     */
    override suspend fun write(bytes: UByteArray) {
        tcpClient?.write(bytes.toByteArray())
        bytesWritten += bytes.size
    }

    /**
     * Reads bytes from transport
     * @return UByte
     */
    override suspend fun read(): UByte? {
        val b =  tcpClient?.read()?.toUByte()
        bytesRead += 1
        return b
    }

    /**
     * Get exclusive access to transport
     */
    override suspend fun lock() {
        tcpClient?.lock()
    }

    /**
     * Release exclusive access to transport
     */
    override suspend fun unlock() {
        tcpClient?.unlock()
    }


    /**
     * Close the transport
     * @param timeout Duration? Period to wait for transport to close
     * @return Boolean Returns true if closed within timeout
     */
    override suspend fun close(timeout: Duration?): Boolean = coroutineScope {
        scope.cancel()
        metricsJob?.join()
        _tcpClient?.close(timeout)?:true
    }


}