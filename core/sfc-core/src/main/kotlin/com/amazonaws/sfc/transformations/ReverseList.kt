
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["ReverseList"])
class ReverseList : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: List<Any>?): List<Any>? =
        target?.reversed()

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<ReverseList>(o)
        fun create() = ReverseList()
    }
}