
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc

// Interface for IPC client
interface IpcClient {

    fun close()

    var lastError: Exception?

    val ok: Boolean
        get() = (lastError == null)
    val error: Boolean
        get() = !ok
}

