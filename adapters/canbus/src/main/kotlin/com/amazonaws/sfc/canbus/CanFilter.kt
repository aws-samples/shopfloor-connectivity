// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//
// Partial port to Kotlin port of https://github.com/jazdw/jnaCan
//
package com.amazonaws.sfc.canbus

import com.amazonaws.sfc.canbus.jna.can_filter

class CanFilter (var canId: CanId, var mask: CanId) {

    constructor(id: Int, mask: Int) : this(CanId(id), CanId(mask))

    var inverted : Boolean
        get() = canId.isInverted
        set(value) { if (value) canId.setInverted() else canId.clearInverted()}


    fun toJna(): can_filter {
        return can_filter(canId.id, mask.id)
    }

    fun copyTo(filter: can_filter) {
        filter.can_id = canId.id
        filter.can_mask = mask.id
    }
}