
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["DecodeToString"])
class DecodeToString() : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: List<Byte>?): String? {

        if (target == null) {
            return null
        }
        return target.toByteArray().decodeToString()
    }


    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<DecodeToString>(o)
        fun create() = DecodeToString()
    }

}