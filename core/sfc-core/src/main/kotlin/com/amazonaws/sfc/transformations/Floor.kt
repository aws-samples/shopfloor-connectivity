
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject
import kotlin.math.floor

@ConfigurationClass
@TransformerOperator(["Floor"])
class Floor : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number): Number =
        when (target) {
            is Float -> floor(target.toFloat())
            is Double -> floor(target.toDouble())
            else -> target
        }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Floor>(o)
        fun create() = Floor()
    }
}