
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

/**
 * Data class for returning detailed validation errors
 * @property operator Operator
 * @property order Int
 * @property error String
 */
data class TransformValidationError(val operator: TransformationOperator, val order: Int, val error: String){
    override fun toString(): String {
        return "TransformValidationError(Operator=$operator, Order=$order, Error='$error')"
    }
}
