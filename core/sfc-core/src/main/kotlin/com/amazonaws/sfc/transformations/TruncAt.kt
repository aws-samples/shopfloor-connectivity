
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.JsonObject
import kotlin.math.pow
import kotlin.math.truncate


@ConfigurationClass
@TransformerOperator(["TruncAt"])
class TruncAt(operand: Number?) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: Number?): Number? {

        if (operand == null) return null

        val p = operand.toInt()
        if ((target is Float) || (target is Double)) {
            val m = (10.0).pow(p)
            when (target) {
                is Float -> return (truncate(target * m).toFloat() / m).toFloat()
                is Double -> return (truncate(target * m) / m)
            }
        } else {
            if (p < 0) {
                if (10.0.pow(p * -1) > target!!.toDouble()) {
                    return 0
                }

                val m = 10.0.pow(p)
                val result = (truncate(target.toDouble() * m)) / m
                when (target) {
                    is Int -> return result.toInt()
                    is Byte -> return result.toInt().toByte()
                    is Short -> return result.toInt().toShort()
                    is Long -> return result.toLong()
                }
            }
        }
        return target
    }

    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a numeric value",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this)
    }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<TruncAt>(o)
        fun create(operand: Int) = TruncAt(operand)
    }

}