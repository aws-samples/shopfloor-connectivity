
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Not", "!"])
class Not : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Boolean?): Boolean? =
        when (target) {
            is Boolean -> !target
            else -> null
        }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Not>(o)
        fun create() = Not()
    }
}