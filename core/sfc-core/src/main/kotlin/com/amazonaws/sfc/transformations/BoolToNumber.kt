
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["BoolToNumber"])
class BoolToNumber : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Boolean): Number = if (target) 1 else 0


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<BoolToNumber>(o)
        fun create() = BoolToNumber()
    }
}