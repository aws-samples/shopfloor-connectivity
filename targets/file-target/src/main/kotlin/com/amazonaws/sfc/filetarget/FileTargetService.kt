
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filetarget


import com.amazonaws.sfc.filetarget.config.FileTargetWriterConfiguration.Companion.FILE_TARGET
import com.amazonaws.sfc.ipc.IpcTargetServer.Companion.createIpcTargetServer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.service.ServiceMain
import kotlinx.coroutines.runBlocking

/**
 * File writer target IPC service
 */
class FileTargetService : ServiceMain() {

    override fun createServiceInstance(args: Array<String>, configuration: String, logger: Logger): Service? {
        return createIpcTargetServer(args, configuration, FILE_TARGET, logger) { _configReader, _targetID, _logger, _ ->
            FileTargetWriter.newInstance(_configReader, _targetID, _logger, null)
        }
    }

    companion object {
        @JvmStatic
        @JvmName("main")
        fun main(args: Array<String>): Unit = runBlocking {
            FileTargetService().run(args)
        }
    }

}

