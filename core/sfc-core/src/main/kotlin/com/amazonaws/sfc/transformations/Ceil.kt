
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject
import kotlin.math.ceil

@ConfigurationClass
@TransformerOperator(["Ceil"])
class Ceil : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number): Number =
        when (target) {
            is Float -> ceil(target.toFloat())
            is Double -> ceil(target.toDouble())
            else -> target
        }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Ceil>(o)
        fun create() = Ceil()
    }
}