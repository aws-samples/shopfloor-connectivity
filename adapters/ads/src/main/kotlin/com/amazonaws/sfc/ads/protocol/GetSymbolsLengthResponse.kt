/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.toInt32
import com.amazonaws.sfc.ads.protocol.GetSymbolsResponse.Companion.RESPONSE_EXPECTED_STATE_FLAGS


class GetSymbolsLengthResponse(source: RequestResponse, expectedInvokeId: Int? = null) :
    ReadResponse(source, expectedInvokeId = expectedInvokeId) {

    init {
        checkResponse("Reading symbols length", expectedInvokeId, CommandId.READ, expectedDataSize = 8, RESPONSE_EXPECTED_STATE_FLAGS)
    }

    val symbolLength by lazy {
        val result = source.data?.toInt32 ?: 0
        if (result != 0) throw AdsException("${CommandId.READ} result for getting symbol returned $result")
        source.data?.toInt32(12) ?: 0
    }
}

