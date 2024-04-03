
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject
import java.time.Instant

@ConfigurationClass
@TransformerOperator(["EpocSecondsToTimestamp"])
class EpocSecondsToTimestamp : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: Long?): Instant? =

        if (target == null) null else
            try {
                Instant.ofEpochSecond(target)
            } catch (_: IllegalArgumentException) {
                null
            }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<EpocSecondsToTimestamp>(o)
        fun create() = EpocSecondsToTimestamp()
    }
}