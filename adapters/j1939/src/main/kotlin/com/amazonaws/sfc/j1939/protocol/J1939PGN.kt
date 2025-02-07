// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.j1939.protocol

class J1939PGN(
    val name: String,
    val canId: UInt,
    // Not used for now
    // val dlc: Int,
    // val source: String
) {

    private var _signals = mutableListOf<J1939Signal>()

    val pngId = CanFrameIdentifier(canId).pgn

    val signals: List<J1939Signal>
        get() = _signals

    fun addSignal(signal: J1939Signal) {
        _signals.add(signal)
    }

}