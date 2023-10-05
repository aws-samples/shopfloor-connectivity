/*

Copyright (c) 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved. 
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.data

import com.amazonaws.sfc.log.Logger

/**
 * Helper class for acknowledging serials by targets to a result handler
 * @property targetResultHandler TargetResultHandler?
 * @property logger Logger Logger for output
 */
class TargetResultBufferedHelper(targetID: String, targetResultHandler: TargetResultHandler?, logger: Logger) :
        TargetResultHelperBase(targetID, targetResultHandler, logger) {

    private val className = this::class.java.simpleName

    private val messageBuffer = if (returnedData.returnAnyMessages) mutableListOf<TargetData>() else null
    private val _serialBuffer = if ((messageBuffer == null) && returnedData.returnAnySerials) mutableListOf<String>() else null

    private val serialBuffer: List<String>
        get() = _serialBuffer ?: messageBuffer?.map { it.serial } ?: emptyList()

    fun add(targetData: TargetData) {
        if (!hasHandler) return
        _serialBuffer?.add(targetData.serial)
        messageBuffer?.add(targetData)
    }

    /**
     * Clears the buffer
     */
    fun clear() {
        _serialBuffer?.clear()
        messageBuffer?.clear()
    }

    /**
     *  Acknowledges all buffered serials to the result handler
     */
    fun ackBuffered() {
        if (!hasHandler) return
        val loggers = logger.getCtxLoggers(className, "ackBuffered")
        try {
            val result = TargetResult(
                targetID = targetID,
                ackSerialList = if (returnedData.returnAckSerials) serialBuffer.toList() else emptyList(),
                ackMessageList = if (returnedData.returnAckMessages) messageBuffer?.toList() else emptyList(),
                nackSerialList = emptyList(),
                nackMessageList = emptyList(),
                errorSerialList = emptyList(),
                errorMessageList = emptyList()
            )
            targetResultHandler?.handleResult(result)
            loggers.trace("Returning ACK for target data with serial numbers ${_serialBuffer ?: messageBuffer?.map { it.serial }}")
        } catch (e: Throwable) {
            loggers.error("Error returning ACK for target data with serial numbers serials ${_serialBuffer ?: messageBuffer?.map { it.serial }}, $e")
        } finally {
            clear()
        }
    }

    /**
     * NACK all buffered serials to the handler
     */
    fun nackBuffered() {
        if (!hasHandler) return
        val loggers = logger.getCtxLoggers(className, "nackBuffered")
        try {
            val result = TargetResult(
                targetID = targetID,
                ackSerialList = emptyList(),
                ackMessageList = emptyList(),
                nackSerialList = if (returnedData.returnNackSerials) serialBuffer.toList() else emptyList(),
                nackMessageList = if (returnedData.returnNackMessages) messageBuffer?.toList() else emptyList(),
                errorSerialList = emptyList(),
                errorMessageList = emptyList()
            )
            targetResultHandler?.handleResult(result)
            loggers.trace("Returning NACK for target data with serial numbers ${_serialBuffer ?: messageBuffer?.map { it.serial }} ")
        } catch (e: Throwable) {
            loggers.error("Error returning ERROR for data with serial numbers ${_serialBuffer ?: messageBuffer?.map { it.serial }}, $e")
        } finally {
            clear()
        }
    }

    /**
     * Reports all buffered serials as failed to the result handler
     */
    fun errorBuffered() {
        if (!hasHandler) return
        val loggers = logger.getCtxLoggers(className, "errorBuffered")
        try {
            val result = TargetResult(
                targetID = targetID,
                ackSerialList = emptyList(),
                ackMessageList = emptyList(),
                nackSerialList = emptyList(),
                nackMessageList = emptyList(),
                errorSerialList = if (returnedData.returnErrorSerials) serialBuffer.toList() else emptyList(),
                errorMessageList = if (returnedData.returnErrorMessages) messageBuffer?.toList() else emptyList(),
            )
            targetResultHandler?.handleResult(result)
            loggers.trace("Reporting error processing data for serial numbers $serialBuffer")
        } catch (e: Throwable) {
            loggers.error("Error returning ERROR for target data with serial numbers $serialBuffer, $e")
        } finally {
            clear()
        }
    }


}