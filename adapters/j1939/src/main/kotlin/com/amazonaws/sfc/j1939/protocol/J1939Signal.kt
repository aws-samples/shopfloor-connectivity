// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.j1939.protocol


class J1939Signal(
    val name: String,
    val startBit: Int,
    val length: Int,
    val byteOrder: ByteOrder,
    val valueType: ValueType,
    val factor: Double,
    val offset: Double,
    val minimum: Double,
    val maximum: Double,
    // Not used for now
    //  val unit: String,
    //  val receivers: List<String>
)