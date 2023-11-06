
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import kotlin.math.max
import kotlin.math.min

@ConfigurationClass
@TransformerOperator(["SubString"])
class SubString(operand: StrRange?) : TransformationImpl<StrRange>(operand) {


    @TransformerMethod
    fun apply(target: String?): String? {

        if (operand == null || target == null) {
            return null
        }

        val startIndex = min(
            target.length,
            if (operand.start >= 0) operand.start else max(0, (target.length + operand.start))
        )

        val endIndex =
            when {
                operand.end == null -> target.length
                (operand.end!! >= 0) -> min(target.length, operand.end!!)
                else -> max(0, target.length + operand.end!!)
            }



        if (startIndex >= endIndex) {
            return ""
        }

        return target.substring(startIndex, endIndex)

    }

    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a string range value",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this)

        operand?.validate()
    }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorWithOperand.fromJson<SubString, StrRange>(o) { op ->
            try {
                Gson().fromJson(op.toString(), StrRange::class.java)
            } catch (e: JsonSyntaxException) {
                throw TransformationException("Invalid Json \"$op\" for MapStringToNumber operand, $e", "SubString")
            } catch (e: Exception) {
                throw TransformationException(e.toString(), "SubString")
            }
        }

        fun create(operand: StrRange) = SubString(operand)
    }


}