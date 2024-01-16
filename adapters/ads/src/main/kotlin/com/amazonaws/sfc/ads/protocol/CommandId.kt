/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

enum class CommandId {
    INVALID,
    READ_DEVICE_INFO,
    READ,
    WRITE,
    READ_STATE,
    WRITE_CONTROL,
    ADD_DEVICE_NOTIFICATION,
    DELETE_DEVICE_NOTIFICATION,
    DEVICE_NOTIFICATION,
    READ_WRITE
}

