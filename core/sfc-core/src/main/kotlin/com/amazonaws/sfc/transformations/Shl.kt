
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Shl", "<<"])
class Shl(operand: Number?) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: Number?): Number? =
        if (operand == null) null else
            when (target) {
                is Int -> target shl operand.toInt()
                is Byte -> ((target.toInt() shl operand.toInt()) and 0xFF).toByte()
                is Short -> ((target.toInt() shl operand.toInt()) and 0xFFFF).toShort()
                is Long -> target shl operand.toInt()
                else -> null
            }

    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a numeric value",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this)
    }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<Shl>(o)
        fun create(operand: Number) = Shl(operand)
    }
}