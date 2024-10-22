
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcuatarget

import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.opcuatarget.config.OpcuaWriterConfiguration.Companion.OPCUA_TARGET
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking

/**
 *  IPC service for OPCUA target
 */
class OpcuaTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service {
        return createIpcTargetServer(args, configuration, OPCUA_TARGET, logger) { _configReader, _targetID, _logger, _resultHandler ->
            OpcuaTargetWriter.newInstance(_configReader, _targetID, _logger, _resultHandler)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            OpcuaTargetService().run(args)
        }
    }
}




