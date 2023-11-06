/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.pccc.protocol


class ConnectPath(backplane: Byte = 0x01, slot: Byte = 0x00) {
    val bytes = byteArrayOf(0x01, 0x00, backplane, slot)
}