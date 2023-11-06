
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["BytesToInt16"])
class BytesToInt16 : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: List<Byte>?): Short? =

        if (target?.size == 2)
            (((target[0].toInt() and 255) shl 8) or (target[1].toInt() and 255)).toShort()
        else
            null


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<BytesToInt16>(o)
        fun create() = BytesToInt16()
    }
}