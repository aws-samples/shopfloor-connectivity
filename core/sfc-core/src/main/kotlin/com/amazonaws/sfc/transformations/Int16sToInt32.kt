
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Int16sToInt32"])
class Int16sToInt32 : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: List<Short>?): Int? =

        if (target?.size == 2)
            (((target[0].toInt() and 0xFFFF) shl 16) or (target[1].toInt() and 0xFFFF))
        else
            null


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Int16sToInt32>(o)
        fun create() = Int16sToInt32()
    }
}