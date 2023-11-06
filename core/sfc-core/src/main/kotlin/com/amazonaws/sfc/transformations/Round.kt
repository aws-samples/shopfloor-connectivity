
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject
import kotlin.math.round

@ConfigurationClass
@TransformerOperator(["Round"])
class Round : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number): Number =
        when (target) {
            is Float -> round(target.toFloat())
            is Double -> round(target.toDouble())
            else -> target
        }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Round>(o)
        fun create() = Round()
    }
}