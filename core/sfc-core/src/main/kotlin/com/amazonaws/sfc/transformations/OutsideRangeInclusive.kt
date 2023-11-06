
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["OutsideRangeInclusive"])
class OutsideRangeInclusive(operand: Range) : RangeTransformation(operand), Validate {


    @TransformerMethod
    override fun apply(target: Number?): Boolean? =

        if (operand == null || target == null) null
        else (target.toDouble() <= operand.minValue.toDouble() || target.toDouble() >= operand.maxValue.toDouble())

    override fun validate() {
        ConfigurationException.check(
            (operand != null),
            "Operand for ${this::class.simpleName} operator must be set to a range value",
            "${this::class.simpleName}.${TransformationsDeserializer.CONFIG_TRANSFORMATION_OPERATOR}",
            this)

        operand?.validate()
    }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = fromJson<OutsideRangeInclusive>(o)
        fun create(operand: Range) = OutsideRangeInclusive(operand)
    }


}