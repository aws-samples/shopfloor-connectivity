
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import com.amazonaws.sfc.log.Logger

/**
 * Helper class for acknowledging serials by targets to a result handler
 * @property targetResultHandler TargetResultHandler?
 * @property logger Logger Logger for output
 */
open class TargetResultHelper(targetID: String, targetResultHandler: TargetResultHandler?, logger: Logger) :
        TargetResultHelperBase(targetID, targetResultHandler, logger) {

    private val className = this::class.java.simpleName

    fun ack(targetData: TargetData) {
        ack(listOf(TargetDataSerialMessagePair(targetData.serial, targetData)))
    }

    @JvmName("ackTargetData")
    fun ack(targetDataList: Iterable<TargetData>?) {
        ack(targetDataList?.map { TargetDataSerialMessagePair(it.serial, it) })
    }


    fun ack(targetDataList: Iterable<TargetDataSerialMessagePair>?) {
        if (!hasHandler) return
        val loggers = logger.getCtxLoggers(className, "ack")
        try {
            val result = TargetResult(
                targetID = targetID,
                ackSerialList = if (returnedData.returnAckSerials) targetDataList.serials else emptyList(),
                ackMessageList = if (returnedData.returnAckMessages) targetDataList.messages else emptyList(),
                nackSerialList = emptyList(),
                nackMessageList = emptyList(),
                errorSerialList = emptyList(),
                errorMessageList = emptyList(),
            )

            targetResultHandler?.handleResult(result)
            loggers.trace("Returning ACK for target data with serial numbers ${targetDataList?.map { it.serial }}")
        } catch (e: Exception) {
            loggers.errorEx("Error returning ACK for target data with serial numbers ${targetDataList?.map { it.serial }}", e)
        }
    }


    override fun nack(targetData: TargetData) {
        nackList(listOf(TargetDataSerialMessagePair(targetData.serial, targetData)))
    }

    override fun nackIteration(targetDataList: Iterable<TargetData>?) {
        nackList(targetDataList?.map { TargetDataSerialMessagePair(it.serial, it) })
    }

    override fun nackList(targetDataList: List<TargetDataSerialMessagePair>?) {
        if (!hasHandler) return
        val loggers = logger.getCtxLoggers(className, "nack")
        try {
            val result = TargetResult(
                targetID = targetID,
                ackSerialList = emptyList(),
                ackMessageList = emptyList(),
                nackSerialList = if (returnedData.returnNackSerials) targetDataList.serials else emptyList(),
                nackMessageList = if (returnedData.returnNackMessages) targetDataList.messages else emptyList(),
                errorSerialList = emptyList(),
                errorMessageList = emptyList(),
            )

            targetResultHandler?.handleResult(result)
            loggers.trace("Returning NACK for target data with serial numbers ${targetDataList?.map { it.serial }}")
        } catch (e: Exception) {
            loggers.errorEx("Error returning NACK target data with serial numbers ${targetDataList?.map { it.serial }}", e)
        }
    }


    fun ackAndError(ackTargetDataList: Iterable<TargetDataSerialMessagePair>?, errorTargetDataList: Iterable<TargetDataSerialMessagePair>?) {
        if (!hasHandler) return

        val loggers = logger.getCtxLoggers(className, "ackAndError")
        try {

            val result = TargetResult(
                targetID = targetID,
                ackSerialList = if (returnedData.returnAckSerials) ackTargetDataList.serials else emptyList(),
                ackMessageList = if (returnedData.returnAckMessages) ackTargetDataList.messages else emptyList(),
                nackSerialList = emptyList(),
                nackMessageList = emptyList(),
                errorSerialList = if (returnedData.returnErrorSerials) errorTargetDataList.serials else emptyList(),
                errorMessageList = if (returnedData.returnErrorMessages) errorTargetDataList.messages else emptyList(),
            )

            targetResultHandler?.handleResult(result)
            loggers.trace("Returning ACK and for  target data with serial numbers ${ackTargetDataList?.map { it.serial }}, reporting ERROR for target data with serial numbers ${errorTargetDataList?.map { it.serial }}")
        } catch (e: Exception) {
            loggers.errorEx("Error returning ACK for target data with serial numbers ${ackTargetDataList?.map { it.serial }} and  error for target data with serial numbers ${errorTargetDataList?.map { it.serial }}", e)
        }
    }


}