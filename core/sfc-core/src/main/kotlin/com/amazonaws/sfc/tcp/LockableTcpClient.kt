package com.amazonaws.sfc.tcp

import com.amazonaws.sfc.config.TcpConfiguration
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.LookupCacheHandler
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

// Cache for sharing lockable TCP clients for protocols sharing  a single TCP client connection
// from the same client IP address. If multiple sources read from same device these will share the same
// TCP client connection. The client can be locked to prevent overlapping read/writes for different sources.

typealias TcpClientCache = LookupCacheHandler<String, LockableTcpClient?, TcpConfiguration>
// This client supports locking to prevent overlapping read/writes by different consumers
// for protocols that do not allow multiple TCP connection from same client IP address, so this client is
// used to share a connection to a device between multiple sources which might read in parallel,
class LockableTcpClient(private val configuration: TcpConfiguration, private val logger: Logger) : TcpClient(config =configuration, logger =  logger) {

    private val className = this.javaClass.simpleName

    private val clientMutex = Mutex()

    suspend fun acquire(timeout: Duration): Boolean {

        val log = logger.getCtxLoggers(className, "acquire")

        log.trace("Acquiring lock for client connected to ${configuration.address}")
        return try {
            withTimeout(timeout) {
                clientMutex.lock()
                log.trace("Acquired lock for client connected to ${configuration.address}")
                true
            }
        } catch (_: TimeoutCancellationException) {
            log.error("Timeout while waiting for acquiring client connected to ${configuration.address}")
            false
        }
    }

    fun release() {
        val log = logger.getCtxLoggers(className, "release")
        try {
            clientMutex.unlock()
            log.trace("Released lock for client connected to ${configuration.address}")
        } catch (e: IllegalStateException) {
            // ignore
        }
    }
}