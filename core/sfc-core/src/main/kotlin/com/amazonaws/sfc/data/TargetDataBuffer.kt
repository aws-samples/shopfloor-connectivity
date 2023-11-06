
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

data class TargetDataSerialMessagePair(val serial: String, val message: TargetData?) {
    constructor(targetData: TargetData) : this(targetData.serial, targetData)
}

val Iterable<TargetDataSerialMessagePair>?.messages: List<TargetData>
    get() = this?.mapNotNull { it.message } ?: emptyList()

val Iterable<TargetDataSerialMessagePair>?.serials: List<String>
    get() = this?.map { it.serial } ?: emptyList()


class TargetDataBuffer(private val storeFullMessage: Boolean) {

    private val _payload = mutableListOf<String>()
    private val _serials = if (!storeFullMessage) mutableListOf<String>() else null
    private val _messages = if (storeFullMessage) mutableListOf<TargetData>() else null

    private var _payloadSize = 0L

    fun add(targetData: TargetData, payload: String) {
        _payload.add(payload)
        _payloadSize += payload.length
        if (storeFullMessage) {
            _messages?.add(targetData)
        } else {
            _serials?.add(targetData.serial)
        }
    }

    fun get(index: Int): TargetDataSerialMessagePair? {
        if (storeFullMessage) {
            val message = _messages?.getOrNull(index)
            return if (message != null) TargetDataSerialMessagePair(message) else null
        }
        val serial = _serials?.getOrNull(index)
        return if (serial != null) TargetDataSerialMessagePair(serial, null) else null
    }

    val payloadSize
        get() = _payloadSize

    val size
        get() = _payload.size

    val payloads
        get() = _payload as List<String>

    val messages
        get() = _messages as List<TargetData>

    val serials
        get() = _serials ?: (_messages?.map { it.serial } ?: emptyList())

    val items: List<TargetDataSerialMessagePair>
        get() {
            return if (storeFullMessage)
                _messages?.map { TargetDataSerialMessagePair(it) } ?: emptyList()
            else
                _serials?.map { TargetDataSerialMessagePair(it, null) } ?: emptyList()
        }

    fun message(serial: String): TargetData? = if (storeFullMessage) _messages?.find { it.serial == serial } else null

    fun clear() {
        _payload.clear()
        _payloadSize = 0L
        _messages?.clear()
        _serials?.clear()
    }

    companion object {
        fun newTargetDataBuffer(resultHandler: TargetResultHandler?): TargetDataBuffer {
            val saveFullMessage = if (resultHandler == null) false else {
                resultHandler.returnedData?.returnAnyMessages
            }
            return TargetDataBuffer(saveFullMessage ?: false)
        }

    }


}