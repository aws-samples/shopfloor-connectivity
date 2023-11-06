
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["WithinRangeExclusive"])
class WithinRangeExclusive(operand: Range) : RangeTransformation(operand) {


    @TransformerMethod
    override fun apply(target: Number?): Boolean? =

        if (operand == null || target == null) null
        else (target.toDouble() > operand.minValue.toDouble() && target.toDouble() < operand.maxValue.toDouble())


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = fromJson<WithinRangeExclusive>(o)
        fun create(operand: Range) = WithinRangeExclusive(operand)
    }


}