
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["ParseInt"])
class ParseInt : TransformationImpl<Number>() {

    @TransformerMethod
    fun apply(target: String?): Int? {
        return try {
            Integer.decode(target)
        } catch (_: java.lang.Exception) {
            null
        }
    }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<ParseInt>(o)
        fun create() = ParseInt()
    }

}