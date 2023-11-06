
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

/**
 * Data class for returning detailed validation errors
 * @property Operator Operator
 * @property Order Int
 * @property Error String
 */
data class TransformValidationError(val Operator: TransformationOperator, val Order: Int, val Error: String)