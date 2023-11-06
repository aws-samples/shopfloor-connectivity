
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Plus", "Add", "+"])
class Plus(operand: Number?) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: Number?): Number? {

        if (operand == null) return target

        val result = when (target) {
            is Int -> target + operand.toInt()
            is Float -> (target + operand.toFloat())
            is Double -> target + operand.toDouble()
            is Byte -> (target + operand.toByte()).toByte()
            is Short -> (target + operand.toShort()).toShort()
            is Long -> (target + operand.toLong())
            else -> null
        }
        return if (result == null || result.toDouble().isNaN() || result.toDouble().isInfinite()) null else result
    }

    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a numeric value",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this)
    }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<Plus>(o)
        fun create(operand: Number) = Plus(operand)
    }

}