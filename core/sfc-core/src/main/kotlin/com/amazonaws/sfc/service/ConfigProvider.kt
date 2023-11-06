
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.service

import kotlinx.coroutines.channels.Channel

interface ConfigProvider {
    val configuration: Channel<String>?
}