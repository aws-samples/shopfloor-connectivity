
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Divide", "/"])
class Divide(operand: Number) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: Number?): Number? {

        val result =
            if (operand == 0 || operand == null || target == null) null
            else
                when (target) {
                    is Float -> target.toFloat() / operand.toFloat()
                    else -> (target.toDouble() / operand.toDouble())
                }

        return if (result == null || result.toDouble().isNaN() || result.toDouble().isInfinite()) null else result
    }

    override fun validate() {
        if (validated) return
        ConfigurationException.check(
            (operand != null && operand.toFloat() > 0.0),
            "Operand for ${this::class.simpleName} operator must be set and > 0",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this
        )
        validated = true
    }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<Divide>(o)
        fun create(operand: Number) = Divide(operand)
    }

}