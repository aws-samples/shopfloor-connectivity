
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.natstarget

import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.natstarget.config.NatsWriterConfiguration.Companion.NATS_TARGET
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking

/**
 *  IPC service for NATS target writer
 */
class NatsTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service? {
        return createIpcTargetServer(args, configuration, NATS_TARGET, logger) { _configReader, _targetID, _logger, _resultHandler ->
            NatsTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            NatsTargetService().run(args)
        }
    }
}




