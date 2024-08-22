// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

@ConfigurationClass
@TransformerOperator(["NumbersToFloatBE"])
class NumbersToFloatBE : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: List<Any>?): Float? {

        if (target?.size != 2) return null

        val t: List<Number> = target.listOfNumbers ?: return null

        val bytes = listOf(
            (t[0].toInt() shr 8).toByte(),
            (t[0].toInt() and 0xFF).toByte(),
            (t[1].toInt() shr 8).toByte(),
            (t[1].toInt() and 0xFF).toByte()
        )

        val fl = ByteBuffer.wrap(bytes.toByteArray()).order(ByteOrder.BIG_ENDIAN).getFloat()
        return if (fl.isNaN()) null else fl

    }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<NumbersToFloatBE>(o)
        fun create() = NumbersToFloatBE()
    }


}
