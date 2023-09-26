/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.data

import com.amazonaws.sfc.log.Logger

open class TargetResultHelperBase(protected val targetID: String,
                                  protected val targetResultHandler: TargetResultHandler?,
                                  protected val logger: Logger) {

    private val className = this::class.java.simpleName

    protected val hasHandler = (targetResultHandler != null)
    protected val returnedData = targetResultHandler?.returnedData ?: ResulHandlerReturnedData.returnNoData


    /**
     * Reports a single serial as succeeded to the result handler
     * @param targetData String
     */
    fun success(targetData: TargetData) {
        successList(listOf(TargetDataSerialMessagePair(targetData.serial, targetData)))
    }

    @JvmName("successTargetData")
    fun successIteration(targetDataList: Iterable<TargetData>?) {
        successList(targetDataList?.map { TargetDataSerialMessagePair(it.serial, it) })
    }

    /**
     * Reports a set of serials as failed to the result handler
     * @param targetDataList String Serials to reports
     */
    fun successList(targetDataList: List<TargetDataSerialMessagePair>?) {
        returnResults(ackTargetDataList = targetDataList)
    }


    /**
     * Reports a single serial as failed to the result handler
     * @param targetData String
     */
    fun error(targetData: TargetData) {
        errorList(listOf(TargetDataSerialMessagePair(targetData.serial, targetData)))
    }


    fun errorIteration(targetDataList: Iterable<TargetData>?) {
        errorList(targetDataList?.map { TargetDataSerialMessagePair(it.serial, it) })
    }

    /**
     * Reports a set of serials as failed to the result handler
     * @param targetDataList String Serials to reports
     */


    fun errorList(targetDataList: Iterable<TargetDataSerialMessagePair>?) {
        returnResults(errorTargetDataList = targetDataList)
    }


    /**
     * Reports a single serial as succeeded to the result handler
     * @param targetData String
     */
    open fun nack(targetData: TargetData) {
        nackList(listOf(TargetDataSerialMessagePair(targetData.serial, targetData)))
    }

    open fun nackIteration(targetDataList: Iterable<TargetData>?) {
        nackList(targetDataList?.map { TargetDataSerialMessagePair(it.serial, it) })
    }

    /**
     * Reports a set of serials as failed to the result handler
     * @param targetDataList String Serials to reports
     */
    open fun nackList(targetDataList: List<TargetDataSerialMessagePair>?) {
        returnResults(nackTargetDataList = targetDataList)
    }


    fun returnResults(ackTargetDataList: Iterable<TargetDataSerialMessagePair>? = null,
                      errorTargetDataList: Iterable<TargetDataSerialMessagePair>? = null,
                      nackTargetDataList: Iterable<TargetDataSerialMessagePair>? = null) {
        if (!hasHandler) return

        val allSerials = listOf(
            ackTargetDataList?.map { it.serial } ?: emptyList(),
            nackTargetDataList?.map { it.serial } ?: emptyList(),
            errorTargetDataList?.map { it.serial } ?: emptyList()).flatten().toSet().toList()

        val loggers = logger.getCtxLoggers(className, "error")
        try {
            val result = TargetResult(
                targetID = targetID,
                ackSerialList = if (returnedData.returnAckSerials) ackTargetDataList.serials.toList() else emptyList(),
                ackMessageList = if (returnedData.returnAckMessages) ackTargetDataList.messages.toList() else emptyList(),
                nackSerialList = if (returnedData.returnNackSerials) nackTargetDataList.serials.toList() else emptyList(),
                nackMessageList = if (returnedData.returnNackMessages) nackTargetDataList.messages.toList() else emptyList(),
                errorSerialList = if (returnedData.returnErrorSerials) errorTargetDataList.serials.toList() else emptyList(),
                errorMessageList = if (returnedData.returnErrorMessages) errorTargetDataList.messages.toList() else emptyList(),
            )

            targetResultHandler?.handleResult(result)
            loggers.trace("Reporting results for data with  serial numbers $allSerials")
        } catch (e: Throwable) {
            loggers.error("Error reporting results for data with serial numbers $allSerials , $e")
        }
    }
}