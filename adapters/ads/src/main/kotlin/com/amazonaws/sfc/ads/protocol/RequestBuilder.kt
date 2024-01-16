/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

class RequestBuilder(val config : ClientConfig) {

    fun getSymbolsLengthRequest() = GetSymbolsLengthRequest(
        targetAmsNetId = config.targetAmsNetId,
        targetAmsPort = config.targetAmsPort,
        sourceAmsNetId = config.sourceAmsNetId,
        sourceAmsPort = config.sourceAmsPort
    )

    fun getSymbolsRequest(length : Int) = GetSymbolsRequest(
        targetAmsNetId = config.targetAmsNetId,
        targetAmsPort = config.targetAmsPort,
        sourceAmsNetId = config.sourceAmsNetId,
        sourceAmsPort = config.sourceAmsPort,
        length = length
    )

    fun multiReadRequest(symbols: List<Symbol>) = MultiReadRequest(
        targetAmsNetId = config.targetAmsNetId,
        targetAmsPort = config.targetAmsPort,
        sourceAmsNetId = config.sourceAmsNetId,
        sourceAmsPort = config.sourceAmsPort,
        symbols = symbols
    )

}


