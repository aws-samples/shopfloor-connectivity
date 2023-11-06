
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0



package com.amazonaws.sfc.data

/**
 * Data to be returned to result event handle
 * @property ack ResultHandlerData
 * @property nack ResultHandlerData
 * @property error ResultHandlerData
 * @constructor
 */
class ResulHandlerReturnedData(
    val ack: ResultHandlerData,
    val nack: ResultHandlerData,
    val error: ResultHandlerData,
) {

    enum class ResultHandlerData {
        NONE,
        SERIALS,
        MESSAGES
    }

    val returnsAnyData = (ack != ResultHandlerData.NONE) || (nack != ResultHandlerData.NONE) || (error != ResultHandlerData.NONE)

    val returnAckSerials = (ack == ResultHandlerData.SERIALS)
    val returnAckMessages = (ack == ResultHandlerData.MESSAGES)

    val returnNackSerials = (nack == ResultHandlerData.SERIALS)
    val returnNackMessages = (nack == ResultHandlerData.MESSAGES)

    val returnErrorSerials = (error == ResultHandlerData.SERIALS)
    val returnErrorMessages = (error == ResultHandlerData.MESSAGES)

    val returnAnySerials = (returnAckSerials || returnNackSerials || returnErrorSerials)
    val returnAnyMessages = (returnAckMessages || returnNackMessages || returnErrorMessages)

    companion object {
        val returnNoData = ResulHandlerReturnedData(ResultHandlerData.NONE, nack = ResultHandlerData.NONE, error = ResultHandlerData.NONE)
    }
}

