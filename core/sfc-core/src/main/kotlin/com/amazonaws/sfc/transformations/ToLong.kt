
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["ToLong"])
class ToLong : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Number?): Long? = target?.toLong()

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<ToLong>(o)
        fun create() = ToLong()
    }
}