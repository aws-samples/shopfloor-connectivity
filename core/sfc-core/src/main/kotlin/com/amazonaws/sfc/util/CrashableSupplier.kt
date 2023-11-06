
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.util

/**
 * Like Supplier, but exceptions pass through. It is normally used in situations where
 * the caller is prepared to take corrective action on the exception.
 */
fun interface CrashableSupplier<R, E : Throwable?> {
    fun apply(): R
}