
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Min"])
class Min(operand: Number?) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: Number?): Number? =

        if (operand == null) null
        else when (target) {
            is Int -> minOf(target, operand.toInt())
            is Float -> minOf(target, operand.toFloat())
            is Double -> minOf(target, operand.toDouble())
            is Byte -> minOf(target, operand.toByte())
            is Short -> minOf(target, operand.toShort())
            is Long -> minOf(target, operand.toLong())
            else -> null
        }

    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a numeric value",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this
        )
    }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<Min>(o)
        fun create(operand: Number) = Min(operand)
    }

}