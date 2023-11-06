
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Fahrenheit"])
class Fahrenheit : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number?): Number? =

        if (target == null || !isNumeric(target::class)) null else
            when (target) {
                is Float -> ((target.toFloat() * 1.8f) + 32f)
                else -> ((target.toDouble() * 1.8) + 32.0)
            }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Fahrenheit>(o)
        fun create() = Fahrenheit()
    }
}