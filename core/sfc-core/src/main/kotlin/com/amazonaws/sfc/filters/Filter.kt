
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filters

/**
 * Abstracts filter types
 */
interface Filter {
    fun apply(value: Any): Boolean
}

