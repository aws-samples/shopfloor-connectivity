
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.util

open class SfcException(message: String) : Exception(message) {
    constructor(e: Exception) : this(e.toString())
}