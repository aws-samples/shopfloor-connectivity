// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["Flatten"])
class Flatten : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Any?): List<Any?> =
        flatten(target)

    private fun flatten(target: Any?): List<Any?> =
        if (target is List<*>)
            if (target.isEmpty()) emptyList() else
                if (target.first() is List<*>)
                    target.flatMap { flatten(it ?: emptyList<Any>()) }
                else
                    target
        else listOf(target)

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<Flatten>(o)
        fun create() = Flatten()
    }
}