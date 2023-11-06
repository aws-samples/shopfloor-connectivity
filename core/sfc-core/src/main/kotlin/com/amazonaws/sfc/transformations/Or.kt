
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Or", "|"])
class Or(operand: Number?) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: Number?): Number? =

        if (operand == null) null
        else when (target) {
            is Int -> target or operand.toInt()
            is Byte -> (target.toInt() or operand.toInt()).toByte()
            is Short -> (target.toInt() or operand.toInt()).toShort()
            is Long -> target.toLong() or operand.toLong()
            is Double -> (target.toLong() or operand.toLong()).toDouble()
            is Float -> (target.toLong() or operand.toLong()).toFloat()
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
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<Or>(o)
        fun create(operand: Number) = Or(operand)
    }

}