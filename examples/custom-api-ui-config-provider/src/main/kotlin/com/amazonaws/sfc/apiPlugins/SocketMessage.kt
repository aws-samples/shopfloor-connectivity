// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.apiPlugins

interface SocketMessage {
        suspend fun sendMessage(message: String)
        suspend fun receiveMessage(message: (String) -> Unit)
}