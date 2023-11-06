
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.storeforward.config

import kotlin.time.Duration

interface MessageBufferConfiguration {
    val retainPeriod: Duration?
    val retainNumber: Int?
    val retainSize: Long?
    val fifo: Boolean
    val writeTimeout: Duration
    val cleanupInterval: Duration
}