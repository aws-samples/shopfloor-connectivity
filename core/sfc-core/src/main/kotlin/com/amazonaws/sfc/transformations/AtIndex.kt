
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.transformations.TransformationsDeserializer.Companion.CONFIG_TRANSFORMATION_OPERATOR
import com.google.gson.JsonObject
import kotlin.math.absoluteValue

@ConfigurationClass
@TransformerOperator(["AtIndex", "[]"])
class AtIndex(operand: Number) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: List<Any>): Any? {

        val position: Int? = operand?.toInt()
        if ((position == null) || (position < 0 && position.absoluteValue > target.size) || (position >= target.size)) return null
        val index = if (position >= 0) position else target.size + position
        return target.getOrNull(index)
    }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<AtIndex>(o)
        fun create(operand: Number) = AtIndex(operand)
    }

    override fun validate() {
        if (validated) return
        ConfigurationException.check(
            (operand != null),
            "Operand for  ${this::class.simpleName} operator must be set to a numeric value",
            "${this::class.simpleName}.$CONFIG_TRANSFORMATION_OPERATOR",
            this
        )
        validated = true
    }

}