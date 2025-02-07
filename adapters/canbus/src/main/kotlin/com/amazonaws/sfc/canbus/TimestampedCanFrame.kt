// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//
// Partial port to Kotlin port of https://github.com/jazdw/jnaCan
//
package com.amazonaws.sfc.canbus


import com.amazonaws.sfc.canbus.Utils.toInstant
import com.amazonaws.sfc.canbus.jna.TimeValue
import com.amazonaws.sfc.canbus.jna.can_frame
import java.time.Instant


class TimestampedCanFrame(jnaFrame: can_frame, val time: TimeValue) : CanFrame(jnaFrame) {

    val timestamp: Instant by lazy {
        time.toInstant
    }

    override fun toString(): String {
        return String.format("$timestamp ${super.toString()}")
    }
}