
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject

@ConfigurationClass
@TransformerOperator(["ToSigned"])
class ToSigned : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Any?): Any? = target?.toSigned()

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<ToSigned>(o)
        fun create() = ToSigned()
    }

}