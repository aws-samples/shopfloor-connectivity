
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


@file:Suppress("unused")

package com.amazonaws.sfc.opcua.config

import com.google.gson.annotations.SerializedName

enum class OpcuaSourceReadingMode {
    @SerializedName("Polling")
    POLLING,

    @SerializedName("Subscription")
    SUBSCRIPTION
}

