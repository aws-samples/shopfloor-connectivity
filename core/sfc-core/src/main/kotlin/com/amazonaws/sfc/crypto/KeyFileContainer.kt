
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.crypto

import java.io.File

class KeyFileContainer(private val filename: String) : KeyContainer() {
    override val scheme: String = uriFromPossibleFileURIString(filename).scheme
    override val keyBytes: ByteArray
        get() = File(filename).readBytes()

    override fun toString(): String = filename
}