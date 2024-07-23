// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp.protocol

import com.amazonaws.sfc.slmp.config.SlmpControllerConfiguration
import com.amazonaws.sfc.tcp.TcpClient

abstract class SlmpRequest(private val headerBuilder: SlmpHeader.Builder) {

    protected abstract fun requestData(): ByteArray

    private fun requestHeader() : ByteArray = headerBuilder.dataLength((requestData().size + 2).toShort()).build().bytes

    protected suspend fun sendCommandAndGetResponse(client: TcpClient, config: SlmpControllerConfiguration): SlmpResponse {

        client.write(requestHeader() + requestData())

        val slmpResponse = SlmpResponse.read(client, config)

        if (slmpResponse.endCode != SlmpError.NO_ERROR) {
            throw SlmpException("${SlmpError.errorString(slmpResponse.endCode)}, (${slmpResponse.errorInfo})")
        }
        return slmpResponse
    }
}