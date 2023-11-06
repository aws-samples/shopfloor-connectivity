
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Celsius"])
class Celsius : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number?): Number? =
        if (target == null || !isNumeric(target::class)) null else
            when (target) {
                is Float -> ((target - 32f) * 5f / 9f)
                else -> ((target.toDouble() - 32.0) * 5.0 / 9.0)
            }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Celsius>(o)
        fun create() = Celsius()
    }
}