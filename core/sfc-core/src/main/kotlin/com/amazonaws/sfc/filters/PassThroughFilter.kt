
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filters

/**
 * Pass through filter
 */
object PassThroughFilter : Filter {
    override fun apply(value: Any): Boolean = true
}