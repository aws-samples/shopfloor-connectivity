// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.apiPlugins

import io.ktor.websocket.*
import java.util.concurrent.atomic.AtomicInteger

class SocketConn (val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(0)
    }
    val name = "client${lastId.getAndIncrement()}"
}