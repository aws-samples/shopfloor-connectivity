
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.zenohtarget

import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.zenohtarget.config.ZenohWriterConfiguration.Companion.ZENOH_TARGET
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking

/**
 *  IPC service for Zenoh target writer
 */
class ZenohTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service? {
        return createIpcTargetServer(args, configuration, ZENOH_TARGET, logger) { _configReader, _targetID, _logger, _resultHandler ->
            ZenohTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            ZenohTargetService().run(args)
        }
    }
}




