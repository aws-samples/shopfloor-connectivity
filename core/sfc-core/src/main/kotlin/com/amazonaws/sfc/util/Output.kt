
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.util

import org.apache.commons.io.FileUtils

fun byteCountToString(i: Int): String = FileUtils.byteCountToDisplaySize(i.toLong())

val Int.byteCountString: String
    get() = byteCountToString(this)

fun byteCountToString(l: Long): String = FileUtils.byteCountToDisplaySize(l)
val Long.byteCountString: String
    get() = byteCountToString(this)

fun byteCountToString(n: Number): String = FileUtils.byteCountToDisplaySize(n)
val Number.byteCountString: String
    get() = byteCountToString(this)

