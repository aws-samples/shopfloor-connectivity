
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.JsonObject

class TransformationOperatorNumericOperand : TransformationOperatorWithOperand() {

    companion object {
        inline fun <reified R : TransformationOperator> fromJson(o: JsonObject): TransformationOperator = fromJson<R, Number>(o) { op ->
            // use string to allow parsing of hex etc.
            val s = op.asString.toString()
            if (s.isBlank()) throw TransformationException("Numeric operator can not be empty", "TransformationOperatorNumericOperand")
            try {
                Integer.decode(s)
            } catch (e: NumberFormatException) {
                throw TransformationException("\"$s\" is not a valid number", "TransformationOperatorNumericOperand")
            }
        }
    }
}