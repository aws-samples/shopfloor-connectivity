
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Mod", "%"])
class Mod(operand: Number?) : TransformationImpl<Number>(operand) {
    @TransformerMethod
    fun apply(target: Number?): Number? =

        if (operand == null) null else

            when (target) {
                is Int -> target.rem(operand.toInt())
                is Float -> (target.rem(operand.toFloat()))
                is Double -> target.rem(operand.toDouble())
                is Byte -> (target.rem(operand.toByte())).toByte()
                is Short -> (target.rem(operand.toShort())).toShort()
                is Long -> target.rem(operand.toLong())
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
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<Mod>(o)
        fun create(operand: Number) = Mod(operand)
    }


}