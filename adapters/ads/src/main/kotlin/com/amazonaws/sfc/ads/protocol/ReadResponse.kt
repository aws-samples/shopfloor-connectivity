/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */


package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.toInt32
import com.amazonaws.sfc.ads.protocol.GetSymbolsResponse.Companion.RESPONSE_EXPECTED_STATE_FLAGS

open class ReadResponse(val source: RequestResponse, expectedInvokeId: Int?, expectedDataSize: Int? = null) :
    RequestResponse(source) {
    init {
        checkResponse(
            message = "Reading symbols",
            expectedInvokeId = expectedInvokeId,
            expectedCommandId = CommandId.READ,
            expectedDataSize = expectedDataSize,
            expectedStateFlags = RESPONSE_EXPECTED_STATE_FLAGS
        )
    }

    val readResult: Int
        get() = source.data?.toInt32 ?: 0

    val readData: ByteArray
        get() = source.data?.sliceArray(8..<(data?.size ?: 8)) ?: byteArrayOf()

}

