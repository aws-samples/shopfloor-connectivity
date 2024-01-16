/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

import kotlin.time.Duration

data class  ClientConfig(
    val targetAmsNetId: String,
    val targetAmsPort: Int,
    val sourceAmsNetId: String,
    val sourceAmsPort: Int,
    val commandTimeout: Duration,
    val readTimeout : Duration
)