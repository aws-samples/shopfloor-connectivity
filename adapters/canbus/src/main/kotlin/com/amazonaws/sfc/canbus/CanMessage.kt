// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//
// Partial port to Kotlin port of https://github.com/jazdw/jnaCan
//
package com.amazonaws.sfc.canbus

import com.sun.jna.Structure

interface CanMessage<T : Structure> {
    fun toJna(): T
}