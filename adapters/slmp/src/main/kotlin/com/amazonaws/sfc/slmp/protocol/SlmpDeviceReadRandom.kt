// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp.protocol

import com.amazonaws.sfc.slmp.config.SlmpControllerConfiguration
import com.amazonaws.sfc.slmp.protocol.SlmpDecoder.lowAndHighByte
import com.amazonaws.sfc.tcp.TcpClient

class SlmpDeviceReadRandom(
    private val items: List<SlmpDeviceItem>,

    headerBuilder: SlmpHeader.Builder = SlmpHeader.newBuilder()
) : SlmpRequest(headerBuilder) {

    private val wordItems = items.wordItems
    private val doublewordItems = items.doublewordItems

    override fun requestData(): ByteArray{

            var bytes : ByteArray = DEVICE_RANDOM_READ_COMMAND +
                    DEVICE_RANDOM_READ_SUB_COMMAND_MAX_192_ACCESS_POINTS +
                    items.wordItems.size.toByte() +
                    items.doublewordItems.size.toByte()

            wordItems.forEach { item ->
                bytes += item.accessPoint.deviceNumber.lowAndHighByte +
                        item.accessPoint.device.deviceCodeBytes
            }

            doublewordItems.forEach { item ->
                bytes += item.accessPoint.deviceNumber.lowAndHighByte +
                        item.accessPoint.device.deviceCodeBytes
            }

            return bytes
        }


    suspend fun execute(client: TcpClient, config: SlmpControllerConfiguration): List<Any> {

        try {

            if (items.isEmpty() || items.size > 192) throw SlmpException("Number of items for RandomRead must be between 1 and 192")

            val response = sendCommandAndGetResponse(client, config)

            val words = SlmpDecoder.decodeWords(response.responseData, wordItems.size)
            val w = (if (words is List<*>) words else listOf(words)).toMutableList()
            val doubleWords = SlmpDecoder.decodeDoubleWords(
                response.responseData.sliceArray(wordItems.size * 2 until response.responseData.size),
                doublewordItems.size
            )
            val dw = (if (doubleWords is List<*>) doubleWords else listOf(doubleWords)).toMutableList()

            return items.map {
                when(it.dataType){
                    SlmpDataType.WORD -> w.removeFirst()
                    SlmpDataType.DOUBLEWORD -> dw.removeFirst()
                    SlmpDataType.BIT-> ((w.removeFirst() as Short).toInt() and 0x01 == 0x01)
                    else -> throw SlmpException("${it.dataType} not yet implemented for ReadRandom")
                } as Any

            }

        } catch (e: Exception) {
            throw SlmpException("Error executing SlmpRandomReadDevice, $e")
        }
    }

    companion object {
        private val DEVICE_RANDOM_READ_COMMAND = 0x0403.toShort().lowAndHighByte
        private val DEVICE_RANDOM_READ_SUB_COMMAND_MAX_192_ACCESS_POINTS = 0x0000.toShort().lowAndHighByte


        val List<SlmpDeviceItem>.wordItems: List<SlmpDeviceItem>
            get() =
                this.filter { it: SlmpDeviceItem ->
                    (it.dataType == SlmpDataType.WORD) || (it.dataType == SlmpDataType.BIT)
                }

        val List<SlmpDeviceItem>.doublewordItems: List<SlmpDeviceItem>
            get() =
                this.filter { it: SlmpDeviceItem ->
                    (it.dataType == SlmpDataType.DOUBLEWORD)
                }

    }
}

