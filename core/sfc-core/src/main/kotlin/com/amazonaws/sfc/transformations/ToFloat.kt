
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["ToFloat"])
class ToFloat : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number?): Float? = target?.toFloat()

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<ToFloat>(o)
        fun create() = ToFloat()
    }
}