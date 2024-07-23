/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 */

package com.amazonaws.sfc.slmp.protocol

import com.amazonaws.sfc.slmp.protocol.SlmpDecoder.lowAndHighByte


class SlmpHeader(
    val header: ByteArray = byteArrayOf(0x00, 0x00),
    val networkNumber: Byte = DEFAULT_NETWORK_NUMBER,
    val stationNumber: Byte = DEFAULT_STATION_NUMBER,
    val moduleNumber: Short = DEFAULT_MODULE_NUMBER,
    val multiDropStationNumber: Byte = DEFAULT_MULTI_DROP_STATION_NUMBER,
    val monitoringTimer: Short = DEFAULT_MONITORING_TIMER,
    val dataLength: Short
) {

    val bytes: ByteArray =
        header +
                networkNumber +
                stationNumber +
                moduleNumber.lowAndHighByte +
                multiDropStationNumber +
                dataLength.lowAndHighByte +
                monitoringTimer.lowAndHighByte

    class Builder(
        private var header: ByteArray = COMMAND_MESSAGE,
        private var networkNumber: Byte = DEFAULT_NETWORK_NUMBER,
        private var stationNumber: Byte = DEFAULT_STATION_NUMBER,
        private var moduleNumber: Short = DEFAULT_MODULE_NUMBER,
        private var multiDropStationNumber: Byte = DEFAULT_MULTI_DROP_STATION_NUMBER,
        private var monitoringTimer: Short = DEFAULT_MONITORING_TIMER,
        private var dataLength: Short = 0
    ) {
        fun header(header: ByteArray) = apply { this.header = header }
        fun networkNumber(networkNumber: Byte) = apply { this.networkNumber = networkNumber }
        fun stationNumber(stationNumber: Byte) = apply { this.stationNumber = stationNumber }
        fun moduleNumber(moduleNumber: Short) = apply { this.moduleNumber = moduleNumber }
        fun multiDropStationNumber(multiDropStationNumber: Byte) = apply { this.multiDropStationNumber = multiDropStationNumber }
        fun monitoringTimer(monitoringTimer: Short) = apply { this.monitoringTimer = monitoringTimer }
        fun dataLength(dataLength: Short) = apply { this.dataLength = dataLength }
        fun build() = SlmpHeader(header, networkNumber, stationNumber, moduleNumber, multiDropStationNumber, monitoringTimer, dataLength)
    }

    companion object {
        val COMMAND_MESSAGE = byteArrayOf(0x50, 0x00)
        val COMMAND_RESPONSE_NO_SERIAL = byteArrayOf(0xD0.toByte(), 0x00)
        val COMMAND_RESPONSE_WITH_SERIAL = byteArrayOf(0xD4.toByte(), 0x00)

        const val DEFAULT_NETWORK_NUMBER: Byte = 0x00.toByte()
        const val DEFAULT_STATION_NUMBER: Byte = 0xFF.toByte()
        const val DEFAULT_MODULE_NUMBER: Short = 0x03FF.toShort()
        const val DEFAULT_MULTI_DROP_STATION_NUMBER = 0x00.toByte()
        const val DEFAULT_MONITORING_TIMER: Short = 0.toShort()

        fun newBuilder() = Builder()
    }
}