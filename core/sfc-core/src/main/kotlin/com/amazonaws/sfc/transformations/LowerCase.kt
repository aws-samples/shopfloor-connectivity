
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject
import software.amazon.awssdk.utils.StringUtils.lowerCase

@ConfigurationClass
@TransformerOperator(["LowerCase"])
class LowerCase : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: String?): String? = if (target == null) null else lowerCase(target)

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<LowerCase>(o)

        fun create() = LowerCase()
    }
}