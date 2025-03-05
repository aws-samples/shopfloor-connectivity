
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data
interface TargetDataBufferInterface<T> {
    fun add(targetData: TargetData, payload: Any, size  : Int)
    fun clear()
    fun get(index: Int): TargetDataSerialMessagePair?
    fun message(serial: String): TargetData?
    val items: List<TargetDataSerialMessagePair>
    val messages: List<TargetData>
    val payloadSize : Long
    val payloads: List<T>
    val serials: List<String>
    val size : Int

}

data class TargetDataSerialMessagePair(val serial: String, val message: TargetData?) {
    constructor(targetData: TargetData) : this(targetData.serial, targetData)
}

val Iterable<TargetDataSerialMessagePair>?.messages: List<TargetData>
    get() = this?.mapNotNull { it.message } ?: emptyList()

val Iterable<TargetDataSerialMessagePair>?.serials: List<String>
    get() = this?.map { it.serial } ?: emptyList()


class TargetDataStringBuffer(private val storeFullMessage: Boolean) : TargetDataBufferImpl<String>(storeFullMessage){
    fun add(targetData: TargetData, payload: String,) {
        super.add(targetData, payload, payload.length)
    }

    override fun add(targetData: TargetData, payload: Any, size: Int) {
        add(targetData, payload as String)
    }

    companion object{
        fun newTargetDataStringBuffer(resultHandler: TargetResultHandler?): TargetDataStringBuffer {
            val saveFullMessage = if (resultHandler == null) false else {
                resultHandler.returnedData?.returnAnyMessages
            }
            return TargetDataStringBuffer(saveFullMessage ?: false)
        }
    }
}

class TargetDataByteBuffer(private val storeFullMessage: Boolean) : TargetDataBufferImpl<ByteArray>(storeFullMessage){
    fun add(targetData: TargetData, payload: ByteArray,) {
        super.add(targetData, payload, payload.size)
    }

    override fun add(targetData: TargetData, payload: Any, size: Int) {
        add(targetData, payload as ByteArray)
    }

    companion object{
        fun newTargetDataByteBuffer(resultHandler: TargetResultHandler?): TargetDataByteBuffer {
            val saveFullMessage = if (resultHandler == null) false else {
                resultHandler.returnedData?.returnAnyMessages
            }
            return TargetDataByteBuffer(saveFullMessage ?: false)
        }
    }
}

open class TargetDataBufferImpl<T>(private val storeFullMessage: Boolean) : TargetDataBufferInterface<T>{

    private val _payload = mutableListOf<T>()
    private val _serials = if (!storeFullMessage) mutableListOf<String>() else null
    private val _messages = if (storeFullMessage) mutableListOf<TargetData>() else null

    private var _payloadSize = 0L

    @JvmName("addTargetDataImpl")
    fun add(targetData: TargetData, payload: T, size  : Int) {
        _payload.add(payload)
        _payloadSize += size
        if (storeFullMessage) {
            _messages?.add(targetData)
        } else {
            _serials?.add(targetData.serial)
        }
    }

    override fun add(targetData: TargetData, payload: Any, size: Int) {
        @Suppress("UNCHECKED_CAST")
       add(targetData, payload as T, size)
    }


    override fun get(index: Int): TargetDataSerialMessagePair? {
        if (storeFullMessage) {
            val message = _messages?.getOrNull(index)
            return if (message != null) TargetDataSerialMessagePair(message) else null
        }
        val serial = _serials?.getOrNull(index)
        return if (serial != null) TargetDataSerialMessagePair(serial, null) else null
    }

    override val payloadSize
        get() = _payloadSize

    override val size
        get() = _payload.size

    override val payloads
        @Suppress("UNCHECKED_CAST")
        get() = _payload as List<T>

    override val messages
        get() = _messages as List<TargetData>

    override val serials
        get() = _serials ?: (_messages?.map { it.serial } ?: emptyList())

    override val items: List<TargetDataSerialMessagePair>
        get() {
            return if (storeFullMessage)
                _messages?.map { TargetDataSerialMessagePair(it) } ?: emptyList()
            else
                _serials?.map { TargetDataSerialMessagePair(it, null) } ?: emptyList()
        }

    override fun message(serial: String): TargetData? = if (storeFullMessage) _messages?.find { it.serial == serial } else null

    override fun clear() {
        _payload.clear()
        _payloadSize = 0L
        _messages?.clear()
        _serials?.clear()
    }


    companion object {
        fun <T>newTargetDataBuffer(resultHandler: TargetResultHandler?): TargetDataBufferImpl<T> {
            val saveFullMessage = if (resultHandler == null) false else {
                resultHandler.returnedData?.returnAnyMessages
            }
            return TargetDataBufferImpl<T>(saveFullMessage ?: false)
        }

    }

}
