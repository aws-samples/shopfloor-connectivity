
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject
import software.amazon.awssdk.utils.StringUtils.upperCase

@ConfigurationClass
@TransformerOperator(["UpperCase"])
class UpperCase : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: String?): String? = if (target == null) null else upperCase(target)

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<UpperCase>(o)

        fun create() = UpperCase()
    }
}