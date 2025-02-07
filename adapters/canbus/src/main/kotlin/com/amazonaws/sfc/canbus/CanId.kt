// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//
// Partial port to Kotlin port of https://github.com/jazdw/jnaCan
//

package com.amazonaws.sfc.canbus


import com.amazonaws.sfc.canbus.jna.CLibrary.*
import com.amazonaws.sfc.util.asHexString

class CanId(id: Int) {
    var id: Int

    init {
        this.id = id
    }

    var sFFid: Int
        get() = id and CAN_SFF_MASK
        set(id) {
            val newId: Int = this.id and CAN_SFF_MASK.inv()
            this.id = newId or (id and CAN_SFF_MASK)
        }

    var eFFid: Int
        get() = id and CAN_EFF_MASK
        set(id) {
            val newId: Int = this.id and CAN_EFF_MASK.inv()
            this.id = newId or (id and CAN_EFF_MASK)
        }


    fun setEFF() {
        id = id or CAN_EFF_FLAG
    }

    fun clearEFF() {
        id = id and CAN_EFF_FLAG.inv()
    }

    val isEFF: Boolean
        get() = (id and CAN_EFF_FLAG) != 0

    fun setRTR() {
        id = id or CAN_RTR_FLAG
    }

    fun clearRTR() {
        id = id and CAN_RTR_FLAG.inv()
    }

    val isRTR: Boolean
        get() = (id and CAN_RTR_FLAG) != 0


    fun setERR() {
        id = id or CAN_ERR_FLAG
    }


    fun clearERR() {
        id = id and CAN_ERR_FLAG.inv()
    }

    val isERR: Boolean
        get() = (id and CAN_ERR_FLAG) != 0


    fun setInverted() {
        id = id or CAN_INV_FILTER
    }

    fun clearInverted() {
        id = id and CAN_INV_FILTER.inv()
    }

    val isInverted: Boolean
        get() = (id and CAN_INV_FILTER) != 0

    override fun toString(): String =
        "${
            if (this.isEFF)
                eFFid.asHexString().padStart(8, '0')
            else sFFid.asHexString().padStart(3, '0')
        } ${if (isRTR) " RTR" else ""}"

}
