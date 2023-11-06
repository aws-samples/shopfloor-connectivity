
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["ParseNumber"])
class ParseNumber : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: String?): Number? =
        target?.toDoubleOrNull()


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<ParseNumber>(o)
        fun create() = ParseNumber()
    }

}