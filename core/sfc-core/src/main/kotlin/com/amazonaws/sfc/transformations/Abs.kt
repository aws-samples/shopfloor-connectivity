
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Abs"])
class Abs : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number?): Number? =
        when (target) {
            is Int -> kotlin.math.abs(target)
            is Float -> kotlin.math.abs(target)
            is Double -> kotlin.math.abs(target)
            is Byte -> kotlin.math.abs(target.toInt()).toByte()
            is Short -> kotlin.math.abs(target.toInt()).toShort()
            is Long -> kotlin.math.abs(target.toLong())
            else -> null
        }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Abs>(o)
        fun create() = Abs()
    }
}