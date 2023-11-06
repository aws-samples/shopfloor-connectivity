
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.transformations.TransformationsDeserializer.Companion.CONFIG_TRANSFORMATION_OPERAND
import com.google.gson.Gson
import com.google.gson.JsonObject


abstract class RangeTransformation(operand: Range) : TransformationImpl<Range>(operand), Validate {

    abstract fun apply(target: Number?): Boolean?

    companion object {
        inline fun <reified T : RangeTransformation> fromJson(o: JsonObject): TransformationOperator = TransformationOperatorWithOperand.fromJson<T, Range>(o) { op ->
            try {
                Gson().fromJson(op.toString(), Range::class.java)
            } catch (e: Exception) {
                throw ConfigurationException("Invalid Json \"${op}\" for Range operand, $e", CONFIG_TRANSFORMATION_OPERAND)
            }
        }
    }


    private var _validated = false
    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {
        if (validated) return
        ConfigurationException.check(
            (operand != null),
            "Range operand must be set",
            CONFIG_TRANSFORMATION_OPERAND,
            this
        )

        operand!!.validate()

        validated = true
    }


}