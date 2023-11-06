
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Int16ToBytes"])
class Int16ToBytes : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Int?): List<Byte>? =

        if (target == null) null else listOf((target.toInt() shr 8).toByte(), (target.toInt() and 0xFF).toByte())

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Int16ToBytes>(o)
        fun create() = Int16ToBytes()
    }
}