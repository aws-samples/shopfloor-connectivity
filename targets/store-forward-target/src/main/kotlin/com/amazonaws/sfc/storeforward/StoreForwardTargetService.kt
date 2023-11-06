
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.storeforward


import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import com.amazonaws.sfc.storeforward.config.StoreForwardWriterConfiguration.Companion.STORE_FORWARD
import kotlinx.coroutines.runBlocking

/**
 * InMemoryMessageStore and Forward IPC server
 */
class AwsStoreForwardTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return createIpcTargetServer(args, configuration, STORE_FORWARD, logger) { _configReader, _targetID, _logger, _resultHandler ->
            StoreForwardTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            AwsStoreForwardTargetService().run(args)
        }
    }
}

