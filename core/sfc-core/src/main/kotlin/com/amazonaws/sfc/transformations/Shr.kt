
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Shr", ">>"])
class Shr(operand: Number?) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: Number?): Number? =
        if (operand == null) null else
            when (target) {
                is Int -> target shr operand.toInt()
                is Byte -> ((target.toInt() shr operand.toInt()) and 0xFF).toByte()
                is Short -> ((target.toInt() shr operand.toInt()) and 0xFFFF).toShort()
                is Long -> target shr operand.toInt()
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
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<Shr>(o)
        fun create(operand: Number) = Shr(operand)
    }
}