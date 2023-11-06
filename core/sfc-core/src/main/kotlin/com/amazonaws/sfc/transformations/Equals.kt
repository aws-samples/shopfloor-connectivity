
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Equals", "=="])
class Equals(operand: Number?) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: Number?): Boolean =

        if (operand == null) (target == null)
        else when (target) {
            is Int -> target == operand.toInt()
            is Float -> target == operand.toFloat()
            is Double -> target == operand.toDouble()
            is Byte -> target == operand.toByte()
            is Short -> target == operand.toShort()
            is Long -> target == operand.toLong()
            else -> false
        }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<Equals>(o)
        fun create(operand: Number?) = Equals(operand)
    }

}