/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */


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

