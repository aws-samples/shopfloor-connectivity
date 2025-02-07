// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//
// Partial port to Kotlin port of https://github.com/jazdw/jnaCan
//
package com.amazonaws.sfc.canbus

import com.amazonaws.sfc.canbus.jna.can_frame
import com.amazonaws.sfc.util.asHexString

open class CanFrame() : CanMessage<can_frame> {

    lateinit var canId: CanId
    lateinit var data: ByteArray

    constructor(jnaFrame: can_frame) : this(){
        val size = jnaFrame.can_dlc.toInt()
        canId = CanId(jnaFrame.can_id)
        data = ByteArray(size){i->jnaFrame.data[i]}
    }

    constructor(canId: Int, vararg data: Byte) : this(){
        this.canId = CanId(canId)
        this.data = data
    }

    constructor(id: CanId, vararg data: Byte) : this(){
        this.data = data
        this.canId = id
    }

    constructor(canId: CanId, vararg data: Int) : this() {
        this.canId = canId
        this.data =data.map { it.toByte() }.toByteArray()
    }


    override fun toJna(): can_frame {
        val jnaData = ByteArray(data.size)
        System.arraycopy(this.data, 0, jnaData, 0, this.data.size )
        return can_frame(canId.id, data.size.toByte() , jnaData)
    }


    override fun toString(): String {
        return "$canId ${data.asHexString()}"
    }
}