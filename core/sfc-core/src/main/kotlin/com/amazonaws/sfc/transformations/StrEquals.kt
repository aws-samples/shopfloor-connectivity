
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["StrEquals"])
class StrEquals(operand: String) : TransformationImpl<String>(operand) {

    @TransformerMethod
    fun apply(target: String?): Boolean = (target.toString() == operand.toString())

    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a string value",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this)
    }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorWithOperand.fromJson<StrEquals, String>(o) { op ->
            try {
                op.asString
            } catch (e: IllegalStateException) {
                throw TransformationException("Value is an array, but single string expected, $e", "StrEquals")
            } catch (e: Exception) {
                throw TransformationException("Invalid operand value, string expected, $e", "StrEquals")
            }
        }


        fun create(operand: String) = StrEquals(operand)

    }

}