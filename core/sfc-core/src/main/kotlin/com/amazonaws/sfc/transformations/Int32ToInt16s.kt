
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Int32ToInt16s"])
class Int32ToInt16s : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number?): List<Short>? =

        if (target == null) null else listOf((target.toInt() shr 16).toShort(), (target.toInt() and 0xFFFF).toShort())

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Int32ToInt16s>(o)
        fun create() = Int32ToInt16s()
    }
}