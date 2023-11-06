
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Asin"])
class Asin : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number?): Number? {

        val result = when (target) {
            null -> null
            is Float -> kotlin.math.asin(target)
            else -> kotlin.math.asin(target.toDouble())
        }

        return if (result == null || result.toDouble().isNaN()) null else result
    }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Asin>(o)
        fun create() = Asin()
    }
}