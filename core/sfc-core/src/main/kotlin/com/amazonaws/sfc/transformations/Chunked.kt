
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Chunked"])
class Chunked(operand: Number) : TransformationImpl<Number>(operand) {

    @TransformerMethod
    fun apply(target: List<Any>): List<List<Any>> =

        if (operand == null) listOf(target)
        else target.chunked(operand.toInt())


    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a numeric value",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this)

        ConfigurationException.check(
            ((operand as Int) > 0),
            "Operand for ${this::class.simpleName} operator must be set to a value > 0",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this)
    }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNumericOperand.fromJson<Chunked>(o)
        fun create(operand: Int) = Chunked(operand)
    }

}