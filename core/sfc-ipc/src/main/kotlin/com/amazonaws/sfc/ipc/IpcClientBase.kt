
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ServerConfiguration
import com.amazonaws.sfc.config.ServiceConfiguration
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.ServerConnectionType
import io.grpc.ManagedChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Base clas for IPC clients, the actual client is created by calling the createClient method parameter which returns a client of generic type T.
// The main purpose of this class is to  have uniform method to build a channel/client from the configuration that is passed as a parameter to
// the createClient method.
sealed class IpcClientBase<T : IpcClient>(protected val configReader: ConfigReader,
                                          protected val serverConfig: ServerConfiguration,
                                          protected val logger: Logger,
                                          private val createClient: (channel: ManagedChannel) -> T) {

    private val className = this::class.simpleName.toString()

    protected val configuration by lazy { configReader.getConfig<ServiceConfiguration>() }

    private var certificateExpiryChecker: CertificateExpiryChecker? = null

    private var _client: T? = null
    private val clientMutex = Mutex()

    suspend fun getIpcClient(): T {
        clientMutex.withLock {
            if (_client != null) {
                return _client as T
            }
            _client = IpcClientBuilder.createIpcClient(serverConfig, logger, createClient)

            certificateExpiryChecker = initializeCertificateExpiryChecker()

            _client!!.lastError = null
            return _client as T

        }
    }

    private fun initializeCertificateExpiryChecker(): CertificateExpiryChecker? =
        if (serverConfig.serverConnectionType == ServerConnectionType.MutualTLS)
            CertificateExpiryChecker(
                certificateFiles = listOf(serverConfig.caCertificate!!, serverConfig.clientCertificate!!),
                expirationWarningPeriodDays = serverConfig.expirationWarningPeriod,
                logger = logger)
        else
            null


    suspend fun resetIpcClient() {
        clientMutex.withLock {
            certificateExpiryChecker?.stop()
            _client?.close()
            _client = null
        }
    }

    /**
     * Closes the reader
     */
    open suspend fun close() {
        certificateExpiryChecker?.stop()
        _client?.close()
    }

}

