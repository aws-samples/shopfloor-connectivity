
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["ToShort"])
class ToShort : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number?): Short? = target?.toShort()

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<ToShort>(o)
        fun create() = ToShort()
    }
}