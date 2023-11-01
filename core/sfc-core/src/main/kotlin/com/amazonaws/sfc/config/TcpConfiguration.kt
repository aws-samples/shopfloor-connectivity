package com.amazonaws.sfc.config

import kotlin.time.Duration

interface TcpConfiguration {
    val waitAfterConnectError: Duration
    val waitAfterWriteError: Duration
    val waitAfterReadError: Duration
    val connectTimeout: Duration
    val address: String
    val port: Int
}