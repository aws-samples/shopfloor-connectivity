
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Log2"])
class Log2 : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number?): Number? {

        val result = when (target) {
            null -> null
            is Float -> kotlin.math.log2(target)
            else -> kotlin.math.ln(target.toDouble())
        }

        return if (result == null || result.toDouble().isNaN()) null else result
    }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Log2>(o)
        fun create() = Log2()
    }
}