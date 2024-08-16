
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

@ConfigurationClass
@TransformerOperator(["BytesToDoubleBE"])
class BytesToDoubleBE : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: List<Byte>?): Double? =

        if (target?.size == 8)
            ByteBuffer.wrap(target.toByteArray()).order((ByteOrder.BIG_ENDIAN)).getDouble()
        else
            null


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<BytesToDoubleBE>(o)
        fun create() = BytesToDoubleBE()
    }
}