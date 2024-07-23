// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp.protocol


import com.amazonaws.sfc.slmp.config.SlmpControllerConfiguration
import com.amazonaws.sfc.slmp.protocol.SlmpDecoder.lowAndHighByte
import com.amazonaws.sfc.slmp.protocol.SlmpDevice.Companion.subCommand
import com.amazonaws.sfc.tcp.TcpClient

class SlmpDeviceRead(
    private val item: SlmpDeviceItem,
    headerBuilder: SlmpHeader.Builder = SlmpHeader.newBuilder()
) : SlmpRequest(headerBuilder) {


    override fun requestData() = DEVICE_READ_COMMAND +
            item.accessPoint.device.subCommand.lowAndHighByte +
            item.accessPoint.deviceNumber.lowAndHighByte +
            item.accessPoint.device.deviceCodeBytes +
            item.devicesToRead.lowAndHighByte

    suspend fun execute(client: TcpClient, config: SlmpControllerConfiguration): Any {

        try {
            val slmpResponse = sendCommandAndGetResponse(client, config)
            return SlmpDecoder.decode(item, slmpResponse.responseData)
        } catch (e: Exception) {
            throw SlmpException("Error executing SlmpReadDevice, $e")
        }
    }

    companion object {

        fun build( accessPointStr : String, itemString: String): SlmpDeviceRead =
            SlmpDeviceRead(
                item = SlmpDeviceItem.build(accessPointStr, itemString)
            )



        private val DEVICE_READ_COMMAND = 0x0401.toShort().lowAndHighByte
    }

}

