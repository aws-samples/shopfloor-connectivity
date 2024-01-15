package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.config.TcpConfiguration
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.tcp.TcpClient
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

// This client supports locking to prevent overlapping read/writes by different consumers
// ADS does not allow multiple TSP connection from same client IP address, so this client is
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