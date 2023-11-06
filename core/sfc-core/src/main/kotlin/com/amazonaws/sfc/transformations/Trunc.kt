
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject
import kotlin.math.truncate

@ConfigurationClass
@TransformerOperator(["Trunc"])
class Trunc : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number?): Number? =

        when (target) {
            is Float -> truncate(target)
            is Double -> truncate(target)
            else -> target
        }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Trunc>(o)
        fun create() = Trunc()
    }

}